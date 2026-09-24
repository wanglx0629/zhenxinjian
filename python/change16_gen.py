# -*- coding: utf-8 -*-
"""change16 SQL 生成器：18 类刷数映射（内置 200 + TFDA 1730 按编号精确映射，共建存量按旧码先验转换）
作者: wanglx

输入: apps/zhenxinjian-backend/src/main/resources/foods_200.json（2.1 产物，category 已 18 类）
      data-import/tfda/clean/foods_tfda.json（2.2 产物，category 已 18 类）
输出: sql/change16_food_category18.sql（design D4：备份表 → 临时映射表 UPDATE JOIN
      → 共建存量旧码转换 → schema_migrations 补账 v16；幂等可重跑）
"""
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILTIN = os.path.join(BASE, 'apps', 'zhenxinjian-backend', 'src', 'main', 'resources', 'foods_200.json')
TFDA = os.path.join(BASE, 'data-import', 'tfda', 'clean', 'foods_tfda.json')
OUT = os.path.join(BASE, 'sql', 'change16_food_category18.sql')

BATCH = 250

# 旧 10 类（category_code, category_name）→ 18 类先验转换（design D4：共建存量无编号行用，取各类主体归属）
OLD_TO_NEW = {
    '01': ('02', '谷物类'),          # 谷薯杂豆·主食 → 谷物类（主食主体）
    '02': ('03', '肉类'),            # 畜禽肉及制品 → 肉类
    '03': ('05', '蛋类'),            # 蛋奶及制品 → 蛋类
    '04': ('04', '鱼贝类'),          # 水产及制品 → 鱼贝类
    '05': ('07', '豆类'),            # 大豆及制品 → 豆类
    '06': ('08', '蔬菜类'),          # 蔬菜 → 蔬菜类
    '07': ('09', '菇类'),            # 菌藻 → 菇类
    '08': ('11', '水果类'),          # 水果 → 水果类
    '09': ('12', '坚果及种子类'),    # 坚果·种子 → 坚果及种子类
    '10': ('13', '油脂类'),          # 油脂·调味·饮品 → 油脂类
}
OLD_NAMES = ['谷薯杂豆·主食', '畜禽肉及制品', '蛋奶及制品', '水产及制品', '大豆及制品',
             '蔬菜', '菌藻', '水果', '坚果·种子', '油脂·调味·饮品']


def load_map():
    """编号 → (18类编码, 18类名)：内置 F001–F200 + TFDA 分析编号"""
    mapping = {}
    with open(BUILTIN, encoding='utf-8') as f:
        for food in json.load(f)['foods']:
            code, name = food['category'].split(' ', 1)
            assert food['id'] not in mapping
            mapping[food['id']] = (code, name)
    with open(TFDA, encoding='utf-8') as f:
        for food in json.load(f)['foods']:
            code, name = food['category'].split(' ', 1)
            assert food['code'] not in mapping, f'编号冲突: {food["code"]}'
            mapping[food['code']] = (code, name)
    return mapping


def sql_str(s):
    return "'" + s.replace('\\', '\\\\').replace("'", "''") + "'"


def main():
    mapping = load_map()

    lines = [
        '-- Change 16 — 食物库分类全量切换 TFDA 18 大类（存量刷数，幂等可重跑）',
        '-- 作者: wanglx',
        '-- 依据: openspec/changes/food-category-tfda-18（design D4；映射数据由 python/change16_gen.py 生成，勿手改）',
        '-- 口径: 内置 F001–F200 + TFDA 分析编号按编号精确映射 UPDATE JOIN；',
        '--       共建存量（code 为空、category_name 仍为旧 10 类名）按旧码先验转换；',
        '--       重跑安全：编号映射刷同值无变化，共建转换以旧类名为守卫不会二次命中。',
        '-- 回滚: UPDATE foods f JOIN foods_category15_backup b ON f.id = b.id',
        f'--{" " * 7}SET f.category_code = b.category_code, f.category_name = b.category_name；后删 v16 版本账行',
        '-- 验证: SELECT category_code, category_name, COUNT(*) FROM foods GROUP BY category_code ORDER BY category_code；',
        f'--       （预期分布与 data-import/builtin_recat/report.txt 及 tfda/clean/report.txt 合并口径一致）',
        '',
        'USE zhenxinjian;',
        '',
        '-- ========== 1. 备份表（仅首次创建，保住刷数前状态；回滚依据，验证期后再清理） ==========',
        'CREATE TABLE IF NOT EXISTS foods_category15_backup AS',
        'SELECT id, code, category_code, category_name FROM foods;',
        '',
        '-- ========== 2. 临时映射表（code → 18 类） ==========',
        'DROP TEMPORARY TABLE IF EXISTS cat18_map;',
        'CREATE TEMPORARY TABLE cat18_map (',
        '    code          VARCHAR(8)  NOT NULL PRIMARY KEY,',
        '    category_code VARCHAR(2)  NOT NULL,',
        '    category_name VARCHAR(32) NOT NULL',
        ') ENGINE=InnoDB;',
    ]

    items = sorted(mapping.items())
    for i in range(0, len(items), BATCH):
        chunk = items[i:i + BATCH]
        values = ', '.join(
            f"({sql_str(c)}, {sql_str(cc)}, {sql_str(cn)})" for c, (cc, cn) in chunk)
        lines.append(f'INSERT INTO cat18_map (code, category_code, category_name) VALUES {values};')

    lines += [
        '',
        '-- ========== 3. 刷数：编号精确匹配（内置 + TFDA，含软删行一并刷齐） ==========',
        'UPDATE foods f',
        'JOIN cat18_map m ON f.code = m.code',
        'SET f.category_code = m.category_code, f.category_name = m.category_name;',
        '',
        '-- ========== 4. 共建存量（code 为空的旧 10 类名行，先验转换；重跑以旧类名为守卫不二次命中） ==========',
        '-- 注意: MySQL SET 赋值从左到右求值，category_name 的 CASE 须在 category_code 改写前引用原值，故 name 在前',
        'UPDATE foods',
        'SET category_name = CASE category_code',
    ]
    for old, (new, name) in OLD_TO_NEW.items():
        lines.append(f"        WHEN '{old}' THEN {sql_str(name)}")
    lines.append('    END,')
    lines.append('    category_code = CASE category_code')
    for old, (new, name) in OLD_TO_NEW.items():
        lines.append(f"        WHEN '{old}' THEN '{new}'")
    lines.append('    END')
    old_names_sql = ', '.join(sql_str(n) for n in OLD_NAMES)
    lines.append(f'WHERE code IS NULL AND category_name IN ({old_names_sql});')

    lines += [
        '',
        '-- ========== 5. 收尾：清临时表 + 版本账补账（幂等） ==========',
        'DROP TEMPORARY TABLE IF EXISTS cat18_map;',
        "INSERT IGNORE INTO schema_migrations(version, script) VALUES (16, 'change16_food_category18.sql');",
        '',
    ]

    with open(OUT, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print(f'生成 {OUT}：映射 {len(mapping)} 条（内置 200 + TFDA {len(mapping) - 200}）')


if __name__ == '__main__':
    main()
