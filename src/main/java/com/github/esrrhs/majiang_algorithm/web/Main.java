package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.AIUtil;
import com.github.esrrhs.majiang_algorithm.HuUtil;
import com.github.esrrhs.majiang_algorithm.MaJiangDef;

/**
 * 麻将算法与网页对战平台主入口
 */
public class Main {
    public static void main(String[] args) {
        int port = 8080;
        boolean cliMode = false;

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                } catch (NumberFormatException e) {
                    System.err.println("无效端口参数，使用默认端口: " + port);
                }
            } else if ("--cli".equalsIgnoreCase(arg)) {
                cliMode = true;
            }
        }

        System.out.println("==================================================");
        System.out.println("   麻将算法 & AI 网页对战平台 (Majiang Algorithm)   ");
        System.out.println("==================================================");
        System.out.println("正在预加载高性能胡牌与 AI 查表数据，请稍候...");

        long start = System.currentTimeMillis();
        HuUtil.load();
        long huLoaded = System.currentTimeMillis();
        System.out.printf("胡牌查表 (HuTable) 加载完成，耗时: %d ms%n", (huLoaded - start));

        AIUtil.load();
        long aiLoaded = System.currentTimeMillis();
        System.out.printf("AI 查表 (AITable) 加载完成，耗时: %d ms%n", (aiLoaded - huLoaded));
        System.out.printf("全套查表加载完毕，累计耗时: %d ms%n", (aiLoaded - start));
        System.out.println("--------------------------------------------------");

        if (cliMode) {
            runCliSimulation();
        } else {
            try {
                MahjongHttpServer server = new MahjongHttpServer(port);
                server.start();

                // 挂载关闭钩子
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n正在关闭麻将 Web 服务器...");
                    server.stop();
                }));

                // 保持主线程运行
                Thread.currentThread().join();
            } catch (Exception e) {
                System.err.println("启动服务器失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 终端命令行 4 个 AI 纯自动对局模拟
     */
    private static void runCliSimulation() {
        System.out.println(">>> 启动 4 个 AI 自动对战模拟 (CLI Benchmark) <<<");
        MahjongGame game = new MahjongGame("random_flip", 0, true);
        int step = 0;
        while (game.getPhase() != MahjongGame.Phase.GAME_OVER && step < 500) {
            step++;
            game.step();
        }
        System.out.println(">>> 对战结束！总步数: " + step);
        GameStateView view = game.createView(0);
        if (view.getSettlement() != null) {
            if (view.getSettlement().isDraw()) {
                System.out.println("结果: 荒庄流局");
            } else {
                System.out.printf("获胜座位: %d 号位, 胡牌: %s, 番型: %s, 得分: %d%n",
                        view.getSettlement().getWinnerSeat(),
                        MaJiangDef.cardToString(view.getSettlement().getWinCard()),
                        String.join(", ", view.getSettlement().getPatterns()),
                        view.getSettlement().getPoints());
            }
        }
    }
}
