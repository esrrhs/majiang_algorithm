# Majiang Server HTTP API（Go 版契约）

本文档为 FakeLua 服务（`server/`）对齐用的 HTTP 接口契约（源自原 Go `go/web` 实现）。

- **实现**：`server/lua/`（`http_server.lua`、`game.lua`、`huutil.lua`、`aiutil.lua` 等）
- **前端**：`server/static/`（`app.js` 直接调用下列路径）
- **参考**：旧 Go 源码副本在 `server/ref_go_web/`
- **线上**：`http://majiang.esrrhs.xyz`（nginx → `:8888`）
- **会话模型**：进程内**单局** `current`；真人固定 **座位 0**，1/2/3 为 AI（观战模式下四人皆 AI）
- **协议**：HTTP + JSON；CORS 允许 `*`；多数写接口支持 `OPTIONS` → `204`
- **错误**：panic/异常 → `500 {"error":"..."}`；业务失败通常仍 `200`，用 `success:false` 等字段表示

---

## 1. 牌编码

与 `go/def.go` / Java `MaJiangDef` 一致（对局用 1–34，花牌 35–42 本服未入墙）：

| ID | 牌 |
|----|----|
| 1–9 | 一万 … 九万 |
| 10–18 | 一筒 … 九筒 |
| 19–27 | 一条 … 九条 |
| 28–31 | 东 南 西 北 |
| 32–34 | 中 发 白 |

中文串格式：`"1万,2万,东"`（`StringToCards` / `CardsToString`）。

---

## 2. 对局阶段 `phase`

| 值 | 含义 |
|----|------|
| `DISCARD` | 当前座位出牌（庄家开局已摸第 14 张） |
| `RESPONSE` | 他人打牌后，AI 侧响应结算中（前端一般看不到停留） |
| `WAITING_USER` | 等待座位 0 对上家打出的牌做吃/碰/杠/胡/过 |
| `GAME_OVER` | 已结算（胡或荒庄） |

---

## 3. 接口一览

| Method | Path | 作用 |
|--------|------|------|
| GET/任意 | `/api/status` | 健康检查 |
| POST | `/api/game/new` | 新开一局，返回完整 `GameStateView` |
| GET/任意 | `/api/game/state` | 拉取当前局视图 |
| POST | `/api/game/spectator` | 切换观战（4 AI）模式 |
| POST | `/api/game/discard` | 座位 0 出牌 |
| POST | `/api/game/action` | 座位 0 对他人打牌响应 |
| POST | `/api/game/self_action` | 座位 0 自摸回合：自摸胡 / 暗杠补杠 |
| POST | `/api/game/step` | 推进 AI 一步 |
| POST | `/api/game/auto_run` | 连续推进最多 N 步 |
| POST | `/api/algo/test` | 算法实验室（不改对局状态） |
| GET | `/`、`/*` | 静态资源（`index.html`、`app.js`、`tiles/*.svg` 等） |

---

## 4. 各接口详情

### 4.1 `GET /api/status`

**响应**
```json
{ "status": "ok", "ready": true }
```

---

### 4.2 `POST /api/game/new`

**请求体**
```json
{
  "guiMode": "random_flip",
  "customGuiCard": 0,
  "spectator": false
}
```

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `guiMode` | string | `"random_flip"` | 鬼牌规则，见下表 |
| `customGuiCard` | int | `0` | `guiMode=="card"` 时指定鬼牌 ID（1–34） |
| `spectator` | bool | `false` | `true`=四人皆 AI，可 auto step |

**`guiMode`**

| 值 | 行为 |
|----|------|
| `random_flip` / `""` | 翻指示牌，鬼=下一张（如翻 5 万则鬼为 6 万） |
| `card` | 鬼=`customGuiCard` |
| `bai` | 白板当鬼 |
| `zhong` | 红中当鬼 |
| `none` 及其它 | 无鬼 |

**响应**：完整 [`GameStateView`](#5-gamestateview)（HTTP 200，非 `{view:...}` 包装）。

前端实际只传 `guiMode` + `spectator`（radio：`random_flip` / `bai` / `zhong` / `none`）。

---

### 4.3 `GET /api/game/state`

**响应**：当前局 [`GameStateView`](#5-gamestateview)。

---

### 4.4 `POST /api/game/spectator`

**请求体**
```json
{ "spectator": true }
```

**响应**：更新后的 [`GameStateView`](#5-gamestateview)。

---

### 4.5 `POST /api/game/discard`

座位 0 在 `phase==DISCARD` 且 `currentSeat==0` 时出牌。

**请求体**
```json
{ "card": 9 }
```

**响应**
```json
{ "success": true, "view": { /* GameStateView */ } }
```

---

### 4.6 `POST /api/game/action`

座位 0 在 `phase==WAITING_USER` 时响应上家打出的牌。

**请求体**
```json
{
  "action": "chi",
  "card": 0,
  "chi1": 10,
  "chi2": 11
}
```

| 字段 | 说明 |
|------|------|
| `action` | `"hu"` / `"peng"` / `"gang"` / `"chi"` / `"pass"` |
| `card` | 杠时可选目标牌；吃/碰/胡/过可传 `0` |
| `chi1`,`chi2` | 吃时手中两张牌的 ID（与 `lastDiscard` 组成顺子） |

**响应**：`{ "success": bool, "view": GameStateView }`

---

### 4.7 `POST /api/game/self_action`

座位 0 在自己出牌回合：自摸胡或暗杠/补杠。

**请求体**
```json
{ "action": "gang", "card": 1 }
```

| `action` | 说明 |
|----------|------|
| `"hu"` | 自摸胡 |
| `"gang"` | 暗杠或补杠，`card` 为杠牌 ID |

**响应**：`{ "success": bool, "view": GameStateView }`

---

### 4.8 `POST /api/game/step`

推进 AI / 自动逻辑一步（观战或 AI 座位时使用）。

**请求体**：可空。

**响应**
```json
{ "stepped": true, "view": { /* GameStateView */ } }
```

`stepped=false` 表示无法再推进（例如在等真人，或已 `GAME_OVER`）。

---

### 4.9 `POST /api/game/auto_run`

**请求体**
```json
{ "maxSteps": 10 }
```

| 字段 | 默认 | 说明 |
|------|------|------|
| `maxSteps` | `10` | 最多连续 `Step()` 次数 |

**响应**
```json
{ "executedSteps": 7, "view": { /* GameStateView */ } }
```

---

### 4.10 `POST /api/algo/test`

算法实验室，**不修改**当前对局。

**请求体**（`cards` / `guiCards` 可为 int 数组或中文串）
```json
{
  "cards": [1,1,1,2,3,4,5,6,7,8,9,28,28,28],
  "guiCards": [],
  "gui": 0
}
```

兼容：`guiCards` 为空且 `gui>0` 时当作单鬼。

**响应示例**
```json
{
  "cards": [1,1,1,2,3,4,5,6,7,8,9,28,28,28],
  "cardsStr": "1万,1万,1万,2万,3万,4万,5万,6万,7万,8万,9万,东,东,东,",
  "guiCards": [],
  "isHu": true,
  "huTimeMicros": 762.241,
  "tingCards": [],
  "tingCardsStr": "",
  "tingTimeMicros": 1972.055,
  "recommendedOut": 9,
  "recommendedOutStr": "9万",
  "aiScore": 60,
  "aiTimeMicros": 4602.042
}
```

规则：
- `isHu`：仅当 `len(cards)%3==2` 时调用 `IsHuExtra`
- `tingCards`：`IsTingExtra`
- `recommendedOut` / `aiScore`：仅当 `len(cards)%3==2` 时 `OutAI` + 打出后 `Calc`

---

### 4.11 静态资源 `GET /*`

从 embed 的 `static/` 提供文件；`/` → `index.html`。禁止 `..` 路径。

---

## 5. `GameStateView`

对局相关接口的核心载荷（`new` / `state` / `spectator` 直接返回；其余包在 `view` 里）。

```json
{
  "gameId": 1001,
  "wallCount": 83,
  "guiCards": [23],
  "guiIndicator": 22,
  "dealerSeat": 0,
  "currentSeat": 0,
  "phase": "DISCARD",
  "lastDiscard": 0,
  "lastDiscardSeat": -1,
  "players": [ /* PlayerView x4 */ ],
  "availableActions": { /* AvailableActions */ },
  "currentTingTargets": [ { "card": 5, "remainingCount": 2 } ],
  "discardToTing": {
    "9": [ { "card": 5, "remainingCount": 2 } ]
  },
  "aiRecommendation": { /* 可省略 */ },
  "logs": [ "本局鬼牌(癞子)为: 【五条】" ],
  "settlement": { /* 可省略，GAME_OVER 时有 */ },
  "cardRemainCounts": { "1": 3, "2": 4, "...": 0 },
  "spectatorMode": false
}
```

### 5.1 `PlayerView`

| 字段 | 类型 | 说明 |
|------|------|------|
| `seat` | int | 0–3 |
| `name` | string | 如 `"玩家 (南)"` / `"AI-东"` |
| `isAi` | bool | |
| `tileCount` | int | 手牌张数 |
| `hand` | int[] | **始终下发完整手牌**（明暗由前端控制） |
| `melds` | Meld[] | 吃碰杠副露 |
| `discards` | int[] | 弃牌河 |
| `lastDrawnCard` | int | 刚摸的牌，无则 0 |
| `isTing` / `isHu` | bool | |
| `score` | int | 初始 1000 |

### 5.2 `Meld`

| 字段 | 说明 |
|------|------|
| `type` | `CHI` / `PENG` / `MING_GANG` / `AN_GANG` / `BU_GANG` |
| `triggerCard` | 触发牌 |
| `fromSeat` | 来源座位；暗杠为 `-1` |
| `cards` | 3 或 4 张牌 ID |

### 5.3 `AvailableActions`

| 字段 | 说明 |
|------|------|
| `canDiscard` | 可否出牌 |
| `canHu` / `canPeng` / `canGang` / `canChi` / `canPass` | 按钮开关 |
| `gangCards` | 可杠的牌 ID 列表 |
| `chiOptions` | `[[chi1,chi2], ...]` 手中两张与 `lastDiscard` 成顺 |

### 5.4 `AIRecommendation`（可 `omitempty`）

| 字段 | 说明 |
|------|------|
| `recommendedAction` | `discard` / `peng` / `gang` / `hu` / `chi` / `pass` |
| `recommendedDiscard` | 推荐打出的牌（discard 时） |
| `recommendedCard` | 推荐相关单牌 |
| `recommendedCards` | 相关牌列表 |
| `score` | 浮点评分 |
| `reason` | 中文说明 |

### 5.5 `SettlementInfo`（可 `omitempty`）

| 字段 | 说明 |
|------|------|
| `isDraw` | 荒庄 |
| `winnerSeat` | 赢家座位 |
| `providerSeat` | 点炮者；自摸为 `-1` |
| `isZimo` | 是否自摸 |
| `winCard` | 胡的那张牌 |
| `patterns` | 番型字符串列表 |
| `totalFan` / `points` | 番数 / 得分 |
| `scoreDeltas` | `map[string]int`，key 为座位号字符串 |

### 5.6 其它 map 字段

- `discardToTing`：`map[string][]TingTarget`，key=打出牌 ID 字符串  
- `cardRemainCounts`：`map[string]int`，key=`"1"`…`"34"`，值为场上剩余可见估计（4−已曝光）

`TingTarget`：`{ "card": int, "remainingCount": int }`

---

## 6. 典型调用时序（前端）

1. `POST /api/game/new` → 渲染  
2. 若观战：循环 `POST /api/game/step` 或 `auto_run`  
3. 人机：`phase==DISCARD && currentSeat==0` → `discard` 或 `self_action`  
4. `phase==WAITING_USER` → `action`（吃碰杠胡过）  
5. AI 回合：`step` / 轮询 `state`  
6. `phase==GAME_OVER` → 读 `settlement`  
7. 实验室页独立调用 `algo/test`，与对局无关  

---

## 7. 实现注意（fakelua）

1. **路径与 JSON 字段名保持不变**，现有 `static/app.js` 可直接复用。  
2. 必须实现同一套牌 ID、阶段名、动作字符串。  
3. 单进程单局即可；若改为多会话，需另加 `gameId`/session，前端也要改。  
4. 对局 / HTTP 在 Lua；胡牌与 AI 经 `native_majiang.cpp` 调用同仓库 `cpp/` 库（与 Go/Java 查表行为一致）。  
5. 查表数据：`data/majiang_clien_*.txt`、`majiang_ai_*.txt`（及 feng/jian 变体），由 `majiang::Load()` 加载。  
6. 冒烟：`MAJIANG_SMOKE=1 flua lua --once`（校验查表库 + IsHu/OutAI + 观战至 GAME_OVER）。  

---

## 8. 源码索引

| 内容 | 文件 |
|------|------|
| 路由与 handler | `server/lua/http_server.lua` |
| 对局规则引擎 | `server/lua/game.lua` |
| 胡 / 听 | `server/lua/huutil.lua` |
| AI | `server/lua/aiutil.lua` |
| 番型结算 | `server/lua/fan.lua` |
| 查表加载入口 | `server/lua/tables.lua` 校验 `data/majiang.db`（由 `tools/gen_majiang_db.py` 生成）|
| C++ 算法桥 | `server/native_majiang.cpp` + `cpp/` |
| 入口 | `server/lua/main.lua` + `server/main.cpp` |
| 前端调用 | `server/static/app.js` |
| 旧 Go 参考 | `server/ref_go_web/` |
