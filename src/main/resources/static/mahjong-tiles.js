/**
 * 麻将牌面数据与矢量渲染器
 */
const MahjongTiles = {
    names: {
        1: "一万", 2: "二万", 3: "三万", 4: "四万", 5: "五万", 6: "六万", 7: "七万", 8: "八万", 9: "九万",
        10: "一筒", 11: "二筒", 12: "三筒", 13: "四筒", 14: "五筒", 15: "六筒", 16: "七筒", 17: "八筒", 18: "九筒",
        19: "一条", 20: "二条", 21: "三条", 22: "四条", 23: "五条", 24: "六条", 25: "七条", 26: "八条", 27: "九条",
        28: "东风", 29: "南风", 30: "西风", 31: "北风",
        32: "红中", 33: "发财", 34: "白板"
    },

    shortNames: {
        1: "一万", 2: "二万", 3: "三万", 4: "四万", 5: "五万", 6: "六万", 7: "七万", 8: "八万", 9: "九万",
        10: "1筒", 11: "2筒", 12: "3筒", 13: "4筒", 14: "5筒", 15: "6筒", 16: "7筒", 17: "8筒", 18: "9筒",
        19: "1条", 20: "2条", 21: "3条", 22: "4条", 23: "5条", 24: "6条", 25: "7条", 26: "8条", 27: "9条",
        28: "东", 29: "南", 30: "西", 31: "北",
        32: "中", 33: "发", 34: "白"
    },

    unicodes: {
        1: "🀇", 2: "🀈", 3: "🀉", 4: "🀊", 5: "🀋", 6: "🀌", 7: "🀍", 8: "🀎", 9: "🀏",
        10: "🀙", 11: "🀚", 12: "🀛", 13: "🀜", 14: "🀝", 15: "🀞", 16: "🀟", 17: "🀠", 18: "🀡",
        19: "🀐", 20: "🀑", 21: "🀒", 22: "🀓", 23: "🀔", 24: "🀕", 25: "🀖", 26: "🀗", 27: "🀘",
        28: "🀀", 29: "🀁", 30: "🀂", 31: "🀃",
        32: "🀄", 33: "🀅", 34: "🀆"
    },

    tileFiles: {
        1: 'Man1.svg', 2: 'Man2.svg', 3: 'Man3.svg', 4: 'Man4.svg', 5: 'Man5.svg',
        6: 'Man6.svg', 7: 'Man7.svg', 8: 'Man8.svg', 9: 'Man9.svg',
        10: 'Pin1.svg', 11: 'Pin2.svg', 12: 'Pin3.svg', 13: 'Pin4.svg', 14: 'Pin5.svg',
        15: 'Pin6.svg', 16: 'Pin7.svg', 17: 'Pin8.svg', 18: 'Pin9.svg',
        19: 'Sou1.svg', 20: 'Sou2.svg', 21: 'Sou3.svg', 22: 'Sou4.svg', 23: 'Sou5.svg',
        24: 'Sou6.svg', 25: 'Sou7.svg', 26: 'Sou8.svg', 27: 'Sou9.svg',
        28: 'Ton.svg', 29: 'Nan.svg', 30: 'Shaa.svg', 31: 'Pei.svg',
        32: 'Chun.svg', 33: 'Hatsu.svg', 34: 'Haku.svg'
    },

    getType(card) {
        if (card >= 1 && card <= 9) return "wan";
        if (card >= 10 && card <= 18) return "tong";
        if (card >= 19 && card <= 27) return "tiao";
        if (card >= 28 && card <= 31) return "feng";
        if (card >= 32 && card <= 34) return "jian";
        return "other";
    },

    getName(card) {
        return this.names[card] || "未知";
    },

    getShortName(card) {
        return this.shortNames[card] || "";
    },

    /**
     * 渲染麻将牌 HTML
     * @param {number} card 牌ID (1-34)
     * @param {object} options 配置: isGui, size ('normal'|'small'|'large'), extraClass, count
     */
    renderTile(card, options = {}) {
        const sizeClass = options.size ? `mj-${options.size}` : '';
        const extraClass = options.extraClass || '';

        if (!card || card <= 0) {
            return `<div class="mj-tile mj-back ${sizeClass} ${extraClass}"></div>`;
        }

        const type = this.getType(card);
        const name = this.getName(card);
        const isGui = !!options.isGui;
        const countBadge = options.count !== undefined ? `<span class="mj-count-badge">剩${options.count}张</span>` : '';
        const guiBadge = isGui ? `<span class="mj-gui-tag">鬼</span>` : '';
        const svgFile = this.tileFiles[card] || '';

        return `
            <div class="mj-tile mj-${type} ${sizeClass} ${extraClass} ${isGui ? 'mj-is-gui' : ''}" 
                 data-card="${card}" title="${name}${isGui ? ' (鬼牌/癞子)' : ''}">
                <div class="mj-tile-body">
                    <img src="/tiles/${svgFile}" class="mj-tile-img" alt="${name}" draggable="false" />
                    ${guiBadge}
                    ${countBadge}
                </div>
            </div>
        `;
    }
};
