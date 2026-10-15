package "MahjongServer"

function main()
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

    while true do
        runtime.tick()
        os.sleep(1)
    end
    return 0
end

-- 仅加载表 + 跑一次算法自检(CI/冒烟用)
function smoke()
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
