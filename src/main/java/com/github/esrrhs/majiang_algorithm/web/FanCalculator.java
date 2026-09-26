package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.MaJiangDef;

import java.util.*;

/**
 * 麻将番型计算器（参考腾讯麻将/大众麻将规则）
 */
public class FanCalculator {

    public static class FanResult {
        private final List<String> patterns = new ArrayList<>();
        private int totalFan = 0;
        private int points = 0;

        public void addPattern(String name, int fan) {
            patterns.add(name + " (" + fan + "番)");
            totalFan += fan;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public int getTotalFan() {
            return Math.max(1, totalFan);
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }
    }

    /**
     * 计算胡牌番型
     *
     * @param player 获胜玩家
     * @param winCard 胡的那张牌
     * @param isZimo 是否自摸
     * @param isGangShangHua 是否杠上开花
     * @param isHaiDi 是否海底捞月
     * @param isTianHu 是否天胡
     * @param isDiHu 是否地胡
     * @param guiCards 鬼牌列表
     */
    public static FanResult calculateFan(Player player, int winCard, boolean isZimo,
                                         boolean isGangShangHua, boolean isHaiDi,
                                         boolean isTianHu, boolean isDiHu,
                                         List<Integer> guiCards) {
        FanResult result = new FanResult();

        if (isTianHu) {
            result.addPattern("天胡", 16);
            result.setPoints(16 * 10);
            return result;
        }
        if (isDiHu) {
            result.addPattern("地胡", 16);
            result.setPoints(16 * 10);
            return result;
        }

        List<Integer> allTiles = player.getAllTiles();
        if (!isZimo && winCard > 0) {
            allTiles = new ArrayList<>(allTiles);
            allTiles.add(winCard);
        }

        // 1. 清一色 与 混一色
        boolean isQingYiSe = checkQingYiSe(allTiles, guiCards);
        boolean isHunYiSe = !isQingYiSe && checkHunYiSe(allTiles, guiCards);

        if (isQingYiSe) {
            result.addPattern("清一色", 8);
        } else if (isHunYiSe) {
            result.addPattern("混一色", 4);
        }

        // 2. 七对判定（手牌14张且无副牌）
        if (player.getMelds().isEmpty() && allTiles.size() == 14) {
            if (checkQiDui(allTiles, guiCards)) {
                result.addPattern("七对", 8);
            }
        }

        // 3. 碰碰胡判定（副牌全为碰/杠，手牌只剩刻子和将）
        if (checkPengPengHu(player, winCard, isZimo, guiCards)) {
            result.addPattern("碰碰胡", 4);
        }

        // 4. 断幺九（无1、9及字牌）
        if (checkDuanYaoJiu(allTiles, guiCards)) {
            result.addPattern("断幺九", 2);
        }

        // 5. 门前清（没有吃碰明杠，允许暗杠）
        boolean menQianQing = true;
        for (Meld m : player.getMelds()) {
            if (m.getType() != Meld.Type.AN_GANG) {
                menQianQing = false;
                break;
            }
        }
        if (menQianQing && !isZimo) {
            result.addPattern("门前清", 2);
        }

        // 6. 杠上开花
        if (isGangShangHua) {
            result.addPattern("杠上开花", 2);
        }

        // 7. 海底捞月
        if (isHaiDi) {
            result.addPattern("海底捞月", 2);
        }

        // 8. 自摸
        if (isZimo) {
            result.addPattern("自摸", 1);
        }

        // 基础平胡
        if (result.patterns.isEmpty()) {
            result.addPattern("平胡", 1);
        }

        int baseScore = 10;
        int points = baseScore * (int) Math.pow(2, Math.min(6, result.getTotalFan() - 1));
        result.setPoints(points);

        return result;
    }

    private static boolean checkQingYiSe(List<Integer> tiles, List<Integer> guiCards) {
        int colorType = 0;
        for (int c : tiles) {
            if (guiCards.contains(c)) continue; // 鬼牌可算作同花色
            int type = MaJiangDef.type(c);
            if (type == MaJiangDef.TYPE_FENG || type == MaJiangDef.TYPE_JIAN) {
                return false;
            }
            if (colorType == 0) {
                colorType = type;
            } else if (colorType != type) {
                return false;
            }
        }
        return colorType != 0;
    }

    private static boolean checkHunYiSe(List<Integer> tiles, List<Integer> guiCards) {
        int colorType = 0;
        boolean hasZi = false;
        for (int c : tiles) {
            if (guiCards.contains(c)) continue;
            int type = MaJiangDef.type(c);
            if (type == MaJiangDef.TYPE_FENG || type == MaJiangDef.TYPE_JIAN) {
                hasZi = true;
            } else {
                if (colorType == 0) {
                    colorType = type;
                } else if (colorType != type) {
                    return false;
                }
            }
        }
        return colorType != 0 && hasZi;
    }

    private static boolean checkDuanYaoJiu(List<Integer> tiles, List<Integer> guiCards) {
        for (int c : tiles) {
            if (guiCards.contains(c)) continue;
            int type = MaJiangDef.type(c);
            if (type == MaJiangDef.TYPE_FENG || type == MaJiangDef.TYPE_JIAN) return false;
            if (c == MaJiangDef.WAN1 || c == MaJiangDef.WAN9 ||
                c == MaJiangDef.TONG1 || c == MaJiangDef.TONG9 ||
                c == MaJiangDef.TIAO1 || c == MaJiangDef.TIAO9) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkQiDui(List<Integer> tiles, List<Integer> guiCards) {
        Map<Integer, Integer> counts = new HashMap<>();
        int guiCount = 0;
        for (int c : tiles) {
            if (guiCards.contains(c)) {
                guiCount++;
            } else {
                counts.put(c, counts.getOrDefault(c, 0) + 1);
            }
        }
        int singleCount = 0;
        for (int count : counts.values()) {
            if (count % 2 != 0) {
                singleCount++;
            }
        }
        return guiCount >= singleCount;
    }

    private static boolean checkPengPengHu(Player player, int winCard, boolean isZimo, List<Integer> guiCards) {
        // 副牌中不能有“吃”
        for (Meld m : player.getMelds()) {
            if (m.getType() == Meld.Type.CHI) {
                return false;
            }
        }
        // 手牌数量通常为 2, 5, 8, 11, 14 张
        List<Integer> hand = new ArrayList<>(player.getHand());
        if (!isZimo && winCard > 0) {
            hand.add(winCard);
        }
        // 统计手牌非鬼牌频率
        int guiCount = 0;
        Map<Integer, Integer> map = new HashMap<>();
        for (int c : hand) {
            if (guiCards.contains(c)) {
                guiCount++;
            } else {
                map.put(c, map.getOrDefault(c, 0) + 1);
            }
        }

        // 尝试以某种牌为将（或鬼牌为将）
        // 如果全刻子加一对将，则除将以外的牌都需要满足 3张（或用鬼补足到3张）
        for (int candidateJiang : map.keySet()) {
            int needGui = 0;
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int card = entry.getKey();
                int count = entry.getValue();
                if (card == candidateJiang) {
                    if (count < 2) {
                        needGui += (2 - count);
                    } else {
                        int rem = count - 2;
                        if (rem % 3 != 0) {
                            needGui += (3 - (rem % 3));
                        }
                    }
                } else {
                    if (count % 3 != 0) {
                        needGui += (3 - (count % 3));
                    }
                }
            }
            if (needGui <= guiCount && (guiCount - needGui) % 3 == 0) {
                return true;
            }
        }
        return false;
    }
}
