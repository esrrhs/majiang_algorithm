package "MajiangDef"

-- 牌编号,与 Go def.go / Java MaJiangDef 一致

function Wan1() return 1 end
function Tong1() return 10 end
function Tiao1() return 19 end
function FengDong() return 28 end
function FengBei() return 31 end
function JianZhong() return 32 end
function JianBai() return 34 end
function MaxNum() return 42 end

function TypeWan() return 1 end
function TypeTong() return 2 end
function TypeTiao() return 3 end
function TypeFeng() return 4 end
function TypeJian() return 5 end

local FENG_JIAN_HUA = {
    "东", "南", "西", "北", "中", "发", "白",
    "春", "夏", "秋", "冬", "梅", "兰", "竹", "菊"
}

function ToCard(t, index)
    if t == 1 then return 1 + index end
    if t == 2 then return 10 + index end
    if t == 3 then return 19 + index end
    if t == 4 then return 28 + index end
    if t == 5 then return 32 + index end
    return 0
end

function CardType(card)
    if card >= 1 and card <= 9 then return 1 end
    if card >= 10 and card <= 18 then return 2 end
    if card >= 19 and card <= 27 then return 3 end
    if card >= 28 and card <= 31 then return 4 end
    if card >= 32 and card <= 34 then return 5 end
    return 0
end

function CardToString(card)
    if card >= 1 and card <= 9 then
        return tostring(card) .. "万"
    end
    if card >= 10 and card <= 18 then
        return tostring(card - 9) .. "筒"
    end
    if card >= 19 and card <= 27 then
        return tostring(card - 18) .. "条"
    end
    if card >= 28 and card <= 42 then
        return FENG_JIAN_HUA[card - 27]
    end
    return "错误" .. tostring(card)
end

function CardsToString(cards)
    if cards == nil then return "" end
    local ret = ""
    local n = #cards
    local i = 1
    while i <= n do
        ret = ret .. CardToString(cards[i]) .. ","
        i = i + 1
    end
    return ret
end

local function leading_digit(str)
    if str == nil or #str == 0 then return 0 end
    local c = string.byte(str, 1)
    if c >= 48 and c <= 57 then
        return c - 48
    end
    return 0
end

function StringToCard(str)
    if string.find(str, "万", 1, true) then
        return leading_digit(str)
    end
    if string.find(str, "筒", 1, true) then
        return 9 + leading_digit(str)
    end
    if string.find(str, "条", 1, true) then
        return 18 + leading_digit(str)
    end
    local c = 28
    local i = 1
    while i <= #FENG_JIAN_HUA do
        if string.find(str, FENG_JIAN_HUA[i], 1, true) then
            return c
        end
        c = c + 1
        i = i + 1
    end
    return 0
end

function StringToCards(str)
    local ret = {}
    local parts = string.split(str, ",")
    local i = 1
    while i <= #parts do
        local s = parts[i]
        if s ~= nil and #s > 0 then
            ret[#ret + 1] = StringToCard(s)
        end
        i = i + 1
    end
    return ret
end
