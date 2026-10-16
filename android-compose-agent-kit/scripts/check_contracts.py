#!/usr/bin/env python3
"""Static validation of registries and empty audit templates. No Android app required."""
from pathlib import Path
import csv, sys
try: import yaml
except ImportError: raise SystemExit('Install PyYAML: python -m pip install pyyaml')
root = Path(__file__).resolve().parents[1]
t = yaml.safe_load((root/'design-system/typography-registry.yaml').read_text())
expected={'pageTitle':(22,600,28),'cardTitle':(16,600,22),'sectionLabel':(12,500,16),'bodyTitle':(16,500,22),'rowTitle':(14,500,20),'supporting':(12,400,16),'button':(14,600,20),'action':(14,500,20),'heroMetric':(32,600,38),'amountInput':(28,600,34),'inlineAmount':(14,600,20),'chartLabel':(12,400,16)}
for role, exp in expected.items():
 x=t['semantic'][role]; actual=(t['primitives'][x['size']],t['weights'][x['weight']],x['lineHeight']); assert actual==exp,(role,actual,exp)
assert set(t['primitives'].values())=={12,14,16,22,28,32}
c=yaml.safe_load((root/'design-system/color-tokens.yaml').read_text())
for theme in ['light','dark']:
 for group, vals in c['semantic'][theme].items():
  for token, primitive in vals.items(): assert primitive in c['primitives'],(theme,group,token,primitive)
i=yaml.safe_load((root/'design-system/icon-registry.yaml').read_text()); assert set(i['sizesDp'].values())=={16,20,24,28,32}
for f in (root/'docs/qa').glob('*.csv'):
 with f.open(newline='') as h: assert len(next(csv.reader(h)))>=5,f
print('PASS: typography exact registry, color references, icon sizes, audit template headers')
