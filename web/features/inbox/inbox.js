import { api, subscribeRealtime } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';

let inboxItems = [];

async function loadInbox() {
  inboxItems = await api('/inbox');
  renderInbox();
}

function renderInbox() {
  const filter = document.getElementById('filter').value;
  const unread = inboxItems.filter(item => !isRead(item)).length;
  document.getElementById('unreadCount').textContent = `${unread} unread`;
  const visible = inboxItems.filter(item => {
    if (filter === 'UNREAD') return !isRead(item);
    if (filter === 'INVITATIONS') return item.type === 'GROUP_INVITE';
    if (filter === 'GROUP') return item.type.startsWith('GROUP_') && item.type !== 'GROUP_INVITE';
    return true;
  });

  document.getElementById('inboxList').innerHTML = visible.length ? visible.map(item => `
    <div class="list-row ${isRead(item) ? 'read' : 'unread'}">
      ${isRead(item) ? '' : '<span class="unread-dot"></span>'}
      <div>
        <strong>${item.title}</strong><br>
        <span>${item.message}</span><br>
        <span>${new Date(item.createdAt).toLocaleString()}</span>
      </div>
      <div class="nav-links">
        ${item.type === 'GROUP_INVITE' && item.invitationStatus === 'PENDING' ? `
          <button class="btn-primary" data-accept="${item.invitationId}">Accept</button>
          <button class="btn-danger" data-decline="${item.invitationId}">Decline</button>
        ` : ''}
        ${item.type === 'GROUP_INVITE' && item.invitationStatus !== 'PENDING' ? `<span class="badge badge-member">${capitalize(item.invitationStatus)}</span>` : ''}
        ${isRead(item) ? '<span class="badge badge-member">Read</span>' : `<button class="btn-secondary" data-read="${item.id}">Read</button>`}
      </div>
    </div>
  `).join('') : '<div class="empty-state">No inbox messages match this filter.</div>';
}

function isRead(item) {
  return item.isRead ?? item.read;
}

function capitalize(value) {
  return value ? value.charAt(0) + value.slice(1).toLowerCase() : '';
}

document.getElementById('filter').addEventListener('change', renderInbox);

document.getElementById('readAllBtn').addEventListener('click', async () => {
  await api('/inbox/read-all', { method: 'POST' });
  showToast('Inbox marked as read');
  loadInbox();
});

document.getElementById('inboxList').addEventListener('click', async event => {
  const accept = event.target.closest('[data-accept]');
  const decline = event.target.closest('[data-decline]');
  const read = event.target.closest('[data-read]');
  if (accept) {
    await api(`/invitations/${accept.dataset.accept}/accept`, { method: 'POST' });
    showToast('Invitation accepted');
    loadInbox();
  }
  if (decline) {
    await api(`/invitations/${decline.dataset.decline}/decline`, { method: 'POST' });
    showToast('Invitation declined');
    loadInbox();
  }
  if (read) {
    await api(`/inbox/${read.dataset.read}/read`, { method: 'POST' });
    loadInbox();
  }
});

loadInbox();
subscribeRealtime(eventName => {
  if (eventName === 'inbox-updated' || eventName === 'group-invitation') {
    loadInbox();
  }
});
