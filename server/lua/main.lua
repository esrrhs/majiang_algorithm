package "main"

-- 标准生命周期。宿主 flua 固定调用 package "main" 下的这三个函数,每次调用都是
-- 独立顶层 Call(帧 arena 在入口 Reset,请求级临时内存随之回收):
--   init   :启动一次,加载查表并创建 HTTP 服务
--   update :每帧一次,泵 IO/定时器并分发 HTTP 请求
--   fini   :退出前一次(SIGTERM / flua.set_exit / update 返回非 0 都会跑到)
-- 切勿在 Lua 侧写 while true 把三者套进一个 Call(arena 永不 Reset 即内存泄露)。
--
-- 节奏由业务自己控制:宿主不加任何节流,update 返回后立刻再调一次,所以这里
-- 自己 os.sleep 一拍(单位是毫秒)。HTTP 请求在 runtime.tick() 里被分发。
local TICK_INTERVAL_MS = 1000

-- 自检:加载查表 + 判胡/出牌 + 观战步进到 GAME_OVER。
-- 用 MAJIANG_SMOKE=1 flua lua --once 触发,init 返回非 0 即失败。
local function self_check()
    MajiangTables.Load()
    local cards = MajiangDef.StringToCards("1万,1万,1万,2万,3万,4万,5万,6万,7万,8万,9万,东,东,东")
    local gui = {}
    local isHu = MajiangHu.IsHuExtra(cards, gui, 0)
    print("smoke isHu=", tostring(isHu))
    if not isHu then return 1 end
    local out = MajiangAi.OutAI(cards, gui)
    print("smoke OutAI=", MajiangDef.CardToString(out))
    MajiangGame.NewGame("none", 0, true)
    local steps = 0
    while MajiangGame.GetPhase() ~= "GAME_OVER" and steps < 500 do
        if not MajiangGame.Step() then break end
        steps = steps + 1
    end
    print("smoke spectator steps=", tostring(steps), " phase=", MajiangGame.GetPhase())
    return 0
end

function init()
    local smoke = os.getenv("MAJIANG_SMOKE")
    if smoke ~= nil and #smoke > 0 then
        return self_check()
    end

    print("=================================================")
    print(" 麻将算法 & AI 网页对战平台 (FakeLua 版)")
    print("=================================================")
    print("正在预加载查表数据...")
    local t0 = os.clock()
    MajiangTables.Load()
    print("查表加载完成, 耗时 " .. tostring(os.clock() - t0) .. " s")

    -- 默认开一局,避免 state 时空指针
    MajiangGame.NewGame("random_flip", 0, false)

    local port = 8888
    local env_port = os.getenv("MAJIANG_PORT")
    if env_port ~= nil and #env_port > 0 then
        port = math.floor(tonumber(env_port) or 8888)
    end

    local cfg = {}
    cfg["ip"] = "0.0.0.0"
    cfg["port"] = port
    local srv = http.server(cfg)
    srv:dispatch("MajiangHttp.OnRequest")
    print("HTTP 监听 :" .. tostring(port))
    print("浏览器打开 http://127.0.0.1:" .. tostring(port))
    print("--------------------------------------------------")
    return 0
end

function update()
    runtime.tick()
    os.sleep(TICK_INTERVAL_MS)
    return 0
end

function fini()
    print("MahjongServer: shutdown")
    return 0
end

-- 自检逻辑见文件头的 self_check();入口固定为 main.init,
-- 用 MAJIANG_SMOKE=1 flua lua --once 触发。
