/**
 * Graph Editor — SVG-based quest state machine editor with pan/zoom.
 */
const GraphEditor = (() => {
  let quest = null;       // Current quest data
  let nodes = {};         // stateId -> { x, y, ...state }
  let selectedNodeId = null;
  let selectedEdge = null;

  // Canvas state
  let pan = { x: 0, y: 0 };
  let zoom = 1;
  let isPanning = false;
  let panStart = { x: 0, y: 0 };
  let dragNode = null;
  let dragOffset = { x: 0, y: 0 };

  // Edge drawing
  let drawingEdge = false;
  let edgeSourceId = null;
  let tempEdgeLine = null;

  const NODE_W = 180;
  const NODE_H = 80;

  function init() {
    const canvas = document.getElementById('quest-canvas');
    if (!canvas) return;

    canvas.addEventListener('mousedown', onCanvasMouseDown);
    canvas.addEventListener('mousemove', onCanvasMouseMove);
    canvas.addEventListener('mouseup', onCanvasMouseUp);
    canvas.addEventListener('wheel', onCanvasWheel, { passive: false });

    // Palette drag
    document.querySelectorAll('.palette-item').forEach(item => {
      item.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('node-type', item.dataset.nodeType);
      });
    });
    canvas.addEventListener('dragover', (e) => e.preventDefault());
    canvas.addEventListener('drop', onCanvasDrop);

    // Toolbar buttons
    document.getElementById('btn-zoom-in')?.addEventListener('click', () => setZoom(zoom * 1.2));
    document.getElementById('btn-zoom-out')?.addEventListener('click', () => setZoom(zoom / 1.2));
    document.getElementById('btn-zoom-fit')?.addEventListener('click', fitToView);
    document.getElementById('btn-save-quest')?.addEventListener('click', saveQuestToServer);
    document.getElementById('btn-export-json')?.addEventListener('click', exportJson);
    document.getElementById('btn-import-json')?.addEventListener('click', importJson);
    document.getElementById('btn-add-variable')?.addEventListener('click', addVariable);
  }

  document.addEventListener('DOMContentLoaded', init);

  // --- Quest Loading ---

  async function loadQuest(questId) {
    try {
      const allQuests = await QuestForgeAPI.getQuests();
      const meta = allQuests.find(q => q.id === questId);
      if (!meta) return;

      // Fetch the full quest data — we need to get it from localStorage or reconstruct
      const cached = localStorage.getItem(`qf_quest_${questId}`);
      if (cached) {
        quest = JSON.parse(cached);
      } else {
        // Build a minimal quest from what we have — user will need to edit
        quest = {
          questId: questId,
          questName: meta.name,
          startState: 'n1',
          variables: {},
          states: { n1: { type: 'start', title: 'Quest Begin', transitions: [] } }
        };
      }
    } catch {
      const cached = localStorage.getItem(`qf_quest_${questId}`);
      if (cached) quest = JSON.parse(cached);
    }

    if (quest) {
      buildNodesFromQuest();
      render();
      document.getElementById('editor-quest-name').textContent = quest.questName || quest.questId;
      renderVariableList();
    }
  }

  function loadQuestData(data) {
    quest = data;
    buildNodesFromQuest();
    render();
    document.getElementById('editor-quest-name').textContent = quest.questName || quest.questId;
    renderVariableList();
  }

  function buildNodesFromQuest() {
    nodes = {};
    const stateIds = Object.keys(quest.states);
    const cols = Math.ceil(Math.sqrt(stateIds.length));
    stateIds.forEach((id, i) => {
      const state = quest.states[id];
      // Check if we have saved positions
      const saved = localStorage.getItem(`qf_positions_${quest.questId}`);
      let positions = saved ? JSON.parse(saved) : {};
      nodes[id] = {
        ...state,
        id: id,
        x: positions[id]?.x ?? (i % cols) * 240 + 40,
        y: positions[id]?.y ?? Math.floor(i / cols) * 140 + 40
      };
    });
  }

  // --- Rendering ---

  function render() {
    const group = document.getElementById('canvas-group');
    if (!group) return;
    group.innerHTML = '';
    group.setAttribute('transform', `translate(${pan.x},${pan.y}) scale(${zoom})`);

    // Draw edges first (behind nodes)
    Object.entries(nodes).forEach(([id, node]) => {
      (node.transitions || []).forEach(t => {
        const target = nodes[t.to];
        if (!target) return;
        drawEdge(group, node, target, t, id);
      });
    });

    // Draw nodes
    Object.entries(nodes).forEach(([id, node]) => {
      drawNode(group, id, node);
    });

    document.getElementById('zoom-level').textContent = Math.round(zoom * 100) + '%';
  }

  function drawNode(parent, id, node) {
    const fo = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
    fo.setAttribute('x', node.x);
    fo.setAttribute('y', node.y);
    fo.setAttribute('width', NODE_W);
    fo.setAttribute('height', NODE_H);
    fo.dataset.nodeId = id;

    const typeLower = (node.type || 'start').toLowerCase();
    const selected = id === selectedNodeId ? ' selected' : '';
    const dialogueSnippet = node.dialogue ? node.dialogue.substring(0, 40) + (node.dialogue.length > 40 ? '...' : '') : '';

    fo.innerHTML = `
      <div xmlns="http://www.w3.org/1999/xhtml" class="graph-node${selected}" data-node-id="${id}">
        <div class="graph-node-header">
          <span class="graph-node-type type-${typeLower}">${typeLower}</span>
          <span class="graph-node-title">${node.title || id}</span>
        </div>
        <div class="graph-node-body">${dialogueSnippet}</div>
        <div class="port port-in" data-port="in" data-node-id="${id}"></div>
        <div class="port port-out" data-port="out" data-node-id="${id}"></div>
      </div>
    `;

    // Node events
    fo.addEventListener('mousedown', (e) => {
      if (e.target.classList.contains('port')) return;
      e.stopPropagation();
      selectNode(id);
      dragNode = id;
      const pt = canvasPoint(e);
      dragOffset = { x: pt.x - node.x, y: pt.y - node.y };
    });

    // Port events for edge drawing
    const ports = fo.querySelectorAll ? null : null; // handled via delegation below
    fo.addEventListener('mousedown', (e) => {
      if (e.target.dataset.port === 'out') {
        e.stopPropagation();
        drawingEdge = true;
        edgeSourceId = id;
      }
    });
    fo.addEventListener('mouseup', (e) => {
      if (drawingEdge && edgeSourceId && e.target.dataset.port === 'in') {
        const targetId = e.target.dataset.nodeId;
        if (targetId && targetId !== edgeSourceId) {
          addTransition(edgeSourceId, targetId);
        }
      }
    });

    parent.appendChild(fo);
  }

  function drawEdge(parent, source, target, transition, sourceId) {
    const sx = source.x + NODE_W;
    const sy = source.y + NODE_H / 2;
    const tx = target.x;
    const ty = target.y + NODE_H / 2;
    const cx = (sx + tx) / 2;

    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', `M${sx},${sy} C${cx},${sy} ${cx},${ty} ${tx},${ty}`);
    path.setAttribute('stroke', '#666');
    path.setAttribute('stroke-width', '2');
    path.setAttribute('fill', 'none');
    path.setAttribute('marker-end', 'url(#arrowhead)');
    path.style.cursor = 'pointer';

    path.addEventListener('click', (e) => {
      e.stopPropagation();
      selectedEdge = { sourceId, transition };
      selectedNodeId = null;
      showEdgeProperties(sourceId, transition);
    });

    parent.appendChild(path);

    // Edge label
    if (transition.label) {
      const lx = (sx + tx) / 2;
      const ly = (sy + ty) / 2 - 8;
      const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      text.setAttribute('x', lx);
      text.setAttribute('y', ly);
      text.setAttribute('text-anchor', 'middle');
      text.setAttribute('fill', '#8892a4');
      text.setAttribute('font-size', '11');
      text.textContent = transition.label;
      parent.appendChild(text);
    }
  }

  // --- Canvas Interaction ---

  function canvasPoint(e) {
    const svg = document.getElementById('quest-canvas');
    const rect = svg.getBoundingClientRect();
    return {
      x: (e.clientX - rect.left - pan.x) / zoom,
      y: (e.clientY - rect.top - pan.y) / zoom
    };
  }

  function onCanvasMouseDown(e) {
    if (e.target === document.getElementById('quest-canvas') || e.target.id === 'canvas-group') {
      isPanning = true;
      panStart = { x: e.clientX - pan.x, y: e.clientY - pan.y };
      selectedNodeId = null;
      selectedEdge = null;
      clearProperties();
    }
  }

  function onCanvasMouseMove(e) {
    if (isPanning) {
      pan.x = e.clientX - panStart.x;
      pan.y = e.clientY - panStart.y;
      render();
    }
    if (dragNode && nodes[dragNode]) {
      const pt = canvasPoint(e);
      nodes[dragNode].x = pt.x - dragOffset.x;
      nodes[dragNode].y = pt.y - dragOffset.y;
      render();
    }
  }

  function onCanvasMouseUp() {
    if (dragNode) {
      saveNodePositions();
    }
    isPanning = false;
    dragNode = null;
    drawingEdge = false;
    edgeSourceId = null;
  }

  function onCanvasWheel(e) {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setZoom(zoom * delta);
  }

  function onCanvasDrop(e) {
    e.preventDefault();
    const type = e.dataTransfer.getData('node-type');
    if (!type || !quest) return;

    const pt = canvasPoint(e);
    const id = 'n' + (Object.keys(quest.states).length + 1);
    const state = {
      type: type,
      title: type.charAt(0).toUpperCase() + type.slice(1) + ' Node',
      transitions: []
    };
    if (type === 'dialogue') state.dialogue = '';
    if (type === 'objective') { state.objectiveId = ''; state.onObjectiveComplete = ''; }

    quest.states[id] = state;
    nodes[id] = { ...state, id, x: pt.x, y: pt.y };
    render();
    selectNode(id);
  }

  function setZoom(z) {
    zoom = Math.max(0.1, Math.min(3, z));
    render();
  }

  function fitToView() {
    const ids = Object.keys(nodes);
    if (ids.length === 0) return;
    const svg = document.getElementById('quest-canvas');
    const rect = svg.getBoundingClientRect();
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    ids.forEach(id => {
      minX = Math.min(minX, nodes[id].x);
      minY = Math.min(minY, nodes[id].y);
      maxX = Math.max(maxX, nodes[id].x + NODE_W);
      maxY = Math.max(maxY, nodes[id].y + NODE_H);
    });
    const w = maxX - minX + 80;
    const h = maxY - minY + 80;
    zoom = Math.min(rect.width / w, rect.height / h, 2);
    pan.x = (rect.width - w * zoom) / 2 - minX * zoom + 40;
    pan.y = (rect.height - h * zoom) / 2 - minY * zoom + 40;
    render();
  }

  // --- Selection & Properties ---

  function selectNode(id) {
    selectedNodeId = id;
    selectedEdge = null;
    render();
    showNodeProperties(id);
  }

  function showNodeProperties(id) {
    const node = nodes[id];
    const panel = document.getElementById('props-content');
    if (!panel || !node) return;

    const typeLower = (node.type || 'start').toLowerCase();
    let html = `
      <label>State ID</label>
      <input type="text" value="${id}" disabled />
      <label>Type</label>
      <select id="prop-type">
        ${['start','dialogue','objective','branch','end'].map(t =>
          `<option value="${t}" ${t === typeLower ? 'selected' : ''}>${t}</option>`
        ).join('')}
      </select>
      <label>Title</label>
      <input type="text" id="prop-title" value="${node.title || ''}" />
    `;

    if (typeLower === 'dialogue' || typeLower === 'start') {
      html += `<label>Dialogue</label><textarea id="prop-dialogue">${node.dialogue || ''}</textarea>`;
    }
    if (typeLower === 'objective') {
      html += `
        <label>Objective ID</label>
        <input type="text" id="prop-objective-id" value="${node.objectiveId || ''}" />
        <label>On Complete → State</label>
        <select id="prop-on-complete">
          <option value="">— none —</option>
          ${Object.keys(nodes).filter(i => i !== id).map(i =>
            `<option value="${i}" ${node.onObjectiveComplete === i ? 'selected' : ''}>${i} (${nodes[i].title || ''})</option>`
          ).join('')}
        </select>
      `;
    }

    html += `
      <label style="margin-top:1rem">Transitions (${(node.transitions || []).length})</label>
      <div id="prop-transitions"></div>
      <button class="btn btn-sm" id="btn-delete-node" style="margin-top:1rem;width:100%">Delete Node</button>
    `;

    panel.innerHTML = html;

    // Bind events
    panel.querySelector('#prop-type')?.addEventListener('change', (e) => {
      node.type = e.target.value;
      quest.states[id].type = e.target.value;
      render();
      showNodeProperties(id);
    });
    panel.querySelector('#prop-title')?.addEventListener('input', (e) => {
      node.title = e.target.value;
      quest.states[id].title = e.target.value;
      render();
    });
    panel.querySelector('#prop-dialogue')?.addEventListener('input', (e) => {
      node.dialogue = e.target.value;
      quest.states[id].dialogue = e.target.value;
    });
    panel.querySelector('#prop-objective-id')?.addEventListener('input', (e) => {
      node.objectiveId = e.target.value;
      quest.states[id].objectiveId = e.target.value;
    });
    panel.querySelector('#prop-on-complete')?.addEventListener('change', (e) => {
      node.onObjectiveComplete = e.target.value;
      quest.states[id].onObjectiveComplete = e.target.value;
      render();
    });
    panel.querySelector('#btn-delete-node')?.addEventListener('click', () => {
      delete nodes[id];
      delete quest.states[id];
      // Remove transitions pointing to this node
      Object.values(nodes).forEach(n => {
        n.transitions = (n.transitions || []).filter(t => t.to !== id);
      });
      Object.values(quest.states).forEach(s => {
        s.transitions = (s.transitions || []).filter(t => t.to !== id);
      });
      selectedNodeId = null;
      clearProperties();
      render();
    });

    // Render transitions
    renderTransitionList(id);
  }

  function renderTransitionList(nodeId) {
    const container = document.getElementById('prop-transitions');
    if (!container) return;
    const node = nodes[nodeId];
    const transitions = node.transitions || [];

    container.innerHTML = transitions.map((t, i) => `
      <div style="padding:0.3rem 0;border-bottom:1px solid var(--border);font-size:0.8rem;">
        <strong>${t.label || '(no label)'}</strong> → ${t.to}
        <span style="cursor:pointer;color:var(--danger);float:right" data-remove-idx="${i}">&times;</span>
      </div>
    `).join('') || '<div class="hint">No transitions</div>';

    container.querySelectorAll('[data-remove-idx]').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.dataset.removeIdx);
        node.transitions.splice(idx, 1);
        quest.states[nodeId].transitions.splice(idx, 1);
        render();
        showNodeProperties(nodeId);
      });
    });
  }

  function showEdgeProperties(sourceId, transition) {
    const panel = document.getElementById('props-content');
    if (!panel) return;

    panel.innerHTML = `
      <label>From</label>
      <input type="text" value="${sourceId}" disabled />
      <label>To</label>
      <input type="text" value="${transition.to}" disabled />
      <label>Label</label>
      <input type="text" id="prop-edge-label" value="${transition.label || ''}" />
      <label>Conditions</label>
      <div id="prop-conditions"></div>
      <button class="btn btn-sm" id="btn-add-condition" style="margin-top:0.5rem">+ Condition</button>
      <button class="btn btn-sm btn-danger" id="btn-delete-edge" style="margin-top:0.5rem;width:100%">Delete Edge</button>
    `;

    panel.querySelector('#prop-edge-label')?.addEventListener('input', (e) => {
      transition.label = e.target.value;
      render();
    });

    panel.querySelector('#btn-add-condition')?.addEventListener('click', () => {
      if (!transition.conditions) transition.conditions = [];
      transition.conditions.push({ var: '', op: '==', value: '' });
      renderConditions(transition);
    });

    panel.querySelector('#btn-delete-edge')?.addEventListener('click', () => {
      const node = nodes[sourceId];
      const idx = node.transitions.indexOf(transition);
      if (idx >= 0) {
        node.transitions.splice(idx, 1);
        quest.states[sourceId].transitions.splice(idx, 1);
      }
      selectedEdge = null;
      clearProperties();
      render();
    });

    renderConditions(transition);
  }

  function renderConditions(transition) {
    const container = document.getElementById('prop-conditions');
    if (!container) return;
    const conditions = transition.conditions || [];

    container.innerHTML = conditions.map((c, i) => `
      <div style="display:flex;gap:0.2rem;margin-bottom:0.3rem;font-size:0.8rem;">
        <input type="text" value="${c.var}" placeholder="var" style="width:35%" data-cond="${i}" data-field="var" />
        <select data-cond="${i}" data-field="op" style="width:25%">
          ${['==','!=','>','>=','<','<='].map(op =>
            `<option ${c.op === op ? 'selected' : ''}>${op}</option>`
          ).join('')}
        </select>
        <input type="text" value="${c.value}" placeholder="val" style="width:30%" data-cond="${i}" data-field="value" />
        <span style="cursor:pointer;color:var(--danger)" data-remove-cond="${i}">&times;</span>
      </div>
    `).join('') || '<div class="hint">No conditions</div>';

    container.querySelectorAll('[data-field]').forEach(input => {
      input.addEventListener('input', (e) => {
        const idx = parseInt(e.target.dataset.cond);
        const field = e.target.dataset.field;
        conditions[idx][field] = e.target.value;
      });
      input.addEventListener('change', (e) => {
        const idx = parseInt(e.target.dataset.cond);
        const field = e.target.dataset.field;
        conditions[idx][field] = e.target.value;
      });
    });

    container.querySelectorAll('[data-remove-cond]').forEach(btn => {
      btn.addEventListener('click', () => {
        conditions.splice(parseInt(btn.dataset.removeCond), 1);
        renderConditions(transition);
      });
    });
  }

  function clearProperties() {
    const panel = document.getElementById('props-content');
    if (panel) panel.innerHTML = '<p class="hint">Select a node or edge to edit its properties.</p>';
  }

  // --- Transitions ---

  function addTransition(sourceId, targetId) {
    const node = nodes[sourceId];
    if (!node.transitions) node.transitions = [];
    const t = { label: '', to: targetId, conditions: [] };
    node.transitions.push(t);
    if (!quest.states[sourceId].transitions) quest.states[sourceId].transitions = [];
    quest.states[sourceId].transitions.push(t);
    render();
  }

  // --- Variables ---

  function renderVariableList() {
    const container = document.getElementById('variable-list');
    if (!container || !quest) return;
    const vars = quest.variables || {};
    container.innerHTML = Object.entries(vars).map(([name, def]) =>
      `<div class="variable-item">${name} <span style="color:var(--text-muted)">(${def.type})</span></div>`
    ).join('') || '<div class="hint">No variables</div>';
  }

  function addVariable() {
    if (!quest) return;
    const name = prompt('Variable name:');
    if (!name) return;
    const type = prompt('Type (boolean/integer/string):', 'integer');
    if (!type) return;
    const initial = type === 'boolean' ? false : type === 'integer' ? 0 : '';
    if (!quest.variables) quest.variables = {};
    quest.variables[name] = { type, initial };
    renderVariableList();
  }

  // --- Save/Export ---

  function saveNodePositions() {
    if (!quest) return;
    const positions = {};
    Object.entries(nodes).forEach(([id, n]) => {
      positions[id] = { x: n.x, y: n.y };
    });
    localStorage.setItem(`qf_positions_${quest.questId}`, JSON.stringify(positions));
  }

  async function saveQuestToServer() {
    if (!quest) return;
    syncNodesToQuest();
    localStorage.setItem(`qf_quest_${quest.questId}`, JSON.stringify(quest));
    await OfflineQueue.withOfflineSupport(
      () => QuestForgeAPI.saveQuest(quest),
      { type: 'quest', payload: quest }
    );
    saveNodePositions();
    alert('Quest saved!');
  }

  function syncNodesToQuest() {
    // Sync node data back to quest.states
    Object.entries(nodes).forEach(([id, node]) => {
      if (quest.states[id]) {
        quest.states[id].title = node.title;
        quest.states[id].type = node.type;
        quest.states[id].dialogue = node.dialogue;
        quest.states[id].objectiveId = node.objectiveId;
        quest.states[id].onObjectiveComplete = node.onObjectiveComplete;
        quest.states[id].transitions = node.transitions;
      }
    });
  }

  function exportJson() {
    if (!quest) return;
    syncNodesToQuest();
    const blob = new Blob([JSON.stringify(quest, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${quest.questId}.json`;
    a.click();
  }

  function importJson() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.addEventListener('change', async () => {
      const file = input.files[0];
      if (!file) return;
      const text = await file.text();
      try {
        const data = JSON.parse(text);
        if (!data.questId || !data.states) {
          alert('Invalid quest JSON');
          return;
        }
        loadQuestData(data);
        localStorage.setItem(`qf_quest_${data.questId}`, JSON.stringify(data));
      } catch (e) {
        alert('Failed to parse JSON: ' + e.message);
      }
    });
    input.click();
  }

  return { loadQuest, loadQuestData };
})();
