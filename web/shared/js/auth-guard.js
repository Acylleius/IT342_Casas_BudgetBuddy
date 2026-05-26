import { api, ensureSession, subscribeRealtime } from './api.js';

if (!await ensureSession()) {
  window.location.href = '../auth/login.html';
} else {
  updateInboxBadge();
  subscribeRealtime(eventName => {
    if (eventName === 'inbox-updated' || eventName === 'group-invitation') {
      updateInboxBadge();
    }
  });
}

async function updateInboxBadge() {
  const inboxLink = [...document.querySelectorAll('a')].find(link => link.textContent.trim().startsWith('Inbox'));
  if (!inboxLink) return;
  const inbox = await api('/inbox');
  const unread = inbox.filter(item => !(item.isRead ?? item.read)).length;
  inboxLink.innerHTML = unread > 0
    ? `Inbox <span class="nav-unread">${unread}</span>`
    : 'Inbox';
}
