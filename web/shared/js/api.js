import { showToast } from './toast.js';

const API_BASE = '/api/v1';

export function getToken() {
  return localStorage.getItem('budgetbuddy_access_token') || localStorage.getItem('budgetbuddy_token');
}

export function getRefreshToken() {
  return localStorage.getItem('budgetbuddy_refresh_token');
}

export function setSession(authData) {
  const accessToken = authData.accessToken || authData.token;
  localStorage.setItem('budgetbuddy_token', accessToken);
  localStorage.setItem('budgetbuddy_access_token', accessToken);
  if (authData.refreshToken) {
    localStorage.setItem('budgetbuddy_refresh_token', authData.refreshToken);
  }
  localStorage.setItem('budgetbuddy_user', JSON.stringify(authData.user));
}

export function getUser() {
  return JSON.parse(localStorage.getItem('budgetbuddy_user') || 'null');
}

export function clearSession() {
  localStorage.removeItem('budgetbuddy_token');
  localStorage.removeItem('budgetbuddy_access_token');
  localStorage.removeItem('budgetbuddy_refresh_token');
  localStorage.removeItem('budgetbuddy_user');
}

export async function refreshSession() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;
  try {
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
      clearSession();
      return false;
    }
    setSession(payload.data);
    return true;
  } catch {
    clearSession();
    return false;
  }
}

export async function ensureSession() {
  if (!getToken()) return false;
  try {
    await api('/auth/me', {}, false);
    return true;
  } catch {
    return refreshSession();
  }
}

export async function api(path, options = {}) {
  return apiRequest(path, options, true);
}

async function apiRequest(path, options = {}, allowRefresh = true) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const payload = await response.json().catch(() => ({}));
  if (response.status === 401 && allowRefresh && await refreshSession()) {
    return apiRequest(path, options, false);
  }
  if (!response.ok || payload.success === false) {
    const message = payload.message || 'Request failed';
    showToast(message, 'error');
    throw new Error(message);
  }
  return payload.data;
}

export async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' });
  } finally {
    clearSession();
  }
}

export function subscribeRealtime(onMessage) {
  const token = getToken();
  if (!token || !window.EventSource) return null;
  const source = new EventSource(`${API_BASE}/realtime/stream?token=${encodeURIComponent(token)}`);
  [
    'dashboard-updated',
    'groups-updated',
    'shared-expenses-updated',
    'group-transaction-updated',
    'group-invitation',
    'inbox-updated'
  ].forEach(eventName => {
    source.addEventListener(eventName, event => onMessage(eventName, event.data));
  });
  return source;
}

export function formatPeso(value) {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(Number(value || 0));
}
