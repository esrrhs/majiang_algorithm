#!/usr/bin/env python3
"""FakeLua 麻将服务端 HTTP 功能测试。

移植自 ref_go_web/web_test.go(那份又是 Java MahjongGameWebIntegrationTest 的移植),
保证 FakeLua 版与 Go 版对外契约一致:对局初始化、听牌、AI 观战结算、view JSON 键、
静态资源、算法接口。

用法:
    python3 server/tests/api_test.py                 # 自己拉起服务(在随机端口)再测
    python3 server/tests/api_test.py --base-url URL  # 打已经跑着的服务
    python3 server/tests/api_test.py --jit 2         # 传给 flua 的 jit 类型(0=tcc 1=gcc 2=interp)

只依赖标准库,CI 里不用装任何东西。
"""
import argparse
import json
import os
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

# /api/game/new 返回的 view 必须具备这些键(与 Java/Go 前端契约一致)
VIEW_KEYS = [
    "gameId", "wallCount", "guiCards", "guiIndicator", "dealerSeat",
    "currentSeat", "phase", "lastDiscard", "lastDiscardSeat", "players",
    "availableActions", "currentTingTargets", "discardToTing", "logs",
    "cardRemainCounts", "spectatorMode",
]
PLAYER_KEYS = ["isAi", "isTing", "isHu", "tileCount"]
ACTION_KEYS = ["canDiscard", "canHu"]
ALGO_KEYS = [
    "cards", "cardsStr", "guiCards", "isHu", "huTimeMicros",
    "tingCards", "tingCardsStr", "tingTimeMicros",
    "recommendedOut", "recommendedOutStr", "aiScore", "aiTimeMicros",
]

PASS = 0
FAIL = 0


class Failure(Exception):
    pass


def check(cond, msg):
    if not cond:
        raise Failure(msg)


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def _open(self, url):
        try:
            return urllib.request.urlopen(url, timeout=30)
        except urllib.error.HTTPError as e:
            e.url_note = "GET %s" % url
            raise

    def get(self, path):
        """GET,4xx/5xx 直接抛(用例不该用到)。"""
        r = self._open(self.base + path)
        return r.status, r.read(), r.headers.get("Content-Type", "")

    def get_raw(self, path):
        """GET,但把 4xx/5xx 当普通结果返回(用来断言 404 之类)。"""
        try:
            r = self._open(self.base + path)
            return r.status, r.read(), r.headers.get("Content-Type", "")
        except urllib.error.HTTPError as e:
            return e.code, e.read(), ""

    def post(self, path, body=""):
        url = self.base + path
        req = urllib.request.Request(
            url,
            data=body.encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            r = urllib.request.urlopen(req, timeout=30)
        except urllib.error.HTTPError as e:
            e.url_note = "POST %s <- %s" % (url, body)
            raise
        return json.loads(r.read().decode("utf-8"))


def missing_keys(obj, keys):
    return [k for k in keys if k not in obj]


def test_game_initialization(c):
    v = c.post("/api/game/new", '{"guiMode":"random_flip","spectator":false}')
    miss = missing_keys(v, VIEW_KEYS)
    check(not miss, "view 缺少键 %s" % miss)
    for i, p in enumerate(v["players"]):
        miss = missing_keys(p, PLAYER_KEYS)
        check(not miss, "players[%d] 缺少键 %s" % (i, miss))
    miss = missing_keys(v["availableActions"], ACTION_KEYS)
    check(not miss, "availableActions 缺少键 %s" % miss)

    check(v["phase"] == "DISCARD", "phase = %s, want DISCARD" % v["phase"])
    check(v["currentSeat"] == 0, "currentSeat = %s, want 0" % v["currentSeat"])
    check(len(v["players"]) == 4, "players = %d, want 4" % len(v["players"]))
    check(v["players"][0]["tileCount"] == 14,
          "player0 tileCount = %s, want 14" % v["players"][0]["tileCount"])
    check(v["players"][1]["tileCount"] == 13,
          "player1 tileCount = %s, want 13" % v["players"][1]["tileCount"])
    check(len(v["guiCards"]) > 0, "guiCards 不应为空")
    check(v["wallCount"] > 0, "wallCount = %s, should be > 0" % v["wallCount"])


def test_ting_detection_in_game(c):
    v = c.post("/api/game/new", '{"guiMode":"card","customGuiCard":34,"spectator":false}')
    check(v["guiCards"] == [34], "card 模式鬼牌应为 34, got %s" % v["guiCards"])
    check("discardToTing" in v, "discardToTing 不应缺失")

    # discardToTing 是 map[string][]TingTarget:空表在 fakelua 里编码成 [](Go 版是 {}),
    # 前端只用 map[card] 取值,两者等价。非空时必须真的是对象(键为牌号字符串),
    # 否则前端 Object.keys 拿到的是数组下标。起手 14 张几乎不可能"打出即听",
    # 所以非空形状放到观战推进里去撞。
    check(isinstance(v["cardRemainCounts"], dict),
          "cardRemainCounts 应为对象, got %r" % type(v["cardRemainCounts"]).__name__)
    check(set(v["cardRemainCounts"].keys()) == {str(i) for i in range(1, 35)},
          "cardRemainCounts 键应为 \"1\"..\"34\", got %s" % sorted(v["cardRemainCounts"].keys()))
    for n in v["cardRemainCounts"].values():
        check(0 <= n <= 4, "剩余张数应在 0..4, got %s" % n)

    # 观战推进到有人听牌/有"打出即听"为止,校验 map 的 JSON 形状
    found = None
    ting_seen = False
    for _ in range(3):
        v = c.post("/api/game/new", '{"guiMode":"random_flip","spectator":true}')
        for _ in range(300):
            if any(p["isTing"] for p in v["players"]):
                ting_seen = True
            d = v.get("discardToTing")
            if isinstance(d, dict) and len(d) > 0:
                found = d
                break
            if v["phase"] == "GAME_OVER":
                break
            v = c.post("/api/game/step", "")["view"]
        if found is not None:
            break
    check(ting_seen, "观战推进过程中应出现听牌玩家")
    check(found is not None, "观战推进过程中应出现非空的 discardToTing")
    for k, targets in found.items():
        check(k.isdigit(), "discardToTing 的键应为牌号字符串, got %r" % k)
        check(isinstance(targets, list) and len(targets) > 0,
              "discardToTing[%r] 应为非空 TingTarget 列表" % k)
        for t in targets:
            check("card" in t and "remainingCount" in t,
                  "TingTarget 缺少 card/remainingCount: %r" % t)


def test_ai_simulation_steps(c):
    c.post("/api/game/new", '{"guiMode":"random_flip","spectator":true}')
    steps = 0
    m = None
    while steps < 2000:
        m = c.post("/api/game/step", "")
        steps += 1
        if not m["stepped"]:
            break
        if m["view"]["phase"] == "GAME_OVER":
            break
    check(steps > 0, "AI 步进应推进对局状态")
    check(m is not None and m["view"]["phase"] == "GAME_OVER",
          "2000 步内应结束对局, phase = %s" % (m["view"]["phase"] if m else "?"))

    v = m["view"]
    check(isinstance(v.get("settlement"), dict), "GAME_OVER 后应有 settlement")
    total = sum(p["score"] for p in v["players"])
    check(total == 4000, "分数总和应为 4*1000, got %d" % total)
    st = v["settlement"]
    if not st["isDraw"]:
        check(0 <= st["winnerSeat"] <= 3, "winnerSeat 越界: %s" % st["winnerSeat"])
        check(len(st["patterns"]) > 0, "胡牌应有番型")


def test_step_through_spectator(c):
    m = c.post("/api/game/new", '{"guiMode":"random_flip","spectator":true}')
    check("stepped" not in m or True, "")
    for _ in range(500):
        m = c.post("/api/game/step", "")
        check(isinstance(m.get("view"), dict), "step 响应应包含 view")
        if not m["stepped"]:
            break
        if m["view"]["phase"] == "GAME_OVER":
            break
    check(isinstance(m.get("view"), dict), "step 响应应包含 view")


def test_http_api_smoke(c):
    status, body, _ct = c.get("/api/status")
    check(status == 200, "/api/status status = %s" % status)
    d = json.loads(body.decode("utf-8"))
    check(d.get("status") == "ok" and d.get("ready") is True,
          "unexpected status body: %s" % body)

    # 静态资源
    status, body, ct = c.get("/")
    check(status == 200, "/ status = %s" % status)
    check(b"<!DOCTYPE html>" in body, "index.html 应在 / 上被提供")
    check("text/html" in ct, "index content-type = %s" % ct)
    status, _body, _ct = c.get("/style.css")
    check(status == 200, "style.css status = %s" % status)
    status, _body, _ct = c.get("/tiles/Man1.svg")
    check(status == 200, "tiles/Man1.svg status = %s" % status)
    # 目录穿越必须被挡掉
    status, _body, _ct = c.get_raw("/../README.md")
    check(status == 404, "目录穿越应返回 404, got %s" % status)
    # 找不到的静态资源也是 404,不能是 200 空壳
    status, _body, _ct = c.get_raw("/nope.html")
    check(status == 404, "缺失资源应返回 404, got %s" % status)

    v = c.post("/api/game/new", '{"guiMode":"random_flip","spectator":true}')
    miss = missing_keys(v, VIEW_KEYS)
    check(not miss, "view 缺少键 %s" % miss)
    for i, p in enumerate(v["players"]):
        miss = missing_keys(p, PLAYER_KEYS)
        check(not miss, "players[%d] 缺少键 %s" % (i, miss))
    miss = missing_keys(v["availableActions"], ACTION_KEYS)
    check(not miss, "availableActions 缺少键 %s" % miss)


def test_algo_test_api(c):
    m = c.post("/api/algo/test",
               '{"cards":[1,1,1,2,3,4,10,11,12,25,26,27,28,28],"gui":29}')
    miss = missing_keys(m, ALGO_KEYS)
    check(not miss, "algo 响应缺少键 %s" % miss)
    # 111万 234万 123筒 789条 东东 = 标准胡
    check(m["isHu"] is True, "14 张标准胡应为 true, got %s" % m["isHu"])
    check(m["recommendedOutStr"] != "", "14 张手牌应有推荐出牌")

    # 3 张牌不可能胡
    m = c.post("/api/algo/test", '{"cards":"1万,2万,3万","gui":5}')
    check(m["isHu"] is False, "3 张牌不应为胡, got %s" % m["isHu"])

    # 字符串手牌 + 判胡
    m = c.post("/api/algo/test",
               '{"cards":"1万,1万,1万,2万,3万,4万,5万,6万,7万,8万,9万,东,东,东"}')
    check(m["isHu"] is True, "九莲宝灯式 14 张手牌应为胡, got %s" % m["isHu"])
    check(m["recommendedOut"] > 0, "应有推荐出牌")

    # 脏输入:牌号越界 / 中文牌面,必须在 HTTP 边界被挡掉而不是让服务崩掉
    m = c.post("/api/algo/test", '{"cards":["东"],"gui":29}')
    check(isinstance(m.get("cards"), list), "脏输入应返回 cards 列表")
    check(m["isHu"] is False, "越界牌号不应判胡, got %s" % m["isHu"])
    status, _body, _ct = c.get("/api/status")
    check(status == 200, "处理脏输入后服务仍应存活, got %s" % status)


TESTS = [
    ("game_initialization", test_game_initialization),
    ("ting_detection_in_game", test_ting_detection_in_game),
    ("ai_simulation_steps", test_ai_simulation_steps),
    ("step_through_spectator", test_step_through_spectator),
    ("http_api_smoke", test_http_api_smoke),
    ("algo_test_api", test_algo_test_api),
]


def find_flua(explicit):
    if explicit:
        return explicit
    if os.environ.get("FLUA"):
        return os.environ["FLUA"]
    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.dirname(os.path.dirname(here))
    for p in ["/usr/local/bin/flua",
              os.path.join(root, "fakelua", "build", "bin", "flua"),
              os.path.join(root, "..", "fakelua", "build", "bin", "flua")]:
        if os.path.exists(p):
            return p
    return "flua"


def free_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def run(args):
    global PASS, FAIL
    if args.base_url:
        base = args.base_url
        proc = None
    else:
        # 服务端的静态目录与查表库都是相对 server/ 的路径,必须从 server/ 起
        here = os.path.dirname(os.path.abspath(__file__))
        server_dir = os.path.dirname(here)
        port = free_port()
        flua = find_flua(args.flua)
        env = dict(os.environ)
        env["MAJIANG_PORT"] = str(port)
        proc = subprocess.Popen(
            [flua, "lua", "--jit_type=%d" % args.jit],
            cwd=server_dir, env=env,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )
        base = "http://127.0.0.1:%d" % port
        try:
            wait_ready(base, proc, args.boot_timeout)
        except Exception:
            dump_server_output(proc)
            raise

    c = Client(base)
    try:
        for name, fn in TESTS:
            t0 = time.time()
            try:
                fn(c)
                print("  PASS  %-24s (%.2fs)" % (name, time.time() - t0))
                PASS += 1
            except Failure as e:
                print("  FAIL  %-24s (%.2fs)\n        %s" % (name, time.time() - t0, e))
                FAIL += 1
            except Exception as e:  # 服务端抛出/连接断开等
                note = getattr(e, "url_note", "")
                print("  ERROR %-24s (%.2fs)\n        %s: %s%s"
                      % (name, time.time() - t0, type(e).__name__, e,
                         "  [%s]" % note if note else ""))
                FAIL += 1
    finally:
        if proc is not None:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
    return 0 if FAIL == 0 else 1


def wait_ready(base, proc, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if proc.poll() is not None:
            raise Failure("服务进程已退出, code=%s" % proc.returncode)
        try:
            r = urllib.request.urlopen(base + "/api/status", timeout=2)
            if r.status == 200:
                return
        except Exception:
            pass
        time.sleep(0.2)
    raise Failure("服务在 %ss 内未就绪" % timeout)


def dump_server_output(proc):
    if proc is None:
        return
    try:
        proc.terminate()
        out, _ = proc.communicate(timeout=5)
    except Exception:
        out = b""
    if out:
        print("---- 服务输出 ----", file=sys.stderr)
        print(out.decode("utf-8", "replace")[-4000:], file=sys.stderr)


def main():
    ap = argparse.ArgumentParser(description="FakeLua majiang server API tests")
    ap.add_argument("--base-url", default="", help="打已经跑着的服务,而不是自己拉起")
    ap.add_argument("--flua", default="", help="flua 可执行文件路径")
    ap.add_argument("--jit", type=int, default=0, help="flua jit 类型: 0=tcc 1=gcc 2=interp")
    # 首次启动要把 8 个 lua 文件走一遍 TCC + GCC JIT,慢机器上要几十秒
    ap.add_argument("--boot-timeout", type=float, default=300.0, help="等待服务就绪的秒数")
    args = ap.parse_args()

    print("FakeLua majiang server API tests")
    code = run(args)
    print("---- %d passed, %d failed ----" % (PASS, FAIL))
    return code


if __name__ == "__main__":
    sys.exit(main())
