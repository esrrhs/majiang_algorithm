# Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://central.sonatype.com/artifact/com.github.esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/maven.yml?branch=master&label=java%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/go.yml?branch=master&label=go%20ci">](https://github.com/esrrhs/majiang_algorithm/actions)

> High-performance Mahjong winning-hand detection & AI discard algorithm based on **lookup tables**, supporting multiple wildcard tiles (jokers/lazi).

[中文文档](./README_CN.md)

---

## Features

- **Win detection**: Millisecond-level check for winning hands, supports any number of wildcard tiles
- **Waiting-hand calculation**: Quickly lists all tiles that complete the current hand
- **AI discard**: Score-model-driven auto decision for discarding, ponging, and konging
- **Lookup table**: Offline pre-computation; runtime does hash lookups only — extremely fast
- **Full tile coverage**: Characters (Wan), Circles (Tong), Bamboo (Tiao), Wind tiles (East/South/West/North), Arrow tiles (Zhong/Fa/Bai)

---

## Repository Layout

```
java/    Java implementation (Maven project, published to Maven Central)
go/      Go implementation (Go module, behavior-aligned with the Java version)
data/    Pre-computed lookup tables, shared by both implementations
```

Both implementations load the same table files under `data/` and are kept behaviorally in sync by tests:
the algorithm unit tests are ported 1:1 between JUnit and `go test`, and
[go/testdata/parity_cases.txt](./go/testdata/parity_cases.txt) holds 2000+ fixed-seed deals whose
hu/ting/AI results are replayed and asserted by both languages
(`java/.../ParityFixtureTest.java` and `go/parity_test.go`).

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

### Table Generation

To regenerate the lookup tables with either implementation, run from the output directory:

```bash
# Java: gen() writes majiang_clien_*.txt / majiang_server_*.txt / majiang.db / majiang_ai_*.txt
# Go:   HuGen() writes majiang_clien_*.txt + majiang_server_*.txt, AiGen() writes majiang_ai_*.txt
```

---

## Interactive Web Platform & Algorithm Playground

> 🌐 **Live Demo Online**: 👉 **[http://majiang.esrrhs.xyz](http://majiang.esrrhs.xyz)**

An interactive web platform and algorithm laboratory modeled after Tencent Mahjong:

1. **4-Player Mahjong Battle (1 Human vs 3 AI / 4 AI Spectator)**:
   - Green felt table, 3D tiles, and full tile set (Wan, Tong, Tiao, Winds, Dragons).
   - Wildcard (gui / laizi) support: Random flip indicator, specified wildcard, or clean hand.
   - Draw, Discard, Chow (Chi), Pong (Peng), Kong (Gang), and Winning Hand (Hu).
2. **Real-time Ready-Hand (Ting) Detection**:
   - Shows winning tiles and remaining count in the game whenever you are in Ting.
   - Hover over hand tiles during discard to preview winning targets if discarded.
3. **💡 AI Recommendation**:
   - One-click best discard recommendation using `AIUtil.outAI` with expected score.
   - 4-AI auto-play spectator mode with variable speeds (1x ~ 10x).
4. **🧪 Algorithm Playground**:
   - Test any hand (1-14 tiles) with wildcards.
   - Measures `isHu`, `isTing`, and `outAI` execution times in microseconds (µs).

### Start Web Server

```bash
cd java
./mvnw exec:java
# or with custom port:
./mvnw exec:java -Dexec.args="--port=8080"
```
Visit in your browser: 👉 **http://localhost:8080**

### CLI 4-AI Simulation Benchmark
```bash
cd java && ./mvnw exec:java -Dexec.args="--cli"
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
