import hashlib,json,pathlib
from PIL import Image
root=pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets'
questions=json.loads((root/'questions.json').read_text());signs=json.loads((root/'signs.json').read_text())
assert len(questions)==800 and len({q['id'] for q in questions})==800
for ticket in range(1,41):assert sorted(q['number'] for q in questions if q['ticket']==ticket)==list(range(1,21))
paths=set()
for q in questions:
 assert q['text'].strip() and q['explanation'].strip() and q['topics']
 assert 2<=len(q['answers'])<=5 and 0<=q['correct']<len(q['answers']) and all(q['answers'])
 if q['image']:paths.add(q['image'])
assert len({s['number'] for s in signs})==len(signs)
for s in signs:
 assert s['number'] and s['title']
 if s['image']:paths.add(s['image'])
for p in paths:
 with Image.open(root/p) as im:im.verify()
manifest=json.loads((root/'content_manifest.json').read_text())
assert hashlib.sha256((root/'questions.json').read_bytes()).hexdigest()==manifest['sha256']
print(f'PASS: {len(questions)} questions, 40 tickets, {len(signs)} signs, {len(paths)} valid images.')
print(f'Source limitation: {sum(not s["image"] for s in signs)} signs without images.')
