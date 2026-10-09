#!/usr/bin/env python3
"""Independent read-only extractor for host process controls (verifyCoverage, inspectArtifacts).

Usage: host_extract.py --root REPO --host HOST.json --out ROWS.json [--report REPORT.json] [--artifact PATH ...]

Reads only files: the host command's generated report (verifyCoverage) or the requested artifacts (inspectArtifacts),
and the host descriptor the adapter wrote before this command started (operation, command, environment, scope,
operationEvidence, evidenceClass, driverProvenance). It writes the structured rawRows the harness compares with the host
observation (HostObservationValidator.extractedRows): the descriptor fields as read, plus the report's rows or, for an
inspection, each artifact's path/sha256/sizeBytes as read here. It never runs the host command or edits an input.
"""
import argparse, hashlib, json, pathlib


def inside(root, ref):
    path = (root / ref).resolve()
    if not path.is_file() or not path.is_relative_to(root):
        raise SystemExit(f'not a repository file: {ref}')
    return path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=pathlib.Path, required=True)
    parser.add_argument('--host', required=True)
    parser.add_argument('--out', required=True)
    parser.add_argument('--report')
    parser.add_argument('--artifact', action='append', default=[])
    args = parser.parse_args()
    root = args.root.resolve()
    host = json.loads(inside(root, args.host).read_text())
    rows = {}
    if args.report:
        report = json.loads(inside(root, args.report).read_text())
        if report.get('evidenceClass') != host.get('evidenceClass') or report.get('operation') != host.get('operation'):
            raise SystemExit('report class/operation differs from the host descriptor')
        rows.update(report['rows'])
    if args.artifact:
        rows['inspected'] = []
        for ref in args.artifact:
            data = inside(root, ref).read_bytes()
            rows['inspected'].append({'path': ref, 'sha256': hashlib.sha256(data).hexdigest(), 'sizeBytes': len(data)})
    for key in ('operation', 'command', 'environment', 'scope', 'operationEvidence', 'evidenceClass', 'driverProvenance'):
        rows[key] = host[key]
    out = root / args.out
    out.write_text(json.dumps(rows, ensure_ascii=False, sort_keys=True) + '\n')
    print(json.dumps({'rows': len(rows), 'out': args.out}))


if __name__ == '__main__':
    main()
