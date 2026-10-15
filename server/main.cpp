// FakeLua 宿主:编译 server/lua,注册 cpp 算法 native,启动 HTTP 或 smoke。
#include "fakelua.h"

#include <filesystem>
#include <iostream>
#include <string>
#include <string_view>

void RegisterMajiangNatives(fakelua::State *s);

using namespace fakelua;
namespace fs = std::filesystem;

static bool CompileAllWith(State *s, const fs::path &dir, JITType jit) {
    const char *files[] = {
            "def.lua",
            "tables.lua",
            "huutil.lua",
            "aiutil.lua",
            "fan.lua",
            "game.lua",
            "http_server.lua",
            "main.lua",
    };
    CompileConfig cfg;
    cfg.debug_mode = false;
    cfg.disable_jit[JIT_GCC] = (jit != JIT_GCC);
    cfg.disable_jit[JIT_TCC] = (jit != JIT_TCC);
    cfg.disable_jit[JIT_INTERP] = (jit != JIT_INTERP);
    for (const char *name: files) {
        fs::path p = dir / name;
        if (!fs::exists(p)) {
            std::cerr << "missing lua file: " << p << "\n";
            return false;
        }
        std::cout << "compile " << p << "\n";
        CompileFile(s, p.string(), cfg);
    }
    return true;
}

int main(int argc, char **argv) {
    std::string entry = "MahjongServer.main";
    int jit_type = 0; // TCC
    for (int i = 1; i < argc; ++i) {
        std::string_view a = argv[i];
        if (a.starts_with("--entry=")) {
            entry = std::string(a.substr(8));
        } else if (a.starts_with("--jit_type=")) {
            jit_type = std::stoi(std::string(a.substr(11)));
        } else if (a == "smoke") {
            entry = "MahjongServer.smoke";
        }
    }

    if (fs::exists("lua/main.lua")) {
        // already in server/
    } else if (fs::exists("server/lua/main.lua")) {
        fs::current_path("server");
    } else if (fs::exists("../lua/main.lua")) {
        fs::current_path("..");
    }

    const FakeluaStateGuard guard;
    State *s = guard.GetState();
    try {
        RegisterMajiangNatives(s);
        if (!CompileAllWith(s, fs::path("lua"), static_cast<JITType>(jit_type))) {
            return 2;
        }
        int code = 0;
        Call(s, static_cast<JITType>(jit_type), entry, code);
        return code;
    } catch (const std::exception &e) {
        std::cerr << "majiangserver_fakelua: " << e.what() << "\n";
        return 1;
    }
}
