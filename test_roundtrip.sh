#!/usr/bin/env bash
# Round-trip test for Stego.java: for each bitsPerByte (1, 2, 4, 8), build a random
# payload just large enough to force that depth, encode it into CONTAINER, decode the
# result back out, and verify the recovered bytes match the original exactly.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER="${1:-icon.png}"
OUT_DIR="${2:-test_output}"

if [[ ! -f "$SCRIPT_DIR/Stego.class" ]]; then
    echo "Stego.class not found in $SCRIPT_DIR - compile it first with: javac Stego.java" >&2
    exit 1
fi

if [[ ! -f "$CONTAINER" ]]; then
    echo "Container image not found: $CONTAINER" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

# Name the requested output after the container's own extension (e.g. a .jpg container
# gets icon_1bit.jpg requested). Stego.java always writes actual PNG bytes underneath -
# it force-appends ".png" itself when the requested name doesn't already end in .png - so
# the true saved path is parsed back out of its "Encoded PNG saved at ..." message below.
container_base="$(basename "$CONTAINER")"
if [[ "$container_base" == *.* ]]; then
    container_ext=".${container_base##*.}"
    container_base="${container_base%.*}"
else
    container_ext=""
fi

# Ask the program for usable capacity (bytes, after terminator overhead) at each bitsPerByte.
capacity_output="$(printf 'c\n%s\n' "$CONTAINER" | java -cp "$SCRIPT_DIR" Stego)"
declare -A usable
for bits in 1 2 4 8; do
    usable[$bits]="$(awk -v b="$bits" '$0 ~ "^"b" bits per byte:" {print $8}' <<< "$capacity_output")"
done
echo "Usable capacity: 1=${usable[1]}B  2=${usable[2]}B  4=${usable[4]}B  8=${usable[8]}B"

all_passed=true
prev_usable=0
for bits in 1 2 4 8; do
    cap="${usable[$bits]}"
    if (( bits == 8 )); then
        # Max out the image's overall capacity at the top tier, to exercise the
        # boundary where every last usable byte (including the terminator) is used.
        size="$cap"
    elif (( prev_usable == 0 )); then
        # Smallest payload that forces this depth: just over the previous tier's capacity
        # (or a small fixed size for the first tier).
        size=$(( cap < 1000 ? cap : 1000 ))
    else
        size=$(( prev_usable + 1 ))
    fi

    if (( size > cap )); then
        echo "$bits bits per byte: SKIP (image too small to reach this depth)"
        prev_usable="$cap"
        continue
    fi

    secret="$WORK_DIR/secret_${bits}bit.bin"
    decoded="$WORK_DIR/decoded_${bits}bit.bin"
    requested_encoded="$OUT_DIR/${container_base}_${bits}bit${container_ext}"

    head -c "$size" /dev/urandom > "$secret"

    encode_log="$(printf 'e\n%s\nf\n%s\n%s\n' "$CONTAINER" "$secret" "$requested_encoded" | java -cp "$SCRIPT_DIR" Stego)"
    used_bits="$(grep -oP 'Using \K[0-9]+(?= bits per byte)' <<< "$encode_log")"
    encoded="$(grep -oP 'Encoded PNG saved at "\K[^"]+' <<< "$encode_log")"

    # Stego.java force-appends ".png" onto anything not already ending in it (it always
    # writes real PNG bytes underneath, regardless of extension). Rename back down to just
    # the container's extension so the saved file carries only that one extension.
    if [[ "$encoded" != "$requested_encoded" ]]; then
        mv "$encoded" "$requested_encoded"
        encoded="$requested_encoded"
    fi

    printf 'd\n%s\n%s\nf\n%s\n' "$encoded" "$bits" "$decoded" | java -cp "$SCRIPT_DIR" Stego > /dev/null

    if [[ "$used_bits" == "$bits" ]] && cmp -s "$secret" "$decoded"; then
        echo "$bits bits per byte (payload ${size}B, saved as $(basename "$encoded")): PASS"
    else
        echo "$bits bits per byte (payload ${size}B): FAIL (encoder picked ${used_bits:-?} bits; data match: $(cmp -s "$secret" "$decoded" && echo yes || echo no))"
        all_passed=false
    fi

    prev_usable="$cap"
done

if $all_passed; then
    echo "All round-trip tests passed."
else
    echo "Some round-trip tests failed."
    exit 1
fi
