# Majiang FakeLua Server

四人对局引擎、HTTP 服务、查表胡牌 / AI **全部用 FakeLua（Lua）实现，`server/` 下没有任何 C++ 代码**。
宿主生命周期（init / update / fini）与优雅退出由 fakelua 自带的 `flua` 工具负责。

查表数据（`majiang_clien_*.txt` / `majiang_ai_*.txt`，合计 1.7M 行）已转成 sqlite 库
`data/majiang.db`（32MB），按 key 点查，不全量载入内存。

## 资源占用

实测环境：macOS arm64 + `flua` Release 构建，四人对局，HTTP 走 loopback。

| 指标 | 数值 |
|------|------|
| 启动后常驻内存（RSS） | 约 24 MB |
| 稳态常驻内存 | 约 29 MB（不再增长：50 局 29.0 MB，200 局 29.3 MB） |
| 空闲 CPU | 约 0.1%（无请求时节拍退避到 20ms） |
| 查表库 | `data/majiang.db` 磁盘 32 MB，按 key 点查 |
| 并发模型 | 单线程事件循环，最多吃满 1 核 |
| 并发 1 延迟（p50） | 4.3 – 6.7 ms（各接口） |
| 并发 8 吞吐 | `game/step` ≈ 1100 QPS，`game/state` ≈ 1020 QPS，`algo/test` ≈ 490 QPS |

两点值得留意：

- **内存不随对局数增长**。fakelua 没有 GC，但帧 arena 在每次顶层 Call 入口 Reset，
  请求级临时对象随调用结束回收；进程只在前若干局爬到高水位，之后封顶。
  因此不要在 Lua 里写 `while true` 把主循环包进一个 Call（arena 永不 Reset 即泄漏），
  也不要把缓存之类的可变状态挂到 `container.map()`（那是 C++ 堆，没有 GC，会永久驻留）。
- **并发超过约 8 之后瓶颈不在算法**：是单线程事件循环的节拍与 view 序列化。
  单次决策内的重复 sqlite 点查已按花色做单槽复用（打出一张牌只让一个花色的 key 变化），
  详见 `lua/aiutil.lua` 与 `lua/huutil.lua` 里 `fetch` 的 `slot / ck / cv` 参数。

## 依赖

- 已编译的 [fakelua](../../fakelua)，其中 `flua` 在 `build/bin/flua`
- Python 3（仅生成查表库时需要）

## 生成查表库（txt → sqlite）

```bash
python3 tools/gen_majiang_db.py              # 生成 data/majiang.db（约 32MB）
python3 tools/gen_majiang_db.py server/data  # server 用的那份（也可把 server/data 软链到 ../data）
```

## 运行（无构建步骤）

宿主 `flua` 固定调用 `package "main"` 下的 `init` / `update` / `fini`，所以**不需要任何参数**：

```bash
cd server

# 自检：校验查表库 + 判胡/出牌 + 观战至 GAME_OVER
MAJIANG_SMOKE=1 flua lua --once

# HTTP 服务（默认 :8888，可用 MAJIANG_PORT 覆盖）
MAJIANG_PORT=8888 flua lua
```

`update` 每帧只做一帧的活（`runtime.tick()` 泵 IO/定时器并分发请求），节奏由 Lua 侧
`os.sleep()` 自己控制——宿主不加任何节流。
退出方式：`update` 返回非 0 / Lua 侧 `flua.set_exit(code)` / SIGTERM（都会先跑 `fini`）。

**节拍决定延迟**：Boost.Beast 的一个 HTTP 请求要跨三拍才走完（收请求 → 派发 → 把响应泵出去），
所以 `main.lua` 用自适应节拍——请求活跃期 1ms 一拍，空闲期退避到 20ms（空闲 CPU ≈ 0.1%）。

Go 参考实现在 `ref_go_web/`（不参与构建）。C++ 算法参考实现与 parity 测试在 `../cpp/`（不参与服务端运行）。

## 包结构（`lua/`）

| 文件 | 包名 | 职责 |
|------|------|------|
| `def.lua` | MajiangDef | 牌编号与字符串互转 |
| `tables.lua` | MajiangTables | 校验 sqlite 查表库就绪 |
| `huutil.lua` | MajiangHu | 胡 / 听（查 sqlite 判胡表） |
| `aiutil.lua` | MajiangAi | 出牌 / 碰 / 杠 / 吃 AI（查 sqlite AI 表） |
| `fan.lua` | MajiangFan | 番型结算 |
| `game.lua` | MajiangGame | 四人对局（状态 serialize 进 map） |
| `http_server.lua` | MajiangHttp | HTTP 路由 |
| `main.lua` | main | 生命周期入口 `init` / `update` / `fini`（宿主固定调用）|

服务端没有 C++ 代码：`flua` 负责生命周期，查表经 fakelua 自带的 sqlite 模块读取。

## 测试

```bash
# 一次性自检：查表库就绪 + 判胡/听牌/AI 出牌 + 观战推进到 GAME_OVER 且四人分数守恒(4×1000)
cd server && MAJIANG_SMOKE=1 flua lua --once

# HTTP 契约测试：自己拉起服务(随机端口)后打真接口
python3 server/tests/api_test.py
python3 server/tests/api_test.py --base-url URL   # 打已经跑着的服务
```

`api_test.py` 只依赖标准库，覆盖的契约与 Go 参考实现
`ref_go_web/web_test.go` 一致：起手发牌、听牌提示、AI 观战结算与分数守恒、
view JSON 键齐全、静态资源（含目录穿越被挡）、算法接口与脏输入容错。
CI 见 `.github/workflows/server.yml`。
