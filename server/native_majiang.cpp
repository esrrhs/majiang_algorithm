// 将 cpp/ 查表算法以 FakeLua native 形式暴露给 Lua 对局层。
#include "fakelua.h"
#include "majiang/ai_util.h"
#include "majiang/api.h"
#include "majiang/hu_util.h"

#include <map>
#include <string>
#include <vector>

using namespace fakelua;

static int AsInt(VarInterface *v) {
    if (v == nullptr) return 0;
    if (v->ViGetType() == VarInterface::Type::INT) return static_cast<int>(v->ViGetInt());
    if (v->ViGetType() == VarInterface::Type::FLOAT) return static_cast<int>(v->ViGetFloat());
    return 0;
}

static double AsFloat(VarInterface *v) {
    if (v == nullptr) return 0;
    if (v->ViGetType() == VarInterface::Type::FLOAT) return v->ViGetFloat();
    if (v->ViGetType() == VarInterface::Type::INT) return static_cast<double>(v->ViGetInt());
    return 0;
}

static std::vector<int> TableToInts(VarInterface *t) {
    std::vector<int> out;
    if (t == nullptr || t->ViGetType() != VarInterface::Type::TABLE) {
        return out;
    }
    std::map<int64_t, int> ordered;
    const size_t n = t->ViGetTableSize();
    for (size_t i = 0; i < n; ++i) {
        auto [k, v] = t->ViGetTableKv(static_cast<int>(i));
        if (k == nullptr || v == nullptr) continue;
        if (k->ViGetType() != VarInterface::Type::INT) continue;
        int val = 0;
        if (v->ViGetType() == VarInterface::Type::INT) {
            val = static_cast<int>(v->ViGetInt());
        } else if (v->ViGetType() == VarInterface::Type::FLOAT) {
            val = static_cast<int>(v->ViGetFloat());
        } else {
            continue;
        }
        ordered[k->ViGetInt()] = val;
    }
    out.reserve(ordered.size());
    for (auto &kv: ordered) {
        out.push_back(kv.second);
    }
    return out;
}

static VarInterface *BoolResult(bool v) {
    auto *r = new SimpleVarImpl();
    r->ViSetBool(v);
    return r;
}

static VarInterface *IntResult(int64_t v) {
    auto *r = new SimpleVarImpl();
    r->ViSetInt(v);
    return r;
}

static VarInterface *FloatResult(double v) {
    auto *r = new SimpleVarImpl();
    r->ViSetFloat(v);
    return r;
}

static VarInterface *IntArrayResult(const std::vector<int> &vals) {
    auto *r = new SimpleVarImpl();
    std::vector<SimpleVarImpl> keys(vals.size());
    std::vector<SimpleVarImpl> vs(vals.size());
    std::vector<std::pair<VarInterface *, VarInterface *>> kvs;
    kvs.reserve(vals.size());
    for (size_t i = 0; i < vals.size(); ++i) {
        keys[i].ViSetInt(static_cast<int64_t>(i + 1));
        vs[i].ViSetInt(vals[i]);
        kvs.emplace_back(&keys[i], &vs[i]);
    }
    r->ViSetTable(kvs);
    return r;
}

void RegisterMajiangNatives(State *s) {
    RegisterNativeVarFunction(s, "majiang_load", 0, false,
                              [](State *, const std::vector<VarInterface *> &) -> VarInterface * {
                                  return BoolResult(majiang::Load());
                              });

    RegisterNativeVarFunction(s, "majiang_is_hu_extra", 3, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return BoolResult(majiang::IsHuExtra(cards, gui, AsInt(args[2])));
                              });

    RegisterNativeVarFunction(s, "majiang_is_ting_extra", 2, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return IntArrayResult(majiang::IsTingExtra(cards, gui));
                              });

    RegisterNativeVarFunction(s, "majiang_out_ai", 2, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return IntResult(majiang::OutAI(cards, gui));
                              });

    RegisterNativeVarFunction(s, "majiang_calc", 2, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return FloatResult(majiang::Calc(cards, gui));
                              });

    RegisterNativeVarFunction(s, "majiang_peng_ai", 4, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return BoolResult(majiang::PengAI(cards, gui, AsInt(args[2]), AsFloat(args[3])));
                              });

    RegisterNativeVarFunction(s, "majiang_gang_ai", 4, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return BoolResult(majiang::GangAI(cards, gui, AsInt(args[2]), AsFloat(args[3])));
                              });

    RegisterNativeVarFunction(s, "majiang_chi_ai", 5, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return BoolResult(majiang::ChiAI(cards, gui, AsInt(args[2]), AsInt(args[3]), AsInt(args[4])));
                              });

    RegisterNativeVarFunction(s, "majiang_chi_ai_choices", 3, false,
                              [](State *, const std::vector<VarInterface *> &args) -> VarInterface * {
                                  auto cards = TableToInts(args[0]);
                                  auto gui = TableToInts(args[1]);
                                  return IntArrayResult(majiang::ChiAIChoices(cards, gui, AsInt(args[2])));
                              });
}
