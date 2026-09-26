package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.AIUtil;
import com.github.esrrhs.majiang_algorithm.HuUtil;
import com.github.esrrhs.majiang_algorithm.MaJiangDef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MahjongGameWebIntegrationTest {

    @BeforeAll
    public static void setUp() {
        HuUtil.load();
        AIUtil.load();
    }

    @Test
    public void testGameInitialization() {
        MahjongGame game = new MahjongGame("random_flip", 0, false);
        assertNotNull(game);
        assertEquals(MahjongGame.Phase.DISCARD, game.getPhase());
        assertEquals(0, game.getCurrentSeat());

        GameStateView view = game.createView(0);
        assertNotNull(view);
        assertEquals(4, view.getPlayers().size());
        assertEquals(14, view.getPlayers().get(0).getTileCount());
        assertEquals(13, view.getPlayers().get(1).getTileCount());
        assertFalse(view.getGuiCards().isEmpty());
        assertTrue(view.getWallCount() > 0);
    }

    @Test
    public void testTingDetectionInGame() {
        MahjongGame game = new MahjongGame("card", MaJiangDef.JIAN_BAI, false);
        GameStateView view = game.createView(0);

        // 查看是否有听牌分析映射 (discardToTing)
        assertNotNull(view.getDiscardToTing());
    }

    @Test
    public void testAiSimulationSteps() {
        MahjongGame game = new MahjongGame("random_flip", 0, true); // 全AI观战模式
        int steps = 0;
        while (game.getPhase() != MahjongGame.Phase.GAME_OVER && steps < 100) {
            boolean ok = game.step();
            if (!ok) break;
            steps++;
        }
        assertTrue(steps > 0, "AI steps should advance the game state");
    }
}
