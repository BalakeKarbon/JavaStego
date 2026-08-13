#!/usr/bin/env python3
"""Round-trip test for Stego.java: for each bitsPerByte (1, 2, 4, 8), build a random
payload just large enough to force that depth, encode it into CONTAINER, decode the
result back out, and verify the recovered bytes match the original exactly.
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BIT_DEPTHS = (1, 2, 4, 8)


def run_stego(input_text):
    result = subprocess.run(
        ["java", "-cp", SCRIPT_DIR, "Stego"],
        input=input_text,
        capture_output=True,
        text=True,
    )
    return result.stdout


def main():
    container = sys.argv[1] if len(sys.argv) > 1 else "icon.png"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "test_output"

    if not os.path.isfile(os.path.join(SCRIPT_DIR, "Stego.class")):
        print(f"Stego.class not found in {SCRIPT_DIR} - compile it first with: javac Stego.java", file=sys.stderr)
        return 1

    if not os.path.isfile(container):
        print(f"Container image not found: {container}", file=sys.stderr)
        return 1

    os.makedirs(out_dir, exist_ok=True)
    work_dir = tempfile.mkdtemp()

    try:
        # Name the requested output after the container's own extension (e.g. a .jpg
        # container gets icon_1bit.jpg requested). Stego.java always writes actual PNG
        # bytes underneath - it force-appends ".png" itself when the requested name
        # doesn't already end in .png - so the true saved path is parsed back out of its
        # "Encoded PNG saved at ..." message below, then renamed back down to just the
        # container's extension.
        container_base, container_ext = os.path.splitext(os.path.basename(container))

        # Ask the program for usable capacity (bytes, after terminator overhead) at each bitsPerByte.
        capacity_output = run_stego(f"c\n{container}\n")
        usable = {}
        for bits in BIT_DEPTHS:
            match = re.search(rf"^{bits} bits per byte: \d+ bytes total, (\d+) bytes usable$", capacity_output, re.MULTILINE)
            usable[bits] = int(match.group(1))
        print("Usable capacity: " + "  ".join(f"{bits}={usable[bits]}B" for bits in BIT_DEPTHS))

        all_passed = True
        prev_usable = 0
        for bits in BIT_DEPTHS:
            cap = usable[bits]
            if bits == 8:
                # Max out the image's overall capacity at the top tier, to exercise the
                # boundary where every last usable byte (including the terminator) is used.
                size = cap
            elif prev_usable == 0:
                # Smallest payload that forces this depth: just over the previous tier's
                # capacity (or a small fixed size for the first tier).
                size = min(cap, 1000)
            else:
                size = prev_usable + 1

            if size > cap:
                print(f"{bits} bits per byte: SKIP (image too small to reach this depth)")
                prev_usable = cap
                continue

            secret = os.path.join(work_dir, f"secret_{bits}bit.bin")
            decoded = os.path.join(work_dir, f"decoded_{bits}bit.bin")
            requested_encoded = os.path.join(out_dir, f"{container_base}_{bits}bit{container_ext}")

            with open(secret, "wb") as f:
                f.write(os.urandom(size))

            encode_log = run_stego(f"e\n{container}\nf\n{secret}\n{requested_encoded}\n")
            used_bits_match = re.search(r"Using (\d+) bits per byte", encode_log)
            used_bits = used_bits_match.group(1) if used_bits_match else None
            encoded_match = re.search(r'Encoded PNG saved at "([^"]+)"', encode_log)
            encoded = encoded_match.group(1) if encoded_match else None

            # Rename back down to just the container's extension so the saved file
            # carries only that one extension.
            if encoded and encoded != requested_encoded:
                shutil.move(encoded, requested_encoded)
                encoded = requested_encoded

            run_stego(f"d\n{encoded}\n{bits}\nf\n{decoded}\n")

            data_matches = os.path.isfile(decoded) and open(secret, "rb").read() == open(decoded, "rb").read()

            if used_bits == str(bits) and data_matches:
                print(f"{bits} bits per byte (payload {size}B, saved as {os.path.basename(encoded)}): PASS")
            else:
                print(f"{bits} bits per byte (payload {size}B): FAIL "
                      f"(encoder picked {used_bits or '?'} bits; data match: {'yes' if data_matches else 'no'})")
                all_passed = False

            prev_usable = cap

        if all_passed:
            print("All round-trip tests passed.")
            return 0
        else:
            print("Some round-trip tests failed.")
            return 1
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
