import { api, formatPeso, subscribeRealtime } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';
import { categoryLabel, populateCategorySelect } from '../../shared/js/categories.js';

populateCategorySelect(document.getElementById('category'));

async function loadBudgets() {
  const tracking = await api('/budgets/tracking');
  document.getElementById('budgetCount').textContent = tracking.length;
  document.getElementById('budgetTracking').innerHTML = tracking.length ? tracking.map(item => {
    const budget = item.budget;
    const statusClass = budget.status === 'EXCEEDED' ? 'exceeded' : budget.status === 'WARNING' ? 'warning' : '';
    const badgeClass = budget.status === 'EXCEEDED' ? 'badge-expense' : budget.status === 'WARNING' ? 'badge-pending' : 'badge-income';
    return `
      <div class="list-row">
        <div style="flex:1">
          <strong>${budget.name}</strong>
          <span class="badge ${badgeClass}">${budget.status}</span>
          <div class="progress"><div class="progress-bar ${statusClass}" style="--progress:${Math.min(Number(budget.percentageUsed), 100)}%"></div></div>
          <div class="tracking-meta">
            <span class="amount-pill expense">${formatPeso(budget.spentAmount)} spent</span>
            <span>${formatPeso(budget.limitAmount)} limit</span>
            <span>${formatPeso(budget.remainingAmount)} remaining</span>
            <span>${budget.period} / ${categoryLabel(budget.category)}</span>
          </div>
          <small>${item.relatedTransactions.length} related expense transaction(s)</small>
        </div>
      </div>
    `;
  }).join('') : '<div class="empty-state">No budgets yet.</div>';
}

document.getElementById('budgetForm').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = {
    name: document.getElementById('budgetName').value,
    limitAmount: document.getElementById('limitAmount').value,
    period: document.getElementById('period').value,
    category: document.getElementById('category').value,
    startDate: document.getElementById('startDate').value || null,
    endDate: document.getElementById('endDate').value || null
  };
  await api('/budgets', { method: 'POST', body: JSON.stringify(payload) });
  event.target.reset();
  showToast('Budget saved');
  loadBudgets();
});

loadBudgets();
subscribeRealtime(eventName => {
  if (['dashboard-updated', 'inbox-updated'].includes(eventName)) {
    loadBudgets();
  }
});
