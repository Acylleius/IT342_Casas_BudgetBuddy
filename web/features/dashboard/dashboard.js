import { api, formatPeso, logout, subscribeRealtime } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';

async function loadDashboard() {
  const [summary, transactions, activity] = await Promise.all([
    api('/transactions/summary'),
    api('/transactions'),
    api('/activity')
  ]);

  document.getElementById('balance').textContent = formatPeso(summary.balance);
  document.getElementById('income').textContent = `+ ${formatPeso(summary.totalIncome)}`;
  document.getElementById('expense').textContent = `- ${formatPeso(summary.totalExpense)}`;
  document.getElementById('count').textContent = summary.count;

  document.getElementById('transactions').innerHTML = transactions.length ? transactions.map(transaction => `
    <div class="list-row">
      <div><strong>${transaction.category}</strong><br><span>${transaction.type}</span></div>
      <span class="amount-pill ${transaction.type === 'INCOME' ? 'income' : 'expense'} amount">
        ${transaction.type === 'INCOME' ? '+' : '-'} ${formatPeso(transaction.amount)}
      </span>
    </div>
  `).join('') : '<div class="empty-state">No transactions yet - add your first one!</div>';

  document.getElementById('activity').innerHTML = activity.length ? activity.map(item => `
    <div class="list-row">
      <div><strong>${item.description}</strong><br><span>${new Date(item.createdAt).toLocaleString()}</span></div>
      <span class="badge badge-member">${item.action}</span>
    </div>
  `).join('') : '<div class="empty-state">No activity yet.</div>';
}

document.getElementById('transactionForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api('/transactions', {
    method: 'POST',
    body: JSON.stringify({
      type: document.getElementById('type').value,
      amount: document.getElementById('amount').value,
      category: document.getElementById('category').value
    })
  });
  event.target.reset();
  showToast('Transaction saved');
  loadDashboard();
});

document.getElementById('logoutBtn').addEventListener('click', async () => {
  await logout();
  window.location.href = '../auth/login.html';
});

loadDashboard();

const source = subscribeRealtime(() => loadDashboard());
if (!source) {
  document.getElementById('syncStatus').textContent = 'Refresh';
}
