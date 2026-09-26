/**
 * 麻将算法 & AI 网页对战平台 - 前端主控制逻辑
 */

// 全局状态
let gameState = null;
let gameLoopTimer = null;
let playSpeed = 800; // ms
let isProcessingStep = false;
let selectedHandCard = null;
let spectatorMode = false;
let showAiCards = false;
let winModalShownGameId = null;

// 算法实验室状态
let labHand = [];
let labGui = [];

// 页面初始化
document.addEventListener('DOMContentLoaded', () => {
    initTilePicker();
    initLabGuiSelector();
    initPresets();
    fetchGameState();
    startGameLoop();

    // 默认在实验室载入一个测试用例
    loadPreset('liang_mian_ting');

    document.addEventListener('fullscreenchange', () => {
        const btn = document.getElementById('btnFullscreen');
        if (btn) {
            btn.textContent = document.fullscreenElement ? '⛶ 退出全屏' : '⛶ 全屏';
        }
    });
});

function toggleFullScreen() {
    if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(err => {
            console.warn(`Fullscreen error: ${err.message}`);
        });
    } else {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        }
    }
}

/* ==========================================================================
   Tab 切换
   ========================================================================== */
function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.view-section').forEach(view => view.classList.remove('active'));

    if (tabId === 'arena') {
        document.getElementById('tabBtnArena').classList.add('active');
        document.getElementById('viewArena').classList.add('active');
    } else {
        document.getElementById('tabBtnLab').classList.add('active');
        document.getElementById('viewLab').classList.add('active');
    }
}

function toggleSound() {
    const enabled = SoundEffects.toggle();
    const btn = document.getElementById('btnSound');
    btn.textContent = enabled ? '🔊 音效: 开' : '🔈 音效: 关';
}

function toggleSidePanel() {
    const drawer = document.getElementById('sideDrawer');
    drawer.classList.toggle('open');
}

function setPlaySpeed(speed) {
    playSpeed = speed;
    document.querySelectorAll('.speed-btn').forEach(btn => {
        btn.classList.toggle('active', parseInt(btn.dataset.speed) === speed);
    });
}

function toggleSpectatorMode() {
    spectatorMode = document.getElementById('spectatorToggle').checked;
    fetch('/api/game/new', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ spectator: spectatorMode })
    })
    .then(r => r.json())
    .then(data => {
        gameState = data;
        renderGame();
    });
}

function toggleShowAiCards() {
    showAiCards = document.getElementById('showAiCardsToggle').checked;
    renderGame();
}

/* ==========================================================================
   对战台核心状态机与主循环
   ========================================================================== */
function startGameLoop() {
    if (gameLoopTimer) clearInterval(gameLoopTimer);
    gameLoopTimer = setInterval(async () => {
        if (!gameState || isProcessingStep) return;

        // 如果游戏结束或正在等待真人玩家操作，暂停自动推进
        if (gameState.phase === 'GAME_OVER' || gameState.phase === 'WAITING_USER') {
            return;
        }

        // 如果轮到真人玩家出牌，且不是观战模式，等待玩家出牌
        if (gameState.phase === 'DISCARD' && gameState.currentSeat === 0 && !gameState.spectatorMode) {
            return;
        }

        // 否则驱动 AI 进行下一步
        isProcessingStep = true;
        try {
            const res = await fetch('/api/game/step', { method: 'POST' });
            const data = await res.json();
            if (data.view) {
                const prevWall = gameState.wallCount;
                const prevDiscard = gameState.lastDiscard;
                gameState = data.view;
                renderGame();

                // 播放对应音效
                if (gameState.lastDiscard !== prevDiscard && gameState.lastDiscard > 0) {
                    SoundEffects.playDiscard();
                } else if (gameState.wallCount !== prevWall) {
                    SoundEffects.playDraw();
                }

                // 检查是否胡牌
                if (gameState.settlement && !gameState.settlement.isDraw && data.view.phase === 'GAME_OVER') {
                    SoundEffects.playHu();
                    showWinModal(gameState.settlement);
                }
            }
        } catch (e) {
            console.error("Step error:", e);
        } finally {
            isProcessingStep = false;
        }
    }, playSpeed);
}

// 获取当前最新状态
async function fetchGameState() {
    try {
        const res = await fetch('/api/game/state');
        gameState = await res.json();
        renderGame();
    } catch (e) {
        console.error("Fetch state error:", e);
    }
}

/* ==========================================================================
   渲染麻将桌界面
   ========================================================================== */
function renderGame() {
    if (!gameState) return;

    // 当轮到玩家打牌时，默认由 AI 直接推荐并自动选中该牌
    if (gameState.currentSeat === 0 && gameState.phase === 'DISCARD') {
        const userHand = gameState.players[0] ? gameState.players[0].hand : null;
        if (userHand && userHand.length > 0) {
            if (!selectedHandCard || !userHand.includes(selectedHandCard)) {
                if (gameState.aiRecommendation && gameState.aiRecommendation.recommendedDiscard > 0) {
                    selectedHandCard = gameState.aiRecommendation.recommendedDiscard;
                } else {
                    selectedHandCard = userHand[userHand.length - 1];
                }
            }
        }
    } else if (gameState.phase !== 'DISCARD') {
        selectedHandCard = null;
    }

    // 1. 罗盘与公共信息 (0:南-玩家, 1:东-下家, 2:北-对家, 3:西-上家)
    document.getElementById('wallCount').textContent = gameState.wallCount;
    ['South', 'East', 'North', 'West'].forEach((dir, i) => {
        const el = document.getElementById('dir' + dir);
        if (el) {
            el.classList.toggle('turn-active', gameState.currentSeat === i);
        }
    });

    // 2. 宝牌(鬼牌)展示
    const guiPreviewEl = document.getElementById('guiTilePreview');
    if (gameState.guiCards && gameState.guiCards.length > 0) {
        const guiCard = gameState.guiCards[0];
        guiPreviewEl.innerHTML = MahjongTiles.renderTile(guiCard, { isGui: true, size: 'normal' });
    } else {
        guiPreviewEl.innerHTML = `<span style="font-size:12px;color:#94a3b8;margin-left:4px;">无</span>`;
    }

    // 3. 渲染4家玩家
    renderPlayerSeat(0, 'South');
    renderPlayerSeat(1, 'East');
    renderPlayerSeat(2, 'North');
    renderPlayerSeat(3, 'West');

    // 4. 渲染四方弃牌河
    renderRiver(0, 'riverSouth');
    renderRiver(1, 'riverEast');
    renderRiver(2, 'riverNorth');
    renderRiver(3, 'riverWest');

    // 5. 渲染听牌悬浮面板与手牌打出听牌提示
    renderTingHintBoard();

    // 6. 渲染操作按钮面板
    renderActionPanel();

    // 7. 渲染记牌器与日志
    renderCardCounter();
    renderLogs();
    renderAiRadar();

    // 8. 游戏结束处理：胡牌或流局自动展示结算弹窗
    if (gameState.phase === 'GAME_OVER' && gameState.settlement) {
        if (winModalShownGameId !== gameState.gameId) {
            winModalShownGameId = gameState.gameId;
            if (!gameState.settlement.isDraw) {
                SoundEffects.playHu();
                showWinModal(gameState.settlement);
            } else {
                showDrawModal();
            }
        }
    }
}

function renderPlayerSeat(seatIndex, dirName) {
    const p = gameState.players[seatIndex];
    if (!p) return;

    document.getElementById('name' + dirName).textContent = p.name;
    document.getElementById('score' + dirName).textContent = p.score;
    const tingBadge = document.getElementById('ting' + dirName);
    if (tingBadge) {
        tingBadge.classList.toggle('active', !!p.isTing);
    }

    // 渲染副牌 (Melds)
    const meldEl = document.getElementById('melds' + dirName);
    if (meldEl) {
        let html = '';
        if (p.melds) {
            p.melds.forEach(m => {
                html += `<div class="meld-group" title="${m.type}">`;
                m.cards.forEach(c => {
                    const isGui = gameState.guiCards.includes(c);
                    html += MahjongTiles.renderTile(c, { size: 'small', isGui });
                });
                html += `</div>`;
            });
        }
        meldEl.innerHTML = html;
    }

    // 渲染手牌
    const handEl = document.getElementById('hand' + dirName);
    if (!handEl) return;

    // 如果是南家 (玩家自己)
    if (seatIndex === 0) {
        renderUserHand(p);
    } else {
        // AI 手牌：如果勾选了“显示AI手牌”，或处于观战模式，或游戏结束，显示正面，否则显示暗牌背
        let html = '';
        const shouldShowFront = (showAiCards || gameState.spectatorMode || gameState.phase === 'GAME_OVER');
        if (p.hand && shouldShowFront) {
            p.hand.forEach(c => {
                const isGui = gameState.guiCards.includes(c);
                html += MahjongTiles.renderTile(c, { size: 'small', isGui });
            });
        } else {
            const count = p.hand ? p.hand.length : p.tileCount;
            for (let i = 0; i < count; i++) {
                html += MahjongTiles.renderTile(0, { size: 'small' });
            }
        }
        handEl.innerHTML = html;
    }
}

// 渲染玩家自己的手牌（支持打牌听牌提示、AI高亮）
function renderUserHand(player) {
    const handContainer = document.getElementById('handSouth');
    const drawnSlot = document.getElementById('drawnCardSlot');
    if (!player.hand) return;

    const isTurn = gameState.currentSeat === 0 && gameState.phase === 'DISCARD';
    const discardTingMap = gameState.discardToTing || {};
    const rec = gameState.aiRecommendation;
    const recAction = rec ? (rec.recommendedAction || 'discard') : null;
    const recDiscard = rec ? rec.recommendedDiscard : 0;
    const recCard = rec ? (rec.recommendedCard || rec.recommendedDiscard) : 0;
    const recCards = (rec && rec.recommendedCards && rec.recommendedCards.length > 0) ? rec.recommendedCards : (recDiscard ? [recDiscard] : []);

    let handCards = [...player.hand];
    let drawnCard = player.lastDrawnCard;

    // 如果摸牌在手牌中，将其分离显示在单独的摸牌槽位
    if (handCards.length % 3 === 2 && drawnCard && handCards.includes(drawnCard)) {
        const idx = handCards.lastIndexOf(drawnCard);
        handCards.splice(idx, 1);
    } else {
        drawnCard = 0;
    }

    // 辅助函数：判断一张牌是否属于 AI 推荐并返回对应的高亮及角标
    function getAiRecInfo(c) {
        if (!rec) return { isAiRec: false, badge: '' };
        if (isTurn && recAction === 'discard' && c === recDiscard) {
            return { isAiRec: true, badge: 'AI推荐' };
        }
        if (recAction === 'peng' && c === recCard) {
            return { isAiRec: true, badge: 'AI推荐' };
        }
        if (recAction === 'gang' && c === recCard) {
            return { isAiRec: true, badge: 'AI推荐' };
        }
        if (recAction === 'chi' && recCards.includes(c)) {
            return { isAiRec: true, badge: 'AI推荐' };
        }
        return { isAiRec: false, badge: '' };
    }

    // 渲染主力手牌
    handContainer.innerHTML = handCards.map(c => {
        const isGui = gameState.guiCards.includes(c);
        const canTing = !!discardTingMap[c];
        const { isAiRec, badge: aiBadge } = getAiRecInfo(c);
        const isSelected = (selectedHandCard === c);

        let extraClass = '';
        if (isAiRec) extraClass += ' ai-recommended';
        if (isSelected) extraClass += ' selected';

        let badgeHtml = '';
        if (aiBadge) {
            badgeHtml += `<span class="mj-ai-rec-badge">${aiBadge}</span>`;
        }
        if (canTing) {
            badgeHtml += `<span class="mj-can-ting-badge">听</span>`;
        }

        return `
            <div class="user-hand-item" 
                 onclick="onHandCardClick(${c})" 
                 onmouseenter="onHandCardHover(event, ${c})" 
                 onmouseleave="onHandCardLeave()">
                ${MahjongTiles.renderTile(c, { isGui, extraClass })}
                ${badgeHtml}
            </div>
        `;
    }).join('');

    // 渲染最新摸牌槽位
    if (drawnCard > 0) {
        const isGui = gameState.guiCards.includes(drawnCard);
        const canTing = !!discardTingMap[drawnCard];
        const { isAiRec, badge: aiBadge } = getAiRecInfo(drawnCard);
        const isSelected = (selectedHandCard === drawnCard);

        let extraClass = '';
        if (isAiRec) extraClass += ' ai-recommended';
        if (isSelected) extraClass += ' selected';

        let badgeHtml = '';
        if (aiBadge) {
            badgeHtml += `<span class="mj-ai-rec-badge">${aiBadge}</span>`;
        }
        if (canTing) {
            badgeHtml += `<span class="mj-can-ting-badge">听</span>`;
        }

        drawnSlot.innerHTML = `
            <div class="user-hand-item" 
                 onclick="onHandCardClick(${drawnCard})" 
                 onmouseenter="onHandCardHover(event, ${drawnCard})" 
                 onmouseleave="onHandCardLeave()">
                ${MahjongTiles.renderTile(drawnCard, { isGui, extraClass })}
                ${badgeHtml}
            </div>
        `;
    } else {
        drawnSlot.innerHTML = '';
    }
}

// 渲染弃牌河
function renderRiver(seatIndex, elementId) {
    const p = gameState.players[seatIndex];
    const el = document.getElementById(elementId);
    if (!p || !el) return;

    el.innerHTML = p.discards.map((c, idx) => {
        const isLast = (seatIndex === gameState.lastDiscardSeat && idx === p.discards.length - 1);
        const isGui = gameState.guiCards.includes(c);
        return MahjongTiles.renderTile(c, {
            size: 'small',
            isGui,
            extraClass: isLast ? 'last-discard-highlight' : ''
        });
    }).join('');
}

// 渲染听牌看板（核心功能：默认AI自动提示当前听牌或打出后听牌）
function renderTingHintBoard() {
    const board = document.getElementById('tingHintBoard');
    const container = document.getElementById('tingBoardTiles');
    const titleEl = board.querySelector('.ting-board-title');

    // 场景 1: 轮到玩家出牌回合，根据当前选中的牌（默认由AI推荐），自动展示打出此牌后听什么！
    if (gameState.currentSeat === 0 && gameState.phase === 'DISCARD') {
        const discardTingMap = gameState.discardToTing || {};
        const targets = selectedHandCard ? discardTingMap[selectedHandCard] : null;
        if (targets && targets.length > 0) {
            board.classList.add('active');
            titleEl.innerHTML = `🎯 听牌提示 (打出【${MahjongTiles.getName(selectedHandCard)}】可胡)：`;
            container.innerHTML = targets.map(item => {
                const isGui = gameState.guiCards.includes(item.card);
                return `
                    <div class="ting-tile-wrap" style="display:inline-flex;align-items:center;gap:3px;background:rgba(0,0,0,0.3);padding:1px 4px;border-radius:4px;">
                        ${MahjongTiles.renderTile(item.card, { size: 'small', isGui })}
                        <span class="ting-remain-tag ${item.remainingCount <= 1 ? 'danger' : ''}" style="font-size:10px;color:var(--gold);">
                            剩${item.remainingCount}张
                        </span>
                    </div>
                `;
            }).join('');
            return;
        }
    }

    // 场景 2: 手牌已听牌（等待点炮或摸牌胡牌）
    if (gameState.currentTingTargets && gameState.currentTingTargets.length > 0) {
        board.classList.add('active');
        titleEl.innerHTML = `🔥 已听牌！可胡以下牌张：`;
        container.innerHTML = gameState.currentTingTargets.map(item => {
            const isGui = gameState.guiCards.includes(item.card);
            return `
                <div class="ting-tile-wrap" style="display:inline-flex;align-items:center;gap:3px;background:rgba(0,0,0,0.3);padding:1px 4px;border-radius:4px;">
                    ${MahjongTiles.renderTile(item.card, { size: 'small', isGui })}
                    <span class="ting-remain-tag ${item.remainingCount <= 1 ? 'danger' : ''}" style="font-size:10px;color:var(--gold);">
                        剩${item.remainingCount}张
                    </span>
                </div>
            `;
        }).join('');
        return;
    }

    board.classList.remove('active');
    container.innerHTML = '';
}

// 手牌悬浮事件：显示打出此牌听哪些牌
function onHandCardHover(event, card) {
    const tooltip = document.getElementById('tileTingTooltip');
    const discardTingMap = gameState.discardToTing || {};
    const tings = discardTingMap[card];

    if (tings && tings.length > 0) {
        const tingHtml = tings.map(t => 
            `<strong style="color:var(--gold);">${MahjongTiles.getName(t.card)}</strong> (剩${t.remainingCount}张)`
        ).join('、');

        tooltip.innerHTML = `🎯 打出 <strong>【${MahjongTiles.getName(card)}】</strong> 听：${tingHtml}`;
        tooltip.style.display = 'block';
        tooltip.style.left = (event.clientX + 10) + 'px';
        tooltip.style.top = (event.clientY - 40) + 'px';
    }
}

function onHandCardLeave() {
    document.getElementById('tileTingTooltip').style.display = 'none';
}

// 点击手牌：仅选中切换，绝不直接打出！必须点击【打牌】按钮操作
function onHandCardClick(card) {
    if (gameState.currentSeat !== 0 || gameState.phase !== 'DISCARD') {
        return;
    }
    selectedHandCard = card;
    renderGame();
}

async function doDiscard(card) {
    try {
        const res = await fetch('/api/game/discard', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ card })
        });
        const data = await res.json();
        if (data.view) {
            gameState = data.view;
            SoundEffects.playDiscard();
            renderGame();
        }
    } catch (e) {
        console.error("Discard error:", e);
    }
}

/* ==========================================================================
   吃 / 碰 / 杠 / 胡 / 过 / 打牌 操作面板
   ========================================================================== */
function renderActionPanel() {
    const panel = document.getElementById('actionPanel');
    const acts = gameState.availableActions;
    const isMyTurnDiscard = (gameState.currentSeat === 0 && gameState.phase === 'DISCARD');
    const isWaitingUser = (gameState.phase === 'WAITING_USER');

    if (!isMyTurnDiscard && !isWaitingUser) {
        panel.classList.remove('active');
        return;
    }

    panel.classList.add('active');

    const btnDiscard = document.getElementById('btnActionDiscard');
    const btnChi = document.getElementById('btnActionChi');
    const btnPeng = document.getElementById('btnActionPeng');
    const btnGang = document.getElementById('btnActionGang');
    const btnHu = document.getElementById('btnActionHu');
    const btnPass = document.getElementById('btnActionPass');

    if (isMyTurnDiscard) {
        // 出牌回合：显示【打牌】主按钮，若可自摸胡或暗杠/补杠则同时显示相应按钮
        btnDiscard.style.display = 'flex';
        btnChi.style.display = 'none';
        btnPeng.style.display = 'none';
        btnPass.style.display = 'none';
        btnHu.style.display = (acts && acts.canHu) ? 'flex' : 'none';
        btnGang.style.display = (acts && acts.canGang) ? 'flex' : 'none';
    } else if (isWaitingUser) {
        // 他人出牌响应阶段：显示 吃、碰、杠、胡、过
        btnDiscard.style.display = 'none';
        btnChi.style.display = (acts && acts.canChi) ? 'flex' : 'none';
        btnPeng.style.display = (acts && acts.canPeng) ? 'flex' : 'none';
        btnGang.style.display = (acts && acts.canGang) ? 'flex' : 'none';
        btnHu.style.display = (acts && acts.canHu) ? 'flex' : 'none';
        btnPass.style.display = (acts && acts.canPass) ? 'flex' : 'none';
    }

    // 清除上一次所有操作按钮的推荐高亮与角标
    [btnDiscard, btnChi, btnPeng, btnGang, btnHu, btnPass].forEach(btn => {
        btn.classList.remove('ai-rec-btn');
        const tag = btn.querySelector('.btn-ai-tag');
        if (tag) tag.remove();
    });

    // 针对 AI 推荐的操作（打、碰、杠、胡、吃、过）直接高亮并在按钮上标注【AI推荐】
    const rec = gameState.aiRecommendation;
    if (rec && rec.recommendedAction) {
        let recBtn = null;
        if (rec.recommendedAction === 'discard' && isMyTurnDiscard) recBtn = btnDiscard;
        else if (rec.recommendedAction === 'peng') recBtn = btnPeng;
        else if (rec.recommendedAction === 'gang') recBtn = btnGang;
        else if (rec.recommendedAction === 'hu') recBtn = btnHu;
        else if (rec.recommendedAction === 'chi') recBtn = btnChi;
        else if (rec.recommendedAction === 'pass') recBtn = btnPass;

        if (recBtn && recBtn.style.display !== 'none') {
            recBtn.classList.add('ai-rec-btn');
            const tag = document.createElement('span');
            tag.className = 'btn-ai-tag';
            tag.textContent = 'AI推荐';
            recBtn.appendChild(tag);
        }
    }
}

async function onUserActionClick(action) {
    if (action === 'discard') {
        if (selectedHandCard > 0) {
            const cardToDiscard = selectedHandCard;
            selectedHandCard = null;
            await doDiscard(cardToDiscard);
        } else {
            alert("请点击选择一张要打出的手牌！");
        }
        return;
    }
    if (action === 'chi') {
        const opts = gameState.availableActions.chiOptions;
        if (opts && opts.length === 1) {
            // 只有一种顺子直接吃
            await submitAction('chi', 0, opts[0][0], opts[0][1]);
        } else if (opts && opts.length > 1) {
            // 弹出选择弹窗
            openChiModal(opts);
        }
        return;
    }

    if (action === 'peng') {
        selectedHandCard = null;
        await submitAction('peng', gameState.lastDiscard);
        return;
    }

    if (action === 'gang') {
        const gangs = gameState.availableActions.gangCards;
        const target = (gangs && gangs.length > 0) ? gangs[0] : 0;
        if (gameState.phase === 'DISCARD') {
            // 自摸暗杠/补杠
            await submitSelfAction('gang', target);
        } else {
            // 明杠
            await submitAction('gang', target);
        }
        return;
    }

    if (action === 'hu') {
        if (gameState.phase === 'DISCARD') {
            await submitSelfAction('hu', 0);
        } else {
            await submitAction('hu', 0);
        }
        return;
    }

    if (action === 'pass') {
        await submitAction('pass', 0);
    }
}

function openChiModal(options) {
    const modal = document.getElementById('chiSelectionModal');
    const list = document.getElementById('chiOptionsList');
    modal.classList.add('active');

    list.innerHTML = options.map((opt, i) => `
        <div class="chi-opt-card" onclick="selectChiOption(${opt[0]}, ${opt[1]})">
            ${MahjongTiles.renderTile(opt[0], { size: 'small' })}
            ${MahjongTiles.renderTile(gameState.lastDiscard, { size: 'small', extraClass: 'last-discard-highlight' })}
            ${MahjongTiles.renderTile(opt[1], { size: 'small' })}
        </div>
    `).join('');
}

function closeChiModal() {
    document.getElementById('chiSelectionModal').classList.remove('active');
}

async function selectChiOption(c1, c2) {
    closeChiModal();
    await submitAction('chi', 0, c1, c2);
}

async function submitAction(action, card, chi1, chi2) {
    try {
        const res = await fetch('/api/game/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action, card, chi1, chi2 })
        });
        const data = await res.json();
        if (data.view) {
            gameState = data.view;
            if (action === 'hu') {
                SoundEffects.playHu();
                if (gameState.settlement) {
                    winModalShownGameId = gameState.gameId;
                    showWinModal(gameState.settlement);
                }
            } else if (action === 'gang' || action === 'peng') {
                SoundEffects.playAction(action);
            }
            renderGame();
        }
    } catch (e) {
        console.error("Action error:", e);
    }
}

async function submitSelfAction(action, card) {
    try {
        const res = await fetch('/api/game/self_action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action, card })
        });
        const data = await res.json();
        if (data.view) {
            gameState = data.view;
            if (action === 'hu') {
                SoundEffects.playHu();
                if (gameState.settlement) {
                    winModalShownGameId = gameState.gameId;
                    showWinModal(gameState.settlement);
                }
            } else if (action === 'gang') {
                SoundEffects.playAction('gang');
            }
            renderGame();
        }
    } catch (e) {
        console.error("Self action error:", e);
    }
}

/* ==========================================================================
   AI 推荐与辅助面板
   ========================================================================== */
function requestAiHint() {
    if (!gameState || !gameState.aiRecommendation) {
        alert("当前不在出牌阶段或无可用推荐！");
        return;
    }
    const rec = gameState.aiRecommendation;
    selectedHandCard = rec.recommendedDiscard;
    renderGame();
    alert(`💡 AI推荐打出: 【${MahjongTiles.getName(rec.recommendedDiscard)}】\n${rec.reason}`);
}

function renderAiRadar() {
    const el = document.getElementById('aiRadarContent');
    if (gameState && gameState.aiRecommendation) {
        const rec = gameState.aiRecommendation;
        el.innerHTML = `
            <div><strong>推荐出牌:</strong> ${MahjongTiles.getName(rec.recommendedDiscard)}</div>
            <div><strong>期望评分:</strong> ${rec.score.toFixed(2)}</div>
            <div style="font-size:11px;color:#94a3b8;margin-top:4px;">${rec.reason}</div>
        `;
    } else {
        el.textContent = '轮到玩家出牌时可查看 AI 实时分析';
    }
}

function renderCardCounter() {
    const grid = document.getElementById('cardCounterGrid');
    if (!gameState || !gameState.cardRemainCounts) return;

    const rows = [
        { title: '万', start: 1, end: 9 },
        { title: '筒', start: 10, end: 18 },
        { title: '条', start: 19, end: 27 },
        { title: '字', start: 28, end: 34 }
    ];

    grid.innerHTML = rows.map(r => `
        <div class="counter-row">
            ${Array.from({ length: r.end - r.start + 1 }, (_, i) => {
                const card = r.start + i;
                const rem = gameState.cardRemainCounts[card] !== undefined ? gameState.cardRemainCounts[card] : 4;
                const isGui = gameState.guiCards.includes(card);
                return `
                    <div class="counter-item ${isGui ? 'mj-is-gui' : ''}" title="${MahjongTiles.getName(card)}">
                        <span>${MahjongTiles.getShortName(card)}</span>
                        <strong class="counter-num ${rem === 0 ? 'empty' : ''}">${rem}</strong>
                    </div>
                `;
            }).join('')}
        </div>
    `).join('');
}

function renderLogs() {
    const el = document.getElementById('gameLogsList');
    if (!gameState || !gameState.logs) return;

    el.innerHTML = gameState.logs.slice().reverse().map(l => `
        <div class="log-entry">${l}</div>
    `).join('');
}

/* ==========================================================================
   开新局 & 胡牌结算弹窗
   ========================================================================== */
function openNewGameModal() {
    document.getElementById('newGameModal').classList.add('active');
}

function closeNewGameModal() {
    document.getElementById('newGameModal').classList.remove('active');
}

async function confirmNewGame() {
    const guiRule = document.querySelector('input[name="guiRule"]:checked').value;
    closeNewGameModal();
    closeWinModal();
    winModalShownGameId = null;

    try {
        const res = await fetch('/api/game/new', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                guiMode: guiRule,
                spectator: spectatorMode
            })
        });
        gameState = await res.json();
        renderGame();
    } catch (e) {
        console.error("New game error:", e);
    }
}

function showWinModal(settle) {
    if (!settle) return;
    const modal = document.getElementById('winModal');
    if (!modal) return;

    const winnerSeat = settle.winnerSeat !== undefined ? settle.winnerSeat : 0;
    const winnerPlayer = gameState && gameState.players && gameState.players[winnerSeat] 
        ? gameState.players[winnerSeat] 
        : { name: '玩家', hand: [], melds: [] };
    const winnerName = winnerPlayer.name || (winnerSeat === 0 ? '你' : `电脑${winnerSeat}`);
    const isHumanWin = winnerSeat === 0;

    const winTitleEl = document.getElementById('winTitle');
    const winIconEl = document.getElementById('winIcon');
    if (winTitleEl) {
        winTitleEl.textContent = isHumanWin ? '🎉 恭喜你胡牌大吉！' : `🎊 ${winnerName} 胡牌！`;
    }
    if (winIconEl) {
        winIconEl.textContent = isHumanWin ? '🏆' : '🀄';
    }

    const winPlayerNameEl = document.getElementById('winPlayerName');
    if (winPlayerNameEl) winPlayerNameEl.textContent = winnerName;

    const winScoreTextEl = document.getElementById('winScoreText');
    if (winScoreTextEl) {
        const totalFan = settle.totalFan || 1;
        const points = settle.points || (totalFan * 10);
        winScoreTextEl.textContent = `${totalFan} 番 (${points}分)`;
    }

    // 胡牌张展示
    const cardTileEl = document.getElementById('winCardTile');
    if (cardTileEl) {
        const winCard = settle.winCard || 0;
        if (winCard > 0) {
            const isGui = gameState && gameState.guiCards && gameState.guiCards.includes(winCard);
            cardTileEl.innerHTML = MahjongTiles.renderTile(winCard, { size: 'normal', isGui: isGui });
        } else {
            cardTileEl.innerHTML = '<span style="font-size:12px;color:#888;">自摸</span>';
        }
    }

    // 番型展示
    const tagContainer = document.getElementById('winPatternTags');
    if (tagContainer) {
        const patterns = Array.isArray(settle.patterns) && settle.patterns.length > 0 
            ? settle.patterns 
            : ['平胡'];
        tagContainer.innerHTML = patterns.map(p => `<span class="pattern-tag">${p}</span>`).join('');
    }

    // 完整手牌展示
    const handsRevealEl = document.getElementById('winHandsReveal');
    if (handsRevealEl) {
        let html = '<div class="reveal-title" style="font-size:12px;color:#8fa3b7;margin:8px 0 4px 0;text-align:left;">获胜者手牌：</div><div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap;justify-content:center;">';
        // 副露 (碰、杠、吃)
        if (winnerPlayer.melds && winnerPlayer.melds.length > 0) {
            winnerPlayer.melds.forEach(m => {
                html += '<div style="display:flex;gap:1px;background:rgba(0,0,0,0.3);padding:2px;border-radius:4px;margin-right:6px;">';
                const meldCards = m.cards || [];
                meldCards.forEach(c => {
                    html += MahjongTiles.renderTile(c, { size: 'small', isGui: gameState && gameState.guiCards && gameState.guiCards.includes(c) });
                });
                html += '</div>';
            });
        }
        // 手牌
        if (winnerPlayer.hand && winnerPlayer.hand.length > 0) {
            winnerPlayer.hand.forEach(c => {
                html += MahjongTiles.renderTile(c, { size: 'small', isGui: gameState && gameState.guiCards && gameState.guiCards.includes(c) });
            });
        }
        // 额外展示胡牌张
        if (settle.winCard && settle.winCard > 0) {
            html += '<span style="margin:0 4px;font-size:18px;color:#ffd700;font-weight:bold;">+</span>';
            html += MahjongTiles.renderTile(settle.winCard, { size: 'small', isGui: gameState && gameState.guiCards && gameState.guiCards.includes(settle.winCard) });
        }
        html += '</div>';
        handsRevealEl.innerHTML = html;
    }

    modal.classList.add('active');
}

function showDrawModal() {
    const modal = document.getElementById('winModal');
    if (!modal) return;
    const winTitleEl = document.getElementById('winTitle');
    if (winTitleEl) winTitleEl.textContent = '🤝 流局 (黄庄)';
    const winIconEl = document.getElementById('winIcon');
    if (winIconEl) winIconEl.textContent = '💨';
    const winPlayerNameEl = document.getElementById('winPlayerName');
    if (winPlayerNameEl) winPlayerNameEl.textContent = '无';
    const winScoreTextEl = document.getElementById('winScoreText');
    if (winScoreTextEl) winScoreTextEl.textContent = '0 番 (0分)';
    const cardTileEl = document.getElementById('winCardTile');
    if (cardTileEl) cardTileEl.innerHTML = '<span style="font-size:12px;color:#888;">牌墙已空</span>';
    const tagContainer = document.getElementById('winPatternTags');
    if (tagContainer) tagContainer.innerHTML = '<span class="pattern-tag">荒庄流局</span>';
    const handsRevealEl = document.getElementById('winHandsReveal');
    if (handsRevealEl) handsRevealEl.innerHTML = '';
    modal.classList.add('active');
}

function closeWinModal() {
    document.getElementById('winModal').classList.remove('active');
}

/* ==========================================================================
   算法实验室 (Lab View)
   ========================================================================== */
function initTilePicker() {
    const grid = document.getElementById('tilePickerGrid');
    const groups = [
        { label: '万子', start: 1, end: 9 },
        { label: '筒子', start: 10, end: 18 },
        { label: '条子', start: 19, end: 27 },
        { label: '风箭字牌', start: 28, end: 34 }
    ];

    grid.innerHTML = groups.map(g => `
        <div class="tile-picker-row">
            ${Array.from({ length: g.end - g.start + 1 }, (_, i) => {
                const card = g.start + i;
                return `
                    <div onclick="addLabTile(${card})">
                        ${MahjongTiles.renderTile(card, { size: 'normal' })}
                    </div>
                `;
            }).join('')}
        </div>
    `).join('');
}

function initLabGuiSelector() {
    const container = document.getElementById('labGuiSelector');
    // 提供无鬼牌、以及代表性白板、红中、东风等
    const candidates = [
        { id: 0, label: '无鬼牌' },
        { id: 34, label: '白板做鬼' },
        { id: 32, label: '红中做鬼' },
        { id: 1, label: '一万做鬼' },
        { id: 10, label: '一筒做鬼' },
        { id: 19, label: '一条做鬼' }
    ];

    container.innerHTML = candidates.map(c => `
        <button class="preset-btn ${labGui.includes(c.id) || (c.id === 0 && labGui.length === 0) ? 'active' : ''}" 
                id="guiBtn_${c.id}"
                onclick="setLabGui(${c.id})">
            ${c.label}
        </button>
    `).join('');
}

function setLabGui(cardId) {
    if (cardId === 0) {
        labGui = [];
    } else {
        labGui = [cardId];
    }
    initLabGuiSelector();
    renderLabHand();
}

function addLabTile(card) {
    if (labHand.length >= 14) {
        alert("手牌最多 14 张！");
        return;
    }
    labHand.push(card);
    labHand.sort((a, b) => a - b);
    renderLabHand();
}

function removeLabTile(index) {
    labHand.splice(index, 1);
    renderLabHand();
}

function clearLabHand() {
    labHand = [];
    renderLabHand();
}

function renderLabHand() {
    const rack = document.getElementById('labHandDisplay');
    document.getElementById('labHandCount').textContent = labHand.length;

    if (labHand.length === 0) {
        rack.innerHTML = `<span style="color:#64748b;font-size:13px;">请点击下方牌库添加手牌 (1-14张)</span>`;
        return;
    }

    rack.innerHTML = labHand.map((c, i) => `
        <div onclick="removeLabTile(${i})" title="点击移除此牌">
            ${MahjongTiles.renderTile(c, { size: 'normal', isGui: labGui.includes(c) })}
        </div>
    `).join('');
}

// 预设经典牌型载入
function initPresets() {
    window.presets = {
        'standard_hu': {
            // 111万 234万 123筒 789条 东东 (0鬼)
            hand: [1, 1, 1, 2, 3, 4, 10, 11, 12, 25, 26, 27, 28, 28],
            gui: []
        },
        'liang_mian_ting': {
            // 23筒 (听1筒4筒)
            hand: [1, 1, 1, 11, 12, 28, 28],
            gui: []
        },
        'dan_diao_ting': {
            // 123万 1筒 (单钓1筒)
            hand: [1, 2, 3, 10],
            gui: []
        },
        'gui_hu': {
            // 2鬼牌直接胡
            hand: [1, 2, 3, 10, 11, 34, 34],
            gui: [34]
        },
        'gui_ting_multi': {
            // 鬼牌多听
            hand: [1, 1, 10, 12, 11, 34],
            gui: [34]
        },
        'qi_dui': {
            // 七对子
            hand: [1, 1, 3, 3, 10, 10, 15, 15, 20, 20, 28, 28, 32, 32],
            gui: []
        },
        'qing_yi_se': {
            // 清一色万字听牌
            hand: [1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9],
            gui: []
        }
    };
}

function loadPreset(key) {
    const p = window.presets[key];
    if (!p) return;
    labHand = [...p.hand];
    labGui = [...p.gui];
    initLabGuiSelector();
    renderLabHand();
    runAlgoTest();
}

// 执行算法测试
async function runAlgoTest() {
    if (labHand.length === 0) {
        alert("请先添加测试手牌！");
        return;
    }

    try {
        const res = await fetch('/api/algo/test', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                cards: labHand,
                guiCards: labGui
            })
        });
        const data = await res.json();

        // 1. 胡牌检测
        const huStatusEl = document.getElementById('resHuStatus');
        const huTimeEl = document.getElementById('resHuTime');
        if (data.isHu) {
            huStatusEl.innerHTML = `<span style="color:#16a34a;">🎉 已经胡牌！</span>`;
        } else {
            huStatusEl.innerHTML = `<span style="color:#ef4444;">❌ 未胡牌</span>`;
        }
        huTimeEl.textContent = `耗时: ${data.huTimeMicros.toFixed(2)} µs (微秒)`;

        // 2. 听牌计算
        const tingStatusEl = document.getElementById('resTingStatus');
        const tingListEl = document.getElementById('resTingList');
        const tingTimeEl = document.getElementById('resTingTime');

        if (data.tingCards && data.tingCards.length > 0) {
            tingStatusEl.innerHTML = `<span style="color:#f59e0b;">🎯 听 ${data.tingCards.length} 门牌</span>`;
            tingListEl.innerHTML = data.tingCards.map(c => 
                MahjongTiles.renderTile(c, { size: 'small', isGui: labGui.includes(c) })
            ).join('');
        } else {
            tingStatusEl.innerHTML = `<span style="color:#94a3b8;">暂未听牌</span>`;
            tingListEl.innerHTML = '';
        }
        tingTimeEl.textContent = `耗时: ${data.tingTimeMicros.toFixed(2)} µs (微秒)`;

        // 3. AI 出牌推荐
        const aiStatusEl = document.getElementById('resAiStatus');
        const aiTileEl = document.getElementById('resAiTile');
        const aiTimeEl = document.getElementById('resAiTime');

        if (data.recommendedOut > 0) {
            aiStatusEl.innerHTML = `推荐打出: <strong>【${data.recommendedOutStr}】</strong> (评分: ${data.aiScore.toFixed(2)})`;
            aiTileEl.innerHTML = MahjongTiles.renderTile(data.recommendedOut, { size: 'small', extraClass: 'ai-recommended' });
            aiTimeEl.textContent = `耗时: ${data.aiTimeMicros.toFixed(2)} µs`;
        } else {
            aiStatusEl.innerHTML = `<span style="color:#94a3b8;">无需或无法出牌 (手牌模3余2时计算)</span>`;
            aiTileEl.innerHTML = '';
            aiTimeEl.textContent = `耗时: -`;
        }
    } catch (e) {
        console.error("Algo test error:", e);
    }
}
