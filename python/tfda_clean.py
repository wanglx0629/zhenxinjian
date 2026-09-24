# -*- coding: utf-8 -*-
"""TFDA 食品營養成分数据清洗脚本（可复跑）
作者: wanglx

输入: data-import/tfda/raw/（18 分类 JSON，OGL 版本 20.5，溯源见 raw/SOURCE.json）
输出: data-import/tfda/clean/foods_tfda.json（后端 TfdaFoodInitializer 导入源）
      clean/abnormal.json（异常清单）  clean/report.txt（导入统计）
流程: 营养提取与校验 → OpenCC 繁转简 → 名称归一/别名补充 → 与内置 200/TFDA 内部去重
      → 18 大类直读映射 01–18（raw category 字段即分类，不做压缩）
依赖: pip install opencc-python-reimplemented
"""
import json
import os
import re
from collections import Counter, defaultdict

from opencc import OpenCC

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_DIR = os.path.join(BASE, 'data-import', 'tfda', 'raw')
CLEAN_DIR = os.path.join(BASE, 'data-import', 'tfda', 'clean')
BUILTIN = os.path.join(BASE, 'apps', 'zhenxinjian-backend', 'src', 'main', 'resources', 'foods_200.json')

DATA_BATCH = 'TFDA:20.5'
VERSION = '20.5'
T2S = OpenCC('t2s')

# ---------- 18 大类直读映射（raw category 字段 → 系统编码 01–18，与 builtin_recat.py 同一口径） ----------
CATEGORY_18 = {
    '澱粉類': '01', '穀物類': '02', '肉類': '03', '魚貝類': '04', '蛋類': '05',
    '乳品類': '06', '豆類': '07', '蔬菜類': '08', '菇類': '09', '藻類': '10',
    '水果類': '11', '堅果及種子類': '12', '油脂類': '13', '糖類': '14',
    '糕餅點心類': '15', '調味料及香辛料類': '16', '飲料類': '17', '加工調理食品及其他類': '18',
}
CATEGORY_NAME = {
    '01': '淀粉类', '02': '谷物类', '03': '肉类', '04': '鱼贝类', '05': '蛋类',
    '06': '乳品类', '07': '豆类', '08': '蔬菜类', '09': '菇类', '10': '藻类',
    '11': '水果类', '12': '坚果及种子类', '13': '油脂类', '14': '糖类',
    '15': '糕饼点心类', '16': '调味料及香辛料类', '17': '饮料类', '18': '加工调理食品及其他类',
}
# 台湾常用词 → 大陆同义词（追加进 alias，提升简体搜索命中）
MAINLAND_ALIAS = [
    ('马铃薯', ['土豆', '洋芋']),
    ('甘薯', ['红薯', '地瓜']),
    ('蕃薯', ['红薯', '地瓜']),
    ('地瓜', ['红薯']),
    ('高丽菜', ['卷心菜', '包菜', '圆白菜']),
    ('青花菜', ['西兰花']),
    ('茭白笋', ['茭白']),
    ('冬粉', ['粉丝']),
    ('苦茶油', ['山茶油', '茶油']),
    ('豆薯', ['凉薯']),
    ('洋菇', ['蘑菇']),
    ('番茄', ['西红柿']),
    ('蕃茄', ['西红柿']),
]
# 描述中的口径标注 → 追加进 alias（spec：特殊口径保留来源标注）
BASIS_MARKS = [('乾重', '干重'), ('濕重', '湿重'), ('帶殼', '带壳'), ('去殼', '去壳'),
               ('帶皮', '带皮'), ('去皮', '去皮'), ('帶骨', '带骨'), ('去骨', '去骨')]

BRACKET_RE = re.compile(r'[（(][^）)]*[）)]')


def norm_key(name):
    """规范名：去括号内容/空格，繁转简——去重比对键"""
    s = BRACKET_RE.sub('', name)
    return T2S.convert(s).replace(' ', '').strip()


def to_simplified(text):
    """繁转简 + 空格压一"""
    return re.sub(r'\s+', ' ', T2S.convert(text)).strip()


def cap_alias(aliases, limit):
    """顿号拼接别名并截断到列宽：超限逐个丢弃尾部别名"""
    kept = []
    for a in aliases:
        cand = '、'.join(kept + [a])
        if len(cand) > limit:
            break
        kept.append(a)
    return '、'.join(kept)


def resolve_category(tfda_cat):
    """TFDA raw category 字段 → 系统编码 01–18（直读，不做压缩）"""
    return CATEGORY_18.get(tfda_cat)


def find_nutrient(food, group, names):
    """从分析项列表取每 100g 含量（names：候选项名，按序取第一个命中）"""
    for name in names:
        for item in food.get('nutrients', {}).get(group, []):
            if item['name'] == name:
                return item['per100g']
    return None


def num(v):
    """清洗数值：字符串/None → float 或 None"""
    if v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip().replace(',', '')
    if not s or s.lower() == 'null':
        return None
    try:
        return float(s)
    except ValueError:
        return None


def load_builtin_keys():
    """内置 200 规范名 + 别名集合（去重比对集）"""
    keys = set()
    with open(BUILTIN, encoding='utf-8') as f:
        d = json.load(f)
    for food in d['foods']:
        keys.add(norm_key(food['name']))
        alias = food.get('alias') or ''
        for a in alias.split('、'):
            if a.strip():
                keys.add(norm_key(a))
    return keys


def main():
    with open(os.path.join(RAW_DIR, 'version.json'), encoding='utf-8') as f:
        meta = json.load(f)
    builtin_keys = load_builtin_keys()

    foods = []
    abnormal = []
    candidates = defaultdict(list)
    skipped_builtin = []
    seen_codes = set()
    cat_counter = Counter()
    undetected_zero = []

    raw_files = sorted(fn for fn in os.listdir(RAW_DIR)
                       if fn.startswith('data_') and fn.endswith('.json'))
    for fn in raw_files:
        tfda_cat = fn[len('data_'):-len('.json')]
        with open(os.path.join(RAW_DIR, fn), encoding='utf-8') as f:
            items = json.load(f)
        for code, food in items.items():
            if code in seen_codes:
                abnormal.append({'code': code, 'name': food['name'], 'reason': '整合编号重复'})
                continue
            seen_codes.add(code)

            # 营养提取（每 100g）
            kcal = food.get('kcal')
            kcal = int(round(num(kcal))) if num(kcal) is not None else None
            protein = num(find_nutrient(food, '一般成分', ('粗蛋白',)))
            fat = num(find_nutrient(food, '一般成分', ('粗脂肪',)))
            carb = num(find_nutrient(food, '一般成分', ('總碳水化合物', '碳水化合物')))

            # 营养校验（越界/缺失进异常清单）
            errs = []
            if kcal is None or not 0 <= kcal <= 900:
                errs.append(f'kcal={kcal}')
            if protein is None or not 0 <= protein <= 100:
                errs.append(f'protein={protein}')
            if fat is None or not 0 <= fat <= 100:
                errs.append(f'fat={fat}')
            if carb is None or not 0 <= carb <= 100:
                errs.append(f'carb={carb}')
            if errs:
                # 人工裁决（2026-09-24）：飲料類的蛋白/脂肪「未检出」（成分不存在=0）按 0 入库；
                # 其余类目（糖/调味/油脂/糕饼等）按 spec 严格进异常清单不入库
                if tfda_cat == '飲料類' and all(e in ('protein=None', 'fat=None') for e in errs):
                    protein = 0 if protein is None else protein
                    fat = 0 if fat is None else fat
                    undetected_zero.append(f'{code} {food["name"]}: {",".join(errs)}')
                else:
                    abnormal.append({'code': code, 'name': food['name'],
                                     'reason': '营养缺失或越界: ' + ','.join(errs)})
                    continue

            # 繁转简（别名：TFDA 俗名用半角逗号分隔，兼容全角/顿号）
            name = to_simplified(food['name'])
            if len(name) > 100:  # foods.name VARCHAR(100)，超长拒绝入库防启动插入失败
                abnormal.append({'code': code, 'name': name[:40] + '…',
                                 'reason': f'名称超长({len(name)}字符)'})
                continue
            aliases = [to_simplified(a) for a in re.split('[、,，]', food.get('alias') or '') if a.strip()]
            # 台湾方言别名冲突剔除：花生在台俗称「土豆」，大陆语境土豆=马铃薯，保留会误导搜索
            if '花生' in name:
                aliases = [a for a in aliases if a != '土豆']
            desc = to_simplified(food.get('desc') or '')

            # 分类映射（raw category 直读 01–18）
            code18 = resolve_category(tfda_cat)
            if code18 is None:
                abnormal.append({'code': code, 'name': name, 'reason': f'未知分类: {tfda_cat}'})
                continue

            # 别名补充：大陆同义词 + 口径标注
            for tw, mainland in MAINLAND_ALIAS:
                if tw in to_simplified(food['name']) and mainland[0] not in name:
                    aliases.extend(mainland)
            for tw, mark in BASIS_MARKS:
                if mark and tw in desc and mark not in name and mark not in aliases:
                    aliases.append(mark)

            candidates[norm_key(food['name'])].append({
                'code': code,
                'category': f'{code18} {CATEGORY_NAME[code18]}',
                'name': name,
                # 别名列宽 100（foods.alias VARCHAR(100)）：超限逐个丢弃尾部别名
                'alias': cap_alias(dict.fromkeys(aliases), 100),
                'carb': round(carb, 2),
                'protein': round(protein, 2),
                'fat': round(fat, 2),
                'kcal': kcal,
                'kj': int(round(kcal * 4.184)),
                'serving': 100,
                'dataBatch': DATA_BATCH,
            })

    # 去重：与内置 200 / TFDA 内部按规范名分组，组内保留「无括号原味名优先」的代表
    for key, group in candidates.items():
        if key in builtin_keys:
            skipped_builtin.extend({'code': g['code'], 'name': g['name']} for g in group)
            continue
        group.sort(key=lambda g: (bool(BRACKET_RE.search(g['name'])), len(g['name']), g['code']))
        foods.append(group[0])
        cat_counter[group[0]['category']] += 1
        for dup in group[1:]:
            abnormal.append({'code': dup['code'], 'name': dup['name'],
                             'reason': f'规范名重复（保留 {group[0]["name"]}）'})

    # ---------- 产出 ----------
    os.makedirs(CLEAN_DIR, exist_ok=True)
    out = {
        'source': 'TFDA 食品營養成分資料庫 ' + VERSION + '（衛生福利部食品藥物管理署，政府資料開放授權條款 OGL）',
        'basis': '每100g可食部；kJ=kcal×4.184 换算',
        'version': VERSION,
        'dataBatch': DATA_BATCH,
        'total': len(foods),
        'foods': foods,
    }
    with open(os.path.join(CLEAN_DIR, 'foods_tfda.json'), 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    with open(os.path.join(CLEAN_DIR, 'abnormal.json'), 'w', encoding='utf-8') as f:
        json.dump(abnormal, f, ensure_ascii=False, indent=1)

    lines = [
        'TFDA 导入清洗报告',
        '=' * 40,
        f"官方版本: {VERSION}  生成: {meta['generated']}  声明总数: {meta['total_foods']}",
        f"读取原始: {len(seen_codes)} 条",
        f"待导入: {len(foods)} 条",
        f"与内置 200 重复跳过: {len(skipped_builtin)} 条",
        f"规范名去重（TFDA 内部同品种）: {sum(1 for a in abnormal if a['reason'].startswith('规范名'))} 条",
        f"营养异常（缺失/越界，人工处理）: {sum(1 for a in abnormal if a['reason'].startswith('营养'))} 条",
        f"异常清单合计: {len(abnormal)} 条",
        f"飲料類未检出按 0 入库（人工裁决）: {len(undetected_zero)} 条",
        '',
        '分类分布:',
    ]
    for k in sorted(cat_counter):
        lines.append(f'  {k}: {cat_counter[k]}')
    lines.append('')
    lines.append('与内置重复样例（前 30）: ' + '、'.join(x['name'] for x in skipped_builtin[:30]))
    lines.append('')
    lines.append('营养异常条目（全部）:')
    for a in abnormal:
        if a['reason'].startswith('营养'):
            lines.append(f"  {a['code']} {a['name']}: {a['reason']}")
    report = '\n'.join(lines)
    with open(os.path.join(CLEAN_DIR, 'report.txt'), 'w', encoding='utf-8') as f:
        f.write(report)
    print(report)


if __name__ == '__main__':
    main()
