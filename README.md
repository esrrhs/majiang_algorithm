# Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://central.sonatype.com/artifact/com.github.esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/maven.yml?branch=master">](https://github.com/esrrhs/majiang_algorithm/actions)

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
./mvnw exec:java
# or with custom port:
./mvnw exec:java -Dexec.args="--port=8080"
```
Visit in your browser: 👉 **http://localhost:8080**

### CLI 4-AI Simulation Benchmark
```bash
./mvnw exec:java -Dexec.args="--cli"
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
