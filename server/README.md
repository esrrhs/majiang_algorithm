# Majiang FakeLua Server

四人对局引擎与 HTTP 服务用 **FakeLua（Lua）** 实现；查表胡牌 / AI 复用仓库内 **`cpp/`** 算法库，经 `RegisterNativeFunction` 桥接（与 FakeLua「大数据驻留 C++」设计一致）。协议与前端保持原 Go `web` 契约，见 [API.md](./API.md)。

## 依赖

- 已编译的 [fakelua](../../fakelua)（`build/lib64/libfakelua.so`）
- GCC 支持 C++23（与 fakelua 一致，例如 GCC 16）
- `data/` 查表文件（仓库根目录；`server/data` 可软链到 `../data`）
- 同仓库 `cpp/` 静态库（CMake `add_subdirectory` 自动编入）

## 构建与运行

```bash
mkdir -p build && cd build
cmake .. -DCMAKE_CXX_COMPILER=/path/to/g++ -DCMAKE_BUILD_TYPE=Release
cmake --build . -j
cd ..
./build/majiangserver_fakelua smoke   # 冒烟：Load + IsHu/OutAI + 观战至 GAME_OVER
MAJIANG_PORT=8888 ./build/majiangserver_fakelua
```

Go 旧实现参考树在 `ref_go_web/`（不参与构建）。部署见仓库 `deploy/gq/`。

## 包结构（`lua/`）

| 文件 | 包名 | 职责 |
|------|------|------|
| `def.lua` | MajiangDef | 牌编号与字符串互转 |
| `tables.lua` | MajiangTables | 调用 `majiang_load()` 加载 cpp 查表 |
| `huutil.lua` | MajiangHu | 胡 / 听（转发 C++） |
| `aiutil.lua` | MajiangAi | 出牌 / 碰 / 杠 / 吃 AI（转发 C++） |
| `fan.lua` | MajiangFan | 番型结算 |
| `game.lua` | MajiangGame | 四人对局（状态 serialize 进 map） |
| `http_server.lua` | MajiangHttp | HTTP 路由 |
| `main.lua` | MahjongServer | 入口 `main` / `smoke` |

C++ 宿主：`main.cpp` + `native_majiang.cpp`（注册 `majiang_*` native）。
