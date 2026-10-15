package "MajiangAi"

-- AI:转发到 C++ majiang 库

function Calc(input, guiCard)
    return majiang_calc(input, guiCard)
end

function OutAI(input, guiCard)
    return majiang_out_ai(input, guiCard)
end

function PengAI(input, guiCard, card, award)
    return majiang_peng_ai(input, guiCard, card, award)
end

function GangAI(input, guiCard, card, award)
    return majiang_gang_ai(input, guiCard, card, award)
end

function ChiAI(input, guiCard, card, card1, card2)
    return majiang_chi_ai(input, guiCard, card, card1, card2)
end

function ChiAIChoices(input, guiCard, card)
    local t = majiang_chi_ai_choices(input, guiCard, card)
    if t == nil then return {} end
    return t
end
