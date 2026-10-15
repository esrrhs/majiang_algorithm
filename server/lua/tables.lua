package "MajiangTables"

-- 查表数据由 C++ cpp/ 库加载(majiang_load)。此处仅作就绪标记,兼容旧调用。

local loaded = container.map()

function Load()
    if loaded:get("ok") then return true end
    local ok = majiang_load()
    if not ok then
        error("majiang_load 失败:请确认 data/ 查表文件存在")
    end
    loaded:set("ok", 1)
    return true
end

function IsLoaded()
    return loaded:get("ok") == 1
end
