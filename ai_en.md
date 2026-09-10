# AI Discard Algorithm

[中文文档](./ai.md) | [GitHub](https://github.com/esrrhs/majiang_algorithm)

# Overview

Playing Mahjong well requires both luck and skill. A player builds their hand tile by tile, working toward a waiting state and ultimately a winning hand. This document describes how an AI can play Mahjong as intelligently as possible. Because drawing tiles is outside our control, the AI focuses entirely on which tile to discard.

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
AITableJian.load(Files.readAllLines(xxx));
AITableFeng.load(Files.readAllLines(xxx));
AITable.load(Files.readAllLines(xxx));
// Discard
int card = AIUtil.outAI(cards, gui);
// Pong
boolean isPeng = AIUtil.pengAI(cards, gui, pengCard, 0.d);
// Kong
boolean isGang = AIUtil.gangAI(cards, gui, gangCard, 0.d);
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

A wildcard tile can substitute for any tile. In this context we do not need to worry about discarding wildcards, because keeping them in the hand always improves the probability of winning. The strategy is to make the non-wildcard tiles as strong as possible, then let wildcards fill in the remaining gaps to reach a waiting or winning state.

# Case Studies

Here are a few intuitive examples of human decision-making when discarding:

- 1-Wan 2-Wan 3-Wan 2-Tiao: Straightforward — discard the isolated 2-Tiao; the rest already forms a sequence.<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai1.png)
- 1-Wan 2-Wan 3-Wan 1-Tiao 1-Tiao 7-Tiao: Use 1-Tiao as the pair and discard the isolated 7-Tiao.<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai2.png)
- 1-Wan 2-Wan 3-Wan 2-Tong 3-Tong 1-Tiao 2-Tiao: To form sequences you need 3-Tiao, 1-Tong, or 4-Tong. Breaking up 1-Tiao 2-Tiao, discarding 1-Tiao is better — 2-Tiao still has a chance if you later draw 3-Tiao or 4-Tiao.<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai3.png)


# Solution Approach

The examples above show that choosing a discard is really about evaluating the resulting hand and selecting the best outcome.<br />
The algorithm therefore reduces to **scoring a hand** — the higher the score, the stronger the hand and the greater the probability of winning.

## Scoring Method

To score a hand, we simulate drawing N additional tiles and observe how often the hand wins. Intuitively: 1-Wan 2-Wan 3-Wan > 1-Wan 2-Wan 3-Wan 2-Tiao 3-Tiao > 1-Wan 2-Wan 3-Wan 2-Tiao.

- **1-Wan 2-Wan 3-Wan**<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai4.png)<br />
Already won — win rate = 1.
- **1-Wan 2-Wan 3-Wan 2-Tiao**<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai1.png)<br />
Drawing 1 tile: wins only when drawing 2-Tiao. Win rate = 1/9 × (probability of drawing Bamboo). A pair is present.
- **1-Wan 2-Wan 3-Wan 2-Tiao 3-Tiao**<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai5.png)<br />
Drawing 1 tile: wins when drawing 1-Tiao or 4-Tiao. Win rate = 2/9 × (probability of drawing Bamboo). No pair.


## Table Generation

Using the scoring method above, we pre-compute for each suit and each hand the win rate under the has-pair and no-pair conditions, and store the results in a lookup table. This avoids repeating the expensive simulation at runtime.<br />
A known trade-off: the table does not account for which tiles remain on the table, but the performance gain makes this acceptable.

## Tile Encoding

The first step is to encode a hand into a lookup key.

- Group tiles by suit, as shown below<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
Characters (Wan) separated out:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)
- Convert 1-Wan, 2-Wan, 5-Wan, 5-Wan into the 9-digit number `110020000` — the M-th digit from the left equals the count of M-Wan tiles
- This produces five numeric keys for Characters, Circles, Bamboo, Wind, and Arrow tiles respectively

## Table Generation Details

At generation time, compute time is cheap, so we enumerate exhaustively.

- Build three tables: Regular, Wind, and Arrow
- Enumerate all valid keys — for the Regular table: `000000000` through `444200000` (max 4 of any tile, at most 14 tiles total)
- For each key, given N additional draw tiles, compute the win rate under both has-pair and no-pair conditions
- Example — 1-Wan, 2-Wan, 5-Wan, 5-Wan → key `110020000`:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)<br />
Generated entries:
  - No pair: 0.0061 (win probability when this sub-hand does not supply the pair)
  - Has pair: 0.0343 (win probability when this sub-hand supplies the pair — the existing 5-Wan pair means only 3-Wan is needed)

## Scoring Algorithm

With the pre-computed tables, evaluating a hand is straightforward.

- Encode the player's hand into multiple keys and a total wildcard count N. Example hand:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
Keys: `110020000`, `020000000`; wildcard count: 2. Also add keys for suits with no tiles.
- Look up each key to retrieve the win-rate entries (has-pair and no-pair)
- The example yields:
  - 1-Wan 2-Wan 5-Wan 5-Wan: no-pair 0.006 / has-pair 0.03
  - 2-Tong 2-Tong: no-pair 0.02 / has-pair 1.0
  - Bamboo (empty): no-pair 1.0 / has-pair 0.05
  - Wind (empty): no-pair 1.0 / has-pair 0.05
  - Arrow (empty): no-pair 1.0 / has-pair 0.05
- Recurse over the suits, maximizing the total win-rate sum subject to the constraint that exactly one suit supplies the pair. In this example the pair is assigned to Circles: max = 4.006<br />
  Recursive cost: distribute wildcards and the pair across M suits (M ≤ 5).

## Discard Algorithm

- Iterate over all non-wildcard tiles in the hand
- For each candidate, compute the maximum hand score after discarding it
- The tile whose removal produces the highest score is the one to discard
- If discarding a tile puts the hand in a waiting state, prefer the discard that yields the most waiting tiles
- Example hand:<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai6.png)<br />
  - Discarding 2-Wan or 3-Wan → score 3.02
  - Discarding 1-Tiao → score 2.07
  - Discarding East → score 4.02
  - Best discard: East
