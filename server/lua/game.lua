package "MajiangGame"

-- 4人麻将对局引擎 (FakeLua 移植自 go/web/game.go)
-- 状态保存在 package upvalue 中;主循环单 Call 不 Reset,可持续使用。

-- FakeLua 文件级 table 为 const,且 map 不能存 table:对局经 serialize 挂在 container.map。
local store = container.map()
local rng = random.new(1)

local function loadG()
    local bin = store:get("game")
    if bin == nil then return nil end
    return serialize.decode(bin)
end

local function saveG(G)
    if G == nil then return end
    store:set("game", serialize.encode(G))
end

local function next_id()
    local c = store:get("counter")
    if c == nil then c = 1000 end
    c = c + 1
    store:set("counter", c)
    return c
end

local function contains(list, v)
    local i = 1
    while i <= #list do
        if list[i] == v then return true end
        i = i + 1
    end
    return false
end

local function remove_first(list, card)
    local tmp = {}
    local removed = false
    local i = 1
    while i <= #list do
        if not removed and list[i] == card then
            removed = true
        else
            tmp[#tmp + 1] = list[i]
        end
        i = i + 1
    end
    return tmp
end

local function sort_ints(arr)
    table.sort(arr)
end

local function count_card(hand, card)
    local n = 0
    local i = 1
    while i <= #hand do
        if hand[i] == card then n = n + 1 end
        i = i + 1
    end
    return n
end

local function new_actions()
    return {
        canDiscard = false, canHu = false, canPeng = false, canGang = false,
        gangCards = {}, canChi = false, chiOptions = {}, canPass = false
    }
end

local function clone_actions(a)
    local g = {}
    local i = 1
    while i <= #a.gangCards do g[i] = a.gangCards[i]; i = i + 1 end
    local c = {}
    i = 1
    while i <= #a.chiOptions do
        local opt = a.chiOptions[i]
        c[i] = { opt[1], opt[2] }
        i = i + 1
    end
    return {
        canDiscard = a.canDiscard, canHu = a.canHu, canPeng = a.canPeng, canGang = a.canGang,
        gangCards = g, canChi = a.canChi, chiOptions = c, canPass = a.canPass
    }
end

local function new_player(seat, name, isAi)
    return {
        seat = seat, name = name, isAi = isAi, hand = {}, melds = {}, discards = {},
        lastDrawnCard = 0, isHu = false, isTing = false, score = 1000
    }
end

local function player_reset(p)
    p.hand = {}
    p.melds = {}
    p.discards = {}
    p.lastDrawnCard = 0
    p.isHu = false
    p.isTing = false
end

local function player_add(p, card)
    p.hand[#p.hand + 1] = card
    p.lastDrawnCard = card
    sort_ints(p.hand)
end

local function player_remove(p, card)
    local nh = {}
    local removed = false
    local i = 1
    while i <= #p.hand do
        if not removed and p.hand[i] == card then
            removed = true
        else
            nh[#nh + 1] = p.hand[i]
        end
        i = i + 1
    end
    p.hand = nh
    if p.lastDrawnCard == card then p.lastDrawnCard = 0 end
    return removed
end

local function glog(G, msg)
    G.logs[#G.logs + 1] = msg
    if #G.logs > 100 then
        local nl = {}
        local i = 2
        while i <= #G.logs do
            nl[#nl + 1] = G.logs[i]
            i = i + 1
        end
        G.logs = nl
    end
end

local function next_card(card)
    if card >= 1 and card <= 9 then
        if card == 9 then return 1 end
        return card + 1
    end
    if card >= 10 and card <= 18 then
        if card == 18 then return 10 end
        return card + 1
    end
    if card >= 19 and card <= 27 then
        if card == 27 then return 19 end
        return card + 1
    end
    if card >= 28 and card <= 31 then
        if card == 31 then return 28 end
        return card + 1
    end
    if card >= 32 and card <= 34 then
        if card == 34 then return 32 end
        return card + 1
    end
    return card
end

local function wall_empty(G)
    return #G.wall == 0
end

local function update_ting(G)
    local pi = 1
    while pi <= 4 do
        local p = G.players[pi]
        local hn = #p.hand
        if hn % 3 == 1 then
            local ting = MajiangHu.IsTingExtra(p.hand, G.guiCards)
            p.isTing = #ting > 0
        elseif hn % 3 == 2 then
            local can = false
            local seen = {}
            local i = 1
            while i <= hn do
                local c = p.hand[i]
                if seen[c] == nil then
                    seen[c] = true
                    local tmp = remove_first(p.hand, c)
                    local t = MajiangHu.IsTingExtra(tmp, G.guiCards)
                    if #t > 0 then can = true; break end
                end
                i = i + 1
            end
            p.isTing = can
        end
        pi = pi + 1
    end
end

local function find_chi_options(hand, card)
    local options = {}
    local t = MajiangDef.CardType(card)
    if t ~= 1 and t ~= 2 and t ~= 3 then return options end
    if contains(hand, card - 2) and contains(hand, card - 1) and MajiangDef.CardType(card - 2) == t then
        options[#options + 1] = { card - 2, card - 1 }
    end
    if contains(hand, card - 1) and contains(hand, card + 1)
        and MajiangDef.CardType(card - 1) == t and MajiangDef.CardType(card + 1) == t then
        options[#options + 1] = { card - 1, card + 1 }
    end
    if contains(hand, card + 1) and contains(hand, card + 2) and MajiangDef.CardType(card + 2) == t then
        options[#options + 1] = { card + 1, card + 2 }
    end
    return options
end

function trigger_draw(G)
    G.phase = "GAME_OVER"
    G.settlement = {
        isDraw = true, winnerSeat = -1, providerSeat = -1, isZimo = false,
        winCard = 0, patterns = { "荒庄流局 (无输赢)" }, totalFan = 0, points = 0, scoreDeltas = {}
    }
end

function trigger_win(G, winnerSeat, providerSeat, winCard, isZimo, isGangShangHua, isHaiDi, isTianHu, isDiHu)
    G.phase = "GAME_OVER"
    local winner = G.players[winnerSeat + 1]
    winner.isHu = true
    local fan = MajiangFan.Calculate(winner.hand, winner.melds, winCard, isZimo, isGangShangHua, isHaiDi, isTianHu, isDiHu, G.guiCards)
    local settlement = {
        isDraw = false, winnerSeat = winnerSeat, providerSeat = providerSeat, isZimo = isZimo,
        winCard = winCard, patterns = fan.patterns, totalFan = fan.totalFan, points = fan.points, scoreDeltas = {}
    }
    local scoreDelta = fan.points
    if isZimo then
        local i = 1
        while i <= 4 do
            local p = G.players[i]
            if p.seat == winnerSeat then
                p.score = p.score + scoreDelta * 3
                settlement.scoreDeltas[tostring(p.seat)] = scoreDelta * 3
            else
                p.score = p.score - scoreDelta
                settlement.scoreDeltas[tostring(p.seat)] = -scoreDelta
            end
            i = i + 1
        end
    else
        winner.score = winner.score + scoreDelta
        settlement.scoreDeltas[tostring(winnerSeat)] = scoreDelta
        G.players[providerSeat + 1].score = G.players[providerSeat + 1].score - scoreDelta
        settlement.scoreDeltas[tostring(providerSeat)] = -scoreDelta
        local i = 1
        while i <= 4 do
            local p = G.players[i]
            if p.seat ~= winnerSeat and p.seat ~= providerSeat then
                settlement.scoreDeltas[tostring(p.seat)] = 0
            end
            i = i + 1
        end
    end
    G.settlement = settlement
end

function execute_peng(G, seat, fromSeat, card)
    local p = G.players[seat + 1]
    player_remove(p, card)
    player_remove(p, card)
    local meld = { type = "PENG", triggerCard = card, fromSeat = fromSeat, cards = { card, card, card } }
    p.melds[#p.melds + 1] = meld
    glog(G, p.name .. " 【碰】了 " .. G.players[fromSeat + 1].name .. " 的 " .. MajiangDef.CardToString(card))
    G.currentSeat = seat
    G.phase = "DISCARD"
    G.lastDrawFromGang = false
    p.lastDrawnCard = 0
    update_ting(G)
end

function execute_gang(G, seat, fromSeat, card, meldType)
    local p = G.players[seat + 1]
    if meldType == "MING_GANG" then
        player_remove(p, card)
        player_remove(p, card)
        player_remove(p, card)
        local meld = { type = "MING_GANG", triggerCard = card, fromSeat = fromSeat, cards = { card, card, card, card } }
        p.melds[#p.melds + 1] = meld
        glog(G, p.name .. " 【明杠】了 " .. G.players[fromSeat + 1].name .. " 的 " .. MajiangDef.CardToString(card))
    end
    draw_next(G, seat, true)
end

function execute_chi(G, seat, fromSeat, card, c1, c2)
    local p = G.players[seat + 1]
    player_remove(p, c1)
    player_remove(p, c2)
    local meld = { type = "CHI", triggerCard = card, fromSeat = fromSeat, cards = { card, c1, c2 } }
    sort_ints(meld.cards)
    p.melds[#p.melds + 1] = meld
    glog(G, p.name .. " 【吃】了 " .. G.players[fromSeat + 1].name .. " 的 " .. MajiangDef.CardToString(card))
    G.currentSeat = seat
    G.phase = "DISCARD"
    G.lastDrawFromGang = false
    p.lastDrawnCard = 0
    update_ting(G)
end

function draw_next(G, seat, isFromGang)
    G.currentSeat = seat
    G.lastDrawFromGang = isFromGang
    if #G.wall == 0 then
        glog(G, "牌墙已全部摸完，【荒庄流局】！")
        trigger_draw(G)
        return
    end
    local drawn
    if isFromGang then
        drawn = G.wall[#G.wall]
        local nw = {}
        local i = 1
        while i < #G.wall do nw[i] = G.wall[i]; i = i + 1 end
        G.wall = nw
    else
        drawn = G.wall[1]
        local nw = {}
        local i = 2
        while i <= #G.wall do nw[#nw + 1] = G.wall[i]; i = i + 1 end
        G.wall = nw
    end
    local player = G.players[seat + 1]
    player_add(player, drawn)
    if isFromGang then
        glog(G, player.name .. " 杠后补摸一张: 【" .. MajiangDef.CardToString(drawn) .. "】 (剩余 " .. tostring(#G.wall) .. " 张)")
    else
        glog(G, player.name .. " 摸了一张牌 (剩余 " .. tostring(#G.wall) .. " 张)")
    end
    if MajiangHu.IsHuExtra(player.hand, G.guiCards, 0) then
        if player.isAi then
            local tag = " 【自摸胡】"
            if isFromGang then tag = " 【杠上开花】" end
            glog(G, "🎊 " .. player.name .. tag .. "！胡牌: 【" .. MajiangDef.CardToString(drawn) .. "】")
            trigger_win(G, seat, -1, drawn, true, isFromGang, wall_empty(G), false, false)
            return
        end
    end
    if player.isAi then
        local card = 1
        while card <= 34 do
            if not contains(G.guiCards, card) and count_card(player.hand, card) == 4 then
                if MajiangAi.GangAI(player.hand, G.guiCards, card, 0.5) then
                    local k = 1
                    while k <= 4 do player_remove(player, card); k = k + 1 end
                    local meld = { type = "AN_GANG", triggerCard = card, fromSeat = -1, cards = { card, card, card, card } }
                    player.melds[#player.melds + 1] = meld
                    glog(G, player.name .. " 【暗杠】: " .. MajiangDef.CardToString(card))
                    draw_next(G, seat, true)
                    return
                end
            end
            card = card + 1
        end
        local mi = 1
        while mi <= #player.melds do
            local m = player.melds[mi]
            if m.type == "PENG" and not contains(G.guiCards, m.triggerCard) and contains(player.hand, m.triggerCard) then
                if MajiangAi.GangAI(player.hand, G.guiCards, m.triggerCard, 0.5) then
                    player_remove(player, m.triggerCard)
                    m.type = "BU_GANG"
                    m.cards[#m.cards + 1] = m.triggerCard
                    glog(G, player.name .. " 【补杠】: " .. MajiangDef.CardToString(m.triggerCard))
                    draw_next(G, seat, true)
                    return
                end
            end
            mi = mi + 1
        end
    end
    G.phase = "DISCARD"
    update_ting(G)
end

function process_ai_responses(G, card, fromSeat)
    local i = 1
    while i <= 3 do
        local seat = (fromSeat + i) % 4
        local p = G.players[seat + 1]
        if p.isAi and MajiangHu.IsHuExtra(p.hand, G.guiCards, card) then
            glog(G, "🎊 " .. p.name .. " 胡了 " .. G.players[fromSeat + 1].name .. " 点炮的 【" .. MajiangDef.CardToString(card) .. "】！")
            trigger_win(G, seat, fromSeat, card, false, false, wall_empty(G), false, false)
            return
        end
        i = i + 1
    end
    i = 1
    while i <= 3 do
        local seat = (fromSeat + i) % 4
        local p = G.players[seat + 1]
        if p.isAi and count_card(p.hand, card) == 3 and not contains(G.guiCards, card) then
            if MajiangAi.GangAI(p.hand, G.guiCards, card, 0.5) then
                execute_gang(G, seat, fromSeat, card, "MING_GANG")
                return
            end
        end
        i = i + 1
    end
    i = 1
    while i <= 3 do
        local seat = (fromSeat + i) % 4
        local p = G.players[seat + 1]
        if p.isAi and count_card(p.hand, card) >= 2 and not contains(G.guiCards, card) then
            if MajiangAi.PengAI(p.hand, G.guiCards, card, 0.0) then
                execute_peng(G, seat, fromSeat, card)
                return
            end
        end
        i = i + 1
    end
    local nextSeat = (fromSeat + 1) % 4
    local nextPlayer = G.players[nextSeat + 1]
    if nextPlayer.isAi and not contains(G.guiCards, card) then
        local chiPair = MajiangAi.ChiAIChoices(nextPlayer.hand, G.guiCards, card)
        if #chiPair == 2 then
            execute_chi(G, nextSeat, fromSeat, card, chiPair[1], chiPair[2])
            return
        end
    end
    G.players[fromSeat + 1].discards[#G.players[fromSeat + 1].discards + 1] = card
    draw_next(G, (fromSeat + 1) % 4, false)
end

local function check_responses(G, card, fromSeat)
    G.userPendingActions = new_actions()
    local human = G.players[1]
    if not human.isAi and fromSeat ~= 0 then
        local hasAction = false
        if MajiangHu.IsHuExtra(human.hand, G.guiCards, card) then
            G.userPendingActions.canHu = true
            hasAction = true
        end
        if count_card(human.hand, card) >= 2 and not contains(G.guiCards, card) then
            G.userPendingActions.canPeng = true
            hasAction = true
        end
        if count_card(human.hand, card) == 3 and not contains(G.guiCards, card) then
            G.userPendingActions.canGang = true
            G.userPendingActions.gangCards[#G.userPendingActions.gangCards + 1] = card
            hasAction = true
        end
        if (fromSeat + 1) % 4 == 0 and not contains(G.guiCards, card) then
            local chiOpts = find_chi_options(human.hand, card)
            if #chiOpts > 0 then
                G.userPendingActions.canChi = true
                G.userPendingActions.chiOptions = chiOpts
                hasAction = true
            end
        end
        if hasAction then
            G.userPendingActions.canPass = true
            G.phase = "WAITING_USER"
            return
        end
    end
    process_ai_responses(G, card, fromSeat)
end

local function handle_ai_response_player0(G)
    local card = G.lastDiscard
    local fromSeat = G.lastDiscardSeat
    local p = G.players[1]
    if MajiangHu.IsHuExtra(p.hand, G.guiCards, card) then
        glog(G, "🎊 " .. p.name .. " 胡了点炮 【" .. MajiangDef.CardToString(card) .. "】！")
        trigger_win(G, 0, fromSeat, card, false, false, wall_empty(G), false, false)
        return
    end
    if count_card(p.hand, card) == 3 and not contains(G.guiCards, card) and MajiangAi.GangAI(p.hand, G.guiCards, card, 0.5) then
        execute_gang(G, 0, fromSeat, card, "MING_GANG")
        return
    end
    if count_card(p.hand, card) >= 2 and not contains(G.guiCards, card) and MajiangAi.PengAI(p.hand, G.guiCards, card, 0.0) then
        execute_peng(G, 0, fromSeat, card)
        return
    end
    if (fromSeat + 1) % 4 == 0 then
        local chiPair = MajiangAi.ChiAIChoices(p.hand, G.guiCards, card)
        if #chiPair == 2 then
            execute_chi(G, 0, fromSeat, card, chiPair[1], chiPair[2])
            return
        end
    end
    G.phase = "RESPONSE"
    process_ai_responses(G, card, fromSeat)
end

local function init_game(G, guiMode, customGuiCard)
    local i = 1
    while i <= 4 do player_reset(G.players[i]); i = i + 1 end
    G.wall = {}
    G.guiCards = {}
    G.logs = {}
    G.settlement = nil
    G.lastDiscard = 0
    G.lastDiscardSeat = -1
    G.lastDrawFromGang = false
    G.dealerSeat = 0
    G.currentSeat = 0
    G.stepCount = 0
    i = 1
    while i <= 34 do
        local k = 1
        while k <= 4 do
            G.wall[#G.wall + 1] = i
            k = k + 1
        end
        i = i + 1
    end
    -- shuffle
    i = #G.wall
    while i > 1 do
        local j = rng:int(1, i)
        local tmp = G.wall[i]
        G.wall[i] = G.wall[j]
        G.wall[j] = tmp
        i = i - 1
    end
    if guiMode == "random_flip" or guiMode == "" then
        G.guiIndicator = G.wall[1]
        local nw = {}
        i = 2
        while i <= #G.wall do nw[#nw + 1] = G.wall[i]; i = i + 1 end
        G.wall = nw
        local gui = next_card(G.guiIndicator)
        G.guiCards[1] = gui
        glog(G, "本局鬼牌(癞子)为: 【" .. MajiangDef.CardToString(gui) .. "】")
    elseif guiMode == "card" and customGuiCard > 0 and customGuiCard <= 34 then
        G.guiIndicator = customGuiCard
        G.guiCards[1] = customGuiCard
        glog(G, "指定宝牌(鬼牌)为: 【" .. MajiangDef.CardToString(customGuiCard) .. "】")
    elseif guiMode == "bai" then
        G.guiIndicator = 34
        G.guiCards[1] = 34
        glog(G, "经典白板当鬼: 【白板】")
    elseif guiMode == "zhong" then
        G.guiIndicator = 32
        G.guiCards[1] = 32
        glog(G, "经典红中当鬼: 【红中】")
    else
        G.guiIndicator = 0
        glog(G, "本局为经典纯手牌规则 (无鬼牌)")
    end
    local round = 1
    while round <= 13 do
        local s = 1
        while s <= 4 do
            player_add(G.players[s], G.wall[1])
            local nw = {}
            i = 2
            while i <= #G.wall do nw[#nw + 1] = G.wall[i]; i = i + 1 end
            G.wall = nw
            s = s + 1
        end
        round = round + 1
    end
    local dealerCard = G.wall[1]
    local nw = {}
    i = 2
    while i <= #G.wall do nw[#nw + 1] = G.wall[i]; i = i + 1 end
    G.wall = nw
    player_add(G.players[1], dealerCard)
    G.players[1].lastDrawnCard = dealerCard
    glog(G, G.players[1].name .. " 起手14张牌，作为庄家率先出牌")
    if MajiangHu.IsHuExtra(G.players[1].hand, G.guiCards, 0) then
        glog(G, "🎉 震撼！" .. G.players[1].name .. " 起手天胡！")
        trigger_win(G, 0, -1, dealerCard, true, false, false, true, false)
        return
    end
    G.phase = "DISCARD"
    update_ting(G)
end

function NewGame(guiMode, customGuiCard, spectator)
    local name0 = "玩家 (南)"
    if spectator then name0 = "AI-南 (庄)" end
    local G = {}
    G.ready = true
    G.gameId = next_id()
    G.players = {
        new_player(0, name0, spectator),
        new_player(1, "AI-东 (下家)", true),
        new_player(2, "AI-北 (对家)", true),
        new_player(3, "AI-西 (上家)", true)
    }
    G.wall = {}
    G.guiCards = {}
    G.guiIndicator = 0
    G.dealerSeat = 0
    G.currentSeat = 0
    G.phase = "DISCARD"
    G.lastDiscard = 0
    G.lastDiscardSeat = -1
    G.lastDrawFromGang = false
    G.logs = {}
    G.settlement = nil
    G.spectatorMode = spectator
    G.stepCount = 0
    G.userPendingActions = new_actions()
    init_game(G, guiMode, customGuiCard)
    saveG(G)
    return true
end

function Discard(seat, card)
    local G = loadG()
    if G == nil or not G.ready or G.phase ~= "DISCARD" or seat ~= G.currentSeat then return false end
    local player = G.players[seat + 1]
    if not contains(player.hand, card) then return false end
    player_remove(player, card)
    sort_ints(player.hand)
    G.lastDiscard = card
    G.lastDiscardSeat = seat
    glog(G, player.name .. " 打出了 【" .. MajiangDef.CardToString(card) .. "】")
    G.phase = "RESPONSE"
    check_responses(G, card, seat)
    saveG(G)
    return true
end

function UserAction(action, card, chi1, chi2)
    local G = loadG()
    if G == nil or not G.ready or G.phase ~= "WAITING_USER" then return false end
    local human = G.players[1]
    if action == "hu" and G.userPendingActions.canHu then
        glog(G, "🎉 恭喜你胡牌了！")
        trigger_win(G, 0, G.lastDiscardSeat, G.lastDiscard, false, false, wall_empty(G), false, false)
        saveG(G)
        return true
    end
    if action == "gang" and G.userPendingActions.canGang then
        execute_gang(G, 0, G.lastDiscardSeat, G.lastDiscard, "MING_GANG")
        saveG(G)
        return true
    end
    if action == "peng" and G.userPendingActions.canPeng then
        execute_peng(G, 0, G.lastDiscardSeat, G.lastDiscard)
        saveG(G)
        return true
    end
    if action == "chi" and G.userPendingActions.canChi then
        if contains(human.hand, chi1) and contains(human.hand, chi2) then
            execute_chi(G, 0, G.lastDiscardSeat, G.lastDiscard, chi1, chi2)
            saveG(G)
            return true
        end
    end
    if action == "pass" then
        glog(G, human.name .. " 选择了 【过】")
        G.phase = "RESPONSE"
        process_ai_responses(G, G.lastDiscard, G.lastDiscardSeat)
        saveG(G)
        return true
    end
    saveG(G)
    return false
end

function UserSelfAction(action, card)
    local G = loadG()
    if G == nil or not G.ready or G.phase ~= "DISCARD" or G.currentSeat ~= 0 then return false end
    local human = G.players[1]
    if action == "hu" then
        if MajiangHu.IsHuExtra(human.hand, G.guiCards, 0) then
            local winCard = human.lastDrawnCard
            if winCard <= 0 and #human.hand > 0 then winCard = human.hand[#human.hand] end
            glog(G, "🎉 恭喜你【自摸胡】了！")
            trigger_win(G, 0, -1, winCard, true, G.lastDrawFromGang, wall_empty(G), false, false)
            saveG(G)
            return true
        end
    end
    if action == "gang" then
        if count_card(human.hand, card) == 4 and not contains(G.guiCards, card) then
            local k = 1
            while k <= 4 do player_remove(human, card); k = k + 1 end
            human.melds[#human.melds + 1] = { type = "AN_GANG", triggerCard = card, fromSeat = -1, cards = { card, card, card, card } }
            glog(G, human.name .. " 【暗杠】: " .. MajiangDef.CardToString(card))
            draw_next(G, 0, true)
            saveG(G)
            return true
        end
        local mi = 1
        while mi <= #human.melds do
            local m = human.melds[mi]
            if m.type == "PENG" and m.triggerCard == card and contains(human.hand, card) then
                player_remove(human, card)
                m.type = "BU_GANG"
                m.cards[#m.cards + 1] = card
                glog(G, human.name .. " 【补杠】: " .. MajiangDef.CardToString(card))
                draw_next(G, 0, true)
                saveG(G)
                return true
            end
            mi = mi + 1
        end
    end
    saveG(G)
    return false
end

function Step()
    local G = loadG()
    if G == nil or not G.ready then return false end
    G.stepCount = G.stepCount + 1
    if G.phase == "GAME_OVER" then
        saveG(G)
        return false
    end
    if G.phase == "WAITING_USER" then
        if G.spectatorMode or G.players[1].isAi then
            handle_ai_response_player0(G)
            saveG(G)
            return true
        end
        saveG(G)
        return false
    end
    if G.phase == "DISCARD" then
        local current = G.players[G.currentSeat + 1]
        if current.isAi then
            local outCard = MajiangAi.OutAI(current.hand, G.guiCards)
            if outCard == 0 or not contains(current.hand, outCard) then
                outCard = 0
                local i = 1
                while i <= #current.hand do
                    local c = current.hand[i]
                    if not contains(G.guiCards, c) then outCard = c; break end
                    i = i + 1
                end
                if outCard == 0 then outCard = current.hand[1] end
            end
            Discard(G.currentSeat, outCard)
            -- Discard 内部已 saveG,勿再用本函数陈旧副本覆盖
            return true
        end
        saveG(G)
        return false
    end
    saveG(G)
    return false
end

function SetSpectatorMode(spectator)
    local G = loadG()
    if G == nil or not G.ready then return end
    G.spectatorMode = spectator
    G.players[1].isAi = spectator
    if spectator then
        G.players[1].name = "AI-南 (庄)"
    else
        G.players[1].name = "玩家 (南)"
    end
    saveG(G)
end

function GetPhase()
    local G = loadG()
    if G == nil or not G.ready then return "GAME_OVER" end
    return G.phase
end

function CreateView()
    local G = loadG()
    if G == nil or not G.ready then
        return { gameId = 0, phase = "GAME_OVER", players = {}, availableActions = new_actions(),
            currentTingTargets = {}, discardToTing = { _ = true }, logs = {}, cardRemainCounts = { _ = true },
            guiCards = {}, spectatorMode = false, wallCount = 0, guiIndicator = 0, dealerSeat = 0,
            currentSeat = 0, lastDiscard = 0, lastDiscardSeat = -1 }
    end
    -- remain counts
    local visible = {}
    local i = 1
    while i <= 34 do visible[i] = 0; i = i + 1 end
    if G.guiIndicator > 0 then visible[G.guiIndicator] = visible[G.guiIndicator] + 1 end
    local pi = 1
    while pi <= 4 do
        local p = G.players[pi]
        local di = 1
        while di <= #p.discards do
            visible[p.discards[di]] = visible[p.discards[di]] + 1
            di = di + 1
        end
        local mi = 1
        while mi <= #p.melds do
            local m = p.melds[mi]
            local ci = 1
            while ci <= #m.cards do
                visible[m.cards[ci]] = visible[m.cards[ci]] + 1
                ci = ci + 1
            end
            mi = mi + 1
        end
        pi = pi + 1
    end
    local user = G.players[1]
    i = 1
    while i <= #user.hand do
        visible[user.hand[i]] = visible[user.hand[i]] + 1
        i = i + 1
    end
    local remainMap = {}
    i = 1
    while i <= 34 do
        local r = 4 - visible[i]
        if r < 0 then r = 0 end
        remainMap[tostring(i)] = r
        i = i + 1
    end
    local players = {}
    pi = 1
    while pi <= 4 do
        local p = G.players[pi]
        local hand = {}
        i = 1
        while i <= #p.hand do hand[i] = p.hand[i]; i = i + 1 end
        local melds = {}
        i = 1
        while i <= #p.melds do
            local m = p.melds[i]
            local cards = {}
            local ci = 1
            while ci <= #m.cards do cards[ci] = m.cards[ci]; ci = ci + 1 end
            melds[i] = { type = m.type, triggerCard = m.triggerCard, fromSeat = m.fromSeat, cards = cards }
            i = i + 1
        end
        local discards = {}
        i = 1
        while i <= #p.discards do discards[i] = p.discards[i]; i = i + 1 end
        players[pi] = {
            seat = p.seat, name = p.name, isAi = p.isAi, tileCount = #p.hand,
            hand = hand, melds = melds, discards = discards, lastDrawnCard = p.lastDrawnCard,
            isTing = p.isTing, isHu = p.isHu, score = p.score
        }
        pi = pi + 1
    end
    local availableActions = new_actions()
    if G.phase == "WAITING_USER" then
        availableActions = clone_actions(G.userPendingActions)
    elseif G.phase == "DISCARD" and G.currentSeat == 0 and not user.isAi then
        availableActions.canDiscard = true
        if MajiangHu.IsHuExtra(user.hand, G.guiCards, 0) then
            availableActions.canHu = true
        end
        local card = 1
        while card <= 34 do
            if not contains(G.guiCards, card) and count_card(user.hand, card) == 4 then
                availableActions.canGang = true
                availableActions.gangCards[#availableActions.gangCards + 1] = card
            end
            card = card + 1
        end
        local mi = 1
        while mi <= #user.melds do
            local m = user.melds[mi]
            if m.type == "PENG" and not contains(G.guiCards, m.triggerCard) and contains(user.hand, m.triggerCard) then
                availableActions.canGang = true
                availableActions.gangCards[#availableActions.gangCards + 1] = m.triggerCard
            end
            mi = mi + 1
        end
    end
    local currentTingTargets = {}
    local discardToTing = { _placeholder = true }
    discardToTing._placeholder = nil
    if not user.isAi or G.spectatorMode then
        if #user.hand % 3 == 1 then
            local tings = MajiangHu.IsTingExtra(user.hand, G.guiCards)
            i = 1
            while i <= #tings do
                currentTingTargets[i] = { card = tings[i], remainingCount = remainMap[tostring(tings[i])] or 0 }
                i = i + 1
            end
        end
        if #user.hand % 3 == 2 then
            local seen = {}
            i = 1
            while i <= #user.hand do
                local c = user.hand[i]
                if seen[c] == nil then
                    seen[c] = true
                    local sim = remove_first(user.hand, c)
                    local tings = MajiangHu.IsTingExtra(sim, G.guiCards)
                    if #tings > 0 then
                        local targets = {}
                        local ti = 1
                        while ti <= #tings do
                            targets[ti] = { card = tings[ti], remainingCount = remainMap[tostring(tings[ti])] or 0 }
                            ti = ti + 1
                        end
                        discardToTing[tostring(c)] = targets
                    end
                end
                i = i + 1
            end
        end
    end
    -- ensure discardToTing is object not empty array for json
    local hasKey = false
    for k, v in pairs(discardToTing) do
        hasKey = true
        break
    end
    if not hasKey then
        discardToTing["__empty"] = true
        discardToTing["__empty"] = nil
    end
    local guiCards = {}
    i = 1
    while i <= #G.guiCards do guiCards[i] = G.guiCards[i]; i = i + 1 end
    local logs = {}
    i = 1
    while i <= #G.logs do logs[i] = G.logs[i]; i = i + 1 end
    local view = {
        gameId = G.gameId, wallCount = #G.wall, guiCards = guiCards, guiIndicator = G.guiIndicator,
        dealerSeat = G.dealerSeat, currentSeat = G.currentSeat, phase = G.phase,
        lastDiscard = G.lastDiscard, lastDiscardSeat = G.lastDiscardSeat, players = players,
        availableActions = availableActions, currentTingTargets = currentTingTargets,
        discardToTing = discardToTing, logs = logs, cardRemainCounts = remainMap,
        spectatorMode = G.spectatorMode
    }
    if G.settlement ~= nil then
        view.settlement = G.settlement
    end
    -- AI recommendation for human discard turn
    if G.phase == "DISCARD" and G.currentSeat == 0 and not user.isAi then
        if availableActions.canHu then
            view.aiRecommendation = {
                recommendedAction = "hu", recommendedDiscard = 0, recommendedCard = 0,
                recommendedCards = {}, score = 999.0, reason = "AI强烈推荐【自摸胡】！"
            }
        else
            local out = MajiangAi.OutAI(user.hand, G.guiCards)
            if out > 0 then
                local rem = remove_first(user.hand, out)
                local sc = MajiangAi.Calc(rem, G.guiCards)
                view.aiRecommendation = {
                    recommendedAction = "discard", recommendedDiscard = out, recommendedCard = out,
                    recommendedCards = { out }, score = sc,
                    reason = "AI推荐打出【" .. MajiangDef.CardToString(out) .. "】"
                }
            end
        end
    end
    return view
end
