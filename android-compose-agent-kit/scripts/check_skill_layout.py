#!/usr/bin/env python3
from pathlib import Path
r=Path(__file__).resolve().parents[1]
folders=sorted((r/'.agents/skills').glob('android-*'))
assert len(folders)==6,len(folders)
for p in folders:
 s=(p/'SKILL.md').read_text(); assert s.startswith('---\n') and 'description:' in s and (p/'agents/openai.yaml').exists(),p
print('PASS: all six skills contain entrypoint and agent metadata')
