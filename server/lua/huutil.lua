package "MajiangHu"

-- 胡 / 听:转发到 C++ majiang 库(RegisterNativeFunction)

function IsHuExtra(input, guiCard, extra)
    return majiang_is_hu_extra(input, guiCard, extra)
end

function IsTingExtra(input, guiCard)
    local t = majiang_is_ting_extra(input, guiCard)
    if t == nil then return {} end
    return t
end

-- 兼容旧内部调用(AI Calc 已走 C++);保留空壳避免误引用崩溃
function IsTingCard(cards, guiNum)
    return {}
end

function IsHuCard(cards, guiNum)
    return false
end
