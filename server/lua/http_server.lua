package "MajiangHttp"

local static_root = "static"

-- 已派发的请求数。文件级 local 落在常量区不可写,计数放 container.map(C++ 堆)。
-- main.lua 的 update 靠它判断"现在忙不忙",从而在 1ms / 20ms 两档节拍间切换。
local served = container.map()

-- 注意:静态资源也计数。页面加载会连着拉十几个 css/js/svg,
-- 把它们算进来才不会在加载中途掉回慢节拍。
function ServedCount()
    return served:get("n") or 0
end

local function cors_headers()
    local h = {}
    h["Access-Control-Allow-Origin"] = "*"
    h["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
    h["Access-Control-Allow-Headers"] = "Content-Type"
    h["Content-Type"] = "application/json; charset=UTF-8"
    return h
end

local function json_ok(body_tbl)
    local resp = {}
    resp["status"] = 200
    resp["headers"] = cors_headers()
    resp["body"] = json.encode(body_tbl)
    return resp
end

local function json_raw(status, body_str)
    local resp = {}
    resp["status"] = status
    resp["headers"] = cors_headers()
    resp["body"] = body_str
    return resp
end

local function parse_body(body)
    if body == nil or body == "" then
        return {}
    end
    local ok, t = pcall(function()
        return json.decode(body)
    end)
    if ok and type(t) == "table" then
        return t
    end
    return {}
end

local function mime(path)
    if string.ends_with(path, ".html") then return "text/html; charset=UTF-8" end
    if string.ends_with(path, ".css") then return "text/css; charset=UTF-8" end
    if string.ends_with(path, ".js") then return "application/javascript; charset=UTF-8" end
    if string.ends_with(path, ".svg") then return "image/svg+xml" end
    if string.ends_with(path, ".png") then return "image/png" end
    if string.ends_with(path, ".json") then return "application/json; charset=UTF-8" end
    return "application/octet-stream"
end

local function serve_static(path)
    if path == "/" or path == "" then
        path = "/index.html"
    end
    if string.find(path, "..", 1, true) then
        return json_raw(404, "404 Not Found")
    end
    local file_path = static_root .. path
    local f = io.open(file_path, "rb")
    if f == nil then
        return json_raw(404, "404 Not Found")
    end
    local body = f:read("*a")
    f:close()
    local resp = {}
    resp["status"] = 200
    local h = {}
    h["Content-Type"] = mime(path)
    h["Access-Control-Allow-Origin"] = "*"
    resp["headers"] = h
    resp["body"] = body
    return resp
end

-- 牌号只接受 1..34:解析不出来的(0)会让下游算术在 nil 上炸,而 fakelua 里
-- update 抛错等于整个服务退出,所以脏输入必须在 HTTP 边界挡掉
local function valid_cards(list)
    local ret = {}
    local i = 1
    while i <= #list do
        local c = list[i]
        if c >= 1 and c <= 34 then
            ret[#ret + 1] = c
        end
        i = i + 1
    end
    return ret
end

local function parse_cards_field(m, key)
    local v = m[key]
    if v == nil then return {} end
    if type(v) == "string" then
        return valid_cards(MajiangDef.StringToCards(v))
    end
    if type(v) == "table" then
        local nums = {}
        local i = 1
        while i <= #v do
            nums[i] = math.floor(tonumber(v[i]) or 0)
            i = i + 1
        end
        return valid_cards(nums)
    end
    return {}
end

function OnRequest(typ, connid, req)
    if typ ~= "request" then return nil end
    served:set("n", ServedCount() + 1)
    local method = req.method
    local path = req.path
    if method == "OPTIONS" then
        local resp = {}
        resp["status"] = 204
        resp["headers"] = cors_headers()
        resp["body"] = ""
        return resp
    end

    if path == "/api/status" then
        return json_ok({ status = "ok", ready = true })
    end

    if path == "/api/game/new" then
        local m = parse_body(req.body)
        local guiMode = m["guiMode"]
        if guiMode == nil then guiMode = "random_flip" end
        local customGuiCard = math.floor(tonumber(m["customGuiCard"]) or 0)
        local spectator = m["spectator"] == true
        MajiangGame.NewGame(guiMode, customGuiCard, spectator)
        return json_ok(MajiangGame.CreateView())
    end

    if path == "/api/game/state" then
        return json_ok(MajiangGame.CreateView())
    end

    if path == "/api/game/spectator" then
        local m = parse_body(req.body)
        MajiangGame.SetSpectatorMode(m["spectator"] == true)
        return json_ok(MajiangGame.CreateView())
    end

    if path == "/api/game/discard" then
        local m = parse_body(req.body)
        local card = math.floor(tonumber(m["card"]) or 0)
        local success = MajiangGame.Discard(0, card)
        return json_ok({ success = success, view = MajiangGame.CreateView() })
    end

    if path == "/api/game/action" then
        local m = parse_body(req.body)
        local action = tostring(m["action"] or "")
        local card = math.floor(tonumber(m["card"]) or 0)
        local chi1 = math.floor(tonumber(m["chi1"]) or 0)
        local chi2 = math.floor(tonumber(m["chi2"]) or 0)
        local success = MajiangGame.UserAction(action, card, chi1, chi2)
        return json_ok({ success = success, view = MajiangGame.CreateView() })
    end

    if path == "/api/game/self_action" then
        local m = parse_body(req.body)
        local action = tostring(m["action"] or "")
        local card = math.floor(tonumber(m["card"]) or 0)
        local success = MajiangGame.UserSelfAction(action, card)
        return json_ok({ success = success, view = MajiangGame.CreateView() })
    end

    if path == "/api/game/step" then
        local stepped = MajiangGame.Step()
        return json_ok({ stepped = stepped, view = MajiangGame.CreateView() })
    end

    if path == "/api/game/auto_run" then
        local m = parse_body(req.body)
        local maxSteps = math.floor(tonumber(m["maxSteps"]) or 10)
        local executed = 0
        local i = 1
        while i <= maxSteps do
            if not MajiangGame.Step() then break end
            executed = executed + 1
            i = i + 1
        end
        return json_ok({ executedSteps = executed, view = MajiangGame.CreateView() })
    end

    if path == "/api/algo/test" then
        local m = parse_body(req.body)
        local cards = parse_cards_field(m, "cards")
        local guiCards = parse_cards_field(m, "guiCards")
        if #guiCards == 0 then
            local gui = math.floor(tonumber(m["gui"]) or 0)
            if gui >= 1 and gui <= 34 then guiCards[1] = gui end
        end
        table.sort(cards)
        local t0 = os.clock()
        local isHu = false
        if #cards > 0 and #cards % 3 == 2 then
            isHu = MajiangHu.IsHuExtra(cards, guiCards, 0)
        end
        local huMicros = (os.clock() - t0) * 1000000

        t0 = os.clock()
        local tingCards = {}
        if #cards > 0 then
            tingCards = MajiangHu.IsTingExtra(cards, guiCards)
        end
        local tingMicros = (os.clock() - t0) * 1000000

        local recommendedOut = 0
        local aiScore = 0
        local aiMicros = 0
        if #cards > 0 and #cards % 3 == 2 then
            t0 = os.clock()
            recommendedOut = MajiangAi.OutAI(cards, guiCards)
            if recommendedOut > 0 then
                local rem = {}
                local removed = false
                local i = 1
                while i <= #cards do
                    if not removed and cards[i] == recommendedOut then
                        removed = true
                    else
                        rem[#rem + 1] = cards[i]
                    end
                    i = i + 1
                end
                aiScore = MajiangAi.Calc(rem, guiCards)
            end
            aiMicros = (os.clock() - t0) * 1000000
        end

        local recommendedOutStr = ""
        if recommendedOut > 0 then
            recommendedOutStr = MajiangDef.CardToString(recommendedOut)
        end
        return json_ok({
            cards = cards,
            cardsStr = MajiangDef.CardsToString(cards),
            guiCards = guiCards,
            isHu = isHu,
            huTimeMicros = huMicros,
            tingCards = tingCards,
            tingCardsStr = MajiangDef.CardsToString(tingCards),
            tingTimeMicros = tingMicros,
            recommendedOut = recommendedOut,
            recommendedOutStr = recommendedOutStr,
            aiScore = aiScore,
            aiTimeMicros = aiMicros
        })
    end

    return serve_static(path)
end
