import { api, formatPeso, subscribeRealtime } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';

const groupId = new URLSearchParams(window.location.search).get('id');

async function loadGroup() {
  const [group, transactions, summary, balances, history] = await Promise.all([
    api(`/groups/${groupId}`),
    api(`/groups/${groupId}/transactions`),
    api(`/groups/${groupId}/transactions/summary`),
    api(`/groups/${groupId}/balances`),
    api(`/groups/${groupId}/history`)
  ]);

  document.getElementById('groupTitle').textContent = group.name;
  document.getElementById('groupIncome').textContent = formatPeso(summary.totalIncome);
  document.getElementById('groupExpense').textContent = formatPeso(summary.totalExpenses);
  document.getElementById('groupNet').textContent = formatPeso(summary.netBalance);

  document.getElementById('actorUserId').innerHTML = group.members.map(member => `
    <option value="${member.userId}">${member.firstname} ${member.lastname}</option>
  `).join('');

  document.getElementById('members').innerHTML = group.members.map(member => `
    <div class="list-row">
      <span>${member.firstname} ${member.lastname}<br>${member.email}</span>
      <span class="badge ${member.role === 'ADMIN' ? 'badge-admin' : 'badge-member'}">${member.role}</span>
    </div>
  `).join('');

  document.getElementById('groupTransactions').innerHTML = transactions.length ? transactions.map(transaction => `
    <div class="list-row">
      <div><strong>${transaction.actorUsername}</strong><br><span>${transaction.type}: ${transaction.category}</span></div>
      <span class="amount amount-pill ${transaction.type === 'INCOME' ? 'income' : 'expense'}">
        ${transaction.type === 'INCOME' ? '+' : '-'} ${formatPeso(transaction.amount)}
      </span>
    </div>
  `).join('') : '<div class="empty-state">No group transactions yet.</div>';

  document.getElementById('balances').innerHTML = balances.length ? balances.map(balance => `
    <div class="list-row">
      <span>Member #${balance.userId}</span>
      <span class="amount amount-pill ${Number(balance.netBalance) >= 0 ? 'income' : 'expense'}">${formatPeso(balance.netBalance)}</span>
    </div>
  `).join('') : '<div class="empty-state">No balances yet.</div>';

  document.getElementById('history').innerHTML = history.length ? history.map(item => `
    <div class="list-row">
      <div><strong>${item.actorUsername}</strong><br><span>${item.description}</span></div>
      <span>${new Date(item.createdAt).toLocaleString()}</span>
    </div>
  `).join('') : '<div class="empty-state">No group history yet.</div>';
}

document.getElementById('inviteForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api(`/groups/${groupId}/invitations`, {
    method: 'POST',
    body: JSON.stringify({ email: document.getElementById('inviteEmail').value })
  });
  event.target.reset();
  showToast('Invite sent successfully');
  loadGroup();
});

document.getElementById('transactionForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api(`/groups/${groupId}/transactions`, {
    method: 'POST',
    body: JSON.stringify({
      type: document.getElementById('transactionType').value,
      actorUserId: Number(document.getElementById('actorUserId').value),
      amount: document.getElementById('transactionAmount').value,
      category: document.getElementById('transactionCategory').value,
      description: document.getElementById('transactionDescription').value
    })
  });
  event.target.reset();
  showToast('Group transaction saved');
  loadGroup();
});

loadGroup();

const source = subscribeRealtime(eventName => {
  if (['group-transaction-updated', 'groups-updated', 'inbox-updated'].includes(eventName)) {
    document.getElementById('syncStatus').textContent = 'Updated';
    loadGroup();
  }
});
if (!source) {
  document.getElementById('syncStatus').textContent = 'Refresh';
}
