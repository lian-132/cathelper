/* 猫娘助手 · 控制台原型交互 */
(function () {
  'use strict';

  var state = {
    master: false, revoke: false,
    cat: false, tts: false, auto: false,
    g1: false, g2: false, g3: false, g4: false,
    g5: false, g6: false, g7: false, g8: false
  };
  var LABEL = {
    master: ['总开关', 'masterState', ['已关闭', '已开启']],
    revoke: ['防撤回', 'revokeState', ['已关闭', '已开启']]
  };

  /* ---------- 波纹 + 按压 ---------- */
  document.addEventListener('pointerdown', function (e) {
    var el = e.target.closest('.press, .icon-cell');
    if (!el) return;
    var r = el.getBoundingClientRect();
    var d = Math.max(r.width, r.height);
    var s = document.createElement('span');
    s.className = 'ripple';
    s.style.width = s.style.height = d + 'px';
    s.style.left = (e.clientX - r.left - d / 2) + 'px';
    s.style.top = (e.clientY - r.top - d / 2) + 'px';
    el.appendChild(s);
    setTimeout(function () { s.remove(); }, 560);
  });

  /* ---------- JS↔Native 桥 ---------- */
  var bridge = window.nativeBridge || null;

  // 页面加载完从 Native 拿状态恢复开关
  if (bridge) {
    try {
      var saved = JSON.parse(bridge.getStates());
      Object.keys(saved).forEach(function (k) {
        if (!state.hasOwnProperty(k)) return;
        state[k] = !!saved[k];
        var el = document.querySelector('[data-key="' + k + '"]');
        if (el) {
          el.classList.toggle('on', state[k]);
          var st = document.getElementById(k + 'State');
          if (st) st.textContent = state[k] ? '开' : '关';
          var info = LABEL[k];
          if (info) {
            var txt = document.getElementById(info[1]);
            if (txt) txt.textContent = info[2][state[k] ? 1 : 0];
          }
        }
      });
      refreshHero();
    } catch (e) { /* 桥不可用就当纯原型看 */ }
  }

  /* 点击切换（改造：接桥落库） */
  document.addEventListener('click', function (e) {
    var el = e.target.closest('[data-key]');
    if (!el) return;
    var k = el.dataset.key;
    state[k] = !state[k];
    el.classList.toggle('on', state[k]);
    if (window.navigator.vibrate) window.navigator.vibrate(12);
    if (bridge) {
      try { bridge.setToggle(k, state[k]); } catch (err) {}
    }

    var info = LABEL[k];
    if (info) {
      var txt = document.getElementById(info[1]);
      if (txt) txt.textContent = info[2][state[k] ? 1 : 0];
    }
    var st = document.getElementById(k + 'State');
    if (st) st.textContent = state[k] ? '开' : '关';

    refreshHero();
    // 未接线的格子只提示不出功能
    if (k.charAt(0) === 'g' && !state[k]) return;
    if (k.charAt(0) === 'g') toast('「' + el.querySelector('b').textContent + '」还没接线，敬请期待~');
  });

  /* ---------- 顶部大卡联动 ---------- */
  function refreshHero() {
    var anyOn = state.master || state.cat || state.auto;
    var tag = document.getElementById('statusTag');
    var desc = document.getElementById('statusDesc');
    var sub = document.getElementById('subtitle');
    if (tag) {
      tag.textContent = anyOn ? '运行中' : '未启用';
      tag.classList.toggle('on', anyOn);
    }
    if (desc) desc.textContent = anyOn
      ? '猫娘化已在 TIM 内生效，去聊天框试试～'
      : '模块已就绪，等待开启';
    if (sub) sub.textContent = anyOn ? 'TIM 增强 · 已生效' : 'TIM 增强 · 控制台';
  }

  /* ---------- 刷新按钮 ---------- */
  var btn = document.getElementById('btnRefresh');
  if (btn) btn.addEventListener('click', function () {
    toast('状态已刷新');
    refreshHero();
  });

  /* ---------- 大卡轻微跟随手指（3D 倾斜） ---------- */
  var hero = document.querySelector('.hero');
  if (hero) {
    hero.addEventListener('pointermove', function (e) {
      var r = hero.getBoundingClientRect();
      var px = (e.clientX - r.left) / r.width - 0.5;
      var py = (e.clientY - r.top) / r.height - 0.5;
      hero.style.transform = 'perspective(700px) rotateY(' + (px * 8) + 'deg) rotateX(' +
        (-py * 8) + 'deg) scale(1.012)';
    });
    hero.addEventListener('pointerleave', function () {
      hero.style.transform = '';
    });
  }

  /* ---------- Toast ---------- */
  var toastEl = document.getElementById('toast');
  var toastTimer = null;
  function toast(msg) {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove('show'); }, 1600);
  }

  /* ---------- 进入动画 ---------- */
  var app = document.getElementById('app');
  if (app) { app.classList.add('screen-in'); }

  refreshHero();
})();
