import { api, formatPeso } from '../../shared/js/api.js';
import { showToast } from '../../shared/js/toast.js';

async function loadGoals() {
  const goals = await api('/saving-goals');
  document.getElementById('goalCount').textContent = goals.length;
  document.getElementById('goalsList').innerHTML = goals.length ? goals.map(goal => {
    const complete = goal.status === 'COMPLETED';
    return `
      <div class="list-row">
        <div style="flex:1">
          <strong>${goal.title}</strong>
          <span class="badge ${complete ? 'badge-income' : goal.status === 'OVERDUE' ? 'badge-expense' : 'badge-member'}">${goal.status}</span>
          <div class="progress"><div class="progress-bar ${complete ? '' : 'warning'}" style="--progress:${Math.min(Number(goal.percentageUsed), 100)}%"></div></div>
          <div class="tracking-meta">
            <span>${formatPeso(goal.currentAmount)} saved</span>
            <span>${formatPeso(goal.targetAmount)} target</span>
            <span>${formatPeso(goal.remainingAmount)} remaining</span>
            ${goal.deadline ? `<span>Due ${goal.deadline}</span>` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('') : '<div class="empty-state">No saving goals yet.</div>';
}

document.getElementById('goalForm').addEventListener('submit', async event => {
  event.preventDefault();
  await api('/saving-goals', {
    method: 'POST',
    body: JSON.stringify({
      title: document.getElementById('title').value,
      targetAmount: document.getElementById('targetAmount').value,
      currentAmount: document.getElementById('currentAmount').value,
      deadline: document.getElementById('deadline').value || null
    })
  });
  event.target.reset();
  document.getElementById('currentAmount').value = 0;
  showToast('Saving goal saved');
  loadGoals();
});

loadGoals();
