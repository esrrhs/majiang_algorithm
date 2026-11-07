# Mahjong AI

[<img src="https://img.shields.io/github/license/esrrhs/mahjong_ai">](https://github.com/esrrhs/mahjong_ai)
[<img src="https://img.shields.io/github/languages/top/esrrhs/mahjong_ai">](https://github.com/esrrhs/mahjong_ai)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/mahjong_ai">](https://central.sonatype.com/artifact/com.github.esrrhs/mahjong_ai)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/mahjong_ai/maven.yml?branch=master&label=java%20ci">](https://github.com/esrrhs/mahjong_ai/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/mahjong_ai/go.yml?branch=master&label=go%20ci">](https://github.com/esrrhs/mahjong_ai/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/mahjong_ai/cpp.yml?branch=master&label=c%2B%2B%20ci">](https://github.com/esrrhs/mahjong_ai/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/mahjong_ai/server.yml?branch=master&label=server%20ci">](https://github.com/esrrhs/mahjong_ai/actions)

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
server/  FakeLua web battle server (game + HTTP + lookup + AI all in Lua, tables queried per key from sqlite; see server/API.md)
data/    Pre-computed lookup tables + the cross-language parity fixture, shared by all implementations
```

All three implementations load the same table files under `data/` and are kept behaviorally in sync by tests:
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
    <artifactId>mahjong_ai</artifactId>
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
go get github.com/esrrhs/mahjong_ai/go
```

```go
package main

import (
	mahjong "github.com/esrrhs/mahjong_ai/go"
)

func main() {
	// Load pre-computed tables (looked up in ., ./data, ../data)
	mahjong.Load()

	cards := mahjong.StringToCards("1万,2万,3万,东,东")
	gui := mahjong.StringToCard("东")

	// Check if hand is a winning hand
	isHu := mahjong.IsHu(cards, gui)

	// Query which tiles complete the hand
	ting := mahjong.IsTing(cards, []int{gui})

	// AI discard / pong / kong
	out := mahjong.OutAI(cards, []int{gui})
	isPeng := mahjong.PengAI(cards, []int{gui}, pengCard, 0)
	isGang := mahjong.GangAI(cards, []int{gui}, gangCard, 0)
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
./build/mahjong_tests   # or: ctest --test-dir build --output-on-failure
```

```cpp
#include "mahjong/api.h"
#include "mahjong/def.h"
#include "mahjong/hu_util.h"
#include "mahjong/ai_util.h"

// Load pre-computed tables (looked up in ., ./data, ../data)
mahjong::Load();

std::vector<int> cards = mahjong::StringToCards("1万,2万,3万,东,东");
int gui = mahjong::StringToCard("东");

bool isHu = mahjong::IsHu(cards, gui);                 // Check if hand is a winning hand
std::vector<int> ting = mahjong::IsTing(cards, {gui}); // Waiting tiles
int out = mahjong::OutAI(cards, {gui});                // AI discard
bool isPeng = mahjong::PengAI(cards, {gui}, out, 0.0);
bool isGang = mahjong::GangAI(cards, {gui}, out, 0.0);
```

### Table Generation

To regenerate the lookup tables with either implementation, run from the output directory:

```bash
# Java: gen() writes mahjong_clien_*.txt / mahjong_server_*.txt / mahjong.db / mahjong_ai_*.txt
# Go:   HuGen() writes mahjong_clien_*.txt + mahjong_server_*.txt, AiGen() writes mahjong_ai_*.txt
```

---

## Interactive Web Platform & Algorithm Playground

> 🌐 **Live Demo Online**: 👉 **[http://mahjong.esrrhs.xyz](http://mahjong.esrrhs.xyz)**

The web battle / algorithm playground is implemented in **FakeLua** (`server/`): game engine, HTTP, lookup tables and AI are **all pure Lua**, with the tables queried per key from sqlite. Java / Go / C++ ship as pure algorithm libraries (`cpp/` also hosts the parity tests; it is not used at runtime).

HTTP API contract: **[server/API.md](./server/API.md)**. Server details: **[server/README.md](./server/README.md)**.

```bash
# Requires a built FakeLua (see ../../fakelua); flua is at build/bin/flua
python3 tools/gen_mahjong_db.py   # first run: convert lookup txt to sqlite
cd server
MAHJONG_SMOKE=1 flua lua --once   # self check: tables + hu/ting/AI + spectator to GAME_OVER
MAHJONG_PORT=8888 flua lua       # HTTP on :8888 (MAHJONG_PORT to override)
```
Visit in your browser: 👉 **http://localhost:8888**

### Server Resource Footprint

Measured on macOS arm64 (Release build of `flua`), 4-player games, HTTP over loopback:

| Metric | Value |
|--------|-------|
| Resident memory right after boot | **~24 MB** |
| Resident memory in steady state | **~29 MB** (flat — 50 games 29.0 MB, 200 games 29.3 MB) |
| Idle CPU | **~0.1 %** (tick backs off to 20 ms when no request arrives) |
| Lookup table storage | `data/mahjong.db` **32 MB** on disk, queried per key — never fully loaded into RAM |
| Concurrency model | Single-threaded event loop; saturates at one core |
| Latency, concurrency 1 (p50) | **4.3 – 6.7 ms** across all endpoints |
| Throughput, concurrency 8 | `/api/game/step` ≈ 1100 QPS, `/api/game/state` ≈ 1020 QPS, `/api/algo/test` ≈ 490 QPS |

Notes:

- Memory does **not** grow with the number of games: fakelua has no GC, but the frame arena is reset at the entry of every top-level call, so all request-scoped temporaries are recycled. The process climbs to a high-water mark during the first games and then pins.
- Beyond concurrency ~8 the bottleneck is the single-threaded event loop tick plus view serialization, not the mahjong algorithm itself.

### Server Tests

`flua` calls the fixed lifecycle of package `"main"` (`init` / `update` / `fini`), so the
one-shot self check needs no flags. The HTTP contract tests spin up the real server on a
free port and assert the same contract as the Go reference `server/ref_go_web/web_test.go`
(initial deal, ting hints, AI spectator settlement with score conservation, view JSON keys,
static assets, algorithm endpoints):

```bash
python3 server/tests/api_test.py                     # spawns the server itself
python3 server/tests/api_test.py --base-url URL      # against a running server
```

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
