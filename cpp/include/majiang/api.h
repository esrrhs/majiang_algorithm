// 顶层入口:表加载与重新生成,对应 Java HuUtil.load/gen + AIUtil.load/gen 与 Go api.go。
#pragma once

namespace majiang
{

// 加载全部查表文件(判胡表 + AI 表),等价 Java HuUtil.load() + AIUtil.load()。
// 表文件依次在当前目录、data/、../data/ 下查找,与 Java/Go 侧一致。
// 成功返回 true,失败时向 stderr 输出原因并返回 false。
bool Load();

// 只加载判胡表,等价 Java HuUtil.load()。
bool HuLoad();

// 只加载 AI 表,等价 Java AIUtil.load()。
bool AILoad();

// 重新生成判胡查表文件(majiang_clien_*.txt 与 majiang_server_*.txt),写入当前目录。
// 等价 Java HuUtil.gen(),但不含 sqlite 的 majiang.db 输出;输出行按键排序,内容一致。
bool HuGen();

// 重新生成 AI 查表文件(majiang_ai_*.txt),写入当前目录,等价 Java AIUtil.gen()。
bool AiGen();

} // namespace majiang
