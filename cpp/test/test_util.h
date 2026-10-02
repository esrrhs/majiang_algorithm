// 极简单测框架:注册宏 + 断言宏,避免为仓库引入第三方测试依赖。
#pragma once

#include <cstdio>
#include <string>
#include <vector>

namespace majtest
{

struct Test
{
    const char *name;
    void (*fn)();
};

inline std::vector<Test> &tests()
{
    static std::vector<Test> t;
    return t;
}

inline int &failures()
{
    static int f = 0;
    return f;
}

struct Registrar
{
    Registrar(const char *name, void (*fn)()) { tests().push_back({ name, fn }); }
};

inline void markFail(const char *file, int line, const std::string &msg)
{
    std::fprintf(stderr, "  FAIL %s:%d %s\n", file, line, msg.c_str());
    failures()++;
}

inline std::string toStr(const std::string &v)
{
    return v;
}
inline std::string toStr(const char *v)
{
    return v;
}
inline std::string toStr(bool v)
{
    return v ? "true" : "false";
}
inline std::string toStr(int v)
{
    return std::to_string(v);
}
inline std::string toStr(unsigned v)
{
    return std::to_string(v);
}
inline std::string toStr(long v)
{
    return std::to_string(v);
}
inline std::string toStr(long long v)
{
    return std::to_string(v);
}
inline std::string toStr(size_t v)
{
    return std::to_string(v);
}
inline std::string toStr(double v)
{
    return std::to_string(v);
}

template <typename A, typename B>
void expectEqImpl(const A &a, const B &b, const char *ea, const char *eb, const char *file, int line)
{
    if (!(a == b))
    {
        markFail(file, line, std::string(ea) + " == " + eb + " (lhs=" + toStr(a) + ", rhs=" + toStr(b) + ")");
    }
}

} // namespace majtest

#define MAJ_TEST(name)                                           \
    static void name##_impl();                                   \
    static ::majtest::Registrar name##_reg(#name, &name##_impl); \
    static void name##_impl()

#define EXPECT(cond)                                                                      \
    do                                                                                    \
    {                                                                                     \
        if (!(cond))                                                                      \
        {                                                                                 \
            ::majtest::markFail(__FILE__, __LINE__, "expected true: " #cond);             \
        }                                                                                 \
    } while (0)

#define EXPECT_EQ(a, b) ::majtest::expectEqImpl((a), (b), #a, #b, __FILE__, __LINE__)
