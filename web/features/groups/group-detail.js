import { api, formatPeso, subscribeRealtime } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';
import { categoryLabel, populateCategorySelect } from '../../shared/js/categories.js';

const groupId = new URLSearchParams(window.location.search).get('id');
populateCategorySelect(document.getElementById('transactionCategory'));
populateCategorySelect(document.getElementById('budgetCategory'));

async function loadGroup() {
  const [group, transactions, summary, history, budgetTracking, savingGoals] = await Promise.all([
    api(`/groups/${groupId}`),
    api(`/groups/${groupId}/transactions`),
    api(`/groups/${groupId}/transactions/summary`),
    api(`/groups/${groupId}/history`),
    api(`/groups/${groupId}/budgets/tracking`),
    api(`/groups/${groupId}/saving-goals`)
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
      <div>
        <strong>${transaction.actorUsername}</strong>
        <span class="badge ${transaction.verificationStatus === 'APPROVED' ? 'badge-income' : transaction.verificationStatus === 'DECLINED' ? 'badge-expense' : 'badge-pending'}">${transaction.verificationStatus}</span>
        <br><span>${transaction.type}: ${categoryLabel(transaction.category)}</span>
      </div>
      <span class="amount amount-pill ${transaction.type === 'INCOME' ? 'income' : 'expense'}">${transaction.type === 'INCOME' ? '+' : '-'} ${formatPeso(transaction.amount)}</span>
    </div>
  `).join('') : '<div class="empty-state">No group transactions yet.</div>';

  document.getElementById('groupBudgets').innerHTML = budgetTracking.length ? budgetTracking.map(item => {
    const budget = item.budget;
    const statusClass = budget.status === 'EXCEEDED' ? 'exceeded' : budget.status === 'WARNING' ? 'warning' : '';
    const badgeClass = budget.status === 'EXCEEDED' ? 'badge-expense' : budget.status === 'WARNING' ? 'badge-pending' : 'badge-income';
    return `
      <div class="list-row">
        <div style="flex:1">
          <strong>${budget.name}</strong> <span class="badge ${badgeClass}">${budget.status}</span>
          <div class="progress"><div class="progress-bar ${statusClass}" style="--progress:${Math.min(Number(budget.percentageUsed), 100)}%"></div></div>
          <div class="tracking-meta">
            <span>${formatPeso(budget.spentAmount)} spent</span>
            <span>${formatPeso(budget.limitAmount)} limit</span>
            <span>${budget.period} / ${categoryLabel(budget.category)}</span>
          </div>
        </div>
      </div>
    `;
  }).join('') : '<div class="empty-state">No group budgets yet.</div>';

  document.getElementById('groupGoals').innerHTML = savingGoals.length ? savingGoals.map(goal => `
    <div class="list-row">
      <div style="flex:1">
        <strong>${goal.title}</strong> <span class="badge ${goal.status === 'COMPLETED' ? 'badge-income' : 'badge-member'}">${goal.status}</span>
        <div class="progress"><div class="progress-bar" style="--progress:${Math.min(Number(goal.percentageUsed), 100)}%"></div></div>
        <div class="tracking-meta">
          <span>${formatPeso(goal.currentAmount)} saved</span>
          <span>${formatPeso(goal.targetAmount)} target</span>
          <span>${formatPeso(goal.remainingAmount)} remaining</span>
        </div>
        <form class="contribution-form" data-goal-id="${goal.id}">
          <div class="field"><input name="amount" type="number" min="0.01" step="0.01" placeholder="Contribution amount" required></div>
          <button class="btn-secondary" type="submit">Contribute</button>
        </form>
      </div>
    </div>
  `).join('') : '<div class="empty-state">No group saving goals yet.</div>';

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

document.getElementById('budgetForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api(`/groups/${groupId}/budgets`, {
    method: 'POST',
    body: JSON.stringify({
      name: document.getElementById('budgetName').value,
      limitAmount: document.getElementById('budgetLimit').value,
      period: document.getElementById('budgetPeriod').value,
      category: document.getElementById('budgetCategory').value
    })
  });
  event.target.reset();
  showToast('Group budget saved');
  loadGroup();
});

document.getElementById('goalForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api(`/groups/${groupId}/saving-goals`, {
    method: 'POST',
    body: JSON.stringify({
      title: document.getElementById('goalTitle').value,
      targetAmount: document.getElementById('goalTarget').value,
      currentAmount: document.getElementById('goalCurrent').value
    })
  });
  event.target.reset();
  document.getElementById('goalCurrent').value = 0;
  showToast('Group saving goal saved');
  loadGroup();
});

document.getElementById('groupGoals').addEventListener('submit', async event => {
  const form = event.target.closest('.contribution-form');
  if (!form) return;
  event.preventDefault();
  await api(`/groups/${groupId}/saving-goals/${form.dataset.goalId}/contribute`, {
    method: 'POST',
    body: JSON.stringify({ amount: form.elements.amount.value })
  });
  showToast('Contribution added');
  loadGroup();
});

loadGroup();

const source = subscribeRealtime(eventName => {
  if (['group-transaction-updated', 'groups-updated', 'inbox-updated', 'shared-expenses-updated'].includes(eventName)) {
    document.getElementById('syncStatus').textContent = 'Updated';
    loadGroup();
  }
});
if (!source) {
  document.getElementById('syncStatus').textContent = 'Refresh';
}
