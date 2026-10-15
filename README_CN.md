# 麻将算法 · Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://central.sonatype.com/artifact/com.github.esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/maven.yml?branch=master&label=java%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/go.yml?branch=master&label=go%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/cpp.yml?branch=master&label=c%2B%2B%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)

> 高性能麻将胡牌 & AI 出牌算法,基于**查表法**实现,支持多张鬼牌(癞子)。一套算法,三种实现 —— **Java / Go / C++**,由跨语言测试保证行为完全一致。

[English Documentation](./README.md)

---

## 特性

- **胡牌判断**：毫秒级判断是否胡牌，支持任意数量鬼牌
- **听牌计算**：快速列出当前手牌能胡的所有牌
- **AI 出牌**：评分模型驱动，自动决策出牌、碰牌、杠牌
- **查表法**：离线预计算，运行时仅做哈希查找，性能极高
- **覆盖全牌型**：万、筒、条、风牌（東南西北）、箭牌（中發白）
- **三语言实现**：Java、Go、C++17 加载同一份 `data/` 查表文件，2000+ 局固定种子对局在每条 CI 中回放校验，行为完全一致（浮点评分按位相等）

---

## 目录结构

```
java/    Java 纯算法库(Maven 工程,发布到 Maven Central)
go/      Go 纯算法库(不含 web 服务)
cpp/     C++17 纯算法库(CMake,与 Java 版行为对齐)
server/  FakeLua 网页对战服务(对局 + HTTP 在 Lua;查表/AI 复用 cpp/,见 server/API.md)
data/    预计算查表文件 + 跨语言对齐样例,所有实现共用
deploy/  线上演示服务器的部署物料(systemd 单元 + 脚本)
```

两个实现加载同一份 `data/` 下的查表文件,并通过测试保证行为一致:
算法单测在 JUnit、`go test` 与 C++ 测试间一一对应移植;[data/parity_cases.txt](./data/parity_cases.txt)
中保存了 2000+ 局固定种子发牌的胡牌/听牌/AI 决策结果,由
`java/.../ParityFixtureTest.java`、`go/parity_test.go` 与 `cpp/test/parity_test.cpp` 多语言回放校验。

---

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.18</version>
</dependency>
```

### 胡牌 / 听牌

```java
// 加载预计算表
HuTable.load(Files.readAllLines(normalTablePath));
HuTableFeng.load(Files.readAllLines(fengTablePath));
HuTableJian.load(Files.readAllLines(jianTablePath));

// 判断胡牌
boolean isHu = HuUtil.isHu(cards, gui);

// 查询听牌
List<Integer> tingCards = HuUtil.isTing(cards, gui);
```

### AI 出牌

```java
// 加载 AI 评分表
AITable.load(Files.readAllLines(normalTablePath));
AITableFeng.load(Files.readAllLines(fengTablePath));
AITableJian.load(Files.readAllLines(jianTablePath));

// 决策出牌
int card = AIUtil.outAI(cards, gui);

// 决策碰 / 杠
boolean isPeng = AIUtil.pengAI(cards, gui, pengCard, 0.0d);
boolean isGang = AIUtil.gangAI(cards, gui, gangCard, 0.0d);
```

### Go

```bash
go get github.com/esrrhs/majiang_algorithm/go
```

```go
package main

import (
	majiang "github.com/esrrhs/majiang_algorithm/go"
)

func main() {
	// 加载预计算表(依次在当前目录、./data、../data 查找)
	majiang.Load()

	cards := majiang.StringToCards("1万,2万,3万,东,东")
	gui := majiang.StringToCard("东")

	// 判断胡牌
	isHu := majiang.IsHu(cards, gui)

	// 查询听牌
	ting := majiang.IsTing(cards, []int{gui})

	// AI 出牌 / 碰 / 杠
	out := majiang.OutAI(cards, []int{gui})
	isPeng := majiang.PengAI(cards, []int{gui}, pengCard, 0)
	isGang := majiang.GangAI(cards, []int{gui}, gangCard, 0)
}
```

运行 Go 测试(在 `go/` 目录下,与 Java 流水线回放同一批样例):

```bash
cd go && go test ./...
```

### C++

```bash
cd cpp
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
./build/majiang_tests   # 或: ctest --test-dir build --output-on-failure
```

```cpp
#include "majiang/api.h"
#include "majiang/def.h"
#include "majiang/hu_util.h"
#include "majiang/ai_util.h"

// 加载预计算表(依次在当前目录、./data、../data 查找)
majiang::Load();

std::vector<int> cards = majiang::StringToCards("1万,2万,3万,东,东");
int gui = majiang::StringToCard("东");

bool isHu = majiang::IsHu(cards, gui);                  // 判断胡牌
std::vector<int> ting = majiang::IsTing(cards, {gui});  // 听牌列表
int out = majiang::OutAI(cards, {gui});                 // AI 出牌
bool isPeng = majiang::PengAI(cards, {gui}, out, 0.0);
bool isGang = majiang::GangAI(cards, {gui}, out, 0.0);
```

### 重新生成查表文件

三种实现均可离线重新生成查表文件,在输出目录下运行:

```bash
# Java: gen() 输出 majiang_clien_*.txt / majiang_server_*.txt / majiang.db / majiang_ai_*.txt
# Go:   HuGen() 输出 majiang_clien_*.txt + majiang_server_*.txt,AiGen() 输出 majiang_ai_*.txt
# C++:  HuGen() / AiGen() 与 Go 相同
```

---

## 网页端对战与算法演示平台

> 🌐 **在线体验地址**：👉 **[http://majiang.esrrhs.xyz](http://majiang.esrrhs.xyz)**

网页对战与算法演示由 **FakeLua** 实现（`server/`：对局引擎与 HTTP 在 Lua，查表与 AI 经 `RegisterNativeFunction` 复用同仓库 **`cpp/`** 库），线上经 `deploy/gq/` 以 systemd 常驻。Java / Go / C++ 仅提供纯算法库。

HTTP 接口契约见 **[server/API.md](./server/API.md)**。

```bash
# 需已编译 FakeLua 动态库（见 ../../fakelua）
cd server && mkdir -p build && cd build
cmake .. -DCMAKE_CXX_COMPILER=g++ -DCMAKE_BUILD_TYPE=Release
cmake --build . -j
cd .. && ./build/majiangserver_fakelua smoke   # 加载查表 + 算法/观战自检
./build/majiangserver_fakelua                   # HTTP 默认 :8888（可用 MAJIANG_PORT 覆盖）
```
启动后在浏览器中访问：👉 **http://localhost:8888**

## 算法文档

| 文档 | 内容 |
|------|------|
| [胡牌算法](./hu.md) | 鬼牌编码、查表结构、胡牌 & 听牌判断全流程 |
| [AI 算法](./ai.md)  | 牌面评分模型、出牌 / 碰 / 杠决策逻辑       |

---

## 相关项目

| 项目 | 描述 |
|------|------|
| [texas_algorithm](https://github.com/esrrhs/texas_algorithm) | 德州扑克算法 |
| [teenpatti_algorithm](https://github.com/esrrhs/teenpatti_algorithm) | 印度炸金花算法 |
