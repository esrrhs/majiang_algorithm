package "MajiangFan"

-- 番型计算,对齐 Go ref_go_web/fan.go / Java FanCalculator

local function contains(list, v)
    local i = 1
    while i <= #list do
        if list[i] == v then return true end
        i = i + 1
    end
    return false
end

local function min_int(a, b)
    if a < b then return a end
    return b
end

local function all_tiles(hand, melds)
    local tiles = {}
    local i = 1
    while i <= #hand do
        tiles[#tiles + 1] = hand[i]
        i = i + 1
    end
    i = 1
    while i <= #melds do
        local cards = melds[i].cards
        local j = 1
        while j <= #cards do
            tiles[#tiles + 1] = cards[j]
            j = j + 1
        end
        i = i + 1
    end
    return tiles
end

local function check_qing_yi_se(tiles, guiCards)
    local colorType = 0
    local i = 1
    while i <= #tiles do
        local c = tiles[i]
        if not contains(guiCards, c) then
            local t = MajiangDef.CardType(c)
            if t == MajiangDef.TypeFeng() or t == MajiangDef.TypeJian() then
                return false
            end
            if colorType == 0 then
                colorType = t
            elseif colorType ~= t then
                return false
            end
        end
        i = i + 1
    end
    return colorType ~= 0
end

local function check_hun_yi_se(tiles, guiCards)
    local colorType = 0
    local hasZi = false
    local i = 1
    while i <= #tiles do
        local c = tiles[i]
        if not contains(guiCards, c) then
            local t = MajiangDef.CardType(c)
            if t == MajiangDef.TypeFeng() or t == MajiangDef.TypeJian() then
                hasZi = true
            else
                if colorType == 0 then
                    colorType = t
                elseif colorType ~= t then
                    return false
                end
            end
        end
        i = i + 1
    end
    return colorType ~= 0 and hasZi
end

local function check_duan_yao_jiu(tiles, guiCards)
    local i = 1
    while i <= #tiles do
        local c = tiles[i]
        if not contains(guiCards, c) then
            local t = MajiangDef.CardType(c)
            if t == MajiangDef.TypeFeng() or t == MajiangDef.TypeJian() then
                return false
            end
            if c == 1 or c == 9 or c == 10 or c == 18 or c == 19 or c == 27 then
                return false
            end
        end
        i = i + 1
    end
    return true
end

local function check_qi_dui(tiles, guiCards)
    local counts = {}
    local guiCount = 0
    local i = 1
    while i <= #tiles do
        local c = tiles[i]
        if contains(guiCards, c) then
            guiCount = guiCount + 1
        else
            local n = counts[c]
            if n == nil then n = 0 end
            counts[c] = n + 1
        end
        i = i + 1
    end
    local singleCount = 0
    for _, count in pairs(counts) do
        if count % 2 ~= 0 then
            singleCount = singleCount + 1
        end
    end
    return guiCount >= singleCount
end

local function check_peng_peng_hu(hand, melds, winCard, isZimo, guiCards)
    local i = 1
    while i <= #melds do
        if melds[i].type == "CHI" then return false end
        i = i + 1
    end
    local cards = {}
    i = 1
    while i <= #hand do
        cards[#cards + 1] = hand[i]
        i = i + 1
    end
    if not isZimo and winCard > 0 then
        cards[#cards + 1] = winCard
    end
    local guiCount = 0
    local counts = {}
    i = 1
    while i <= #cards do
        local c = cards[i]
        if contains(guiCards, c) then
            guiCount = guiCount + 1
        else
            local n = counts[c]
            if n == nil then n = 0 end
            counts[c] = n + 1
        end
        i = i + 1
    end
    for candidateJiang, _ in pairs(counts) do
        local needGui = 0
        for card, count in pairs(counts) do
            if card == candidateJiang then
                if count < 2 then
                    needGui = needGui + (2 - count)
                else
                    local rem = count - 2
                    if rem % 3 ~= 0 then
                        needGui = needGui + (3 - rem % 3)
                    end
                end
            else
                if count % 3 ~= 0 then
                    needGui = needGui + (3 - count % 3)
                end
            end
        end
        if needGui <= guiCount and (guiCount - needGui) % 3 == 0 then
            return true
        end
    end
    return false
end

local function add_pattern(result, name, fan)
    result.patterns[#result.patterns + 1] = name .. " (" .. tostring(fan) .. "番)"
    result.totalFan = result.totalFan + fan
end

function Calculate(hand, melds, winCard, isZimo, isGangShangHua, isHaiDi, isTianHu, isDiHu, guiCards)
    local result = { patterns = {}, totalFan = 0, points = 0 }

    if isTianHu then
        add_pattern(result, "天胡", 16)
        result.points = 16 * 10
        return result
    end
    if isDiHu then
        add_pattern(result, "地胡", 16)
        result.points = 16 * 10
        return result
    end

    local tiles = all_tiles(hand, melds)
    if not isZimo and winCard > 0 then
        tiles[#tiles + 1] = winCard
    end

    if check_qing_yi_se(tiles, guiCards) then
        add_pattern(result, "清一色", 8)
    elseif check_hun_yi_se(tiles, guiCards) then
        add_pattern(result, "混一色", 4)
    end

    if #melds == 0 and #tiles == 14 then
        if check_qi_dui(tiles, guiCards) then
            add_pattern(result, "七对", 8)
        end
    end

    if check_peng_peng_hu(hand, melds, winCard, isZimo, guiCards) then
        add_pattern(result, "碰碰胡", 4)
    end

    if check_duan_yao_jiu(tiles, guiCards) then
        add_pattern(result, "断幺九", 2)
    end

    local menQianQing = true
    local i = 1
    while i <= #melds do
        if melds[i].type ~= "AN_GANG" then
            menQianQing = false
            break
        end
        i = i + 1
    end
    if menQianQing and not isZimo then
        add_pattern(result, "门前清", 2)
    end

    if isGangShangHua then
        add_pattern(result, "杠上开花", 2)
    end
    if isHaiDi then
        add_pattern(result, "海底捞月", 2)
    end
    if isZimo then
        add_pattern(result, "自摸", 1)
    end

    if #result.patterns == 0 then
        add_pattern(result, "平胡", 1)
    end

    local totalFan = result.totalFan
    if totalFan < 1 then totalFan = 1 end
    result.totalFan = totalFan
    local baseScore = 10
    local exp = min_int(6, totalFan - 1)
    local mul = 1
    local e = 1
    while e <= exp do
        mul = mul * 2
        e = e + 1
    end
    result.points = baseScore * mul
    return result
end
