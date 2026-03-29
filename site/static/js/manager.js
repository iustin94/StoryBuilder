/**
 * Manager screen — NPC-to-quest assignment with drag and drop.
 */
const Manager = (() => {
  let npcs = [];
  let quests = [];

  async function load() {
    await Promise.all([loadNpcs(), loadQuests()]);
  }

  async function loadNpcs() {
    const list = document.getElementById('npc-list');
    if (!list) return;
    try {
      npcs = await QuestForgeAPI.getNpcs();
      renderNpcs(list);
    } catch {
      // Use cached NPCs from localStorage if offline
      const cached = localStorage.getItem('qf_npcs_cache');
      if (cached) {
        npcs = JSON.parse(cached);
        renderNpcs(list);
      } else {
        list.innerHTML = '<div class="loading">Cannot load NPCs — server offline</div>';
      }
    }
  }

  function renderNpcs(container) {
    localStorage.setItem('qf_npcs_cache', JSON.stringify(npcs));
    container.innerHTML = '';
    if (npcs.length === 0) {
      container.innerHTML = '<div class="loading">No NPC roles found</div>';
      return;
    }
    npcs.forEach(npc => {
      const chip = document.createElement('div');
      chip.className = 'npc-chip';
      chip.textContent = npc.role;
      chip.draggable = true;
      chip.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('text/plain', npc.role);
        chip.classList.add('dragging');
      });
      chip.addEventListener('dragend', () => chip.classList.remove('dragging'));
      container.appendChild(chip);
    });
  }

  async function loadQuests() {
    const list = document.getElementById('quest-list');
    if (!list) return;
    try {
      quests = await QuestForgeAPI.getQuests();
      renderQuests(list);
    } catch {
      const cached = localStorage.getItem('qf_quests_cache');
      if (cached) {
        quests = JSON.parse(cached);
        renderQuests(list);
      } else {
        list.innerHTML = '<div class="loading">Cannot load quests — server offline</div>';
      }
    }
  }

  function renderQuests(container) {
    localStorage.setItem('qf_quests_cache', JSON.stringify(quests));
    container.innerHTML = '';
    if (quests.length === 0) {
      container.innerHTML = '<div class="loading">No quests yet. Click "+ New Quest" to create one.</div>';
      return;
    }
    quests.forEach(quest => {
      container.appendChild(createQuestCard(quest));
    });
  }

  function createQuestCard(quest) {
    const card = document.createElement('div');
    card.className = 'quest-card';
    card.dataset.questId = quest.id;

    // Drop zone for NPC assignment
    card.addEventListener('dragover', (e) => { e.preventDefault(); card.classList.add('drag-over'); });
    card.addEventListener('dragleave', () => card.classList.remove('drag-over'));
    card.addEventListener('drop', async (e) => {
      e.preventDefault();
      card.classList.remove('drag-over');
      const npcRole = e.dataTransfer.getData('text/plain');
      if (npcRole && !quest.npcRoles.includes(npcRole)) {
        await OfflineQueue.withOfflineSupport(
          () => QuestForgeAPI.assign(quest.id, npcRole),
          { type: 'assign', payload: { questId: quest.id, npcRole } }
        );
        quest.npcRoles.push(npcRole);
        renderQuests(document.getElementById('quest-list'));
      }
    });

    const npcChips = (quest.npcRoles || []).map(role =>
      `<span class="assigned-npc">${role}<span class="remove-npc" data-role="${role}">&times;</span></span>`
    ).join('');

    card.innerHTML = `
      <div class="quest-card-header">
        <span class="quest-card-title">${quest.name || quest.id}</span>
      </div>
      <div class="quest-card-id">${quest.id}</div>
      <div class="quest-card-npcs">${npcChips || '<span class="hint">No NPCs assigned</span>'}</div>
      <div class="quest-card-actions">
        <button class="btn btn-sm btn-edit">Edit</button>
        <button class="btn btn-sm btn-danger btn-delete">Delete</button>
      </div>
    `;

    // Remove NPC assignment
    card.querySelectorAll('.remove-npc').forEach(btn => {
      btn.addEventListener('click', async () => {
        const role = btn.dataset.role;
        await OfflineQueue.withOfflineSupport(
          () => QuestForgeAPI.unassign(quest.id, role),
          { type: 'unassign', payload: { questId: quest.id, npcRole: role } }
        );
        quest.npcRoles = quest.npcRoles.filter(r => r !== role);
        renderQuests(document.getElementById('quest-list'));
      });
    });

    // Edit quest
    card.querySelector('.btn-edit').addEventListener('click', () => {
      GraphEditor.loadQuest(quest.id);
      Settings.activateScreen('editor');
      document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
      document.querySelector('[data-screen="editor"]')?.classList.add('active');
    });

    // Delete quest
    card.querySelector('.btn-delete').addEventListener('click', async () => {
      if (!confirm(`Delete quest "${quest.name || quest.id}"?`)) return;
      await OfflineQueue.withOfflineSupport(
        () => QuestForgeAPI.deleteQuest(quest.id),
        { type: 'delete', payload: { questId: quest.id } }
      );
      loadQuests();
    });

    return card;
  }

  // New quest button
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btn-new-quest');
    if (btn) {
      btn.addEventListener('click', () => showNewQuestModal());
    }
  });

  function showNewQuestModal() {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <h3>New Quest</h3>
        <div class="form-field">
          <label>Quest ID (snake_case)</label>
          <input type="text" id="new-quest-id" placeholder="lost_blade" />
        </div>
        <div class="form-field">
          <label>Quest Name</label>
          <input type="text" id="new-quest-name" placeholder="The Lost Blade" />
        </div>
        <div class="modal-actions">
          <button class="btn" id="modal-cancel">Cancel</button>
          <button class="btn btn-primary" id="modal-create">Create</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    overlay.querySelector('#modal-cancel').addEventListener('click', () => overlay.remove());
    overlay.querySelector('#modal-create').addEventListener('click', async () => {
      const id = overlay.querySelector('#new-quest-id').value.trim();
      const name = overlay.querySelector('#new-quest-name').value.trim();
      if (!id) return;

      const questData = {
        questId: id,
        questName: name || id,
        startState: 'n1',
        variables: {},
        states: {
          n1: { type: 'start', title: 'Quest Begin', transitions: [] }
        }
      };

      await OfflineQueue.withOfflineSupport(
        () => QuestForgeAPI.saveQuest(questData),
        { type: 'quest', payload: questData }
      );

      overlay.remove();
      loadQuests();
    });
  }

  return { load };
})();
