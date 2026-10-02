// 测试入口:先加载查表文件,再依次执行所有注册的测试。
#include <chrono>
#include <cstdio>
#include <string>

#include "majiang/api.h"
#include "test_util.h"

int main()
{
    if (!majiang::Load())
    {
        std::fprintf(stderr, "加载查表文件失败\n");
        return 1;
    }

    int failed = 0;
    for (const majtest::Test &t : majtest::tests())
    {
        int before = majtest::failures();
        std::printf("[ RUN ] %s\n", t.name);
        auto begin = std::chrono::steady_clock::now();
        t.fn();
        double ms = std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - begin).count();
        if (majtest::failures() > before)
        {
            std::printf("[FAIL] %s (%.1f ms)\n", t.name, ms);
            failed++;
        }
        else
        {
            std::printf("[PASS] %s (%.1f ms)\n", t.name, ms);
        }
    }

    std::printf("total: %zu, failed: %d\n", majtest::tests().size(), failed);
    return failed == 0 ? 0 : 1;
}
