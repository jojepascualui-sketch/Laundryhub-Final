/* Laundry HUB design prototype runtime.
   Renders the design-component files (Main, Rider, Admin, ...) as plain, clickable HTML.
   Sources are registered in assets/sources.js as window.DC_SOURCES[name] = "<file text>". */
(function () {
  'use strict';

  class DCLogic {
    constructor(props) { this.props = props || {}; this.state = {}; }
    setState(patch) {
      const p = typeof patch === 'function' ? patch(this.state, this.props) : patch;
      this.state = Object.assign({}, this.state, p);
      if (this.__rerender) this.__rerender();
    }
    forceUpdate() { if (this.__rerender) this.__rerender(); }
  }
  window.DCLogic = DCLogic;

  const parsed = {};
  const headDone = {};

  function parseSource(name) {
    if (parsed[name]) return parsed[name];
    let src = window.DC_SOURCES[name];
    if (!src) throw new Error('Missing design source: ' + name);
    // Control-flow tags become <template> so the HTML parser keeps them inside tables.
    src = src.replace(/<sc-(for|if)\b/g, '<template data-sc="$1"').replace(/<\/sc-(for|if)>/g, '</template>');
    const doc = new DOMParser().parseFromString(src, 'text/html');
    const xdc = doc.querySelector('x-dc');
    const helmet = xdc.querySelector('helmet');
    const helmetNodes = helmet ? Array.from(helmet.children) : [];
    if (helmet) helmet.remove();
    const scriptEl = doc.querySelector('script[data-dc-script]');
    const code = scriptEl ? scriptEl.textContent : 'class Component extends DCLogic { renderVals(){ return {}; } }';
    const Comp = new Function('DCLogic', code + '\n;return Component;')(DCLogic);
    parsed[name] = { xdc, helmetNodes, Comp };
    return parsed[name];
  }

  function applyHead(name, helmetNodes) {
    if (headDone[name]) return;
    headDone[name] = true;
    helmetNodes.forEach((n) => {
      if (n.tagName === 'LINK' && /fonts\.googleapis|fonts\.gstatic/.test(n.getAttribute('href') || '')) return; // fonts are local
      document.head.appendChild(document.importNode(n, true));
    });
  }

  const HOLE = /\{\{\s*([^}]+?)\s*\}\}/g;
  const WHOLE = /^\s*\{\{\s*([^}]+?)\s*\}\}\s*$/;

  function lookup(path, scope) {
    path = path.trim();
    if (path === 'true') return true;
    if (path === 'false') return false;
    if (path === 'null') return null;
    if (/^-?\d+(\.\d+)?$/.test(path)) return Number(path);
    if (/^'.*'$|^".*"$/.test(path)) return path.slice(1, -1);
    const parts = path.split('.');
    let cur;
    for (let i = scope.length - 1; i >= 0; i--) {
      if (scope[i] && Object.prototype.hasOwnProperty.call(scope[i], parts[0])) { cur = scope[i][parts[0]]; break; }
    }
    for (let j = 1; j < parts.length && cur != null; j++) cur = cur[parts[j]];
    return cur;
  }
  function interp(str, scope) {
    return str.replace(HOLE, (_, p) => { const v = lookup(p, scope); return v == null ? '' : String(v); });
  }
  function camel(s) { return s.replace(/-([a-z])/g, (_, c) => c.toUpperCase()); }
  const EVENT_MAP = { onclick: 'click', onchange: 'change', oninput: 'input', onsubmit: 'submit', onkeydown: 'keydown', onfocus: 'focus', onblur: 'blur' };

  function linkFor(href) {
    if (!href) return href;
    const m = href.match(/([A-Za-z0-9_.-]+\.dc\.html)$/);
    if (m && window.DC_LINKS && window.DC_LINKS[m[1]]) return window.DC_LINKS[m[1]];
    return href;
  }

  function processChildren(srcParent, outParent, scope, inst) {
    Array.from(srcParent.childNodes).forEach((n) => processNode(n, outParent, scope, inst));
  }

  function processNode(node, out, scope, inst) {
    if (node.nodeType === 3) {
      out.appendChild(document.createTextNode(node.nodeValue.indexOf('{{') >= 0 ? interp(node.nodeValue, scope) : node.nodeValue));
      return;
    }
    if (node.nodeType !== 1) return;
    const tag = node.tagName.toLowerCase();

    if (tag === 'template' && node.dataset.sc) {
      const kind = node.dataset.sc;
      const content = node.content;
      if (kind === 'if') {
        const raw = node.getAttribute('value') || '';
        const m = raw.match(WHOLE);
        const v = m ? lookup(m[1], scope) : raw;
        if (v) processChildren(content, out, scope, inst);
      } else {
        const m = (node.getAttribute('list') || '').match(WHOLE);
        const list = m ? lookup(m[1], scope) : [];
        const as = node.getAttribute('as') || 'item';
        (list || []).forEach((item, idx) => {
          const local = {}; local[as] = item; local.$index = idx;
          processChildren(content, out, scope.concat([local]), inst);
        });
      }
      return;
    }

    if (tag === 'dc-import') {
      const props = {};
      let compName = null;
      Array.from(node.attributes).forEach((a) => {
        if (a.name === 'name') { compName = a.value; return; }
        if (a.name.indexOf('hint-') === 0) return;
        const m = a.value.match(WHOLE);
        props[camel(a.name)] = m ? lookup(m[1], scope) : interp(a.value, scope);
      });
      const host = document.createElement('div');
      host.style.display = 'contents';
      out.appendChild(host);
      inst.__childIdx = (inst.__childIdx || 0) + 1;
      const key = compName + '#' + inst.__childIdx;
      inst.__children = inst.__children || {};
      if (!inst.__children[key]) inst.__children[key] = createInstance(compName, props);
      mount(inst.__children[key], host);
      return;
    }

    const el = document.createElementNS(node.namespaceURI, node.localName);
    Array.from(node.attributes).forEach((a) => {
      const name = a.name;
      const val = a.value;
      const m = val.match(WHOLE);
      if (EVENT_MAP[name]) {
        const fn = m ? lookup(m[1], scope) : null;
        if (typeof fn === 'function') el.addEventListener(EVENT_MAP[name], (e) => fn(e));
        return;
      }
      if (m && !(name === 'style' || name === 'class')) {
        const v = lookup(m[1], scope);
        if (v === false || v == null) return;
        if (v === true) { el.setAttribute(name, ''); return; }
        el.setAttribute(name, String(v));
        return;
      }
      let finalVal = val.indexOf('{{') >= 0 ? interp(val, scope) : val;
      if (name === 'href') finalVal = linkFor(finalVal);
      try { el.setAttribute(name, finalVal); } catch (e) { /* ignore invalid attribute names */ }
    });
    if (tag === 'template') { processChildren(node.content, el.content, scope, inst); }
    else processChildren(node, el, scope, inst);
    out.appendChild(el);
  }

  function createInstance(name, props) {
    const p = parseSource(name);
    applyHead(name, p.helmetNodes);
    const inst = new p.Comp(props || {});
    inst.__name = name;
    return inst;
  }

  function mount(inst, host) {
    const p = parsed[inst.__name];
    inst.__rerender = () => {
      inst.__childIdx = 0;
      const vals = (inst.renderVals && inst.renderVals()) || {};
      const frag = document.createDocumentFragment();
      processChildren(p.xdc, frag, [vals], inst);
      host.replaceChildren(frag);
    };
    inst.__rerender();
    if (!inst.__mounted && typeof inst.componentDidMount === 'function') { inst.__mounted = true; inst.componentDidMount(); }
  }

  window.DCRender = function (name, hostEl, props) {
    const inst = createInstance(name, props || {});
    mount(inst, hostEl);
  };
})();
