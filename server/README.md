# Majiang FakeLua Server

四人对局引擎、HTTP 服务、查表胡牌 / AI **全部用 FakeLua（Lua）实现，`server/` 下没有任何 C++ 代码**。
宿主生命周期（init / update / fini）与优雅退出由 fakelua 自带的 `flua` 工具负责。

查表数据（`majiang_clien_*.txt` / `majiang_ai_*.txt`，合计 1.7M 行）已转成 sqlite 库
`data/majiang.db`，按 key 点查，常驻内存只有 page cache（**约 23MB**；全量载入内存时约 145MB）。

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

# 自检：校验查表库 + 判胡/出牌 + 观战至 GAME_OVER（等价旧版的 smoke 入口）
MAJIANG_SMOKE=1 flua lua --once

# HTTP 服务（默认 :8888，可用 MAJIANG_PORT 覆盖）
MAJIANG_PORT=8888 flua lua
```

`update` 每帧只做一帧的活（`runtime.tick()` 泵 IO/定时器并分发请求），节奏由 Lua 侧
`os.sleep(TICK_INTERVAL_MS)` 自己控制——宿主不加任何节流。帧 arena 在每次 Call 入口回收，
所以不要在 Lua 里写 `while true` 把主循环包进一个 Call。
退出方式：`update` 返回非 0 / Lua 侧 `flua.set_exit(code)` / SIGTERM（都会先跑 `fini`）。

Go 旧实现参考树在 `ref_go_web/`（不参与构建）。C++ 算法参考实现与 parity 测试在 `../cpp/`（不参与服务端运行）。

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
