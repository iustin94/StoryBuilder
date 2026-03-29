/**
 * QuestForge API client — handles all HTTP requests to the plugin server.
 */
const QuestForgeAPI = (() => {
  let serverUrl = '';
  let connected = false;

  function init() {
    serverUrl = localStorage.getItem('qf_server_url') || 'http://localhost:7432';
    pollStatus();
    setInterval(pollStatus, 12000);
  }

  function getServerUrl() { return serverUrl; }

  function setServerUrl(url) {
    serverUrl = url.replace(/\/+$/, '');
    localStorage.setItem('qf_server_url', serverUrl);
  }

  function isConnected() { return connected; }

  async function request(method, path, body) {
    const url = serverUrl + path;
    const opts = {
      method,
      headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(url, opts);
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`HTTP ${res.status}: ${text}`);
    }
    return res.json();
  }

  async function pollStatus() {
    try {
      const data = await request('GET', '/questforge/status');
      if (!connected) {
        connected = true;
        updateStatusDot(true);
        OfflineQueue.flush();
      }
    } catch {
      if (connected) {
        connected = false;
        updateStatusDot(false);
      }
    }
  }

  function updateStatusDot(online) {
    const dot = document.getElementById('connection-status');
    if (dot) {
      dot.classList.toggle('online', online);
      dot.classList.toggle('offline', !online);
      dot.title = online ? 'Connected' : 'Disconnected';
    }
  }

  // Public API methods
  async function getStatus() { return request('GET', '/questforge/status'); }
  async function getNpcs() { return request('GET', '/questforge/npcs'); }
  async function getQuests() { return request('GET', '/questforge/quests'); }
  async function saveQuest(questData) { return request('POST', '/questforge/quest', questData); }
  async function deleteQuest(questId) { return request('DELETE', `/questforge/quest/${questId}`); }
  async function assign(questId, npcRole) { return request('POST', '/questforge/assign', { questId, npcRole }); }
  async function unassign(questId, npcRole) { return request('POST', '/questforge/unassign', { questId, npcRole }); }
  async function reload() { return request('POST', '/questforge/reload'); }

  return {
    init, getServerUrl, setServerUrl, isConnected,
    getStatus, getNpcs, getQuests, saveQuest, deleteQuest,
    assign, unassign, reload, pollStatus
  };
})();
