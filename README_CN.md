# 麻将算法 · Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://search.maven.org/artifact/com.github.esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/actions/workflow/status/esrrhs/majiang_algorithm/maven.yml?branch=master">](https://github.com/esrrhs/majiang_algorithm/actions)

> 高性能麻将胡牌 & AI 出牌算法，基于**查表法**实现，支持多张鬼牌（癞子）。

[English Documentation](./README.md)

---

## 特性

- **胡牌判断**：毫秒级判断是否胡牌，支持任意数量鬼牌
- **听牌计算**：快速列出当前手牌能胡的所有牌
- **AI 出牌**：评分模型驱动，自动决策出牌、碰牌、杠牌
- **查表法**：离线预计算，运行时仅做哈希查找，性能极高
- **覆盖全牌型**：万、筒、条、风牌（東南西北）、箭牌（中發白）

---

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.16</version>
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

---

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
