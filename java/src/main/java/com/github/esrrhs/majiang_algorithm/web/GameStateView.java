package com.github.esrrhs.majiang_algorithm.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传输给前端的对局状态视图 DTO
 */
public class GameStateView {
    public static class PlayerView {
        private int seat;
        private String name;
        private boolean isAi;
        private int tileCount;
        private List<Integer> hand; // 玩家自身手牌可见，AI手牌隐藏（观战/结算时全可见）
        private List<Meld> melds;
        private List<Integer> discards;
        private int lastDrawnCard;
        private boolean isTing;
        private boolean isHu;
        private int score;

        public int getSeat() { return seat; }
        public void setSeat(int seat) { this.seat = seat; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isAi() { return isAi; }
        public void setAi(boolean ai) { isAi = ai; }
        public int getTileCount() { return tileCount; }
        public void setTileCount(int tileCount) { this.tileCount = tileCount; }
        public List<Integer> getHand() { return hand; }
        public void setHand(List<Integer> hand) { this.hand = hand; }
        public List<Meld> getMelds() { return melds; }
        public void setMelds(List<Meld> melds) { this.melds = melds; }
        public List<Integer> getDiscards() { return discards; }
        public void setDiscards(List<Integer> discards) { this.discards = discards; }
        public int getLastDrawnCard() { return lastDrawnCard; }
        public void setLastDrawnCard(int lastDrawnCard) { this.lastDrawnCard = lastDrawnCard; }
        public boolean isTing() { return isTing; }
        public void setTing(boolean ting) { isTing = ting; }
        public boolean isHu() { return isHu; }
        public void setHu(boolean hu) { isHu = hu; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class TingTarget {
        private int card;
        private int remainingCount; // 场上剩余张数（4 - 已经可见的张数）

        public TingTarget(int card, int remainingCount) {
            this.card = card;
            this.remainingCount = remainingCount;
        }

        public int getCard() { return card; }
        public int getRemainingCount() { return remainingCount; }
    }

    public static class AvailableActions {
        private boolean canDiscard = false;
        private boolean canHu = false;
        private boolean canPeng = false;
        private boolean canGang = false;
        private List<Integer> gangCards = new ArrayList<>();
        private boolean canChi = false;
        private List<List<Integer>> chiOptions = new ArrayList<>();
        private boolean canPass = false;

        public boolean isCanDiscard() { return canDiscard; }
        public void setCanDiscard(boolean canDiscard) { this.canDiscard = canDiscard; }
        public boolean isCanHu() { return canHu; }
        public void setCanHu(boolean canHu) { this.canHu = canHu; }
        public boolean isCanPeng() { return canPeng; }
        public void setCanPeng(boolean canPeng) { this.canPeng = canPeng; }
        public boolean isCanGang() { return canGang; }
        public void setCanGang(boolean canGang) { this.canGang = canGang; }
        public List<Integer> getGangCards() { return gangCards; }
        public void setGangCards(List<Integer> gangCards) { this.gangCards = gangCards; }
        public boolean isCanChi() { return canChi; }
        public void setCanChi(boolean canChi) { this.canChi = canChi; }
        public List<List<Integer>> getChiOptions() { return chiOptions; }
        public void setChiOptions(List<List<Integer>> chiOptions) { this.chiOptions = chiOptions; }
        public boolean isCanPass() { return canPass; }
        public void setCanPass(boolean canPass) { this.canPass = canPass; }
    }

    public static class AIRecommendation {
        private String recommendedAction; // "discard", "peng", "gang", "hu", "chi", "pass"
        private int recommendedDiscard;
        private int recommendedCard;
        private List<Integer> recommendedCards = new ArrayList<>();
        private double score;
        private String reason;

        public AIRecommendation(int recommendedDiscard, double score, String reason) {
            this.recommendedAction = "discard";
            this.recommendedDiscard = recommendedDiscard;
            this.recommendedCard = recommendedDiscard;
            this.recommendedCards.add(recommendedDiscard);
            this.score = score;
            this.reason = reason;
        }

        public AIRecommendation(String recommendedAction, int recommendedCard, List<Integer> recommendedCards, double score, String reason) {
            this.recommendedAction = recommendedAction;
            this.recommendedDiscard = "discard".equals(recommendedAction) ? recommendedCard : 0;
            this.recommendedCard = recommendedCard;
            if (recommendedCards != null) {
                this.recommendedCards.addAll(recommendedCards);
            }
            this.score = score;
            this.reason = reason;
        }

        public String getRecommendedAction() { return recommendedAction; }
        public int getRecommendedDiscard() { return recommendedDiscard; }
        public int getRecommendedCard() { return recommendedCard; }
        public List<Integer> getRecommendedCards() { return recommendedCards; }
        public double getScore() { return score; }
        public String getReason() { return reason; }
    }

    public static class SettlementInfo {
        private boolean isDraw; // 是否荒庄流局
        private int winnerSeat = -1;
        private int providerSeat = -1; // 点炮者（-1为自摸）
        private boolean isZimo;
        private int winCard;
        private List<String> patterns = new ArrayList<>();
        private int totalFan;
        private int points;
        private Map<Integer, Integer> scoreDeltas = new HashMap<>();

        public boolean isDraw() { return isDraw; }
        public void setDraw(boolean draw) { isDraw = draw; }
        public int getWinnerSeat() { return winnerSeat; }
        public void setWinnerSeat(int winnerSeat) { this.winnerSeat = winnerSeat; }
        public int getProviderSeat() { return providerSeat; }
        public void setProviderSeat(int providerSeat) { this.providerSeat = providerSeat; }
        public boolean isZimo() { return isZimo; }
        public void setZimo(boolean zimo) { isZimo = zimo; }
        public int getWinCard() { return winCard; }
        public void setWinCard(int winCard) { this.winCard = winCard; }
        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
        public int getTotalFan() { return totalFan; }
        public void setTotalFan(int totalFan) { this.totalFan = totalFan; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public Map<Integer, Integer> getScoreDeltas() { return scoreDeltas; }
        public void setScoreDeltas(Map<Integer, Integer> scoreDeltas) { this.scoreDeltas = scoreDeltas; }
    }

    private int gameId;
    private int wallCount;
    private List<Integer> guiCards = new ArrayList<>();
    private int guiIndicator;
    private int dealerSeat;
    private int currentSeat;
    private String phase; // DISCARD, RESPONSE, WAITING_USER, GAME_OVER
    private int lastDiscard;
    private int lastDiscardSeat = -1;
    private List<PlayerView> players = new ArrayList<>();
    private AvailableActions availableActions = new AvailableActions();
    private List<TingTarget> currentTingTargets = new ArrayList<>(); // 当前手牌已听牌时的胡牌列表
    private Map<Integer, List<TingTarget>> discardToTing = new HashMap<>(); // 打哪张牌听哪些牌
    private AIRecommendation aiRecommendation;
    private List<String> logs = new ArrayList<>();
    private SettlementInfo settlement;
    private Map<Integer, Integer> cardRemainCounts = new HashMap<>(); // 1-34每种牌的全场剩余张数
    private boolean spectatorMode; // 是否是观战全开眼模式

    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }
    public int getWallCount() { return wallCount; }
    public void setWallCount(int wallCount) { this.wallCount = wallCount; }
    public List<Integer> getGuiCards() { return guiCards; }
    public void setGuiCards(List<Integer> guiCards) { this.guiCards = guiCards; }
    public int getGuiIndicator() { return guiIndicator; }
    public void setGuiIndicator(int guiIndicator) { this.guiIndicator = guiIndicator; }
    public int getDealerSeat() { return dealerSeat; }
    public void setDealerSeat(int dealerSeat) { this.dealerSeat = dealerSeat; }
    public int getCurrentSeat() { return currentSeat; }
    public void setCurrentSeat(int currentSeat) { this.currentSeat = currentSeat; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public int getLastDiscard() { return lastDiscard; }
    public void setLastDiscard(int lastDiscard) { this.lastDiscard = lastDiscard; }
    public int getLastDiscardSeat() { return lastDiscardSeat; }
    public void setLastDiscardSeat(int lastDiscardSeat) { this.lastDiscardSeat = lastDiscardSeat; }
    public List<PlayerView> getPlayers() { return players; }
    public void setPlayers(List<PlayerView> players) { this.players = players; }
    public AvailableActions getAvailableActions() { return availableActions; }
    public void setAvailableActions(AvailableActions availableActions) { this.availableActions = availableActions; }
    public List<TingTarget> getCurrentTingTargets() { return currentTingTargets; }
    public void setCurrentTingTargets(List<TingTarget> currentTingTargets) { this.currentTingTargets = currentTingTargets; }
    public Map<Integer, List<TingTarget>> getDiscardToTing() { return discardToTing; }
    public void setDiscardToTing(Map<Integer, List<TingTarget>> discardToTing) { this.discardToTing = discardToTing; }
    public AIRecommendation getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(AIRecommendation aiRecommendation) { this.aiRecommendation = aiRecommendation; }
    public List<String> getLogs() { return logs; }
    public void setLogs(List<String> logs) { this.logs = logs; }
    public SettlementInfo getSettlement() { return settlement; }
    public void setSettlement(SettlementInfo settlement) { this.settlement = settlement; }
    public Map<Integer, Integer> getCardRemainCounts() { return cardRemainCounts; }
    public void setCardRemainCounts(Map<Integer, Integer> cardRemainCounts) { this.cardRemainCounts = cardRemainCounts; }
    public boolean isSpectatorMode() { return spectatorMode; }
    public void setSpectatorMode(boolean spectatorMode) { this.spectatorMode = spectatorMode; }
}
