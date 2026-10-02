package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.MaJiangDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 副牌（吃、碰、杠）数据结构
 */
public class Meld {
    public enum Type {
        CHI("吃"),
        PENG("碰"),
        MING_GANG("明杠"),
        AN_GANG("暗杠"),
        BU_GANG("补杠");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Type type;
    private int triggerCard; // 触发此副牌的牌（来自他人或自摸）
    private int fromSeat;    // 来自哪位玩家座位（-1表示自己摸的暗杠）
    private List<Integer> cards; // 包含的所有牌（3张或4张）

    public Meld() {
        this.cards = new ArrayList<>();
    }

    public Meld(Type type, int triggerCard, int fromSeat, List<Integer> cards) {
        this.type = type;
        this.triggerCard = triggerCard;
        this.fromSeat = fromSeat;
        this.cards = new ArrayList<>(cards);
        Collections.sort(this.cards);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getTriggerCard() {
        return triggerCard;
    }

    public void setTriggerCard(int triggerCard) {
        this.triggerCard = triggerCard;
    }

    public int getFromSeat() {
        return fromSeat;
    }

    public void setFromSeat(int fromSeat) {
        this.fromSeat = fromSeat;
    }

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public boolean isGang() {
        return type == Type.MING_GANG || type == Type.AN_GANG || type == Type.BU_GANG;
    }

    @Override
    public String toString() {
        return type.getDisplayName() + ":" + MaJiangDef.cardsToString(cards);
    }
}
