# -*- coding: utf-8 -*-
"""内置 200 食物按 TFDA 18 大类重新归类（一次性迁移脚本，可复跑）
作者: wanglx

输入: apps/zhenxinjian-backend/src/main/resources/foods_200.json
      data-import/tfda/raw/（18 分类 JSON——TFDA 归类惯例的校准依据）
输出: data-import/builtin_recat/manual_review.json（待人工裁决项 + TFDA 惯例建议值）
      data-import/builtin_recat/report.txt（归类统计与明细）
      --apply 时把归类结果写回 foods_200.json（category = "NN 简体类名"，18 类口径）
流程: 名称/别名与 raw 规范名精确匹配 → 采纳 TFDA 原始归类（保证同名食物不分裂）
      → 未命中按 TFDA 惯例校准的关键词规则归类 → 结果越出「旧 10 类先验集合」或无规则命中
      → 进人工裁决清单（MANUAL_OVERRIDES 填写后重跑生效）
依赖: pip install opencc-python-reimplemented
"""
import json
import os
import re

from opencc import OpenCC

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_DIR = os.path.join(BASE, 'data-import', 'tfda', 'raw')
OUT_DIR = os.path.join(BASE, 'data-import', 'builtin_recat')
BUILTIN = os.path.join(BASE, 'apps', 'zhenxinjian-backend', 'src', 'main', 'resources', 'foods_200.json')

T2S = OpenCC('t2s')
BRACKET_RE = re.compile(r'[（(][^）)]*[）)]')

# TFDA 18 大类（繁体类名 → 系统编码 01–18）。与 tfda_clean.py 的 CATEGORY_18 保持一致（单一口径）
CATEGORY_18 = {
    '澱粉類': '01', '穀物類': '02', '肉類': '03', '魚貝類': '04', '蛋類': '05',
    '乳品類': '06', '豆類': '07', '蔬菜類': '08', '菇類': '09', '藻類': '10',
    '水果類': '11', '堅果及種子類': '12', '油脂類': '13', '糖類': '14',
    '糕餅點心類': '15', '調味料及香辛料類': '16', '飲料類': '17', '加工調理食品及其他類': '18',
}
CATEGORY_NAME18 = {
    '01': '淀粉类', '02': '谷物类', '03': '肉类', '04': '鱼贝类', '05': '蛋类',
    '06': '乳品类', '07': '豆类', '08': '蔬菜类', '09': '菇类', '10': '藻类',
    '11': '水果类', '12': '坚果及种子类', '13': '油脂类', '14': '糖类',
    '15': '糕饼点心类', '16': '调味料及香辛料类', '17': '饮料类', '18': '加工调理食品及其他类',
}

# 人工裁决回填（id → 编码）。首次运行产出 manual_review.json，逐条裁决后填写于此再 --apply
# 裁决依据：data-import/tfda/raw/ 实际条目（同名不分裂原则）
MANUAL_OVERRIDES = {
    # ---- 自动归类纠偏（raw 撞名/括号剥离导致偏差）----
    'F030': '07',  # 赤小豆（干豆）：raw 紅豆→豆類；紅豆罐頭(糖漬)别名撞名抢先所致
    'F098': '07',  # 豆浆（无糖）：raw 豆漿(無糖)→豆類；括号剥离误配加工類甜豆漿
    'F189': '13',  # 芝麻油：raw 白/黑/調合芝麻油→油脂類；别名「香油」误配調味料
    # ---- manual_review 21 条裁决 ----
    'F096': '18',  # 千张（豆腐皮）：raw 豆腐皮→加工調理
    'F111': '08',  # 韭菜：蔬菜
    'F113': '08',  # 西兰花：raw 青花菜→蔬菜
    'F117': '08',  # 芦笋：raw 綠/白蘆筍→蔬菜
    'F120': '08',  # 茄子：蔬菜
    'F124': '08',  # 西葫芦：蔬菜
    'F127': '08',  # 尖椒（青辣椒）：蔬菜
    'F128': '08',  # 洋葱：蔬菜
    'F131': '08',  # 莴笋：蔬菜
    'F132': '08',  # 竹笋（鲜笋）：蔬菜（加工類桂竹筍片为腌制品）
    'F135': '07',  # 荷兰豆：raw 豌豆莢（别名荷蘭豆）→豆類
    'F136': '07',  # 四季豆：raw 菜豆莢（别名四季豆）→豆類
    'F137': '08',  # 蒜苔：蔬菜
    'F139': '08',  # 小葱：蔬菜
    'F145': '09',  # 平菇：raw 秀珍菇（别名平菇）→菇類；罐头条目抢先所致
    'F181': '12',  # 葵花子（炒）：raw 原味葵瓜子→堅果及種子類
    'F196': '18',  # 白酒：raw 酒类（啤酒/葡萄酒/紹興）均→加工調理
    'F197': '17',  # 鲜榨橙汁：raw 柳橙汁→飲料類
    'F198': '17',  # 拿铁：raw 拿鐵咖啡→飲料類
    'F199': '17',  # 全糖奶茶：raw 珍珠奶茶/烏龍奶茶→飲料類
    'F200': '16',  # 生抽（酱油）：raw 醬油→調味料
}

# 旧 10 类 → 18 类合理先验集合（越出即进人工裁决，防止关键词误伤）
OLD_PRIOR = {
    '01': {'01', '02', '07', '15', '18'},   # 谷薯杂豆·主食：淀粉/谷物/杂豆/面包(糕饼)/面条粉丝馒头(加工)
    '02': {'03', '18'},                     # 畜禽肉及制品：肉类/火腿香肠腊肉肉松(加工)
    '03': {'05', '06', '13'},               # 蛋奶及制品：蛋/乳品/奶油(油脂)
    '04': {'04', '18'},                     # 水产及制品：鱼贝/鱼丸鱼松蟹棒(加工)
    '05': {'07', '08', '16', '18'},         # 大豆及制品：豆/豆芽(蔬菜)/味噌豆豉(调味)/豆腐豆干腐竹(加工)
    '06': {'01', '08', '09', '10', '16'},   # 蔬菜：蔬菜/土豆山药(淀粉)/菇/藻/腌菜(调味)
    '07': {'09', '10'},                     # 菌藻：菇/藻
    '08': {'11', '14', '16', '17'},         # 水果：水果/果干蜜饯(水果或糖)/果酱(调味)/果汁(饮料)
    '09': {'12', '13', '14', '16'},         # 坚果·种子：坚果种子/坚果油(油脂)/花生糖? /芝麻酱花生酱(调味)
    '10': {'13', '14', '16', '17', '18'},   # 油脂·调味·饮品：油脂/糖/调味/饮料/啤酒(加工)
}

# 关键词规则（有序，命中即归类）。边界均按 TFDA raw 成员名单校准，例如：
#   面包/面条/馒头/豆腐/火腿肠/粉丝 → 18 加工调理（TFDA 口径：土司→糕餅點心、乾麵條/豆腐/培根→加工調理）
#   番茄/南瓜 → 08 蔬菜；玉米 → 02 谷物；啤酒 → 18 加工；果酱 → 16 调味；蜂蜜 → 14 糖
RULES = [
    # 蛋类（先于肉类规则，避免「鸡蛋」误入肉）
    ('05', ['鸡蛋', '鸭蛋', '鹌鹑蛋', '鸽子蛋', '皮蛋', '咸蛋', '蛋白', '蛋黄', '蛋']),
    # 糕饼点心（面包/吐司 TFDA 归糕餅點心類）
    ('15', ['面包', '吐司', '蛋糕', '饼干', '曲奇', '巧克力', '冰淇淋', '雪糕', '布丁',
            '甜甜圈', '蛋挞', '月饼', '酥', '泡芙', '薯片', '锅巴', '沙琪玛', '麻薯',
            '点心', '威化', '派']),
    # 加工调理（TFDA 口径的复合/冷冻/豆制品/面制品/肉制品）
    ('18', ['火腿', '香肠', '培根', '热狗', '午餐肉', '肉松', '肉脯', '腊肉', '腊肠',
            '叉烧', '鸡爪? ', '血', '丸子', '鱼丸', '虾丸', '蟹棒', '汉堡', '三明治',
            '饺子', '馄饨', '包子', '馒头', '花卷', '粽子', '烧麦', '炒饭', '饭团',
            '寿司', '粉丝', '粉条', '粉皮', '拉面', '面条', '挂面', '意面', '通心粉',
            '油条', '麻花', '豆腐', '豆干', '豆皮', '腐竹', '腐皮', '素鸡', '素肉',
            '烤麸', '面筋', '啤酒', '罐头', '泡面', '方便面', '燕麦奶', '甜酒酿',
            '藕粉? ', '披萨', '煎饼', '粥? ']),
    # 油脂（奶油/黄油/猪油/食用油；先于乳品防「奶油」误入奶）
    ('13', ['油']),
    # 乳品（TFDA：木瓜牛奶也归乳品）
    ('06', ['牛奶', '羊奶', '酸奶', '优格', '奶酪', '芝士', '乳酪', '奶粉', '炼乳',
            '鲜奶', '奶昔', '脱脂奶', '全脂奶', '高钙奶', '乳清', '奶']),
    # 豆类（无糖豆浆 TFDA 归豆类；黄豆/红豆/绿豆/豌豆/鹰嘴豆等）
    ('07', ['豆浆', '黄豆', '黑豆', '青豆', '毛豆', '红豆', '绿豆', '芸豆', '花豆',
            '豌豆', '蚕豆', '豇豆', '鹰嘴豆', '扁豆', '四季豆', '豆角', '天贝',
            '味噌? ', '纳豆']),
    # 淀粉（TFDA：马铃薯/甘薯/山药/芋头/莲藕/菱角/荸荠/豆薯→澱粉類）
    ('01', ['土豆', '马铃薯', '红薯', '地瓜', '紫薯', '山药', '芋头', '芋', '莲藕',
            '藕', '菱角', '荸荠', '凉薯', '木薯', '甘薯']),
    # 谷物（TFDA：玉米→穀物類；即食燕麦片/面粉/米→穀物類）
    ('02', ['玉米', '燕麦', '小麦', '大麦', '荞麦', '藜麦', '薏米', '薏仁', '高粱',
            '大米', '粳米', '籼米', '糯米', '糙米', '黑米', '紫米', '紫米? ', '米饭',
            '米粉? ', '面粉', '全麦', '麦片', '麦']),
    # 鱼贝
    ('04', ['鱼', '虾', '蟹', '贝', '鱿鱼', '章鱼', '生蚝', '牡蛎', '扇贝', '蛤蜊',
            '海参', '海蜇', '鲍鱼', '鳕鱼', '三文鱼', '金枪鱼', '带鱼', '鲈鱼',
            '黄花鱼', '鲳鱼', '沙丁鱼', '秋刀鱼', '鳗鱼', '基围虾', '皮皮虾']),
    # 肉类
    ('03', ['鸡胸', '鸡腿', '鸡翅', '鸡爪', '鸡肝', '鸡心', '牛肉', '牛腩', '牛腱',
            '牛排', '猪肉', '猪里脊', '猪排', '排骨', '猪蹄', '猪肝', '猪心', '猪肚',
            '羊肉', '羊排', '鸭肉', '鸭腿', '鹅肉', '驴肉', '瘦肉', '五花肉', '里脊',
            '肥牛', '鸡肉', '鸭胸']),
    # 菇类
    ('09', ['香菇', '蘑菇', '金针菇', '杏鲍菇', '平菇', '木耳', '银耳', '猴头菇',
            '茶树菇', '口蘑', '榛蘑', '草菇', '菇']),
    # 藻类
    ('10', ['海带', '紫菜', '裙带菜', '海苔', '发菜', '麒麟菜', '礁膜']),
    # 坚果及种子（TFDA：花生/栗子/莲子/咖啡豆→堅果及種子類）
    ('12', ['花生', '核桃', '芝麻', '腰果', '杏仁', '巴旦木', '开心果', '栗子',
            '板栗', '莲子', '瓜子', '南瓜子', '葵花籽', '亚麻籽', '奇亚籽', '芡实',
            '榛子', '松子', '夏威夷果', '榴莲? ', '白果']),
    # 糖类（蜂蜜 TFDA 归糖類）
    ('14', ['蜂蜜', '白糖', '冰糖', '红糖', '砂糖', '黑糖', '麦芽糖', '果糖', '糖浆']),
    # 调味料及香辛料（果酱/芝麻酱/花生酱/番茄酱 TFDA 均归調味料）
    ('16', ['酱油', '醋', '盐', '蚝油', '沙拉酱', '番茄酱', '豆瓣酱', '辣椒酱',
            '花生酱', '芝麻酱', '果酱', '味精', '鸡精', '料酒', '咖喱', '胡椒',
            '花椒', '八角', '桂皮', '香叶', '孜然', '茴香', '酱']),
    # 饮料类
    ('17', ['可乐', '咖啡', '茶', '果汁', '汽水', '奶茶', '运动饮料', '豆浆? ',
            '椰奶', '矿泉水', '苏打水']),
    # 水果（放后面兜底；果干 TFDA 归水果類）
    ('11', ['葡萄干', '桂圆', '荔枝', '芒果', '香蕉', '苹果', '梨', '桃', '西瓜',
            '甜瓜', '哈密瓜', '葡萄', '橙子', '橘子', '柚子', '柠檬', '榴莲',
            '菠萝', '凤梨', '樱桃', '草莓', '蓝莓', '猕猴桃', '奇异果', '石榴',
            '柿子', '枣', '山楂', '椰子', '枇杷', '杨梅', '木瓜', '火龙果',
            '百香果', '无花果', '桑葚', '橄榄', '白兰瓜', '香瓜']),
]


def norm_key(name):
    """规范名：去括号内容/空格，繁转简——与 raw 匹配的比对键"""
    s = BRACKET_RE.sub('', name)
    return T2S.convert(s).replace(' ', '').strip()


def to_simplified(text):
    return re.sub(r'\s+', ' ', T2S.convert(text or '')).strip()


def load_raw_lookup():
    """raw 全量 2180 条：规范名（含别名）→ 18 类编码"""
    lookup = {}
    for fn in sorted(os.listdir(RAW_DIR)):
        if not (fn.startswith('data_') and fn.endswith('.json')):
            continue
        tfda_cat = fn[len('data_'):-len('.json')]
        code18 = CATEGORY_18[tfda_cat]
        with open(os.path.join(RAW_DIR, fn), encoding='utf-8') as f:
            items = json.load(f)
        for food in items.values():
            keys = [norm_key(food['name'])]
            for a in re.split('[、,，]', food.get('alias') or ''):
                if a.strip():
                    keys.append(norm_key(a))
            for k in keys:
                # 同名多类冲突时保留首见（raw 内部极少，冲突进报告人工关注）
                lookup.setdefault(k, code18)
    return lookup


def match_rule(name):
    """按序关键词匹配，返回 (编码, 命中关键词) 或 (None, None)"""
    text = norm_key(name)
    for code, words in RULES:
        for w in words:
            w = w.replace('? ', '')
            if w and w in text:
                return code, w
    return None, None


def main():
    apply_mode = '--apply' in os.sys.argv
    with open(BUILTIN, encoding='utf-8') as f:
        data = json.load(f)

    lookup = load_raw_lookup()
    old_codes = set()
    for food in data['foods']:
        old_codes.add(food['category'][:2])

    auto, manual = [], []
    for food in data['foods']:
        fid = food['id']
        name = food['name']
        old_code = food['category'][:2]
        prior = OLD_PRIOR[old_code]

        if fid in MANUAL_OVERRIDES:
            code = MANUAL_OVERRIDES[fid]
            auto.append((food, code, '人工裁决'))
            continue

        # 1) 与 raw 规范名/别名精确匹配 → 采纳 TFDA 原始归类
        keys = [norm_key(name)] + [norm_key(a) for a in re.split('[、,，]', food.get('alias') or '') if a.strip()]
        hit = next((lookup[k] for k in keys if k in lookup), None)
        if hit:
            code, why = hit, 'raw 同名/别名匹配'
        else:
            code, word = match_rule(name)
            why = f'关键词「{word}」' if code else ''

        # 2) 未命中或越出旧类先验 → 人工裁决（附 TFDA 惯例建议）
        if code is None:
            manual.append({'id': fid, 'name': name, 'old': food['category'],
                           'suggest': None, 'reason': '无规则命中'})
        elif code not in prior:
            manual.append({'id': fid, 'name': name, 'old': food['category'],
                           'suggest': code, 'reason': f'{why}（越出旧类先验）'})
        else:
            auto.append((food, code, why))

    # ---------- 产出 ----------
    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, 'manual_review.json'), 'w', encoding='utf-8') as f:
        json.dump(manual, f, ensure_ascii=False, indent=1)

    from collections import Counter
    dist = Counter(CATEGORY_NAME18[c] for _, c, _ in auto)
    lines = [
        '内置 200 → TFDA 18 类 归类报告',
        '=' * 40,
        f'自动归类: {len(auto)} 条；待人工裁决: {len(manual)} 条（见 manual_review.json，'
        f'裁决后填入脚本 MANUAL_OVERRIDES 重跑 --apply）',
        '',
        '自动归类分布:',
    ]
    for k in sorted(dist):
        lines.append(f'  {k}: {dist[k]}')
    lines += ['', '自动归类明细:']
    for food, code, why in auto:
        lines.append(f"  {food['id']} {food['name']}: {CATEGORY_NAME18[code]}（{why}）")
    lines += ['', f'待人工裁决（{len(manual)}）:']
    for m in manual:
        s = CATEGORY_NAME18[m['suggest']] if m['suggest'] else '无建议'
        lines.append(f"  {m['id']} {m['name']}（旧: {m['old']}）→ 建议: {s}，{m['reason']}")
    report = '\n'.join(lines)
    with open(os.path.join(OUT_DIR, 'report.txt'), 'w', encoding='utf-8') as f:
        f.write(report)
    print(report[:2000])

    # ---------- 写回（--apply 且无未裁决项时） ----------
    if apply_mode:
        if manual:
            raise SystemExit(f'仍有 {len(manual)} 条未裁决，先完成 manual_review.json 裁决并填入 MANUAL_OVERRIDES')
        for food, code, _ in auto:
            food['category'] = f'{code} {CATEGORY_NAME18[code]}'
        data['category18'] = True
        with open(BUILTIN, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f'已写回 foods_200.json（{len(auto)} 条）')


if __name__ == '__main__':
    main()
