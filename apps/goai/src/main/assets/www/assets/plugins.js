/* GoAI plugins - extra skills that can be toggled on/off */
(function () {
  const KEY = 'goai_plugins';
  let enabled = [];
  try { enabled = JSON.parse(localStorage.getItem(KEY) || '[]'); } catch (e) { enabled = []; }

  const PICK = arr => arr[Math.floor(Math.random() * arr.length)];

  const plugins = [
    {
      id: 'jokes',
      name: 'Joke Machine',
      icon: '\u{1F600}',
      desc: 'Tells random jokes on demand',
      match: /\b(?:tell me a joke|make me laugh|joke)\b/,
      run() {
        const J = window.GO_DATA && window.GO_DATA.JOKES;
        return J && J.length ? '**Joke Machine plugin**\n\n' + PICK(J) : 'No jokes loaded.';
      }
    },
    {
      id: 'facts',
      name: 'Fact Bot',
      icon: '\u{1F4A1}',
      desc: 'Serves up amazing random facts',
      match: /\b(?:random fact|interesting fact|did you know|tell me a fact)\b/,
      run() {
        const F = window.GO_DATA && window.GO_DATA.FACTS;
        return F && F.length ? '**Fact Bot plugin**\n\nDid you know? ' + PICK(F) : 'No facts loaded.';
      }
    },
    {
      id: 'quotes',
      name: 'Quote Genie',
      icon: '\u{1F4AC}',
      desc: 'Shares wise quotes for any mood',
      match: /\b(?:a quote|inspire me|quote for me|motivate me)\b/,
      run() {
        const Q = window.GO_DATA && window.GO_DATA.QUOTES;
        return Q && Q.length ? '**Quote Genie plugin**\n\n"' + PICK(Q) + '"' : 'No quotes loaded.';
      }
    },
    {
      id: 'coin',
      name: 'Coin Flipper',
      icon: '\u{1FA99}',
      desc: 'Flips a coin when you ask',
      match: /\bflip (?:a |the )?coin\b/,
      run() {
        const r = Math.random() < 0.5 ? 'HEADS' : 'TAILS';
        return '**Coin Flipper plugin**\n\nThe coin landed on... **' + r + '**!';
      }
    }
  ];

  function list() {
    return plugins.map(p => ({ id: p.id, name: p.name, icon: p.icon, desc: p.desc, enabled: enabled.indexOf(p.id) >= 0 }));
  }
  function isOn(id) { return enabled.indexOf(id) >= 0; }
  function toggle(id) {
    if (isOn(id)) enabled = enabled.filter(x => x !== id);
    else enabled.push(id);
    try { localStorage.setItem(KEY, JSON.stringify(enabled)); } catch (e) {}
    return isOn(id);
  }
  // find a matching enabled plugin; returns {plugin, text}
  function find(text) {
    const t = String(text).toLowerCase();
    for (const p of plugins) {
      if (isOn(p.id) && p.match && p.match.test(t)) return p;
    }
    return null;
  }

  window.GoPlugins = { list: list, toggle: toggle, isOn: isOn, find: find, _raw: plugins };
})();
