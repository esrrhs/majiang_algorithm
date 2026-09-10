# Win Detection Algorithm

[中文文档](./hu.md) | [GitHub](https://github.com/esrrhs/majiang_algorithm)

# Overview

Mahjong is a beloved game enjoyed across many regions, each with its own local rules. Most variants include wildcard tiles — known as **jokers** or **lazi** (癞子) — that can substitute for any tile. This document explains the winning-hand detection algorithm for hands containing multiple wildcard tiles.

# Usage

## Maven

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.15</version>
</dependency>
```

```java
// Load tables
HuTableJian.load(Files.readAllLines(xxx));
HuTableFeng.load(Files.readAllLines(xxx));
HuTable.load(Files.readAllLines(xxx));
// Check winning hand
boolean isHu = HuUtil.isHu(cards, gui);
// Query waiting tiles
List<Integer> tingCards = HuUtil.isTing(cards, gui);
```

# Tile Categories

- **Regular tiles**: Characters (Wan), Circles (Tong), Bamboo (Tiao) — each suit contains tiles numbered 1–9, four copies each<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/wan.png)
- **Wind tiles**: East, South, West, North<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/feng.png)
- **Arrow tiles**: Zhong (Red Dragon), Fa (Green Dragon), Bai (White Dragon)<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/jian.png)


# Terminology

- **Sequence (Lianzi)**: Three consecutive tiles of the same suit, e.g. 1-2-3 Wan<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/lianzi.png)
- **Triplet (Kezi)**: Three identical tiles, e.g. three 1-Tong<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/kezi.png)
- **Pair (Jiang)**: Two identical tiles, e.g. two 1-Tiao<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/jiang.png)


# Winning Formula

- N × Sequence + M × Triplet + 1 × Pair
- N ≥ 0, M ≥ 0<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/hu.png)

# Wildcard Tiles

A wildcard tile can substitute for any tile. It is usually designated in advance or determined randomly each round. For example, if Bai (White Dragon) is declared as the wildcard:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/gui.png)<br />
Here the White Dragon substitutes for **3-Wan**, completing the winning hand.

# The Problem

Players experienced with Mahjong know that with a small number of wildcards it is fairly straightforward to judge winning hands. With many wildcards it becomes much harder. For example, when another player discards a **6-Tiao**, does this hand win?<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ting.png)<br />
The tiles that actually complete this hand are:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/tingde.png)<br />
The same difficulty applies to software. A naive brute-force enumeration of all wildcard substitution possibilities produces very poor performance. This document aims to optimize the win-detection algorithm.

# Solution Approach

Since brute-force enumeration is too slow, we pre-compute all possible winning outcomes for every hand and store them in a lookup table — trading space for time.<br />
To keep the data volume manageable, we build separate tables per suit. Since Characters, Circles, and Bamboo share the same structure, a single table covers all three.

## Tile Encoding

The first step is to encode a hand into a lookup key.

- Group tiles by suit, as shown below<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
Characters (Wan) separated out:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)
- Convert 1-Wan, 2-Wan, 5-Wan, 5-Wan into the 9-digit number `110020000` — the M-th digit from the left equals the count of M-Wan tiles
- This produces five numeric keys for Characters, Circles, Bamboo, Wind, and Arrow tiles respectively

## Table Generation

At generation time, compute time is cheap, so we enumerate exhaustively.

- Build three tables: Regular, Wind, and Arrow
- Enumerate all valid keys — for the Regular table: `000000000` through `444420000` (max 4 of any tile, at most 14 tiles total)
- For each key, generate a list of winning-hand info covering all wildcard counts
- Each entry in the list records: given N wildcards, which tiles complete this key, and whether there is a pair
- Example — 1-Wan, 2-Wan, 5-Wan, 5-Wan → key `110020000`:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)<br />
Generated entries:
  - 0 wildcards, **has pair**, wins on **3-Wan** (wildcards represent 3-Wan; a pair exists)
  - 1 wildcard, **no pair**, wins on **3-Wan or 5-Wan** (wildcard can be 3-Wan or a second 5-Wan; no pair)
  - 1 wildcard, **has pair**, **already won** (wildcard becomes 3-Wan; pair exists)
  - … and so on

## Win Detection Algorithm

With the pre-computed tables, win detection is straightforward.

- Encode the player's hand into multiple keys and a total wildcard count N. Example hand:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
Keys: `110020000`, `020000000`; wildcard count: 2
- Look up each key in the table to retrieve the corresponding win-info list
- Filter each list: discard entries whose required wildcard count exceeds N, and entries with no winning tile. The example yields:
  - 1-Wan 2-Wan 5-Wan 5-Wan: 1 wildcard, has pair, already won
  - 2-Tong 2-Tong: 1 wildcard, no pair, already won
- Recurse over the filtered lists to check whether any combination satisfies: total wildcards ≤ N and exactly one pair
- If any valid combination exists, the hand wins. In this example the constraints are satisfied, so the hand wins
- Total cost: (table lookup) × M + (recursive distribution of wildcards and pair across M suits)<br />
  M = number of suits, M ≤ 5

## Waiting-Hand Query

- Similar to win detection: retrieve win-info lists by key
- Recurse to find the union of all winning tiles appearing in entries that do not yet win on their own, subject to the wildcard and single-pair constraints — this union is the set of tiles that complete the hand
- Optimization: once it is known that 1-Wan, 2-Wan, 3-Wan all complete the hand, skip recursing on the subset (e.g., only 2-Wan)

# Notes

- Both the frontend and backend of the current project use this algorithm. The frontend reads the table from SQLite; the backend loads the text file into an in-memory HashMap.
- The rules can be modified and the tables regenerated — for instance, some regional variants treat East-West-South as a valid sequence.
