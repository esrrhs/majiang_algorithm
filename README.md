# Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://central.sonatype.com/artifact/com.github.esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/maven.yml?branch=master&label=java%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/go.yml?branch=master&label=go%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/cpp.yml?branch=master&label=c%2B%2B%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)

> High-performance Mahjong winning-hand detection & AI discard algorithm based on **lookup tables**, supporting multiple wildcard tiles (jokers). One algorithm, three implementations — **Java / Go / C++** — kept behavior-identical by shared cross-language tests.

[中文文档](./README_CN.md)

---

## Features

- **Win detection**: Millisecond-level check for winning hands, supports any number of wildcard tiles
- **Waiting-hand calculation**: Quickly lists all tiles that complete the current hand
- **AI discard**: Score-model-driven auto decision for discarding, ponging, and konging
- **Lookup table**: Offline pre-computation; runtime does hash lookups only — extremely fast
- **Full tile coverage**: Characters (Wan), Circles (Tong), Bamboo (Tiao), Wind tiles (East/South/West/North), Arrow tiles (Zhong/Fa/Bai)
- **Three implementations**: Java, Go and C++17 load the same lookup tables under `data/`; 2000+ fixed-seed deals are replayed by every CI to pin identical behavior (including bit-exact scores)

---

## Repository Layout

```
java/    Java pure algorithm library (Maven project, published to Maven Central)
go/      Go pure algorithm library (no web server)
cpp/     C++17 pure algorithm library (CMake, behavior-aligned with the Java version)
server/  FakeLua web battle server (game + HTTP in Lua; lookup/AI via cpp/, see server/API.md)
data/    Pre-computed lookup tables + the cross-language parity fixture, shared by all implementations
```

Both implementations load the same table files under `data/` and are kept behaviorally in sync by tests:
the algorithm unit tests are ported 1:1 between JUnit, `go test` and C++ tests, and
[data/parity_cases.txt](./data/parity_cases.txt) holds 2000+ fixed-seed deals whose
hu/ting/AI results are replayed and asserted by every language
(`java/.../ParityFixtureTest.java`, `go/parity_test.go` and `cpp/test/parity_test.cpp`).

---

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.18</version>
</dependency>
```

### Win Detection / Waiting Hand

```java
// Load pre-computed tables
HuTable.load(Files.readAllLines(normalTablePath));
HuTableFeng.load(Files.readAllLines(fengTablePath));
HuTableJian.load(Files.readAllLines(jianTablePath));

// Check if hand is a winning hand
boolean isHu = HuUtil.isHu(cards, gui);

// Query which tiles complete the hand
List<Integer> tingCards = HuUtil.isTing(cards, gui);
```

### AI Discard

```java
// Load AI scoring tables
AITable.load(Files.readAllLines(normalTablePath));
AITableFeng.load(Files.readAllLines(fengTablePath));
AITableJian.load(Files.readAllLines(jianTablePath));

// Decide which tile to discard
int card = AIUtil.outAI(cards, gui);

// Decide whether to pong / kong
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
	// Load pre-computed tables (looked up in ., ./data, ../data)
	majiang.Load()

	cards := majiang.StringToCards("1万,2万,3万,东,东")
	gui := majiang.StringToCard("东")

	// Check if hand is a winning hand
	isHu := majiang.IsHu(cards, gui)

	// Query which tiles complete the hand
	ting := majiang.IsTing(cards, []int{gui})

	// AI discard / pong / kong
	out := majiang.OutAI(cards, []int{gui})
	isPeng := majiang.PengAI(cards, []int{gui}, pengCard, 0)
	isGang := majiang.GangAI(cards, []int{gui}, gangCard, 0)
}
```

Run the Go test suite (from `go/`, replays the same cases as the Java pipeline):

```bash
cd go && go test ./...
```

### C++

```bash
cd cpp
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
./build/majiang_tests   # or: ctest --test-dir build --output-on-failure
```

```cpp
#include "majiang/api.h"
#include "majiang/def.h"
#include "majiang/hu_util.h"
#include "majiang/ai_util.h"

// Load pre-computed tables (looked up in ., ./data, ../data)
majiang::Load();

std::vector<int> cards = majiang::StringToCards("1万,2万,3万,东,东");
int gui = majiang::StringToCard("东");

bool isHu = majiang::IsHu(cards, gui);                 // Check if hand is a winning hand
std::vector<int> ting = majiang::IsTing(cards, {gui}); // Waiting tiles
int out = majiang::OutAI(cards, {gui});                // AI discard
bool isPeng = majiang::PengAI(cards, {gui}, out, 0.0);
bool isGang = majiang::GangAI(cards, {gui}, out, 0.0);
```

### Table Generation

To regenerate the lookup tables with either implementation, run from the output directory:

```bash
# Java: gen() writes majiang_clien_*.txt / majiang_server_*.txt / majiang.db / majiang_ai_*.txt
# Go:   HuGen() writes majiang_clien_*.txt + majiang_server_*.txt, AiGen() writes majiang_ai_*.txt
```

---

## Interactive Web Platform & Algorithm Playground

> 🌐 **Live Demo Online**: 👉 **[http://majiang.esrrhs.xyz](http://majiang.esrrhs.xyz)**

The web battle / algorithm playground is implemented in **FakeLua** (`server/`): game engine, HTTP, lookup tables and AI are **all pure Lua**, with the tables queried per key from sqlite. Java / Go / C++ ship as pure algorithm libraries (`cpp/` also hosts the parity tests; it is not used at runtime).

HTTP API contract: **[server/API.md](./server/API.md)**.

```bash
# Requires a built FakeLua (see ../../fakelua); flua is at build/bin/flua
python3 tools/gen_majiang_db.py            # first run: convert lookup txt to sqlite
cd server
flua lua --init=MahjongServer.smoke --update= --fini=   # smoke: tables + algo/spectator
MAJIANG_PORT=8888 flua lua \               # HTTP on :8888 (MAJIANG_PORT to override)
  --init=MahjongServer.init --update=MahjongServer.update --fini=MahjongServer.fini
```
Visit in your browser: 👉 **http://localhost:8888**

## Algorithm Documentation

| Document | Content |
|----------|---------|
| [Win Detection Algorithm](./hu_en.md) | Wildcard encoding, table structure, full win-detection & waiting-hand flow |
| [AI Algorithm](./ai_en.md) | Hand scoring model, discard / pong / kong decision logic |

---

## Related Projects

| Project | Description |
|---------|-------------|
| [texas_algorithm](https://github.com/esrrhs/texas_algorithm) | Texas Hold'em algorithm |
| [teenpatti_algorithm](https://github.com/esrrhs/teenpatti_algorithm) | Teen Patti algorithm |
