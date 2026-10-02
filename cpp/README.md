# majiang_algorithm C++ 实现

Java 版 [majiang_algorithm](../java) 的 C++17 移植,基于查表的高性能麻将判胡、听牌与 AI 决策。
与 Java/Go 版共用 [data/](../data) 下的同一批查表文件,行为逐位对齐(含浮点评分)。
无第三方依赖,仅需支持 C++17 的编译器与 CMake。

## 构建

```bash
cd cpp
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
```

## 使用

```cpp
#include "majiang/api.h"
#include "majiang/def.h"
#include "majiang/hu_util.h"
#include "majiang/ai_util.h"

int main()
{
    // 加载预计算表(依次在当前目录、data/、../data/ 查找)
    majiang::Load();

    std::vector<int> cards = majiang::StringToCards("1万,2万,3万,东,东");
    int gui = majiang::StringToCard("东");

    bool isHu = majiang::IsHu(cards, gui);                              // 判断胡牌
    std::vector<int> ting = majiang::IsTing(cards, {gui});              // 听牌列表
    int out = majiang::OutAI(cards, {gui});                             // AI 出牌
    bool peng = majiang::PengAI(cards, {gui}, out, 0.0);                // 是否碰
    bool gang = majiang::GangAI(cards, {gui}, out, 0.0);                // 是否杠
    return 0;
}
```

库目标为静态库 `majiang`,头文件在 `include/majiang/`,链接时把 `cpp/include` 加入头文件搜索路径即可。

## 测试

```bash
cd cpp
ctest --test-dir build --output-on-failure
# 或直接运行: ./build/majiang_tests
```

- `def_test.cpp` / `hu_util_test.cpp` / `ai_util_test.cpp`:与 Java JUnit、Go `go test` 用例一一对应。
- `gen_test.cpp`:内存重新生成 jian/feng 判胡表与 AI 表,与仓库已提交的查表文件逐行比对。
- `parity_test.cpp`:回放 `../data/parity_cases.txt`(Java `ParityDump` 以固定种子导出的
  2000+ 局样例),校验 C++ 与 Java/Go 的 isHu / isTing / calc / outAI / chiAI / pengAI / gangAI 完全一致。

## 表生成

```cpp
majiang::HuGen(); // 输出 majiang_clien_*.txt 与 majiang_server_*.txt 到当前目录
majiang::AiGen(); // 输出 majiang_ai_*.txt 到当前目录
```

与 Java 的差异:不输出 sqlite 的 `majiang.db`;输出行按键排序(Java 为多线程乱序写入),内容一致。
编译选项含 `-ffp-contract=off`,保证浮点结果与 Java/Go 逐位一致。
