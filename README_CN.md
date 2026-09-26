# 麻将算法 · Majiang Algorithm

[<img src="https://img.shields.io/github/license/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/github/languages/top/esrrhs/majiang_algorithm">](https://github.com/esrrhs/majiang_algorithm)
[<img src="https://img.shields.io/maven-central/v/com.github.esrrhs/majiang_algorithm">](https://central.sonatype.com/artifact/com.github.esrrhs/majiang_algorithm)
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

---

## 网页端对战与算法演示平台

> 🌐 **在线体验地址**：👉 **[http://majiang.esrrhs.xyz](http://majiang.esrrhs.xyz)**

仿照经典腾讯麻将规则与仿真绿色麻将桌设计的交互式网页端对战与算法实验室：

1. **4人麻将对局体验 (1 真人 vs 3 AI / 4 AI 自动对战)**：
   - 沉浸式绿色丝绒麻将桌与精细 3D 质感麻将牌。
   - 完整支持万、筒、条、风、箭全牌型，开局翻牌定鬼（或指定白板/红中做鬼/无鬼）。
   - 完整实现摸牌、打牌、吃、碰、杠（明杠/暗杠/补杠）、胡（点炮胡/自摸胡/杠上开花）规则。
2. **实时听牌智能提示**：
   - 当手牌听牌时，界面常驻高亮显示“听牌提示面板”，罗列可胡的所有牌面及全场剩余张数。
   - 轮到出牌时，鼠标悬停或选中手牌，实时浮动提示“打出此牌听：[三筒(剩2张), 六筒(剩3张)]”。
3. **💡 AI 智能决策与推荐**：
   - 轮到出牌时一键获取 AIUtil 推荐的最优出牌与期望评分。
   - 支持一键切换全自动 4 AI 观战模式，自由调节 1x ~ 10x 对局速度。
4. **🧪 算法实验室 (Playground)**：
   - 任意点选 1~14 张手牌和鬼牌，一键执行 `HuUtil.isHu`、`HuUtil.isTing` 与 `AIUtil.outAI`，微秒级（µs）展示计算耗时与牌型结果。
   - 内置经典预设：两面听牌、单钓将听牌、七对、多鬼牌大胡、清一色等。

### 启动网页端平台

执行以下命令启动本地 Web 服务：
```bash
./mvnw exec:java
# 或指定端口:
./mvnw exec:java -Dexec.args="--port=8080"
```
启动后在浏览器中访问：👉 **http://localhost:8080**

### 命令行 4 AI 纯自动对局模拟 (Benchmark)
```bash
./mvnw exec:java -Dexec.args="--cli"
```

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
