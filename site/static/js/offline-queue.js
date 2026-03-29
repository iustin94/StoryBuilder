/**
 * Offline queue — stores failed operations in localStorage and replays them when the server is reachable.
 */
const OfflineQueue = (() => {
  const STORAGE_KEY = 'qf_offline_queue';

  function init() {
    updateBadge();
  }

  function getQueue() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
    } catch { return []; }
  }

  function saveQueue(queue) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    updateBadge();
  }

  function enqueue(operation) {
    const queue = getQueue();
    queue.push({ ...operation, timestamp: Date.now() });
    saveQueue(queue);
  }

  async function flush() {
    const queue = getQueue();
    if (queue.length === 0) return;

    const remaining = [];
    for (const op of queue) {
      try {
        await replayOperation(op);
      } catch {
        remaining.push(op);
      }
    }
    saveQueue(remaining);
  }

  async function replayOperation(op) {
    switch (op.type) {
      case 'quest':
        await QuestForgeAPI.saveQuest(op.payload);
        break;
      case 'assign':
        await QuestForgeAPI.assign(op.payload.questId, op.payload.npcRole);
        break;
      case 'unassign':
        await QuestForgeAPI.unassign(op.payload.questId, op.payload.npcRole);
        break;
      case 'delete':
        await QuestForgeAPI.deleteQuest(op.payload.questId);
        break;
      default:
        console.warn('Unknown offline operation type:', op.type);
    }
  }

  function clear() {
    saveQueue([]);
  }

  function count() {
    return getQueue().length;
  }

  function updateBadge() {
    const badge = document.getElementById('sync-badge');
    if (!badge) return;
    const c = count();
    badge.textContent = c;
    badge.classList.toggle('hidden', c === 0);
  }

  /**
   * Wraps an API call: if it fails due to network error, enqueues the operation.
   */
  async function withOfflineSupport(apiCall, offlineOp) {
    try {
      return await apiCall();
    } catch (e) {
      if (!QuestForgeAPI.isConnected()) {
        enqueue(offlineOp);
        return null;
      }
      throw e;
    }
  }

  return { init, enqueue, flush, clear, count, updateBadge, withOfflineSupport };
})();
