package "MajiangTables"

-- 查表数据在 sqlite(data/majiang.db),由 MajiangHu / MajiangAi 在文件级各自打开连接。
-- 本模块只负责就绪校验:确认库可读且确实有数据。
-- 数据由 tools/gen_majiang_db.py 从 majiang_clien_*.txt / majiang_ai_*.txt 生成。

local db = sqlite.open("data/majiang.db")
local state = container.map()

function Load()
    if state:get("ok") == 1 then
        return true
    end
    -- 抽查一个固定 key(0 在 6 张表里都存在),比 COUNT(*) 扫全表快得多
    local rows = db:exec("SELECT (SELECT COUNT(*) FROM hu_normal WHERE key=0) + (SELECT COUNT(*) FROM ai_normal WHERE key=0) AS n")
    if rows == nil or rows[1] == nil or rows[1].n == nil or rows[1].n < 1 then
        error("majiang.db 查表数据缺失:请先运行 python3 tools/gen_majiang_db.py 生成 data/majiang.db")
    end
    state:set("ok", 1)
    return true
end

function IsLoaded()
    return state:get("ok") == 1
end
