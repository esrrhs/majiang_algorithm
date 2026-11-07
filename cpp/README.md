# mahjong_ai C++ 实现

Java 版 [mahjong_ai](../java) 的 C++17 移植,基于查表的高性能麻将判胡、听牌与 AI 决策。
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
#include "mahjong/api.h"
#include "mahjong/def.h"
#include "mahjong/hu_util.h"
#include "mahjong/ai_util.h"

int main()
{
    // 加载预计算表(依次在当前目录、data/、../data/ 查找)
    mahjong::Load();

    std::vector<int> cards = mahjong::StringToCards("1万,2万,3万,东,东");
    int gui = mahjong::StringToCard("东");

    bool isHu = mahjong::IsHu(cards, gui);                              // 判断胡牌
    std::vector<int> ting = mahjong::IsTing(cards, {gui});              // 听牌列表
    int out = mahjong::OutAI(cards, {gui});                             // AI 出牌
    bool peng = mahjong::PengAI(cards, {gui}, out, 0.0);                // 是否碰
    bool gang = mahjong::GangAI(cards, {gui}, out, 0.0);                // 是否杠
    return 0;
}
```

库目标为静态库 `mahjong`,头文件在 `include/mahjong/`,链接时把 `cpp/include` 加入头文件搜索路径即可。

## 测试

```bash
cd cpp
ctest --test-dir build --output-on-failure
# 或直接运行: ./build/mahjong_tests
```

- `def_test.cpp` / `hu_util_test.cpp` / `ai_util_test.cpp`:与 Java JUnit、Go `go test` 用例一一对应。
- `gen_test.cpp`:内存重新生成 jian/feng 判胡表与 AI 表,与仓库已提交的查表文件逐行比对。
- `parity_test.cpp`:回放 `../data/parity_cases.txt`(Java `ParityDump` 以固定种子导出的
  2000+ 局样例),校验 C++ 与 Java/Go 的 isHu / isTing / calc / outAI / chiAI / pengAI / gangAI 完全一致。

## 表生成

```cpp
mahjong::HuGen(); // 输出 mahjong_clien_*.txt 与 mahjong_server_*.txt 到当前目录
mahjong::AiGen(); // 输出 mahjong_ai_*.txt 到当前目录
```

与 Java 的差异:不输出 sqlite 的 `mahjong.db`;输出行按键排序(Java 为多线程乱序写入),内容一致。
编译选项含 `-ffp-contract=off`,保证浮点结果与 Java/Go 逐位一致。
