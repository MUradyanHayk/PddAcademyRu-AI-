"""Normalize a local checkout of etspring/pdd_russia. No network at app runtime."""
import argparse, hashlib, json, pathlib, re, shutil, io
import cairosvg
from PIL import Image

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=pathlib.Path)
    args = parser.parse_args()
    dest = pathlib.Path(__file__).resolve().parents[1] / 'app/src/main/assets'
    (dest/'images').mkdir(parents=True, exist_ok=True)
    questions = []
    for f in sorted((args.source/'questions/A_B/tickets').glob('*.json')):
        ticket = int(re.search(r'\d+', f.stem)[0])
        rows = json.loads(f.read_text())
        assert len(rows) == 20, f
        for n, q in enumerate(rows, 1):
            assert int(re.search(r'\d+', q['title'])[0]) == n
            correct = [i for i,a in enumerate(q['answers']) if a['is_correct']]
            assert len(correct) == 1
            path = q['image'].removeprefix('./')
            image = None
            if not path.endswith('no_image.jpg'):
                original = args.source/path
                assert original.exists(), original
                image = 'images/'+original.stem+'.webp'
                with Image.open(original) as im:
                    im.convert('RGB').save(dest/image, 'WEBP', quality=88)
            questions.append(dict(id=f'ab-{ticket:02}-{n:02}', sourceId=q['id'], ticket=ticket,
                number=n, text=q['question'].strip(), answers=[a['answer_text'].strip() for a in q['answers']],
                correct=correct[0], explanation=q['answer_tip'].strip(), topics=q['topic'], image=image))
    questions.sort(key=lambda q:(q['ticket'],q['number']))
    assert len(questions)==800 and len({q['id'] for q in questions})==800
    assert all(q['text'] and q['explanation'] and q['topics'] for q in questions)
    (dest/'questions.json').write_text(json.dumps(questions,ensure_ascii=False,indent=2))
    signs=[]
    for category, entries in json.loads((args.source/'signs/signs.json').read_text()).items():
        for number,s in entries.items():
            original=args.source/s['image'].removeprefix('./')
            # Use a raster image for predictable offline decoding.
            if not original.exists():
                signs.append(dict(number=number,title=s['title'],category=category,description=s.get('description') or '',image=''))
                continue
            image='images/sign-'+original.stem+'.webp'
            image_source = io.BytesIO(cairosvg.svg2png(url=str(original), output_width=320)) if original.suffix.lower()=='.svg' else original
            with Image.open(image_source) as im: im.convert('RGBA').save(dest/image,'WEBP',lossless=True)
            signs.append(dict(number=number,title=s['title'],category=category,description=s.get('description') or '',image=image))
    (dest/'signs.json').write_text(json.dumps(signs,ensure_ascii=False,indent=2))
    manifest=dict(source='https://github.com/etspring/pdd_russia',imported='2026-09-07',questions=len(questions),signs=len(signs),
        sha256=hashlib.sha256((dest/'questions.json').read_bytes()).hexdigest(),verifiedCurrent=False)
    (dest/'content_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
    print(json.dumps(manifest,ensure_ascii=False))
if __name__=='__main__': main()
