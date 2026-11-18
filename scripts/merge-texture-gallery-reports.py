"""Combine completed native captures from sequential gallery batches without recapturing them."""
import argparse
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--views', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('reports', nargs='+', type=Path)
    args = parser.parse_args()
    views = json.loads(args.views.read_text(encoding='utf-8-sig'))
    expected = {v['label']: v for v in views}
    if len(expected) != len(views):
        raise ValueError('Duplicate expected labels')
    captures, batches = {}, []
    before, source = None, None
    for path in args.reports:
        report = json.loads(path.read_text(encoding='utf-8-sig'))
        if before is not None and before != report['before']:
            raise ValueError('Cannot combine before and after captures')
        before, source = report['before'], report['source']
        labels = []
        for capture in report['captures']:
            label = capture['label']
            if label not in expected or label in captures:
                raise ValueError('Unexpected or duplicate capture: ' + label)
            if capture['id'] != expected[label]['id']:
                raise ValueError('Capture subject differs: ' + label)
            image = args.output.parent / (label + '.png')
            if not image.is_file() or image.stat().st_size == 0:
                raise ValueError('Missing native image: ' + str(image))
            captures[label] = capture
            labels.append(label)
        batches.append(dict(report=str(path.as_posix()), status=report['status'],
                            capturedAt=report.get('capturedAt'), completedViews=labels,
                            failure=report.get('failure')))
    if captures.keys() != expected.keys():
        raise ValueError('Incomplete capture set: ' + str(sorted(expected.keys() - captures.keys())))
    result = dict(status='passed', capturedAt=batches[-1]['capturedAt'], before=before,
                  source=source, requested=len(views), captures=[captures[v['label']] for v in views],
                  batchProvenance=batches,
                  completionNote='Completed native frames from sequential isolated runs; failed preparation produced no included frame. Image bytes are unchanged.')
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(status='passed', captures=len(captures), batches=len(batches))))


if __name__ == '__main__':
    main()
