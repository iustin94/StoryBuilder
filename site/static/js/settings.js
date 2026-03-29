/**
 * Settings screen — server address config, test connection, sync controls.
 */
const Settings = (() => {
  function init() {
    const urlInput = document.getElementById('server-url');
    const testBtn = document.getElementById('btn-test-connection');
    const syncBtn = document.getElementById('btn-sync-now');
    const clearBtn = document.getElementById('btn-clear-queue');

    if (urlInput) {
      urlInput.value = QuestForgeAPI.getServerUrl();
      urlInput.addEventListener('change', () => {
        QuestForgeAPI.setServerUrl(urlInput.value);
      });
    }

    if (testBtn) {
      testBtn.addEventListener('click', testConnection);
    }
    if (syncBtn) {
      syncBtn.addEventListener('click', async () => {
        await OfflineQueue.flush();
        updatePendingCount();
      });
    }
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        if (confirm('Clear all pending offline operations?')) {
          OfflineQueue.clear();
          updatePendingCount();
        }
      });
    }

    // Nav link routing
    document.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const screen = link.dataset.screen;
        activateScreen(screen);
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
      });
    });

    // Activate initial screen
    activateScreen('manager');
    document.querySelector('[data-screen="manager"]')?.classList.add('active');

    updatePendingCount();
  }

  function activateScreen(name) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    const screen = document.getElementById(`${name}-screen`);
    if (screen) screen.classList.add('active');

    if (name === 'manager') Manager.load();
    if (name === 'settings') updatePendingCount();
  }

  async function testConnection() {
    const result = document.getElementById('connection-result');
    if (!result) return;
    result.textContent = 'Testing...';
    result.className = 'connection-result';
    try {
      const data = await QuestForgeAPI.getStatus();
      result.textContent = `Connected — v${data.version}, ${data.questsLoaded} quests loaded`;
      result.className = 'connection-result success';
    } catch (e) {
      result.textContent = `Failed: ${e.message}`;
      result.className = 'connection-result error';
    }
  }

  function updatePendingCount() {
    const el = document.getElementById('pending-count');
    if (el) el.textContent = OfflineQueue.count();
  }

  return { init, activateScreen };
})();
