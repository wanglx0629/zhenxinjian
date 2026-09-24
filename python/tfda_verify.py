# -*- coding: utf-8 -*-
"""验证 TFDA 原始数据：文件、条数、字段结构、营养素口径"""
import json
import os

RAW = r'E:\project\zhenxinjian\data-import\tfda\raw'

with open(os.path.join(RAW, 'version.json'), encoding='utf-8') as f:
    version = json.load(f)
print('version:', version['version'], '| total_foods:', version['total_foods'], '| generated:', version['generated'])

files = sorted(os.listdir(RAW))
print('files:')
total = 0
for fn in files:
    size = os.path.getsize(os.path.join(RAW, fn))
    print(f"  {size:>9}  {fn.encode('gbk', 'replace').decode('gbk')}")
    if fn.startswith('data_') and fn.endswith('.json'):
        with open(os.path.join(RAW, fn), encoding='utf-8') as f:
            d = json.load(f)
        total += len(d)

print('data files total foods =', total)

# 抽样查看一条完整记录的字段（找米饭类）
with open(os.path.join(RAW, 'data_穀物類.json'), encoding='utf-8') as f:
    grains = json.load(f)
print('\nsample codes:', list(grains.keys())[:5])
code = list(grains.keys())[0]
food = grains[code]
print('sample keys:', list(food.keys()))
print('name:', food['name'], '| alias:', food['alias'], '| category:', food['category'])
print('kcal:', food['kcal'], '| desc:', (food.get('desc') or '')[:40])
print('nutrient groups:', list(food['nutrients'].keys()))
print('\ngeneral components:')
for item in food['nutrients'].get('一般成分', []):
    print('   ', item['name'], '|', item['unit'], '|', item['per100g'])
