#!/usr/bin/env bash
# Round-trip test for Stego.java: for each bitsPerByte (1, 2, 4, 8), build a random
# payload just large enough to force that depth, encode it into CONTAINER, decode the
# result back out, and verify the recovered bytes match the original exactly.
set -euo pipefail

CONTAINER="${1:-icon.png}"
OUT_DIR="${2:-test_output}"

if [[ ! -f "$CONTAINER" ]]; then
    echo "Container image not found: $CONTAINER" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

javac Stego.java -d "$WORK_DIR"

# Ask the program for usable capacity (bytes, after terminator overhead) at each bitsPerByte.
capacity_output="$(printf 'c\n%s\n' "$CONTAINER" | java -cp "$WORK_DIR" Stego)"
declare -A usable
for bits in 1 2 4 8; do
    usable[$bits]="$(awk -v b="$bits" '$0 ~ "^"b" bits per byte:" {print $8}' <<< "$capacity_output")"
done
echo "Usable capacity: 1=${usable[1]}B  2=${usable[2]}B  4=${usable[4]}B  8=${usable[8]}B"

all_passed=true
prev_usable=0
for bits in 1 2 4 8; do
    cap="${usable[$bits]}"
    # Smallest payload that forces this depth: just over the previous tier's capacity
    # (or a small fixed size for the first tier).
    if (( prev_usable == 0 )); then
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
    encoded="$OUT_DIR/icon_${bits}bit.png"

    head -c "$size" /dev/urandom > "$secret"

    encode_log="$(printf 'e\n%s\nf\n%s\n%s\n' "$CONTAINER" "$secret" "$encoded" | java -cp "$WORK_DIR" Stego)"
    used_bits="$(grep -oP 'Using \K[0-9]+(?= bits per byte)' <<< "$encode_log")"

    printf 'd\n%s\n%s\nf\n%s\n' "$encoded" "$bits" "$decoded" | java -cp "$WORK_DIR" Stego > /dev/null

    if [[ "$used_bits" == "$bits" ]] && cmp -s "$secret" "$decoded"; then
        echo "$bits bits per byte (payload ${size}B): PASS"
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
