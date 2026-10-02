package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.MaJiangDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家状态类
 */
public class Player {
    private final int seat;          // 0: 玩家/东, 1: 下家/南, 2: 对家/西, 3: 上家/北
    private String name;             // 玩家名字
    private boolean isAi;            // 是否 AI
    private final List<Integer> hand;      // 纯手牌（未公开）
    private final List<Meld> melds;        // 副牌（吃碰杠，已公开）
    private final List<Integer> discards;  // 弃牌河里的牌
    private int lastDrawnCard = 0;   // 本轮摸到的牌（0表示无）
    private boolean isHu = false;    // 是否已胡牌
    private boolean isTing = false;  // 是否已听牌
    private int score = 1000;        // 初始积分

    public Player(int seat, String name, boolean isAi) {
        this.seat = seat;
        this.name = name;
        this.isAi = isAi;
        this.hand = new ArrayList<>();
        this.melds = new ArrayList<>();
        this.discards = new ArrayList<>();
    }

    public void reset() {
        hand.clear();
        melds.clear();
        discards.clear();
        lastDrawnCard = 0;
        isHu = false;
        isTing = false;
    }

    public int getSeat() {
        return seat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAi() {
        return isAi;
    }

    public void setAi(boolean isAi) {
        this.isAi = isAi;
    }

    public List<Integer> getHand() {
        return hand;
    }

    public List<Meld> getMelds() {
        return melds;
    }

    public List<Integer> getDiscards() {
        return discards;
    }

    public int getLastDrawnCard() {
        return lastDrawnCard;
    }

    public void setLastDrawnCard(int lastDrawnCard) {
        this.lastDrawnCard = lastDrawnCard;
    }

    public boolean isHu() {
        return isHu;
    }

    public void setHu(boolean hu) {
        isHu = hu;
    }

    public boolean isTing() {
        return isTing;
    }

    public void setTing(boolean ting) {
        isTing = ting;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public void addCard(int card) {
        hand.add(card);
        this.lastDrawnCard = card;
        sortHand();
    }

    public boolean removeCard(int card) {
        boolean removed = hand.remove((Integer) card);
        if (removed && lastDrawnCard == card) {
            lastDrawnCard = 0;
        }
        return removed;
    }

    public void sortHand() {
        Collections.sort(hand);
    }

    public void addDiscard(int card) {
        discards.add(card);
    }

    public void addMeld(Meld meld) {
        melds.add(meld);
    }

    /**
     * 手牌中某种牌的张数
     */
    public int countCard(int card) {
        int count = 0;
        for (int c : hand) {
            if (c == card) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取所有手牌加上副牌的全部牌（用于算番如清一色）
     */
    public List<Integer> getAllTiles() {
        List<Integer> all = new ArrayList<>(hand);
        for (Meld m : melds) {
            all.addAll(m.getCards());
        }
        return all;
    }
}
