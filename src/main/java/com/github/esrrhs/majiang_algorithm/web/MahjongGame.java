package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.AIUtil;
import com.github.esrrhs.majiang_algorithm.HuUtil;
import com.github.esrrhs.majiang_algorithm.MaJiangDef;

import java.util.*;

/**
 * 4人麻将完整游戏对局引擎
 */
public class MahjongGame {
    public enum Phase {
        DISCARD,        // 当前玩家出牌
        RESPONSE,       // 等待其他玩家对所打出的牌进行响应（吃碰杠胡）
        WAITING_USER,   // 专门等待真人玩家点击响应按钮（吃碰杠胡过）
        GAME_OVER       // 游戏结束结算
    }

    private static int ID_COUNTER = 1000;
    private final int gameId;
    private final List<Player> players = new ArrayList<>();
    private final List<Integer> wall = new ArrayList<>();
    private final List<Integer> guiCards = new ArrayList<>();
    private int guiIndicator = 0;
    private int dealerSeat = 0;
    private int currentSeat = 0;
    private Phase phase = Phase.DISCARD;
    private int lastDiscard = 0;
    private int lastDiscardSeat = -1;
    private boolean lastDrawFromGang = false;
    private final List<String> logs = new ArrayList<>();
    private GameStateView.SettlementInfo settlement = null;
    private boolean spectatorMode = false;
    private int stepCount = 0;

    // 用户响应等待上下文
    private final GameStateView.AvailableActions userPendingActions = new GameStateView.AvailableActions();

    public MahjongGame(String guiMode, int customGuiCard, boolean spectator) {
        this.gameId = ++ID_COUNTER;
        this.spectatorMode = spectator;

        // 初始化4个玩家 (0:南-玩家, 1:东-下家, 2:北-对家, 3:西-上家)
        players.add(new Player(0, spectator ? "AI-南 (庄)" : "玩家 (南)", spectator));
        players.add(new Player(1, "AI-东 (下家)", true));
        players.add(new Player(2, "AI-北 (对家)", true));
        players.add(new Player(3, "AI-西 (上家)", true));

        initGame(guiMode, customGuiCard);
    }

    private void log(String msg) {
        logs.add(msg);
        if (logs.size() > 100) {
            logs.remove(0);
        }
    }

    /**
     * 洗牌、定鬼、发牌
     */
    public void initGame(String guiMode, int customGuiCard) {
        players.forEach(Player::reset);
        wall.clear();
        guiCards.clear();
        logs.clear();
        settlement = null;
        lastDiscard = 0;
        lastDiscardSeat = -1;
        lastDrawFromGang = false;
        dealerSeat = 0;
        currentSeat = dealerSeat;
        stepCount = 0;

        // 1. 生成136张牌
        for (int i = 1; i <= 34; i++) {
            for (int k = 0; k < 4; k++) {
                wall.add(i);
            }
        }
        Collections.shuffle(wall);

        // 2. 确定鬼牌
        if ("random_flip".equalsIgnoreCase(guiMode) || guiMode == null) {
            // 翻一张指示牌
            guiIndicator = wall.remove(0);
            int gui = calculateNextCard(guiIndicator);
            guiCards.add(gui);
            log("本局鬼牌(癞子)为: 【" + MaJiangDef.cardToString(gui) + "】");
        } else if ("card".equalsIgnoreCase(guiMode) && customGuiCard > 0 && customGuiCard <= 34) {
            guiIndicator = customGuiCard;
            guiCards.add(customGuiCard);
            log("指定宝牌(鬼牌)为: 【" + MaJiangDef.cardToString(customGuiCard) + "】");
        } else if ("bai".equalsIgnoreCase(guiMode)) {
            guiIndicator = MaJiangDef.JIAN_BAI;
            guiCards.add(MaJiangDef.JIAN_BAI);
            log("经典白板当鬼: 【白板】");
        } else if ("zhong".equalsIgnoreCase(guiMode)) {
            guiIndicator = MaJiangDef.JIAN_ZHONG;
            guiCards.add(MaJiangDef.JIAN_ZHONG);
            log("经典红中当鬼: 【红中】");
        } else {
            // 无鬼牌
            guiIndicator = 0;
            log("本局为经典纯手牌规则 (无鬼牌)");
        }

        // 3. 发牌：闲家各13张，庄家14张
        for (int i = 0; i < 13; i++) {
            for (Player p : players) {
                p.addCard(wall.remove(0));
            }
        }
        // 庄家摸第14张
        int dealerCard = wall.remove(0);
        players.get(dealerSeat).addCard(dealerCard);
        players.get(dealerSeat).setLastDrawnCard(dealerCard);
        log(players.get(dealerSeat).getName() + " 起手14张牌，作为庄家率先出牌");

        // 检查天胡
        if (HuUtil.isHuExtra(players.get(dealerSeat).getHand(), guiCards, 0)) {
            log("🎉 震撼！" + players.get(dealerSeat).getName() + " 起手天胡！");
            triggerWin(dealerSeat, -1, dealerCard, true, false, false, true, false);
            return;
        }

        phase = Phase.DISCARD;
        updateTingStatus();
    }

    private int getFirstGui() {
        return guiCards.isEmpty() ? 0 : guiCards.get(0);
    }

    private int calculateNextCard(int card) {
        if (card >= MaJiangDef.WAN1 && card <= MaJiangDef.WAN9) {
            return card == MaJiangDef.WAN9 ? MaJiangDef.WAN1 : card + 1;
        }
        if (card >= MaJiangDef.TONG1 && card <= MaJiangDef.TONG9) {
            return card == MaJiangDef.TONG9 ? MaJiangDef.TONG1 : card + 1;
        }
        if (card >= MaJiangDef.TIAO1 && card <= MaJiangDef.TIAO9) {
            return card == MaJiangDef.TIAO9 ? MaJiangDef.TIAO1 : card + 1;
        }
        if (card >= MaJiangDef.FENG_DONG && card <= MaJiangDef.FENG_BEI) {
            return card == MaJiangDef.FENG_BEI ? MaJiangDef.FENG_DONG : card + 1;
        }
        if (card >= MaJiangDef.JIAN_ZHONG && card <= MaJiangDef.JIAN_BAI) {
            return card == MaJiangDef.JIAN_BAI ? MaJiangDef.JIAN_ZHONG : card + 1;
        }
        return card;
    }

    /**
     * 玩家打牌
     */
    public synchronized boolean discard(int seat, int card) {
        if (phase != Phase.DISCARD || seat != currentSeat) {
            return false;
        }
        Player player = players.get(seat);
        if (!player.getHand().contains(card)) {
            return false;
        }

        player.removeCard(card);
        player.sortHand();
        lastDiscard = card;
        lastDiscardSeat = seat;
        log(player.getName() + " 打出了 【" + MaJiangDef.cardToString(card) + "】");

        // 检查全场对该牌的响应
        phase = Phase.RESPONSE;
        checkResponses(card, seat);
        return true;
    }

    /**
     * 检查其他3家对打出牌的响应
     */
    private void checkResponses(int card, int fromSeat) {
        userPendingActions.setCanDiscard(false);
        userPendingActions.setCanHu(false);
        userPendingActions.setCanPeng(false);
        userPendingActions.setCanGang(false);
        userPendingActions.getGangCards().clear();
        userPendingActions.setCanChi(false);
        userPendingActions.getChiOptions().clear();
        userPendingActions.setCanPass(false);

        // 如果0号是真人玩家，且不是自己出的牌，检查真人玩家可做动作
        Player human = players.get(0);
        if (!human.isAi() && fromSeat != 0) {
            boolean hasAction = false;

            // 1. 点炮胡
            if (HuUtil.isHuExtra(human.getHand(), guiCards, card)) {
                userPendingActions.setCanHu(true);
                hasAction = true;
            }
            // 2. 碰
            if (human.countCard(card) >= 2 && !guiCards.contains(card)) {
                userPendingActions.setCanPeng(true);
                hasAction = true;
            }
            // 3. 明杠
            if (human.countCard(card) == 3 && !guiCards.contains(card)) {
                userPendingActions.setCanGang(true);
                userPendingActions.getGangCards().add(card);
                hasAction = true;
            }
            // 4. 吃（仅当下家为玩家，即 (fromSeat + 1) % 4 == 0）
            if ((fromSeat + 1) % 4 == 0 && !guiCards.contains(card)) {
                List<List<Integer>> chiOpts = findChiOptions(human.getHand(), card);
                if (!chiOpts.isEmpty()) {
                    userPendingActions.setCanChi(true);
                    userPendingActions.setChiOptions(chiOpts);
                    hasAction = true;
                }
            }

            if (hasAction) {
                userPendingActions.setCanPass(true);
                phase = Phase.WAITING_USER;
                return;
            }
        }

        // 若真人玩家无动作（或真人已过/是AI模式），让AI决定响应
        processAiResponses(card, fromSeat);
    }

    /**
     * AI 自动响应判定（按照 胡 > 杠/碰 > 吃 的优先级）
     */
    private void processAiResponses(int card, int fromSeat) {
        // 1. 优先检查谁胡牌（点炮胡）
        for (int i = 1; i <= 3; i++) {
            int seat = (fromSeat + i) % 4;
            Player p = players.get(seat);
            if (p.isAi() && HuUtil.isHuExtra(p.getHand(), guiCards, card)) {
                log("🎊 " + p.getName() + " 胡了 " + players.get(fromSeat).getName() + " 点炮的 【" + MaJiangDef.cardToString(card) + "】！");
                triggerWin(seat, fromSeat, card, false, false, wall.isEmpty(), false, false);
                return;
            }
        }

        // 2. 检查杠牌 (明杠)
        for (int i = 1; i <= 3; i++) {
            int seat = (fromSeat + i) % 4;
            Player p = players.get(seat);
            if (p.isAi() && p.countCard(card) == 3 && !guiCards.contains(card)) {
                boolean aiWantGang = AIUtil.gangAI(p.getHand(), guiCards, card, 0.5d);
                if (aiWantGang) {
                    executeGang(seat, fromSeat, card, Meld.Type.MING_GANG);
                    return;
                }
            }
        }

        // 3. 检查碰牌
        for (int i = 1; i <= 3; i++) {
            int seat = (fromSeat + i) % 4;
            Player p = players.get(seat);
            if (p.isAi() && p.countCard(card) >= 2 && !guiCards.contains(card)) {
                boolean aiWantPeng = AIUtil.pengAI(p.getHand(), guiCards, card, 0.0d);
                if (aiWantPeng) {
                    executePeng(seat, fromSeat, card);
                    return;
                }
            }
        }

        // 4. 检查吃牌（仅限下家）
        int nextSeat = (fromSeat + 1) % 4;
        Player nextPlayer = players.get(nextSeat);
        if (nextPlayer.isAi() && !guiCards.contains(card)) {
            ArrayList<Integer> chiPair = AIUtil.chiAI(nextPlayer.getHand(), guiCards, card);
            if (chiPair != null && chiPair.size() == 2) {
                executeChi(nextSeat, fromSeat, card, chiPair.get(0), chiPair.get(1));
                return;
            }
        }

        // 均无响应：此牌入弃牌堆，轮到下家摸牌
        players.get(fromSeat).addDiscard(card);
        drawNext((fromSeat + 1) % 4, false);
    }

    /**
     * 玩家选择响应动作
     */
    public synchronized boolean userAction(String action, int card, int chi1, int chi2) {
        if (phase != Phase.WAITING_USER) {
            return false;
        }

        Player human = players.get(0);
        if ("hu".equalsIgnoreCase(action) && userPendingActions.isCanHu()) {
            log("🎉 恭喜你胡牌了！胡了 " + players.get(lastDiscardSeat).getName() + " 点炮的 【" + MaJiangDef.cardToString(lastDiscard) + "】！");
            triggerWin(0, lastDiscardSeat, lastDiscard, false, false, wall.isEmpty(), false, false);
            return true;
        }

        if ("gang".equalsIgnoreCase(action) && userPendingActions.isCanGang()) {
            executeGang(0, lastDiscardSeat, lastDiscard, Meld.Type.MING_GANG);
            return true;
        }

        if ("peng".equalsIgnoreCase(action) && userPendingActions.isCanPeng()) {
            executePeng(0, lastDiscardSeat, lastDiscard);
            return true;
        }

        if ("chi".equalsIgnoreCase(action) && userPendingActions.isCanChi()) {
            // 校验 chi1, chi2
            if (human.getHand().contains(chi1) && human.getHand().contains(chi2)) {
                executeChi(0, lastDiscardSeat, lastDiscard, chi1, chi2);
                return true;
            }
        }

        if ("pass".equalsIgnoreCase(action)) {
            log(human.getName() + " 选择了 【过】");
            phase = Phase.RESPONSE;
            // 玩家放弃后，继续判定剩余AI是否响应
            processAiResponses(lastDiscard, lastDiscardSeat);
            return true;
        }

        return false;
    }

    /**
     * 玩家在自摸回合执行杠牌或自摸胡
     */
    public synchronized boolean userSelfAction(String action, int card) {
        if (phase != Phase.DISCARD || currentSeat != 0) {
            return false;
        }
        Player human = players.get(0);

        if ("hu".equalsIgnoreCase(action)) {
            if (HuUtil.isHuExtra(human.getHand(), guiCards, 0)) {
                int winCard = human.getLastDrawnCard();
                if (winCard <= 0 && !human.getHand().isEmpty()) {
                    winCard = human.getHand().get(human.getHand().size() - 1);
                }
                log("🎉 恭喜你【自摸胡】了！胡牌张: 【" + MaJiangDef.cardToString(winCard) + "】");
                triggerWin(0, -1, winCard, true, lastDrawFromGang, wall.isEmpty(), false, false);
                return true;
            }
        }

        if ("gang".equalsIgnoreCase(action)) {
            // 检查暗杠
            if (human.countCard(card) == 4 && !guiCards.contains(card)) {
                for (int i = 0; i < 4; i++) {
                    human.removeCard(card);
                }
                Meld meld = new Meld(Meld.Type.AN_GANG, card, -1, Arrays.asList(card, card, card, card));
                human.addMeld(meld);
                log(human.getName() + " 【暗杠】: " + MaJiangDef.cardToString(card));
                drawNext(0, true);
                return true;
            }
            // 检查补杠
            for (Meld m : human.getMelds()) {
                if (m.getType() == Meld.Type.PENG && m.getTriggerCard() == card && human.getHand().contains(card)) {
                    human.removeCard(card);
                    m.setType(Meld.Type.BU_GANG);
                    m.getCards().add(card);
                    log(human.getName() + " 【补杠】: " + MaJiangDef.cardToString(card));
                    drawNext(0, true);
                    return true;
                }
            }
        }

        return false;
    }

    private void executePeng(int seat, int fromSeat, int card) {
        Player p = players.get(seat);
        p.removeCard(card);
        p.removeCard(card);
        Meld meld = new Meld(Meld.Type.PENG, card, fromSeat, Arrays.asList(card, card, card));
        p.addMeld(meld);
        log(p.getName() + " 【碰】了 " + players.get(fromSeat).getName() + " 的 " + MaJiangDef.cardToString(card));
        currentSeat = seat;
        phase = Phase.DISCARD;
        lastDrawFromGang = false;
        p.setLastDrawnCard(0);
        updateTingStatus();
    }

    private void executeGang(int seat, int fromSeat, int card, Meld.Type type) {
        Player p = players.get(seat);
        if (type == Meld.Type.MING_GANG) {
            p.removeCard(card);
            p.removeCard(card);
            p.removeCard(card);
            Meld meld = new Meld(Meld.Type.MING_GANG, card, fromSeat, Arrays.asList(card, card, card, card));
            p.addMeld(meld);
            log(p.getName() + " 【明杠】了 " + players.get(fromSeat).getName() + " 的 " + MaJiangDef.cardToString(card));
        }
        // 杠后从牌墙尾部摸牌
        drawNext(seat, true);
    }

    private void executeChi(int seat, int fromSeat, int card, int c1, int c2) {
        Player p = players.get(seat);
        p.removeCard(c1);
        p.removeCard(c2);
        Meld meld = new Meld(Meld.Type.CHI, card, fromSeat, Arrays.asList(card, c1, c2));
        p.addMeld(meld);
        log(p.getName() + " 【吃】了 " + players.get(fromSeat).getName() + " 的 " + MaJiangDef.cardToString(card)
                + " (组合: " + MaJiangDef.cardToString(c1) + "," + MaJiangDef.cardToString(card) + "," + MaJiangDef.cardToString(c2) + ")");
        currentSeat = seat;
        phase = Phase.DISCARD;
        lastDrawFromGang = false;
        p.setLastDrawnCard(0);
        updateTingStatus();
    }

    /**
     * 轮到摸牌
     */
    private void drawNext(int seat, boolean isFromGang) {
        currentSeat = seat;
        lastDrawFromGang = isFromGang;

        if (wall.isEmpty()) {
            log("牌墙已全部摸完，【荒庄流局】！");
            triggerDraw();
            return;
        }

        int drawn = isFromGang ? wall.remove(wall.size() - 1) : wall.remove(0);
        Player player = players.get(seat);
        player.addCard(drawn);

        if (isFromGang) {
            log(player.getName() + " 杠后补摸一张: 【" + MaJiangDef.cardToString(drawn) + "】 (剩余 " + wall.size() + " 张)");
        } else {
            log(player.getName() + " 摸了一张牌 (剩余 " + wall.size() + " 张)");
        }

        // 检查摸牌玩家是否自摸
        if (HuUtil.isHuExtra(player.getHand(), guiCards, 0)) {
            if (player.isAi()) {
                log("🎊 " + player.getName() + (isFromGang ? " 【杠上开花】" : " 【自摸胡】") + "！胡牌: 【" + MaJiangDef.cardToString(drawn) + "】");
                triggerWin(seat, -1, drawn, true, isFromGang, wall.isEmpty(), false, false);
                return;
            }
        }

        // 如果是AI玩家摸牌，还可以判定是否杠牌
        if (player.isAi()) {
            // 暗杠判定
            for (int card = 1; card <= 34; card++) {
                if (!guiCards.contains(card) && player.countCard(card) == 4) {
                    if (AIUtil.gangAI(player.getHand(), guiCards, card, 0.5d)) {
                        for (int k = 0; k < 4; k++) player.removeCard(card);
                        player.addMeld(new Meld(Meld.Type.AN_GANG, card, -1, Arrays.asList(card, card, card, card)));
                        log(player.getName() + " 【暗杠】: " + MaJiangDef.cardToString(card));
                        drawNext(seat, true);
                        return;
                    }
                }
            }
            // 补杠判定
            for (Meld m : player.getMelds()) {
                if (m.getType() == Meld.Type.PENG && !guiCards.contains(m.getTriggerCard()) && player.getHand().contains(m.getTriggerCard())) {
                    if (AIUtil.gangAI(player.getHand(), guiCards, m.getTriggerCard(), 0.5d)) {
                        player.removeCard(m.getTriggerCard());
                        m.setType(Meld.Type.BU_GANG);
                        m.getCards().add(m.getTriggerCard());
                        log(player.getName() + " 【补杠】: " + MaJiangDef.cardToString(m.getTriggerCard()));
                        drawNext(seat, true);
                        return;
                    }
                }
            }
        }

        phase = Phase.DISCARD;
        updateTingStatus();
    }

    /**
     * 游戏步进推进（驱动AI自动思考打牌，或单步向前）
     */
    public synchronized boolean step() {
        stepCount++;
        if (phase == Phase.GAME_OVER) {
            return false;
        }

        if (phase == Phase.WAITING_USER) {
            if (spectatorMode || players.get(0).isAi()) {
                handleAiResponseForPlayer0();
                return true;
            }
            return false;
        }

        if (phase == Phase.DISCARD) {
            Player current = players.get(currentSeat);
            if (current.isAi()) {
                // AI 决策打牌
                int outCard = AIUtil.outAI(current.getHand(), guiCards);
                if (outCard == 0 || !current.getHand().contains(outCard)) {
                    // 保底打非鬼牌或第一张
                    for (int c : current.getHand()) {
                        if (!guiCards.contains(c)) {
                            outCard = c;
                            break;
                        }
                    }
                    if (outCard == 0) outCard = current.getHand().get(0);
                }
                discard(currentSeat, outCard);
                return true;
            } else {
                // 轮到玩家出牌，暂停等待玩家点击出牌
                return false;
            }
        }

        return false;
    }

    /**
     * 更新各玩家的听牌标志，并对玩家0号计算打牌听牌映射
     */
    private void updateTingStatus() {
        for (Player p : players) {
            // 当手牌模3余1时（未摸牌或刚打牌完），直接检测当前是否听牌
            if (p.getHand().size() % 3 == 1) {
                List<Integer> tingCards = HuUtil.isTingExtra(p.getHand(), guiCards);
                p.setTing(tingCards != null && !tingCards.isEmpty());
            } else if (p.getHand().size() % 3 == 2) {
                // 摸牌后有14张：如果任打出一张能听牌，也算准听
                boolean canTing = false;
                Set<Integer> unique = new HashSet<>(p.getHand());
                for (int c : unique) {
                    List<Integer> tmp = new ArrayList<>(p.getHand());
                    tmp.remove((Integer) c);
                    List<Integer> t = HuUtil.isTingExtra(tmp, guiCards);
                    if (t != null && !t.isEmpty()) {
                        canTing = true;
                        break;
                    }
                }
                p.setTing(canTing);
            }
        }
    }

    /**
     * 寻找所有合法的吃牌顺子组合
     */
    private List<List<Integer>> findChiOptions(List<Integer> hand, int card) {
        List<List<Integer>> options = new ArrayList<>();
        int type = MaJiangDef.type(card);
        if (type != MaJiangDef.TYPE_WAN && type != MaJiangDef.TYPE_TONG && type != MaJiangDef.TYPE_TIAO) {
            return options;
        }

        // [card-2, card-1]
        if (hand.contains(card - 2) && hand.contains(card - 1) && MaJiangDef.type(card - 2) == type) {
            options.add(Arrays.asList(card - 2, card - 1));
        }
        // [card-1, card+1]
        if (hand.contains(card - 1) && hand.contains(card + 1) && MaJiangDef.type(card - 1) == type && MaJiangDef.type(card + 1) == type) {
            options.add(Arrays.asList(card - 1, card + 1));
        }
        // [card+1, card+2]
        if (hand.contains(card + 1) && hand.contains(card + 2) && MaJiangDef.type(card + 2) == type) {
            options.add(Arrays.asList(card + 1, card + 2));
        }

        return options;
    }

    /**
     * 胜负结算
     */
    private void triggerWin(int winnerSeat, int providerSeat, int winCard, boolean isZimo,
                            boolean isGangShangHua, boolean isHaiDi, boolean isTianHu, boolean isDiHu) {
        phase = Phase.GAME_OVER;
        Player winner = players.get(winnerSeat);
        winner.setHu(true);

        FanCalculator.FanResult fan = FanCalculator.calculateFan(winner, winCard, isZimo,
                isGangShangHua, isHaiDi, isTianHu, isDiHu, guiCards);

        settlement = new GameStateView.SettlementInfo();
        settlement.setDraw(false);
        settlement.setWinnerSeat(winnerSeat);
        settlement.setProviderSeat(providerSeat);
        settlement.setZimo(isZimo);
        settlement.setWinCard(winCard);
        settlement.setPatterns(fan.getPatterns());
        settlement.setTotalFan(fan.getTotalFan());
        settlement.setPoints(fan.getPoints());

        int scoreDelta = fan.getPoints();
        if (isZimo) {
            // 其余3家各扣 scoreDelta
            for (Player p : players) {
                if (p.getSeat() == winnerSeat) {
                    p.addScore(scoreDelta * 3);
                    settlement.getScoreDeltas().put(p.getSeat(), scoreDelta * 3);
                } else {
                    p.addScore(-scoreDelta);
                    settlement.getScoreDeltas().put(p.getSeat(), -scoreDelta);
                }
            }
        } else {
            // 点炮者单独扣分
            winner.addScore(scoreDelta);
            settlement.getScoreDeltas().put(winnerSeat, scoreDelta);
            players.get(providerSeat).addScore(-scoreDelta);
            settlement.getScoreDeltas().put(providerSeat, -scoreDelta);
            for (Player p : players) {
                if (p.getSeat() != winnerSeat && p.getSeat() != providerSeat) {
                    settlement.getScoreDeltas().put(p.getSeat(), 0);
                }
            }
        }
    }

    private void triggerDraw() {
        phase = Phase.GAME_OVER;
        settlement = new GameStateView.SettlementInfo();
        settlement.setDraw(true);
        settlement.setWinnerSeat(-1);
        settlement.setProviderSeat(-1);
        settlement.setPatterns(Collections.singletonList("荒庄流局 (无输赢)"));
    }

    /**
     * 构建发送给前端的完整视图对象
     */
    public synchronized GameStateView createView(int requestSeat) {
        GameStateView view = new GameStateView();
        view.setGameId(gameId);
        view.setWallCount(wall.size());
        view.setGuiCards(guiCards);
        view.setGuiIndicator(guiIndicator);
        view.setDealerSeat(dealerSeat);
        view.setCurrentSeat(currentSeat);
        view.setPhase(phase.name());
        view.setLastDiscard(lastDiscard);
        view.setLastDiscardSeat(lastDiscardSeat);
        view.setLogs(new ArrayList<>(logs));
        view.setSettlement(settlement);
        view.setSpectatorMode(spectatorMode);

        // 计算全场每张牌已曝光数量与剩余张数
        int[] visibleCounts = new int[35];
        if (guiIndicator > 0) {
            visibleCounts[guiIndicator]++;
        }
        for (Player p : players) {
            for (int d : p.getDiscards()) visibleCounts[d]++;
            for (Meld m : p.getMelds()) {
                for (int c : m.getCards()) visibleCounts[c]++;
            }
        }
        // 如果是玩家自己，手牌里的牌也是已知的
        Player user = players.get(0);
        for (int c : user.getHand()) visibleCounts[c]++;

        Map<Integer, Integer> remainMap = new HashMap<>();
        for (int i = 1; i <= 34; i++) {
            remainMap.put(i, Math.max(0, 4 - visibleCounts[i]));
        }
        view.setCardRemainCounts(remainMap);

        // 玩家视图
        List<GameStateView.PlayerView> pviews = new ArrayList<>();
        for (Player p : players) {
            GameStateView.PlayerView pv = new GameStateView.PlayerView();
            pv.setSeat(p.getSeat());
            pv.setName(p.getName());
            pv.setAi(p.isAi());
            pv.setTileCount(p.getHand().size());
            pv.setMelds(p.getMelds());
            pv.setDiscards(p.getDiscards());
            pv.setLastDrawnCard(p.getLastDrawnCard());
            pv.setTing(p.isTing());
            pv.setHu(p.isHu());
            pv.setScore(p.getScore());

            // 总是提供完整手牌数据，由前端根据开关一键控制明牌/暗牌展示
            pv.setHand(new ArrayList<>(p.getHand()));
            pviews.add(pv);
        }
        view.setPlayers(pviews);

        // 可用动作
        if (phase == Phase.WAITING_USER) {
            view.setAvailableActions(userPendingActions);
        } else if (phase == Phase.DISCARD && currentSeat == 0 && !user.isAi()) {
            GameStateView.AvailableActions actions = new GameStateView.AvailableActions();
            actions.setCanDiscard(true);
            if (HuUtil.isHuExtra(user.getHand(), guiCards, 0)) {
                actions.setCanHu(true);
            }
            // 检查暗杠或补杠
            for (int card = 1; card <= 34; card++) {
                if (!guiCards.contains(card) && user.countCard(card) == 4) {
                    actions.setCanGang(true);
                    actions.getGangCards().add(card);
                }
            }
            for (Meld m : user.getMelds()) {
                if (m.getType() == Meld.Type.PENG && !guiCards.contains(m.getTriggerCard()) && user.getHand().contains(m.getTriggerCard())) {
                    actions.setCanGang(true);
                    actions.getGangCards().add(m.getTriggerCard());
                }
            }
            view.setAvailableActions(actions);
        }

        // 听牌提示计算
        if (!user.isAi() || spectatorMode) {
            // 1. 如果当前是听牌状态（手牌模3余1），计算当前直接胡哪些牌
            if (user.getHand().size() % 3 == 1) {
                List<Integer> tings = HuUtil.isTingExtra(user.getHand(), guiCards);
                if (tings != null) {
                    List<GameStateView.TingTarget> targets = new ArrayList<>();
                    for (int t : tings) {
                        targets.add(new GameStateView.TingTarget(t, remainMap.getOrDefault(t, 0)));
                    }
                    view.setCurrentTingTargets(targets);
                }
            }

            // 2. 如果处于出牌阶段（手牌模3余2），计算打出哪张牌能听牌（即：打牌听牌分析）
            if (user.getHand().size() % 3 == 2) {
                Map<Integer, List<GameStateView.TingTarget>> discardTingMap = new HashMap<>();
                Set<Integer> uniqueCards = new HashSet<>(user.getHand());
                for (int c : uniqueCards) {
                    List<Integer> simHand = new ArrayList<>(user.getHand());
                    simHand.remove((Integer) c);
                    List<Integer> tings = HuUtil.isTingExtra(simHand, guiCards);
                    if (tings != null && !tings.isEmpty()) {
                        List<GameStateView.TingTarget> targets = new ArrayList<>();
                        for (int t : tings) {
                            targets.add(new GameStateView.TingTarget(t, remainMap.getOrDefault(t, 0)));
                        }
                        discardTingMap.put(c, targets);
                    }
                }
                view.setDiscardToTing(discardTingMap);
            }
        }

        // AI 推荐评估（当轮到玩家出牌或响应时，提供智能建议）
        if (phase == Phase.DISCARD && currentSeat == 0 && !user.isAi()) {
            try {
                GameStateView.AvailableActions acts = view.getAvailableActions();
                if (acts != null && acts.isCanHu()) {
                    view.setAiRecommendation(new GameStateView.AIRecommendation("hu", 0, Collections.emptyList(), 999.0, "AI强烈推荐【自摸胡】！"));
                } else {
                    boolean wantGang = false;
                    int gCard = 0;
                    if (acts != null && acts.isCanGang()) {
                        for (int gc : acts.getGangCards()) {
                            if (AIUtil.gangAI(user.getHand(), guiCards, gc, 0.5d)) {
                                wantGang = true;
                                gCard = gc;
                                break;
                            }
                        }
                    }
                    if (wantGang) {
                        List<Integer> gCards = new ArrayList<>();
                        for (int c : user.getHand()) {
                            if (c == gCard) gCards.add(c);
                        }
                        view.setAiRecommendation(new GameStateView.AIRecommendation("gang", gCard, gCards, 888.0, "AI推荐【杠】" + MaJiangDef.cardToString(gCard)));
                    } else {
                        int bestOut = AIUtil.outAI(user.getHand(), guiCards);
                        double score = 0;
                        if (bestOut != 0) {
                            List<Integer> tmp = new ArrayList<>(user.getHand());
                            tmp.remove((Integer) bestOut);
                            score = AIUtil.calc(tmp, guiCards);
                        }
                        String reason = "AI推荐打出【" + MaJiangDef.cardToString(bestOut) + "】，打出后手牌期望评分: " + String.format("%.2f", score);
                        view.setAiRecommendation(new GameStateView.AIRecommendation("discard", bestOut, Collections.singletonList(bestOut), score, reason));
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (phase == Phase.WAITING_USER && !user.isAi()) {
            try {
                if (userPendingActions.isCanHu()) {
                    view.setAiRecommendation(new GameStateView.AIRecommendation("hu", lastDiscard, Collections.emptyList(), 999.0, "AI强烈推荐【胡牌】！"));
                } else if (userPendingActions.isCanGang()) {
                    boolean wantGang = false;
                    int gCard = 0;
                    for (int gc : userPendingActions.getGangCards()) {
                        if (AIUtil.gangAI(user.getHand(), guiCards, gc, 0.5d)) {
                            wantGang = true;
                            gCard = gc;
                            break;
                        }
                    }
                    if (wantGang) {
                        List<Integer> gCards = new ArrayList<>();
                        for (int c : user.getHand()) {
                            if (c == gCard) gCards.add(c);
                        }
                        view.setAiRecommendation(new GameStateView.AIRecommendation("gang", gCard, gCards, 888.0, "AI推荐【杠】" + MaJiangDef.cardToString(gCard)));
                    } else if (userPendingActions.isCanPeng() && AIUtil.pengAI(user.getHand(), guiCards, lastDiscard, 0.0d)) {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("peng", lastDiscard, Arrays.asList(lastDiscard, lastDiscard), 777.0, "AI推荐【碰】" + MaJiangDef.cardToString(lastDiscard)));
                    } else {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("pass", 0, Collections.emptyList(), 0.0, "AI建议【过】"));
                    }
                } else if (userPendingActions.isCanPeng()) {
                    if (AIUtil.pengAI(user.getHand(), guiCards, lastDiscard, 0.0d)) {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("peng", lastDiscard, Arrays.asList(lastDiscard, lastDiscard), 777.0, "AI推荐【碰】" + MaJiangDef.cardToString(lastDiscard)));
                    } else {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("pass", 0, Collections.emptyList(), 0.0, "AI建议【过】"));
                    }
                } else if (userPendingActions.isCanChi()) {
                    ArrayList<Integer> chiPair = AIUtil.chiAI(user.getHand(), guiCards, lastDiscard);
                    if (chiPair != null && chiPair.size() == 2) {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("chi", lastDiscard, chiPair, 666.0, "AI推荐【吃】" + MaJiangDef.cardToString(chiPair.get(0)) + "和" + MaJiangDef.cardToString(chiPair.get(1))));
                    } else {
                        view.setAiRecommendation(new GameStateView.AIRecommendation("pass", 0, Collections.emptyList(), 0.0, "AI建议【过】"));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return view;
    }

    private void handleAiResponseForPlayer0() {
        Player p0 = players.get(0);
        if (userPendingActions.isCanHu()) {
            userAction("hu", lastDiscard, 0, 0);
        } else if (userPendingActions.isCanGang() && !userPendingActions.getGangCards().isEmpty()) {
            userAction("gang", userPendingActions.getGangCards().get(0), 0, 0);
        } else if (userPendingActions.isCanPeng() && AIUtil.pengAI(p0.getHand(), guiCards, lastDiscard, 0.0d)) {
            userAction("peng", lastDiscard, 0, 0);
        } else if (userPendingActions.isCanChi()) {
            ArrayList<Integer> chiPair = AIUtil.chiAI(p0.getHand(), guiCards, lastDiscard);
            if (chiPair != null && chiPair.size() == 2) {
                userAction("chi", lastDiscard, chiPair.get(0), chiPair.get(1));
            } else {
                userAction("pass", 0, 0, 0);
            }
        } else {
            userAction("pass", 0, 0, 0);
        }
    }

    public synchronized void setSpectatorMode(boolean spectator) {
        this.spectatorMode = spectator;
        Player p0 = players.get(0);
        p0.setAi(spectator);
        p0.setName(spectator ? "AI-南 (托管)" : "玩家 (南)");
        if (spectator && phase == Phase.WAITING_USER) {
            handleAiResponseForPlayer0();
        }
    }

    public int getGameId() { return gameId; }
    public Phase getPhase() { return phase; }
    public int getCurrentSeat() { return currentSeat; }
}
