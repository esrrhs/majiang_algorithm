package com.github.esrrhs.majiang_algorithm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MajiangAlgorithmIntegrationTest
{
	@BeforeAll
	public static void setUp()
	{
		HuUtil.load();
		AIUtil.load();
	}

	@Test
	public void testHuDetectionBasic()
	{
		// 1万, 1万 (pair with gui = 1万)
		String init = "1万,1万";
		String gui = "1万";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		assertTrue(HuUtil.isHu(cards, MaJiangDef.stringToCard(gui)));
	}

	@Test
	public void testHuDetectionCompleteHands()
	{
		// 111万 234万 123筒 789条 东东 (standard 14-card winning hand, 0 gui)
		String hand = "1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东";
		List<Integer> cards = MaJiangDef.stringToCards(hand);
		assertTrue(HuUtil.isHuExtra(cards, Arrays.asList(), 0));

		// Not a winning hand (missing pair or meld)
		String notHu = "1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,南";
		List<Integer> notHuCards = MaJiangDef.stringToCards(notHu);
		assertFalse(HuUtil.isHuExtra(notHuCards, Arrays.asList(), 0));

		// With gui wildcard
		List<Integer> guiCards = Arrays.asList(MaJiangDef.FENG_NAN);
		assertTrue(HuUtil.isHuExtra(notHuCards, guiCards, 0));
	}

	@Test
	public void testTingCalculation()
	{
		String init = "1万,1万,1筒,3筒,2筒,2条,3条,4条,东,东";
		String gui = "1筒";
		List<Integer> cards = MaJiangDef.stringToCards(init);

		List<Integer> tingCards = HuUtil.isTing(cards, MaJiangDef.stringToCard(gui));
		assertNotNull(tingCards);
		assertFalse(tingCards.isEmpty());

		List<Integer> tingExtraCards = HuUtil.isTingExtra(cards, MaJiangDef.stringToCards(gui));
		assertNotNull(tingExtraCards);
		assertFalse(tingExtraCards.isEmpty());
	}

	@Test
	public void testAIOutDecision()
	{
		String init = "1万,2万,2万,1条,1条,东";
		String guiStr = "1万";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		List<Integer> gui = MaJiangDef.stringToCards(guiStr);

		int out = AIUtil.outAI(cards, gui);
		assertTrue(cards.contains(out));
		// Single wind tile '东' should typically be preferred to discard
		assertEquals(MaJiangDef.FENG_DONG, out);
	}

	@Test
	public void testAIPengAndGangDecision()
	{
		String initPeng = "1万,2万,2万,1条,1条,2筒,4筒,4筒";
		String guiStr = "1万";
		List<Integer> cardsPeng = MaJiangDef.stringToCards(initPeng);
		List<Integer> gui = MaJiangDef.stringToCards(guiStr);

		boolean peng = AIUtil.pengAI(cardsPeng, gui, MaJiangDef.stringToCard("2万"), 0.0d);
		assertNotNull(peng);

		String initGang = "1万,2万,2万,2万,3万,4万,4筒,4筒";
		List<Integer> cardsGang = MaJiangDef.stringToCards(initGang);
		boolean gang = AIUtil.gangAI(cardsGang, gui, MaJiangDef.stringToCard("2万"), 1.0d);
		assertNotNull(gang);
	}

	@Test
	public void testChiDecision()
	{
		String init = "1万,2万,2万,1条,1条,1筒,2筒,4筒,4筒,5筒";
		String guiStr = "1万";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		List<Integer> gui = MaJiangDef.stringToCards(guiStr);

		List<Integer> chiChoices = AIUtil.chiAI(cards, gui, MaJiangDef.stringToCard("3筒"));
		assertNotNull(chiChoices);
	}

	@Test
	public void testBenchmarkHuDetection()
	{
		String hand = "1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东";
		List<Integer> cards = MaJiangDef.stringToCards(hand);
		List<Integer> emptyGui = Arrays.asList();

		// Warmup
		for (int i = 0; i < 1000; i++)
		{
			HuUtil.isHuExtra(cards, emptyGui, 0);
		}

		long start = System.nanoTime();
		int iterations = 10000;
		for (int i = 0; i < iterations; i++)
		{
			HuUtil.isHuExtra(cards, emptyGui, 0);
		}
		long elapsedNanos = System.nanoTime() - start;
		double avgMicros = (elapsedNanos / (double) iterations) / 1000.0;

		System.out.printf("[BENCHMARK] isHuExtra average execution time: %.3f µs per call (%d calls)%n",
				avgMicros, iterations);
		// Winning check with hash lookup table is expected to be under 100 microseconds
		assertTrue(avgMicros < 100.0, "Check took longer than expected: " + avgMicros + " µs");
	}
}
