#!/usr/bin/env python3
"""Validate saved coverage against current input files and independent reassembly."""
import argparse
import json
import pathlib
import sys
from assemble import validate_saved


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('manifest', nargs='?', default='verification/harness/target/evidence/runtime-manifest.json')
    parser.add_argument('--root', type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[2])
    parser.add_argument('--index', default='verification/coverage/runtime-evidence-index.json')
    args = parser.parse_args()
    value = json.loads((args.root / args.manifest).read_text())
    validate_saved(args.root, value, args.index)
    print(json.dumps({'validation': 'VALID', 'runtimeStatus': value['runtimeStatus'], 'gateComplete': value['gateComplete']}))
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError) as error:
        print('INVALID_OR_INCOMPLETE_EVIDENCE: ' + str(error), file=sys.stderr)
        sys.exit(1)
