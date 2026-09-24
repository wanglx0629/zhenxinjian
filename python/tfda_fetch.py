# -*- coding: utf-8 -*-
"""TFDA 食品營養成分数据获取（OGL）
来源链: TFDA 官方 data.gov.tw 资料集14197 / data.fda.gov.tw DatasetId=43
       → GitHub 镜像 ating1234/food（GitHub Actions 每月对官方 SHA256 比对同步，OGL 允许再分发）
产出: data-import/tfda/raw/ 下 18 分类 JSON + foods_index.json + version.json
"""
import json
import subprocess
from urllib.parse import quote

BASE = 'https://raw.githubusercontent.com/ating1234/food/main/'
RAW_DIR = r'E:\project\zhenxinjian\data-import\tfda\raw'

# 18 大分类（与 version.json 一致）
CATEGORIES = [
    '乳品類', '加工調理食品及其他類', '堅果及種子類', '水果類', '油脂類', '澱粉類',
    '穀物類', '糕餅點心類', '糖類', '肉類', '菇類', '蔬菜類', '藻類', '蛋類',
    '調味料及香辛料類', '豆類', '飲料類', '魚貝類',
]

FILES = ['foods_index.json', 'version.json'] + [f'data/{c}.json' for c in CATEGORIES]

ok_count = 0
for f in FILES:
    url = BASE + quote(f)
    out = RAW_DIR + '\\' + f.replace('/', '_')
    r = subprocess.run(['curl.exe', '-sS', '--max-time', '120',
                        '--socks5-hostname', '127.0.0.1:10808',
                        '-o', out, url], capture_output=True)
    ok = r.returncode == 0
    # 验证 JSON 可解析
    if ok:
        try:
            with open(out, encoding='utf-8') as fp:
                json.load(fp)
        except Exception as e:
            print('BAD-JSON', f, e)
            ok = False
    print(('OK  ' if ok else 'FAIL'), f)
    ok_count += ok

print(f'done {ok_count}/{len(FILES)}')
