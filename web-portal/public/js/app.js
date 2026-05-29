async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    ...options
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const errorMessage = body.message || 'Request failed';
    if (response.status === 401) {
      throw new Error(errorMessage || 'Your session has expired. Please log in again.');
    }
    if (response.status === 403) {
      throw new Error(errorMessage || 'You do not have permission to perform this action.');
    }
    if (response.status === 404) {
      throw new Error(errorMessage || 'The requested resource was not found.');
    }
    if (response.status === 429) {
      throw new Error(errorMessage || 'Too many requests. Please wait before trying again.');
    }
    throw new Error(errorMessage);
  }
  return body;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));
}

function formatDate(value) {
  if (!value) return 'Not assigned';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Not assigned';
  return date.toLocaleString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function statusBadge(status) {
  const value = escapeHtml(status || 'PENDING');
  return `<span class="status-badge status-${String(status || 'pending').toLowerCase()}">${value}</span>`;
}

function initials(name) {
  return String(name || 'Borrower')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('') || 'U';
}

function showMessage(selector, message, type = 'info') {
  const el = document.querySelector(selector);
  if (!el) return;
  el.textContent = message;
  el.classList.add('visible');
  el.classList.toggle('success', type === 'success');
  el.classList.toggle('error', type === 'error');
  el.classList.toggle('warning', type === 'warning');
}

function hideMessage(selector) {
  const el = document.querySelector(selector);
  if (!el) return;
  el.textContent = '';
  el.classList.remove('visible', 'success', 'error', 'warning');
}

function showToast(message, type = 'info', duration = 4500) {
  const container = document.getElementById('toast-container') || createToastContainer();
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${type === 'success' ? 'OK' : type === 'error' ? '!' : 'i'}</span>
    <span class="toast-message">${escapeHtml(message)}</span>
    <button class="toast-close" aria-label="Close notification">x</button>
  `;
  container.appendChild(toast);
  toast.querySelector('.toast-close').addEventListener('click', () => removeToast(toast));
  if (duration > 0) setTimeout(() => removeToast(toast), duration);
}

function createToastContainer() {
  const container = document.createElement('div');
  container.id = 'toast-container';
  container.className = 'toast-container';
  document.body.appendChild(container);
  return container;
}

function removeToast(toast) {
  if (!toast || !toast.parentNode) return;
  toast.classList.add('hiding');
  setTimeout(() => toast.remove(), 220);
}

function showModal({ title, message, confirmText = 'Close', cancelText, onConfirm, onCancel }) {
  hideModal();
  const overlay = document.createElement('div');
  overlay.id = 'modal-overlay';
  overlay.className = 'modal-overlay active';
  overlay.innerHTML = `
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div class="modal-header" id="modal-title">${escapeHtml(title)}</div>
      <div class="modal-body">${message}</div>
      <div class="modal-footer">
        ${cancelText ? `<button class="modal-btn modal-btn-secondary" data-action="cancel">${escapeHtml(cancelText)}</button>` : ''}
        <button class="modal-btn modal-btn-primary" data-action="confirm">${escapeHtml(confirmText)}</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);

  overlay.querySelector('[data-action="confirm"]').addEventListener('click', () => {
    if (onConfirm) onConfirm();
    hideModal();
  });
  const cancelButton = overlay.querySelector('[data-action="cancel"]');
  if (cancelButton) {
    cancelButton.addEventListener('click', () => {
      if (onCancel) onCancel();
      hideModal();
    });
  }
  overlay.addEventListener('click', event => {
    if (event.target === overlay) {
      if (onCancel) onCancel();
      hideModal();
    }
  });
}

function hideModal() {
  const overlay = document.getElementById('modal-overlay');
  if (overlay) overlay.remove();
}

function skeletonCards(count = 3) {
  return Array.from({ length: count }, () => `
    <article class="skeleton-card">
      <span class="skeleton skeleton-title"></span>
      <span class="skeleton skeleton-text"></span>
      <span class="skeleton skeleton-text"></span>
    </article>
  `).join('');
}

function emptyState(title, detail = '') {
  return `
    <div class="empty-state">
      <strong>${escapeHtml(title)}</strong>
      ${detail ? `<p>${escapeHtml(detail)}</p>` : ''}
    </div>
  `;
}

async function getCurrentUser() {
  const payload = await requestJson('/api/user');
  return payload.user || null;
}

async function requireGuestPage() {
  const user = await getCurrentUser();
  if (user) {
    window.location.href = '/';
  }
}

async function initAuthenticatedPage() {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = '/login.html';
    return null;
  }
  if (!['USER', 'STUDENT'].includes(user.role)) {
    await logout(false);
    window.location.href = '/login.html';
    return null;
  }
  buildProfileButton(user);
  buildNavLogout();
  return user;
}

async function logout(redirect = true) {
  await requestJson('/api/logout', { method: 'POST' }).catch(() => {});
  if (redirect) window.location.href = '/login.html';
}

function buildProfileButton(user) {
  const button = document.querySelector('#profile-button');
  if (!button) return;
  button.textContent = initials(user.fullName);
  button.addEventListener('click', event => {
    event.preventDefault();
    window.location.href = '/account.html';
  });
}

function buildNavLogout() {
  document.querySelectorAll('#logout-link, #logout-account-button').forEach(link => {
    link.addEventListener('click', event => {
      event.preventDefault();
      logout();
    });
  });
}

function formPayload(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function initLoginPage() {
  await requireGuestPage();
  const form = document.querySelector('#login-form');
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#login-feedback');
    try {
      const payload = await requestJson('/api/login', {
        method: 'POST',
        body: JSON.stringify(formPayload(form))
      });
      if (!['USER', 'STUDENT'].includes(payload.role)) {
        showMessage('#login-feedback', 'Borrower portal access only.', 'error');
        return;
      }
      window.location.href = '/';
    } catch (error) {
      showMessage('#login-feedback', error.message, 'error');
    }
  });
}

async function initRegisterPage() {
  await requireGuestPage();
  const form = document.querySelector('#register-form');
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#register-feedback');
    try {
      const payload = await requestJson('/api/register', {
        method: 'POST',
        body: JSON.stringify(formPayload(form))
      });
      if (payload.user) {
        window.location.href = '/';
        return;
      }
      const tokenNote = payload.devVerificationToken
        ? ` Dev token: ${payload.devVerificationToken}`
        : '';
      showMessage('#register-feedback', `${payload.message}${tokenNote}`, 'success');
      form.reset();
    } catch (error) {
      showMessage('#register-feedback', error.message, 'error');
    }
  });
}

async function initForgotPasswordPage() {
  await requireGuestPage();
  const form = document.querySelector('#forgot-password-form');
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#forgot-feedback');
    try {
      const payload = await requestJson('/api/forgot-password', {
        method: 'POST',
        body: JSON.stringify(formPayload(form))
      });
      const tokenNote = payload.devResetToken ? ` Dev token: ${payload.devResetToken}` : '';
      showMessage('#forgot-feedback', `${payload.message}${tokenNote}`, 'success');
      form.reset();
    } catch (error) {
      showMessage('#forgot-feedback', error.message, 'error');
    }
  });
}

async function initResetPasswordPage() {
  await requireGuestPage();
  const form = document.querySelector('#reset-password-form');
  if (!form) return;
  const token = new URLSearchParams(window.location.search).get('token');
  if (token) form.elements.token.value = token;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#reset-feedback');
    try {
      const payload = await requestJson('/api/reset-password', {
        method: 'POST',
        body: JSON.stringify(formPayload(form))
      });
      showMessage('#reset-feedback', payload.message, 'success');
      form.reset();
    } catch (error) {
      showMessage('#reset-feedback', error.message, 'error');
    }
  });
}

async function initVerifyEmailPage() {
  await requireGuestPage();
  const form = document.querySelector('#verify-email-form');
  if (!form) return;
  const token = new URLSearchParams(window.location.search).get('token');
  if (token) form.elements.token.value = token;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#verify-feedback');
    try {
      const payload = await requestJson('/api/verify-email', {
        method: 'POST',
        body: JSON.stringify(formPayload(form))
      });
      showMessage('#verify-feedback', payload.message, 'success');
      form.reset();
    } catch (error) {
      showMessage('#verify-feedback', error.message, 'error');
    }
  });
}

async function initDashboardPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;

  const recent = document.querySelector('#recent-activity');
  const dueSoon = document.querySelector('#due-soon-list');
  const notificationList = document.querySelector('#notification-list');
  if (recent) recent.innerHTML = skeletonCards(2);
  if (dueSoon) dueSoon.innerHTML = skeletonCards(2);
  if (notificationList) notificationList.innerHTML = skeletonCards(2);

  try {
    const payload = await requestJson('/api/dashboard');
    document.querySelector('#metric-pending').textContent = payload.summary.pending;
    document.querySelector('#metric-borrowed').textContent = payload.summary.borrowed;
    document.querySelector('#metric-overdue').textContent = payload.summary.overdue;
    document.querySelector('#metric-total').textContent = payload.summary.totalReservations;

    recent.innerHTML = payload.recent.length
      ? payload.recent.map(item => `
          <article class="list-row">
            <div>
              <strong>${escapeHtml(item.name)}</strong>
              <span>${escapeHtml(item.asset_tag)} - ${formatDate(item.request_date)}</span>
            </div>
            ${statusBadge(item.status)}
          </article>
        `).join('')
      : emptyState('No reservation activity yet.', 'Equipment requests will appear here.');

    dueSoon.innerHTML = payload.dueSoon.length
      ? payload.dueSoon.map(item => `
          <article class="list-row warning-row">
            <div>
              <strong>${escapeHtml(item.name)}</strong>
              <span>${escapeHtml(item.asset_tag)} - ${formatDate(item.due_date)}</span>
            </div>
          </article>
        `).join('')
      : emptyState('No due dates within three days.');
  } catch (error) {
    showToast(error.message, 'error');
  }

  await loadNotifications();
}

async function loadNotifications() {
  const notificationList = document.querySelector('#notification-list');
  const count = document.querySelector('#notification-count');
  if (!notificationList) return;
  try {
    const payload = await requestJson('/api/notifications');
    if (count) count.textContent = payload.notifications.length;
    notificationList.innerHTML = payload.notifications.length
      ? payload.notifications.map(item => `
          <article class="list-row">
            <div>
              <strong>${escapeHtml(item.title)}</strong>
              <span>${escapeHtml(item.message)}</span>
            </div>
            <time>${formatDate(item.createdAt)}</time>
          </article>
        `).join('')
      : emptyState('No notifications.');
  } catch (error) {
    notificationList.innerHTML = emptyState('Notifications unavailable.', error.message);
  }
}

async function initEquipmentPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;

  const list = document.querySelector('#equipment-list');
  const searchInput = document.querySelector('#search-input');
  const categoryFilter = document.querySelector('#category-filter');
  const sortFilter = document.querySelector('#sort-filter');
  const searchButton = document.querySelector('#search-button');
  const clearButton = document.querySelector('#clear-filters');
  const pagination = document.querySelector('#pagination-controls');
  let currentPage = 1;

  async function loadCategories() {
    try {
      const payload = await requestJson('/api/equipment/categories');
      payload.categories.forEach(category => {
        const option = document.createElement('option');
        option.value = category;
        option.textContent = category;
        categoryFilter.appendChild(option);
      });
    } catch (error) {
      showToast(error.message, 'error');
    }
  }

  async function loadItems(page = 1) {
    currentPage = page;
    hideMessage('#equipment-feedback');
    list.innerHTML = skeletonCards(6);
    try {
      const params = new URLSearchParams({
        page: String(page),
        limit: '12',
        sort: sortFilter.value
      });
      if (searchInput.value.trim()) params.set('search', searchInput.value.trim());
      if (categoryFilter.value) params.set('category', categoryFilter.value);

      const payload = await requestJson(`/api/equipment?${params.toString()}`);
      list.innerHTML = payload.equipment.length
        ? payload.equipment.map(item => equipmentCard(item)).join('')
        : emptyState('No available equipment found.', 'Try a different search or category.');

      list.querySelectorAll('[data-reserve-id]').forEach(button => {
        button.addEventListener('click', () => reserveEquipment(button));
      });

      renderPagination(payload.pagination);
    } catch (error) {
      list.innerHTML = emptyState('Unable to load equipment.', error.message);
    }
  }

  function equipmentCard(item) {
    const code = initials(item.category || item.name);
    return `
      <article class="equipment-card">
        <div class="equipment-image" aria-hidden="true">${escapeHtml(code)}</div>
        <div class="card-header">
          <div>
            <h2>${escapeHtml(item.name)}</h2>
            <span>${escapeHtml(item.asset_tag)}</span>
          </div>
          ${statusBadge(item.status)}
        </div>
        <p>${escapeHtml(item.description || 'No description provided.')}</p>
        <dl class="meta-grid">
          <div><dt>Category</dt><dd>${escapeHtml(item.category)}</dd></div>
          <div><dt>Available</dt><dd>${escapeHtml(item.available_quantity)}</dd></div>
        </dl>
        <div class="reserve-row">
          <label>
            Qty
            <input type="number" min="1" max="${escapeHtml(item.available_quantity)}" value="1" data-quantity-for="${escapeHtml(item.equipment_id)}">
          </label>
          <button data-reserve-id="${escapeHtml(item.equipment_id)}">Request</button>
        </div>
      </article>
    `;
  }

  function renderPagination(pageData) {
    if (!pagination || !pageData || pageData.totalPages <= 1) {
      pagination.innerHTML = '';
      return;
    }
    pagination.innerHTML = `
      <button class="secondary-button" ${pageData.hasPrev ? '' : 'disabled'} data-page="${pageData.page - 1}">Previous</button>
      <span>Page ${pageData.page} of ${pageData.totalPages}</span>
      <button class="secondary-button" ${pageData.hasNext ? '' : 'disabled'} data-page="${pageData.page + 1}">Next</button>
    `;
    pagination.querySelectorAll('[data-page]').forEach(button => {
      button.addEventListener('click', () => loadItems(Number(button.dataset.page)));
    });
  }

  function reserveEquipment(button) {
    const equipmentId = button.dataset.reserveId;
    const quantityInput = document.querySelector(`[data-quantity-for="${equipmentId}"]`);
    const requested = Number(quantityInput.value || 1);
    const max = Number(quantityInput.max || 1);
    const quantity = Math.max(1, Math.min(requested, max));

    showModal({
      title: 'Confirm Reservation',
      message: `<p>Submit a reservation request for quantity <strong>${quantity}</strong>?</p>`,
      confirmText: 'Submit',
      cancelText: 'Cancel',
      onConfirm: async () => {
        try {
          const payload = await requestJson('/api/reservations', {
            method: 'POST',
            body: JSON.stringify({ equipmentId, quantity })
          });
          showMessage('#equipment-feedback', `${payload.message} Reference: ${payload.referenceNumber}`, 'success');
          await loadItems(currentPage);
        } catch (error) {
          showMessage('#equipment-feedback', error.message, 'error');
        }
      }
    });
  }

  searchButton.addEventListener('click', () => loadItems(1));
  clearButton.addEventListener('click', () => {
    searchInput.value = '';
    categoryFilter.value = '';
    sortFilter.value = 'name';
    loadItems(1);
  });
  searchInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') loadItems(1);
  });
  categoryFilter.addEventListener('change', () => loadItems(1));
  sortFilter.addEventListener('change', () => loadItems(1));

  await loadCategories();
  await loadItems();
}

function reservationRow(item, options = {}) {
  const receiptButton = options.receipt
    ? `<button class="secondary-button" data-receipt-id="${escapeHtml(item.reservation_id)}">Receipt</button>`
    : '';
  const cancelButton = item.status === 'PENDING'
    ? `<button class="danger-button" data-cancel-id="${escapeHtml(item.reservation_id)}">Cancel</button>`
    : '';
  const reference = item.reference_number || `BRW-${String(item.reservation_id).padStart(6, '0')}`;
  return `
    <article class="list-row reservation-row">
      <div>
        <strong>${escapeHtml(item.name)}</strong>
        <span>${escapeHtml(item.asset_tag)} - Ref ${escapeHtml(reference)}</span>
        <span>Qty ${escapeHtml(item.quantity)} - Requested ${formatDate(item.request_date)}</span>
        ${item.due_date ? `<span>Due ${formatDate(item.due_date)}</span>` : ''}
        ${item.remarks ? `<span>${escapeHtml(item.remarks)}</span>` : ''}
      </div>
      <div class="row-actions">
        ${statusBadge(item.status)}
        ${receiptButton}
        ${cancelButton}
      </div>
    </article>
  `;
}

async function initPendingRequestsPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;
  const list = document.querySelector('#pending-list');

  async function loadPending() {
    hideMessage('#pending-feedback');
    list.innerHTML = skeletonCards(3);
    try {
      const payload = await requestJson('/api/reservations/pending');
      list.innerHTML = payload.reservations.length
        ? payload.reservations.map(item => reservationRow(item, { receipt: true })).join('')
        : emptyState('No pending requests.');
      bindReservationActions(loadPending);
    } catch (error) {
      list.innerHTML = emptyState('Unable to load pending requests.', error.message);
    }
  }

  await loadPending();
}

async function initCurrentLoansPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;
  const list = document.querySelector('#current-loans-list');
  list.innerHTML = skeletonCards(3);
  try {
    const payload = await requestJson('/api/reservations/current');
    list.innerHTML = payload.reservations.length
      ? payload.reservations.map(item => reservationRow(item, { receipt: true })).join('')
      : emptyState('No borrowed items.');
    bindReservationActions(() => initCurrentLoansPage());
  } catch (error) {
    list.innerHTML = emptyState('Unable to load borrowed items.', error.message);
  }
}

async function initHistoryPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;
  const list = document.querySelector('#history-list');

  async function loadHistory() {
    hideMessage('#history-feedback');
    list.innerHTML = skeletonCards(4);
    try {
      const payload = await requestJson('/api/reservations/history');
      list.innerHTML = payload.reservations.length
        ? payload.reservations.map(item => reservationRow(item, { receipt: true })).join('')
        : emptyState('No reservation history.');
      bindReservationActions(loadHistory);
    } catch (error) {
      list.innerHTML = emptyState('Unable to load history.', error.message);
    }
  }

  await loadHistory();
}

function bindReservationActions(refreshCallback) {
  document.querySelectorAll('[data-cancel-id]').forEach(button => {
    button.addEventListener('click', () => {
      const reservationId = button.dataset.cancelId;
      showModal({
        title: 'Cancel Request',
        message: '<p>This will cancel the pending reservation request.</p>',
        confirmText: 'Cancel Request',
        cancelText: 'Keep Request',
        onConfirm: async () => {
          try {
            await requestJson(`/api/reservations/${reservationId}`, { method: 'DELETE' });
            showToast('Reservation canceled.', 'success');
            await refreshCallback();
          } catch (error) {
            showToast(error.message, 'error');
          }
        }
      });
    });
  });

  document.querySelectorAll('[data-receipt-id]').forEach(button => {
    button.addEventListener('click', () => showReceipt(button.dataset.receiptId));
  });
}

async function showReceipt(reservationId) {
  try {
    const payload = await requestJson(`/api/reservations/${reservationId}/receipt`);
    const receipt = payload.receipt;
    showModal({
      title: 'Reservation Receipt',
      message: `
        <dl class="receipt-grid">
          <div><dt>Reference</dt><dd>${escapeHtml(receipt.reference_number)}</dd></div>
          <div><dt>User</dt><dd>${escapeHtml(receipt.full_name)} (${escapeHtml(receipt.username)})</dd></div>
          <div><dt>Equipment</dt><dd>${escapeHtml(receipt.name)} - ${escapeHtml(receipt.asset_tag)}</dd></div>
          <div><dt>Status</dt><dd>${escapeHtml(receipt.status)}</dd></div>
          <div><dt>Quantity</dt><dd>${escapeHtml(receipt.quantity)}</dd></div>
          <div><dt>Requested</dt><dd>${formatDate(receipt.request_date)}</dd></div>
          <div><dt>Due</dt><dd>${formatDate(receipt.due_date)}</dd></div>
        </dl>
      `
    });
  } catch (error) {
    showToast(error.message, 'error');
  }
}

async function initAccountPage() {
  const user = await initAuthenticatedPage();
  if (!user) return;
  const profileForm = document.querySelector('#profile-form');
  const passwordForm = document.querySelector('#change-password-form');
  const overdueList = document.querySelector('#overdue-list');

  try {
    const payload = await requestJson('/api/profile');
    const profile = payload.user;
    Object.entries({
      fullName: profile.fullName,
      username: profile.username,
      email: profile.email,
      branch: profile.branch,
      course: profile.course,
      block: profile.block,
      yearLevel: profile.yearLevel,
      phoneNumber: profile.phoneNumber
    }).forEach(([key, value]) => {
      if (profileForm.elements[key]) profileForm.elements[key].value = value || '';
    });
  } catch (error) {
    showMessage('#account-feedback', error.message, 'error');
  }

  profileForm.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#account-feedback');
    try {
      const payload = await requestJson('/api/profile', {
        method: 'PUT',
        body: JSON.stringify({ phoneNumber: profileForm.elements.phoneNumber.value })
      });
      buildProfileButton(payload.user);
      showMessage('#account-feedback', payload.message, 'success');
    } catch (error) {
      showMessage('#account-feedback', error.message, 'error');
    }
  });

  passwordForm.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#account-feedback');
    try {
      const payload = await requestJson('/api/change-password', {
        method: 'POST',
        body: JSON.stringify(formPayload(passwordForm))
      });
      passwordForm.reset();
      showMessage('#account-feedback', payload.message, 'success');
    } catch (error) {
      showMessage('#account-feedback', error.message, 'error');
    }
  });

  try {
    const payload = await requestJson('/api/overdues');
    overdueList.innerHTML = payload.overdues.length
      ? payload.overdues.map(item => `
          <article class="list-row">
            <div>
              <strong>Reservation ${escapeHtml(item.reservation_id)}</strong>
              <span>${escapeHtml(item.days_late)} day(s) late - penalty ${escapeHtml(item.penalty_days)} day(s)</span>
              <span>Penalty end: ${formatDate(item.penalty_end_date)}</span>
            </div>
            <span class="status-badge ${item.settled ? 'status-returned' : 'status-overdue'}">${item.settled ? 'SETTLED' : 'OPEN'}</span>
          </article>
        `).join('')
      : emptyState('No overdue records.');
  } catch (error) {
    overdueList.innerHTML = emptyState('Overdue records unavailable.', error.message);
  }
}

async function runPage() {
  const page = document.body.dataset.page;
  if (page === 'login') return initLoginPage();
  if (page === 'register') return initRegisterPage();
  if (page === 'forgot-password') return initForgotPasswordPage();
  if (page === 'reset-password') return initResetPasswordPage();
  if (page === 'verify-email') return initVerifyEmailPage();
  if (page === 'dashboard') return initDashboardPage();
  if (page === 'equipment') return initEquipmentPage();
  if (page === 'pending-requests') return initPendingRequestsPage();
  if (page === 'current-loans') return initCurrentLoansPage();
  if (page === 'history') return initHistoryPage();
  if (page === 'account') return initAccountPage();
}

runPage().catch(error => {
  console.error(error);
  showToast(error.message, 'error');
});
