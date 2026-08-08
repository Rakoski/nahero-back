#!/usr/bin/env python3
"""Parse a JaCoCo XML report into overall coverage + a list of uncovered spots.

Run from anywhere; pass the report path (absolute or relative to CWD).

Usage:
    python3 gaps.py ../target/site/jacoco/jacoco.xml [--metric INSTRUCTION] [--top 10]

Prints JSON to stdout:
{
  "metrics": {"INSTRUCTION": 87.4, "BRANCH": 71.0, "LINE": 90.1, ...},
  "primary": 87.4,
  "gaps": [
    {"class": "com.example.OrderService", "file": "OrderService.java",
     "missed_lines": [42, 43, 44], "partial_branch_lines": [51],
     "missed_methods": ["cancel(long)"], "missed_instructions": 37}
  ]
}
"""
import argparse
import json
import sys
import xml.etree.ElementTree as ET


def pct(missed: int, covered: int) -> float:
    total = missed + covered
    return 100.0 if total == 0 else round(100.0 * covered / total, 2)


def counters(node) -> dict:
    return {
        c.get("type"): (int(c.get("missed", 0)), int(c.get("covered", 0)))
        for c in node.findall("counter")
    }


def collapse(nums):
    """[1,2,3,7,8] -> ['1-3', '7-8'] so prompts stay short."""
    out, run = [], []
    for n in sorted(nums):
        if run and n == run[-1] + 1:
            run.append(n)
        else:
            if run:
                out.append(str(run[0]) if len(run) == 1 else f"{run[0]}-{run[-1]}")
            run = [n]
    if run:
        out.append(str(run[0]) if len(run) == 1 else f"{run[0]}-{run[-1]}")
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("report")
    ap.add_argument("--metric", default="INSTRUCTION",
                    choices=["INSTRUCTION", "BRANCH", "LINE", "METHOD", "CLASS"])
    ap.add_argument("--top", type=int, default=10,
                    help="how many worst-offending classes to report")
    args = ap.parse_args()

    try:
        root = ET.parse(args.report).getroot()
    except (OSError, ET.ParseError) as exc:
        print(json.dumps({"error": f"cannot read {args.report}: {exc}"}))
        return 2

    metrics = {k: pct(*v) for k, v in counters(root).items()}
    if args.metric not in metrics:
        print(json.dumps({"error": f"no {args.metric} counter in report"}))
        return 2

    gaps = []
    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "").replace("/", ".")

        # line-level detail lives on <sourcefile>, keyed by file name
        by_file = {}
        for sf in pkg.findall("sourcefile"):
            missed, partial = [], []
            for line in sf.findall("line"):
                nr = int(line.get("nr"))
                if int(line.get("mi", 0)) > 0:
                    missed.append(nr)
                elif int(line.get("mb", 0)) > 0:  # line runs, but a branch never does
                    partial.append(nr)
            by_file[sf.get("name")] = (missed, partial)

        for cls in pkg.findall("class"):
            cnt = counters(cls)
            missed_instr = cnt.get("INSTRUCTION", (0, 0))[0]
            missed_branch = cnt.get("BRANCH", (0, 0))[0]
            if missed_instr == 0 and missed_branch == 0:
                continue

            src = cls.get("sourcefilename", "")
            missed, partial = by_file.get(src, ([], []))
            uncovered_methods = [
                f'{m.get("name")}{m.get("desc")}'
                for m in cls.findall("method")
                if counters(m).get("INSTRUCTION", (0, 0))[0] > 0
            ]
            gaps.append({
                "class": cls.get("name", "").replace("/", "."),
                "file": f"{pkg_name.replace('.', '/')}/{src}",
                "missed_instructions": missed_instr,
                "missed_branches": missed_branch,
                "missed_lines": collapse(missed),
                "partial_branch_lines": collapse(partial),
                "uncovered_methods": uncovered_methods[:15],
            })

    gaps.sort(key=lambda g: (g["missed_instructions"], g["missed_branches"]), reverse=True)
    json.dump({"metrics": metrics,
               "primary": metrics[args.metric],
               "primary_metric": args.metric,
               "gaps": gaps[:args.top]}, sys.stdout, indent=2)
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())