package com.github.esrrhs.majiang_algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Dumps deterministic algorithm results (hu/ting/AI decisions) for a fixed-seed deal set.
 * The output file is the cross-language parity contract: java ParityFixtureTest,
 * go parity_test.go and the C++ tests replay the same cases and must reproduce
 * every field exactly.
 *
 * Regenerate with:
 *   cd java && mvn -q test-compile exec:java -Dexec.mainClass=com.github.esrrhs.majiang_algorithm.ParityDump \
 *     -Dexec.classpathScope=test -Dexec.args="../data/parity_cases.txt"
 */
public class ParityDump
{
	private static final long SEED = 20260202L;
	private static final int RANDOM_CASES = 2000;

	public static void main(String[] args) throws Exception
	{
		HuUtil.load();
		AIUtil.load();

		String out = args.length > 0 ? args[0] : "parity_cases.txt";
		try (BufferedWriter w = Files.newBufferedWriter(Paths.get(out), StandardCharsets.UTF_8))
		{
			w.write("# hand|gui|isHu|isHuExtra|ting|tingExtra|calc|out|chiCard|chiChoices|chiBool|peng|gang\n");

			for (String[] fixed : fixedCases())
			{
				dump(w, fixed[0], MaJiangDef.stringToCard(fixed[1]), MaJiangDef.stringToCard(fixed[2]));
			}

			Random r = new Random(SEED);
			List<Integer> deck = new ArrayList<>();
			for (int c = MaJiangDef.WAN1; c <= MaJiangDef.JIAN_BAI; c++)
			{
				for (int i = 0; i < 4; i++)
				{
					deck.add(c);
				}
			}
			for (int i = 0; i < RANDOM_CASES; i++)
			{
				List<Integer> shuffled = new ArrayList<>(deck);
				Collections.shuffle(shuffled, r);
				List<Integer> hand = new ArrayList<>(shuffled.subList(0, 13));
				Collections.sort(hand);
				int gui = r.nextInt(MaJiangDef.JIAN_BAI) + 1;
				int chiCard = r.nextInt(MaJiangDef.JIAN_BAI) + 1;
				dump(w, MaJiangDef.cardsToString(hand), gui, chiCard);
			}
		}
		System.out.println("parity cases written to " + out);
	}

	private static List<String[]> fixedCases()
	{
		List<String[]> ret = new ArrayList<>();
		ret.add(new String[]
		{ "1万,1万", "1万", "1万" });
		ret.add(new String[]
		{ "1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东", "南", "东" });
		ret.add(new String[]
		{ "1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,南", "南", "东" });
		ret.add(new String[]
		{ "1万,1万,1筒,3筒,2筒,2条,3条,4条,东,东", "1筒", "2筒" });
		ret.add(new String[]
		{ "1万,2万,2万,1条,1条,东", "1万", "东" });
		ret.add(new String[]
		{ "1万,2万,2万,1条,1条,1筒,2筒,4筒,4筒,5筒", "1万", "4筒" });
		ret.add(new String[]
		{ "1万,2万,2万,2万,3万,4万,4筒,4筒", "1万", "2万" });
		ret.add(new String[]
		{ "东,东,东", "1万", "东" });
		ret.add(new String[]
		{ "中,发,白,东,南,西,北,1万,9万,1筒,9筒,1条,9条", "中", "北" });
		return ret;
	}

	private static void dump(BufferedWriter w, String handStr, int gui, int chiCard) throws IOException
	{
		List<Integer> hand = MaJiangDef.stringToCards(handStr);
		List<Integer> guiList = new ArrayList<>();
		guiList.add(gui);

		List<Integer> chiChoices = AIUtil.chiAI(hand, guiList, chiCard);
		int chiBool = AIUtil.chiAI(hand, guiList, chiCard, chiCard - 1, chiCard + 1) ? 1 : 0;
		int peng = AIUtil.pengAI(hand, guiList, chiCard, 0.d) ? 1 : 0;
		int gang = AIUtil.gangAI(hand, guiList, chiCard, 1.d) ? 1 : 0;

		StringBuilder sb = new StringBuilder();
		sb.append(handStr).append("|");
		sb.append(gui).append("|");
		sb.append(HuUtil.isHu(hand, gui) ? 1 : 0).append("|");
		sb.append(HuUtil.isHuExtra(hand, guiList, 0) ? 1 : 0).append("|");
		sb.append(MaJiangDef.cardsToString(HuUtil.isTing(hand, gui))).append("|");
		sb.append(MaJiangDef.cardsToString(HuUtil.isTingExtra(hand, guiList))).append("|");
		sb.append(AIUtil.calc(hand, guiList)).append("|");
		sb.append(AIUtil.outAI(hand, guiList)).append("|");
		sb.append(chiCard).append("|");
		sb.append(MaJiangDef.cardsToString(chiChoices)).append("|");
		sb.append(chiBool).append("|");
		sb.append(peng).append("|");
		sb.append(gang);
		w.write(sb.toString());
		w.write("\n");
	}
}
