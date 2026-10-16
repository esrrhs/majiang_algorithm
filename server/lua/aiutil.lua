package "MajiangAi"

-- AI 评分与出牌决策。查表数据放在 sqlite(data/majiang.db),按 key 点查。
--
-- 表结构 ai_{normal,feng,jian}(key, seq, jiang, val):
--   key   该花色计数串展开成的十进制整数,万/筒/条共用 normal 表;
--   seq   同一 key 内的条目序号(无将在前、有将在后);
--   jiang 该条目是否要求有将;
--   val   该条目在"当前轮数"下的基础概率 P。
--
-- 流程与 C++ 版(cpp/src/ai_util.cpp 的 calcAITableInfo 等)逐步对应。
-- 牌面小工具(count_cards / suit_keys 等)与 huutil.lua 有意重复:fakelua 的包作用域
-- 在编译期按文件生效,包间只能运行时调用,没法在文件级共享局部函数。

local MAX_NUM = 42

-- 表索引:万/筒/条 -> normal,风 -> feng,箭 -> jian
local TABLE_OF = { 1, 1, 1, 2, 3 }

-- 文件级语句在编译期执行,db 与语句句柄落在常量堆,可跨帧存活
local db = sqlite.open("data/majiang.db")
local stmt = {
    db:prepare("SELECT jiang, val FROM ai_normal WHERE key=? ORDER BY seq"),
    db:prepare("SELECT jiang, val FROM ai_feng WHERE key=? ORDER BY seq"),
    db:prepare("SELECT jiang, val FROM ai_jian WHERE key=? ORDER BY seq")
}

-- 注意:文件级 table 是常量,运行时不可修改(fakelua 会在写入时报
-- "attempt to modify a const table"),所以这里不做跨调用的条目缓存。
-- 实际每个 key 在单次操作内只查一次,无需缓存。

-- 取某表某 key 的全部条目,返回 {jiang数组, P数组, 条目数};key 不存在返回 nil
local function fetch(idx, key)
    local st = stmt[idx]
    st:bind(key)
    local jg, ps = {}, {}
    local n = 0
    while true do
        local row = st:step()
        if row == nil then
            break
        end
        n = n + 1
        jg[n] = row.jiang
        ps[n] = row.val
    end
    st:reset()
    if n == 0 then
        return nil
    end
    return { jg, ps, n }
end

local function count_cards(input)
    local cards = {}
    local i = 1
    while i <= MAX_NUM do
        cards[i] = 0
        i = i + 1
    end
    i = 1
    while i <= #input do
        local c = input[i]
        cards[c] = cards[c] + 1
        i = i + 1
    end
    return cards
end

local function suit_keys(cards)
    local keys = {}
    local i, key = 1, 0
    while i <= 9 do
        key = key * 10 + cards[i]
        i = i + 1
    end
    keys[1] = key
    key = 0
    i = 10
    while i <= 18 do
        key = key * 10 + cards[i]
        i = i + 1
    end
    keys[2] = key
    key = 0
    i = 19
    while i <= 27 do
        key = key * 10 + cards[i]
        i = i + 1
    end
    keys[3] = key
    key = 0
    i = 28
    while i <= 31 do
        key = key * 10 + cards[i]
        i = i + 1
    end
    keys[4] = key
    key = 0
    i = 32
    while i <= 34 do
        key = key * 10 + cards[i]
        i = i + 1
    end
    keys[5] = key
    return keys
end

local function contains(list, v)
    local i = 1
    while i <= #list do
        if list[i] == v then
            return true
        end
        i = i + 1
    end
    return false
end

-- 移除第一个等于 card 的元素,返回新表(对应 Java List.remove)
local function remove_first(input, card)
    local tmp = {}
    local removed = false
    local i = 1
    while i <= #input do
        if (not removed) and input[i] == card then
            removed = true
        else
            tmp[#tmp + 1] = input[i]
        end
        i = i + 1
    end
    return tmp
end

local function frequency(list, v)
    local n = 0
    local i = 1
    while i <= #list do
        if list[i] == v then
            n = n + 1
        end
        i = i + 1
    end
    return n
end

-- 对应 C++ calcAITableInfo:沿各花色条目累加概率,jiang 链必须从"无将"起
local function calc_ai_table_info(ret, lists, index, jiang, cur)
    if index > 5 then
        if jiang then
            ret[#ret + 1] = cur
        end
        return
    end
    local e = lists[index]
    if e == nil then
        return
    end
    local jg, ps, n = e[1], e[2], e[3]
    local i = 1
    while i <= n do
        if jiang then
            if jg[i] ~= 1 then
                calc_ai_table_info(ret, lists, index + 1, jiang, cur + ps[i])
            end
        else
            calc_ai_table_info(ret, lists, index + 1, jg[i] == 1, cur + ps[i])
        end
        i = i + 1
    end
end

-- 手牌评分:能听牌直接给"听牌张数 × 10",否则取各花色概率组合的最大值
function Calc(input, guiCard)
    local cards = count_cards(input)
    local guiNum = 0
    local i = 1
    while i <= #guiCard do
        local c = guiCard[i]
        guiNum = guiNum + cards[c]
        cards[c] = 0
        i = i + 1
    end

    local ting = MajiangHu.IsTingCard(cards, guiNum)
    if #ting > 0 then
        return #ting * 10
    end

    local keys = suit_keys(cards)
    local lists = {}
    i = 1
    while i <= 5 do
        -- 与 C++ 一致:查不到的键以 nil 进入列表,递归里直接跳过
        lists[i] = fetch(TABLE_OF[i], keys[i])
        i = i + 1
    end

    local ret = {}
    calc_ai_table_info(ret, lists, 1, false, 0)
    if #ret == 0 then
        return 0
    end
    local d = ret[1]
    i = 2
    while i <= #ret do
        if ret[i] > d then
            d = ret[i]
        end
        i = i + 1
    end
    return d
end

-- 推荐出牌:对每张非鬼牌尝试打出,取评分最高的
function OutAI(input, guiCard)
    local ret = 0
    local max = nil
    local seen = {}
    local i = 1
    while i <= #input do
        local c = input[i]
        if seen[c] == nil then
            seen[c] = true
            if not contains(guiCard, c) then
                local score = Calc(remove_first(input, c), guiCard)
                if max == nil or score > max then
                    max = score
                    ret = c
                end
            end
        end
        i = i + 1
    end
    return ret
end

-- 是否应该碰:碰后评分 + 奖励 >= 碰前评分
function PengAI(input, guiCard, card, award)
    if contains(guiCard, card) then
        return false
    end
    if frequency(input, card) < 2 then
        return false
    end
    local score = Calc(input, guiCard)
    local tmp = remove_first(remove_first(input, card), card)
    return Calc(tmp, guiCard) + award >= score
end

-- 是否应该杠(明杠/暗杠):与碰同构,只是多移除一张
function GangAI(input, guiCard, card, award)
    if contains(guiCard, card) then
        return false
    end
    if frequency(input, card) < 3 then
        return false
    end
    local score = Calc(input, guiCard)
    local tmp = remove_first(remove_first(remove_first(input, card), card), card)
    return Calc(tmp, guiCard) + award >= score
end

-- 是否应该吃指定的这两张
function ChiAI(input, guiCard, card, card1, card2)
    if contains(guiCard, card) or contains(guiCard, card1) or contains(guiCard, card2) then
        return false
    end
    if frequency(input, card1) < 1 or frequency(input, card2) < 1 then
        return false
    end
    local score = Calc(input, guiCard)
    local tmp = remove_first(remove_first(input, card1), card2)
    return Calc(tmp, guiCard) >= score
end

-- 吃牌的所有候选组合里取评分最高的一组;有提升才返回,否则空表
function ChiAIChoices(input, guiCard, card)
    local ret = {}
    if contains(guiCard, card) then
        return ret
    end
    local score = Calc(input, guiCard)
    local score_new_max = 0
    local card1, card2 = 0, 0

    if frequency(input, card - 2) > 0 and frequency(input, card - 1) > 0 and
            MajiangDef.CardType(card) == MajiangDef.CardType(card - 2) and
            MajiangDef.CardType(card) == MajiangDef.CardType(card - 1) then
        local tmp = remove_first(remove_first(input, card - 2), card - 1)
        local score_new = Calc(tmp, guiCard)
        if score_new > score_new_max then
            score_new_max = score_new
            card1 = card - 2
            card2 = card - 1
        end
    end

    if frequency(input, card - 1) > 0 and frequency(input, card + 1) > 0 and
            MajiangDef.CardType(card) == MajiangDef.CardType(card - 1) and
            MajiangDef.CardType(card) == MajiangDef.CardType(card + 1) then
        local tmp = remove_first(remove_first(input, card - 1), card + 1)
        local score_new = Calc(tmp, guiCard)
        if score_new > score_new_max then
            score_new_max = score_new
            card1 = card - 1
            card2 = card + 1
        end
    end

    if frequency(input, card + 1) > 0 and frequency(input, card + 2) > 0 and
            MajiangDef.CardType(card) == MajiangDef.CardType(card + 1) and
            MajiangDef.CardType(card) == MajiangDef.CardType(card + 2) then
        local tmp = remove_first(remove_first(input, card + 1), card + 2)
        local score_new = Calc(tmp, guiCard)
        if score_new > score_new_max then
            score_new_max = score_new
            card1 = card + 1
            card2 = card + 2
        end
    end

    if score_new_max > score then
        ret[1] = card1
        ret[2] = card2
    end
    return ret
end
