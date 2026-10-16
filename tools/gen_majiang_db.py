#!/usr/bin/env python3
"""把查表 txt 转成 sqlite 库 majiang.db。

查表数据(majiang_clien_*.txt / majiang_ai_*.txt)合计 1.7M 行、全量载入内存约 120MB。
转成 sqlite 后服务端按 key 点查,常驻内存只剩 page cache。

表结构统一为 (key, seq, ...) + WITHOUT ROWID:
  key 是各花色计数串展开的十进制整数,seq 是该 key 下的条目序号(保持 txt 里的原始顺序)。
  WITHOUT ROWID 让同一 key 的条目在 B 树里连续存放,`WHERE key=?` 是一次范围扫描。

用法:
    python3 tools/gen_majiang_db.py [数据目录] [输出 db]
默认数据目录 data/,输出 data/majiang.db。
"""
import os
import sqlite3
import sys

# (txt 前缀, 表名后缀, 是否胡牌表)
TABLES = [
    ("majiang_clien", "hu", True),
    ("majiang_ai", "ai", False),
]
SUITS = ["normal", "feng", "jian"]


def build(data_dir: str, out_path: str) -> None:
    if os.path.exists(out_path):
        os.remove(out_path)
    db = sqlite3.connect(out_path)
    db.executescript("PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF;")

    for prefix, kind, is_hu in TABLES:
        for suit in SUITS:
            name = f"{kind}_{suit}"
            table = f"CREATE TABLE {name} (key INTEGER NOT NULL, seq INTEGER NOT NULL, " \
                    f"need_gui INTEGER NOT NULL, jiang INTEGER NOT NULL, val, " \
                    f"PRIMARY KEY(key, seq)) WITHOUT ROWID"
            db.execute(table)
            path = os.path.join(data_dir, f"{prefix}_{suit}.txt")
            rows = []
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split()
                    key = int(parts[0])
                    if is_hu:
                        # key needGui jiang hupaiFold(整数,-1 表示无需进牌即胡)
                        rows.append((key, int(parts[1]), int(parts[2]), int(parts[3])))
                    else:
                        # key jiang P ...
                        rows.append((key, 0, int(parts[1]), float(parts[2])))
            # seq 在同一个 key 内按"文件中出现次序"递增。
            # 注意 data/*.txt 由 Java 多线程写出,同一 key 的条目并不连续,
            # 所以要用字典计数,不能靠相邻两行判断;顺序仍与 C++ 加载(txt 按行 push_back)一致。
            seq_of = {}

            def gen_with_seq():
                for key, need_gui, jiang, val in rows:
                    seq = seq_of.get(key, 0)
                    seq_of[key] = seq + 1
                    yield key, seq, need_gui, jiang, val

            db.executemany(
                f"INSERT INTO {name}(key, seq, need_gui, jiang, val) VALUES (?,?,?,?,?)",
                gen_with_seq(),
            )
            print(f"{name}: {len(rows)} 行 <- {os.path.basename(path)}")
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"已生成 {out_path} ({os.path.getsize(out_path) / 1024 / 1024:.1f} MB)")


def main() -> None:
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "data")
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(data_dir, "majiang.db")
    build(data_dir, out)


if __name__ == "__main__":
    main()
