package "MajiangHu"

-- 胡 / 听判定。查表数据放在 sqlite(data/majiang.db),按 key 点查,不再全量载入内存。
--
-- 表结构 hu_{normal,feng,jian}(key, seq, need_gui, jiang, val):
--   key      该花色计数串展开成的十进制整数,万/筒/条共用 normal 表,风、箭各自一张;
--   seq      同一 key 内的条目序号,保持生成时的顺序;
--   need_gui 需要几张鬼牌;
--   jiang    该条目是否要求有将;
--   val      hupai 折叠整数(每位一个 0/1 标记),-1 表示无需进牌即已胡。
--
-- 判定流程与 C++ 版(cpp/src/hu_util.cpp 的 isHuTableInfo / isTingHuTableInfo)逐步对应。

-- 牌编号区间,与 def.lua 一致;这里用字面量,热路径里不必反复调函数
local MAX_NUM = 42

local N_DIGIT = { 9, 4, 3 } -- normal(9 位)/ feng(4 位)/ jian(3 位)

-- 表索引:万/筒/条 -> normal,风 -> feng,箭 -> jian
local TABLE_OF = { 1, 1, 1, 2, 3 }

-- 文件级语句在编译期执行,db 与语句句柄落在常量堆,可跨帧存活
local db = sqlite.open("data/majiang.db")
local stmt = {
    db:prepare("SELECT need_gui, jiang, val FROM hu_normal WHERE key=? ORDER BY seq"),
    db:prepare("SELECT need_gui, jiang, val FROM hu_feng WHERE key=? ORDER BY seq"),
    db:prepare("SELECT need_gui, jiang, val FROM hu_jian WHERE key=? ORDER BY seq")
}

-- 注意:文件级 table 是常量,运行时不可修改(fakelua 会在写入时报
-- "attempt to modify a const table"),所以这里不做跨调用的条目缓存。
-- 实际每个 key 在单次操作内只查一次(递归用的是已取回的列表),无需缓存。

-- 把 hupai 折叠整数展开成每位数字的数组(低位在末尾,与 C++ 还原顺序一致)
local function unfold(v, n)
    local d = {}
    local i = n
    local tmp = v
    while i >= 1 do
        d[i] = tmp % 10
        tmp = math.floor(tmp / 10)
        i = i - 1
    end
    return d
end

-- 取某表某 key 的全部条目,返回 {need_gui数组, jiang数组, hupai数字数组(nil=无需进牌), 条目数}
-- key 不存在返回 nil(与 C++ 查不到键返回空指针的语义一致)
local function fetch(idx, key)
    local st = stmt[idx]
    st:bind(key)
    local ng, jg, hp = {}, {}, {}
    local n = 0
    while true do
        local row = st:step()
        if row == nil then
            break
        end
        n = n + 1
        ng[n] = row.need_gui
        jg[n] = row.jiang
        local v = row.val
        if v == -1 then
            hp[n] = nil
        else
            hp[n] = unfold(v, N_DIGIT[idx])
        end
    end
    st:reset()
    if n == 0 then
        return nil
    end
    return { ng, jg, hp, n }
end

-- 手牌列表 -> 计数数组(下标即牌编号 1..42)
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

-- 五个花色的计数串 key(万/筒/条/风/箭),对应 C++ IsHuCard 里的同名变量
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

-- 对应 C++ isHuTableInfo:各花色条目组合,判断能否在还剩 guiNum 张鬼牌时胡牌
local function is_hu_table_info(lists, count, index, guiNum, jiang)
    if index > count then
        if guiNum % 3 == 0 then
            return jiang
        end
        return guiNum % 3 == 2 and not jiang
    end
    local e = lists[index]
    local ng, jg, n = e[1], e[2], e[4]
    local i = 1
    while i <= n do
        if ng[i] <= guiNum then
            if jiang then
                if jg[i] ~= 1 then
                    if is_hu_table_info(lists, count, index + 1, guiNum - ng[i], jiang) then
                        return true
                    end
                end
            else
                if is_hu_table_info(lists, count, index + 1, guiNum - ng[i], jg[i] == 1) then
                    return true
                end
            end
        end
        i = i + 1
    end
    return false
end

-- 对应 C++ isTingHuTableInfo:与 is_hu_table_info 同构,但跳过当前听牌所在的花色
local function is_ting_hu_table_info(tmp_type, tmp, count, index, guiNum, jiang, ting_type)
    if index > count then
        return guiNum == 0 and jiang
    end
    if tmp_type[index] == ting_type then
        return is_ting_hu_table_info(tmp_type, tmp, count, index + 1, guiNum, jiang, ting_type)
    end
    local e = tmp[index]
    local ng, jg, hp, n = e[1], e[2], e[3], e[4]
    local i = 1
    while i <= n do
        if hp[i] == nil and ng[i] <= guiNum then
            if jiang then
                if jg[i] ~= 1 then
                    if is_ting_hu_table_info(tmp_type, tmp, count, index + 1, guiNum - ng[i], jiang, ting_type) then
                        return true
                    end
                end
            else
                if is_ting_hu_table_info(tmp_type, tmp, count, index + 1, guiNum - ng[i], jg[i] == 1, ting_type) then
                    return true
                end
            end
        end
        i = i + 1
    end
    return false
end

-- 判胡牌。cards 为计数数组,guiNum 为已确定的鬼牌张数
function IsHuCard(cards, guiNum)
    local keys = suit_keys(cards)

    -- 只收非零花色;条目需满足"无需进牌即胡"(hupai 为 nil)且 need_gui <= guiNum
    local lists = {}
    local count = 0
    local i = 1
    while i <= 5 do
        local key = keys[i]
        if key ~= 0 then
            local e = fetch(TABLE_OF[i], key)
            if e == nil or e[4] == 0 then
                return false
            end
            local src_ng, src_jg, src_hp, n = e[1], e[2], e[3], e[4]
            local f_ng, f_jg = {}, {}
            local m = 0
            local t = 1
            while t <= n do
                if src_hp[t] == nil and src_ng[t] <= guiNum then
                    m = m + 1
                    f_ng[m] = src_ng[t]
                    f_jg[m] = src_jg[t]
                end
                t = t + 1
            end
            if m == 0 then
                return false
            end
            count = count + 1
            lists[count] = { f_ng, f_jg, nil, m }
        end
        i = i + 1
    end
    return is_hu_table_info(lists, count, 1, guiNum, false)
end

-- 听牌。cards 为计数数组,guiNum 为已确定的鬼牌张数,返回听牌列表(空表=不听)
function IsTingCard(cards, guiNum)
    local keys = suit_keys(cards)

    -- 与 C++ 一致:五个 key 全部查表(含 0),任一查不到直接判定不听
    local all = {}
    local i = 1
    while i <= 5 do
        local e = fetch(TABLE_OF[i], keys[i])
        if e == nil then
            return {}
        end
        all[i] = e
        i = i + 1
    end

    local tmp_type, tmp = {}, {}
    local count = 0
    i = 1
    while i <= 5 do
        if keys[i] ~= 0 then
            count = count + 1
            tmp_type[count] = i
            tmp[count] = all[i]
        end
        i = i + 1
    end

    local ret = {}
    local t = 1
    while t <= 5 do
        local e = all[t]
        local ng, jg, hp, n = e[1], e[2], e[3], e[4]
        local hit = {} -- 已被前面候选覆盖的听牌位
        local i2 = 1
        while i2 <= n do
            local digits = hp[i2]
            if digits ~= nil and ng[i2] <= guiNum then
                local covered = true
                local j = 1
                while j <= #digits do
                    if digits[j] > 0 and (hit[j] == nil or hit[j] == 0) then
                        covered = false
                        break
                    end
                    j = j + 1
                end
                if not covered and is_ting_hu_table_info(tmp_type, tmp, count, 1, guiNum - ng[i2], jg[i2] == 1, t) then
                    j = 1
                    while j <= #digits do
                        if digits[j] > 0 then
                            if hit[j] == nil or hit[j] == 0 then
                                ret[#ret + 1] = MajiangDef.ToCard(t, j - 1)
                            end
                            hit[j] = (hit[j] or 0) + 1
                        end
                        j = j + 1
                    end
                end
            end
            i2 = i2 + 1
        end
        t = t + 1
    end
    return ret
end

-- 手牌 + 鬼牌列表 -> 是否已胡牌(extra:额外的第 extra 张牌,0 表示无)
function IsHuExtra(input, guiCard, extra)
    local cards = count_cards(input)
    local guiNum = 0
    local i = 1
    while i <= #guiCard do
        local c = guiCard[i]
        guiNum = guiNum + cards[c]
        cards[c] = 0
        i = i + 1
    end
    if extra ~= 0 then
        cards[extra] = cards[extra] + 1
    end
    return IsHuCard(cards, guiNum)
end

-- 手牌 + 鬼牌列表 -> 听牌列表
function IsTingExtra(input, guiCard)
    local cards = count_cards(input)
    local guiNum = 0
    local i = 1
    while i <= #guiCard do
        local c = guiCard[i]
        guiNum = guiNum + cards[c]
        cards[c] = 0
        i = i + 1
    end
    return IsTingCard(cards, guiNum)
end
