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
--
-- 节拍直接决定 HTTP 延迟:Boost.Beast 的一个请求要跨三拍才走完
-- (收请求 -> 派发 -> 把响应泵出去),1000ms 一拍就是 3s 一个请求。
-- 平时心跳也不该把 CPU 跑满,所以空闲时退避到 IDLE_TICK_INTERVAL_MS,
-- 有请求来的那几拍再回到 1ms —— 由 MajiangHttp 的活跃计数器驱动。
local BUSY_TICK_INTERVAL_MS = 1
local IDLE_TICK_INTERVAL_MS = 20
-- 收到请求后保持多少拍的"忙碌"节拍(够把一次请求的三拍走完)
local BUSY_TICKS = 16

-- 文件级 local 落在常量区不可写,可变节拍状态走 container.map(C++ 堆)
local pace = container.map()

-- 自检:加载查表 + 判胡/出牌 + 观战步进到 GAME_OVER 且分数守恒。
-- 用 MAJIANG_SMOKE=1 flua lua --once 触发,init 返回非 0 即失败(flua 会把退出码透出去)。
local function self_check()
    MajiangTables.Load()

    -- 1) 判胡:111万 234万 567万 789万 东东东
    local cards = MajiangDef.StringToCards("1万,1万,1万,2万,3万,4万,5万,6万,7万,8万,9万,东,东,东")
    local gui = {}
    local isHu = MajiangHu.IsHuExtra(cards, gui, 0)
    print("smoke isHu=", tostring(isHu))
    if not isHu then return 1 end

    -- 2) 听牌:13 张听东 / 听 1、4、7 万
    local ting13 = MajiangHu.IsTingExtra({1,2,3,4,5,6,7,8,9,10,11,12,28}, {})
    if #ting13 ~= 1 or ting13[1] ~= 28 then
        print("smoke ting13 size=", tostring(#ting13))
        return 1
    end

    -- 3) AI 出牌:14 张手牌必须给出合法建议
    local out = MajiangAi.OutAI(cards, gui)
    print("smoke OutAI=", MajiangDef.CardToString(out))
    if out < 1 or out > 34 then return 1 end

    -- 4) 观战跑完整局,结算分数守恒(4 * 1000)
    MajiangGame.NewGame("none", 0, true)
    local steps = 0
    while MajiangGame.GetPhase() ~= "GAME_OVER" and steps < 2000 do
        if not MajiangGame.Step() then break end
        steps = steps + 1
    end
    local phase = MajiangGame.GetPhase()
    print("smoke spectator steps=", tostring(steps), " phase=", phase)
    if steps <= 0 or phase ~= "GAME_OVER" then return 1 end

    local view = MajiangGame.CreateView()
    if view.settlement == nil then
        print("smoke settlement missing")
        return 1
    end
    local total = 0
    local i = 1
    while i <= #view.players do
        total = total + view.players[i].score
        i = i + 1
    end
    print("smoke score total=", tostring(total))
    if total ~= 4000 then return 1 end

    -- 5) JSON 编码契约:字符串键表必须是对象,空表在 fakelua 里是 []
    --    (Go 版是 {};前端用 map[card] 取值,两者等价)
    local as_obj = json.encode({ ["9"] = {{ card = 5, remainingCount = 2 }} })
    if not string.find(as_obj, '"9"', 1, true) then
        print("smoke json object encode broken: ", as_obj)
        return 1
    end

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

-- 自适应节拍:请求活跃期用 1ms 保证响应及时,空闲期退避到 20ms 省 CPU。
-- 破平手段是"有请求就认为接下来还要继续忙 BUSY_TICKS 拍",这样一次请求的三拍
-- (收 -> 派发 -> 回包)全都落在快节拍里。
function update()
    runtime.tick()

    local seen = MajiangHttp.ServedCount()
    if seen ~= pace:get("last_seen") then
        pace:set("last_seen", seen)
        pace:set("busy_left", BUSY_TICKS)
    end

    local ms = IDLE_TICK_INTERVAL_MS
    local busy_left = pace:get("busy_left")
    if busy_left ~= nil and busy_left > 0 then
        ms = BUSY_TICK_INTERVAL_MS
        pace:set("busy_left", busy_left - 1)
    end
    os.sleep(ms)
    return 0
end

function fini()
    print("MahjongServer: shutdown")
    return 0
end

-- 自检逻辑见文件头的 self_check();入口固定为 main.init,
-- 用 MAJIANG_SMOKE=1 flua lua --once 触发。
