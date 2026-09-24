# -*- coding: utf-8 -*-
"""TFDA 食品營養成分数据清洗脚本（可复跑）
作者: wanglx

输入: data-import/tfda/raw/（18 分类 JSON，OGL 版本 20.5，溯源见 raw/SOURCE.json）
输出: data-import/tfda/clean/foods_tfda.json（后端 TfdaFoodInitializer 导入源）
      clean/abnormal.json（异常清单）  clean/report.txt（导入统计）
流程: 营养提取与校验 → OpenCC 繁转简 → 名称归一/别名补充 → 与内置 200/TFDA 内部去重
      → 18 大类映射 01–10 → kcal/kJ 对齐每 100g
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

# ---------- 01–10 分类映射（内置 200 口径：土豆/红薯→01、黄油/糖/可乐→10） ----------
# 固定映射类目
CATEGORY_MAP = {
    '穀物類': '01', '澱粉類': '01', '肉類': '02', '蛋類': '03', '乳品類': '03',
    '魚貝類': '04', '蔬菜類': '06', '菇類': '07', '藻類': '07', '水果類': '08',
    '堅果及種子類': '09', '油脂類': '10', '糖類': '10', '調味料及香辛料類': '10', '飲料類': '10',
}
CATEGORY_NAME = {
    '01': '谷薯杂豆·主食', '02': '畜禽肉及制品', '03': '蛋奶及制品', '04': '水产及制品',
    '05': '大豆及制品', '06': '蔬菜', '07': '菌藻', '08': '水果', '09': '坚果·种子',
    '10': '油脂·调味·饮品',
}
# 豆类拆分：荚用菜豆→06 蔬菜；大豆及制品→05；其余杂豆→01（内置口径：绿豆/红豆归 01、黄豆芽归 05）
BEAN_SOY = ['黄豆', '黑豆', '毛豆', '豆浆', '豆腐', '豆花', '天贝', '面肠', '素肉', '豆芽',
            '豆皮', '腐皮', '腐竹', '味噌', '豆瓣']
# 糕饼点心默认 10（零食甜点），面包类→01（内置「面包」口径）——见 resolve_category
# 加工调理食品：按主成分关键词有序匹配，未命中默认 10
PROCESSED_RULES = [
    ('09', ['瓜子', '瓜仁', '杏仁', '花生', '芝麻', '核桃', '腰果', '开心果', '栗子',
            '莲子', '种仁', '扁桃仁', '松子', '葵花', '夏威夷豆']),
    ('01', ['面', '米粉', '米苔目', '通心', '西谷米', '冬粉', '粄', '河粉', '馒头', '年糕',
            '米浆', '粥', '麦', '饼', '包', '盒子', '粿', '条', '饭', '烧卖', '皮',
            '蚕豆', '甘纳豆', '豌豆', '花豆', '红豆', '玉米', '饺', '米糕', '银丝卷',
            '宽粉', '麸', '花卷', '馄饨', '春卷', '粉圆', '芋圆', '粽']),
    ('05', ['豆浆', '豆腐', '豆干', '豆丝', '豆花', '天贝', '面肠', '素肉', '豆皮', '腐皮',
            '味噌', '豆瓣酱', '豆豉', '腐乳', '豆枣', '豆奶', '黑豆']),
    ('06', ['酸菜', '榨菜', '梅干菜', '菜干', '腌渍', '泡菜', '萝卜干', '冬菜', '甘蓝干',
            '笋', '洋葱', '薤', '辣椒']),
    ('07', ['菇', '银耳', '木耳', '藻']),
    ('04', ['鱼', '虾', '海苔', '蜇', '花枝', '蚵', '鲔', '干贝', '蟹', '小卷', '鲱']),
    ('02', ['肉', '鸡', '猪', '牛', '鸭', '火腿', '香肠', '培根', '肘子', '丸', '鹅',
            '胆肝', '热狗']),
    ('03', ['蛋', '奶酪', '优格', '优酪']),
    ('08', ['果酱', '果干']),
    ('10', ['茶', '咖啡', '果汁', '酒', '酱', '糖', '醋', '油', '盐', '沙拉酱', '蜂蜜',
            '味素', '汤', '口含']),
]
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


def resolve_category(tfda_cat, name):
    """TFDA 18 类 → 本系统 01–10（固定映射 + 豆类/糕饼/加工调理条件规则）"""
    name_s = to_simplified(name)
    if tfda_cat in CATEGORY_MAP:
        return CATEGORY_MAP[tfda_cat]
    if tfda_cat == '豆類':
        if '荚' in name_s:
            return '06'
        if any(k in name_s for k in BEAN_SOY):
            return '05'
        return '01'
    if tfda_cat == '糕餅點心類':
        if any(k in name_s for k in ('面包', '吐司')):
            return '01'
        return '10'
    if tfda_cat == '加工調理食品及其他類':
        for code, words in PROCESSED_RULES:
            if any(w in name_s for w in words):
                return code
        return '10'
    return None


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
    rule10_defaults = []
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

            # 分类映射
            code10 = resolve_category(tfda_cat, food['name'])
            if code10 is None:
                abnormal.append({'code': code, 'name': name, 'reason': f'未知分类: {tfda_cat}'})
                continue
            if tfda_cat == '加工調理食品及其他類' and code10 == '10':
                rule10_defaults.append(name)

            # 别名补充：大陆同义词 + 口径标注
            for tw, mainland in MAINLAND_ALIAS:
                if tw in to_simplified(food['name']) and mainland[0] not in name:
                    aliases.extend(mainland)
            for tw, mark in BASIS_MARKS:
                if mark and tw in desc and mark not in name and mark not in aliases:
                    aliases.append(mark)

            candidates[norm_key(food['name'])].append({
                'code': code,
                'category': f'{code10} {CATEGORY_NAME[code10]}',
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
    lines.append(f'加工调理默认归 10 的条目（{len(rule10_defaults)}）: ' + '、'.join(rule10_defaults[:30]))
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
