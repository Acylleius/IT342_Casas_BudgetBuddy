export function showToast(message, type = 'success') {
  const container = getToastContainer();
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div>
      <strong>${toastTitle(type)}</strong>
      <p>${message}</p>
    </div>
    <button class="toast-close" type="button" aria-label="Close notification">x</button>
  `;
  toast.querySelector('.toast-close').addEventListener('click', () => dismiss(toast));
  container.appendChild(toast);
  setTimeout(() => dismiss(toast), type === 'error' ? 6000 : 3600);
}

function getToastContainer() {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  return container;
}

function toastTitle(type) {
  if (type === 'error') return 'Error';
  if (type === 'warning') return 'Warning';
  if (type === 'info') return 'Info';
  return 'Success';
}

function dismiss(toast) {
  toast.classList.add('toast-hide');
  setTimeout(() => toast.remove(), 180);
}
