async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    ...options
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const errorMessage = body.message || 'Request failed';
    // Provide more specific error messages based on status code
    if (response.status === 401) {
      throw new Error('Your session has expired. Please log in again.');
    } else if (response.status === 403) {
      throw new Error('You do not have permission to perform this action.');
    } else if (response.status === 404) {
      throw new Error('The requested resource was not found.');
    } else if (response.status === 429) {
      throw new Error('Too many requests. Please wait a moment before trying again.');
    } else if (response.status === 500) {
      throw new Error('A server error occurred. Please try again later.');
    }
    throw new Error(errorMessage);
  }
  return body;
}

// Real-time form validation
function validateField(field, value) {
  const errors = {};
  
  if (field === 'username') {
    if (!value || value.trim().length < 3) {
      errors.username = 'Username must be at least 3 characters.';
    }
  }
  
  if (field === 'email') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      errors.email = 'Please enter a valid email address.';
    }
  }
  
  if (field === 'password') {
    if (!value || value.length < 12) {
      errors.password = 'Password must be at least 12 characters.';
    }
  }
  
  if (field === 'phoneNumber') {
    const phoneRegex = /^\d{11}$/;
    if (!phoneRegex.test(value)) {
      errors.phoneNumber = 'Please enter a valid 11-digit phone number.';
    }
  }
  
  return errors;
}

function showFieldError(fieldId, message) {
  const field = document.getElementById(fieldId);
  if (!field) return;
  
  let errorElement = document.getElementById(`${fieldId}-error`);
  if (!errorElement) {
    errorElement = document.createElement('div');
    errorElement.id = `${fieldId}-error`;
    errorElement.className = 'field-error';
    errorElement.style.color = '#dc2626';
    errorElement.style.fontSize = '0.875rem';
    errorElement.style.marginTop = '0.25rem';
    field.parentNode.appendChild(errorElement);
  }
  
  errorElement.textContent = message;
  field.style.borderColor = '#dc2626';
}

function clearFieldError(fieldId) {
  const field = document.getElementById(fieldId);
  if (!field) return;
  
  const errorElement = document.getElementById(`${fieldId}-error`);
  if (errorElement) {
    errorElement.remove();
  }
  field.style.borderColor = '';
}

// Toast Notification System
function showToast(message, type = 'info', duration = 5000) {
  const container = document.getElementById('toast-container') || createToastContainer();
  
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  
  const icons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ'
  };
  
  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || icons.info}</span>
    <span class="toast-message">${message}</span>
    <button class="toast-close" aria-label="Close notification">×</button>
  `;
  
  container.appendChild(toast);
  
  const closeBtn = toast.querySelector('.toast-close');
  closeBtn.addEventListener('click', () => removeToast(toast));
  
  if (duration > 0) {
    setTimeout(() => removeToast(toast), duration);
  }
  
  return toast;
}

function createToastContainer() {
  const container = document.createElement('div');
  container.id = 'toast-container';
  container.className = 'toast-container';
  document.body.appendChild(container);
  return container;
}

function removeToast(toast) {
  toast.classList.add('hiding');
  toast.addEventListener('animationend', () => {
    toast.remove();
  });
}

// Modal Dialog System
function showModal(options) {
  const { title, message, onConfirm, onCancel, confirmText = 'Confirm', cancelText = 'Cancel' } = options;
  
  const overlay = document.getElementById('modal-overlay') || createModalOverlay();
  
  const modal = overlay.querySelector('.modal') || document.createElement('div');
  modal.className = 'modal';
  modal.innerHTML = `
    <div class="modal-header">${title}</div>
    <div class="modal-body">${message}</div>
    <div class="modal-footer">
      <button class="modal-btn modal-btn-secondary" data-action="cancel">${cancelText}</button>
      <button class="modal-btn modal-btn-primary" data-action="confirm">${confirmText}</button>
    </div>
  `;
  
  overlay.appendChild(modal);
  overlay.classList.add('active');
  
  const confirmBtn = modal.querySelector('[data-action="confirm"]');
  const cancelBtn = modal.querySelector('[data-action="cancel"]');
  
  confirmBtn.addEventListener('click', () => {
    if (onConfirm) onConfirm();
    hideModal();
  });
  
  cancelBtn.addEventListener('click', () => {
    if (onCancel) onCancel();
    hideModal();
  });
  
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) {
      if (onCancel) onCancel();
      hideModal();
    }
  });
  
  document.addEventListener('keydown', handleEscape);
  
  return { confirm: confirmBtn, cancel: cancelBtn };
}

function createModalOverlay() {
  const overlay = document.createElement('div');
  overlay.id = 'modal-overlay';
  overlay.className = 'modal-overlay';
  document.body.appendChild(overlay);
  return overlay;
}

function hideModal() {
  const overlay = document.getElementById('modal-overlay');
  if (overlay) {
    overlay.classList.remove('active');
    document.removeEventListener('keydown', handleEscape);
  }
}

function handleEscape(e) {
  if (e.key === 'Escape') {
    hideModal();
  }
}

// Skeleton Loader Helpers
function showSkeletonLoader(containerId, count = 3) {
  const container = document.getElementById(containerId);
  if (!container) return;
  
  container.innerHTML = '';
  
  for (let i = 0; i < count; i++) {
    const skeleton = document.createElement('div');
    skeleton.className = 'skeleton-card';
    skeleton.innerHTML = `
      <div class="skeleton skeleton-title"></div>
      <div class="skeleton skeleton-text"></div>
      <div class="skeleton skeleton-text"></div>
      <div class="skeleton skeleton-text" style="width: 40%"></div>
    `;
    container.appendChild(skeleton);
  }
}

function hideSkeletonLoader(containerId) {
  // Content will be replaced by actual data
}

function showMessage(selector, message, type = 'info') {
  const el = document.querySelector(selector);
  if (!el) return;
  el.textContent = message;
  el.classList.add('visible');
  el.classList.toggle('success', type === 'success');
  el.classList.toggle('error', type === 'error');
}

function hideMessage(selector) {
  const el = document.querySelector(selector);
  if (!el) return;
  el.textContent = '';
  el.classList.remove('visible', 'success', 'error');
}

async function getCurrentUser() {
  const payload = await requestJson('/api/user');
  return payload.user || null;
}

async function requireGuestPage(page) {
  const user = await getCurrentUser();
  if (!user) return;

  if (page === 'login') {
    if (user.role === 'ADMIN') {
      showMessage('#login-feedback', 'You are currently signed in as an admin. Please use the admin portal or logout before signing in as a user.', 'error');
      return;
    }
    window.location.href = '/equipment.html';
    return;
  }

  if (user.role === 'ADMIN') {
    window.location.href = '/admin/dashboard.html';
  } else {
    window.location.href = '/equipment.html';
  }
}

async function initAuthenticatedPage(allowedRoles = ['USER', 'STUDENT']) {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = '/login.html';
    return null;
  }
  await buildProfileButton();
  if (!allowedRoles.includes(user.role)) {
    if (user.role === 'ADMIN') {
      window.location.href = '/admin/dashboard.html';
      return null;
    }
    window.location.href = '/login.html';
    return null;
  }
  return user;
}

async function logout() {
  await requestJson('/api/logout', { method: 'POST' });
  window.location.href = '/login.html';
}

async function buildProfileButton() {
  const button = document.querySelector('#profile-button');
  if (!button) return;
  const user = await getCurrentUser();
  button.addEventListener('click', event => {
    event.preventDefault();
    if (!user) {
      window.location.href = '/login.html';
      return;
    }
    if (user.role === 'ADMIN') {
      window.location.href = '/admin/dashboard.html';
      return;
    }
    window.location.href = '/account.html';
  });
}

function buildNavLogout() {
  const logoutLink = document.querySelector('#logout-link');
  if (logoutLink) {
    logoutLink.addEventListener('click', event => {
      event.preventDefault();
      logout();
    });
  }
}

async function initLoginPage() {
  const form = document.querySelector('#login-form');
  if (!form) return;

  await requireGuestPage(document.body.dataset.page);

  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#login-feedback');
    const data = new FormData(form);
    try {
      const payload = await requestJson('/api/login', {
        method: 'POST',
        body: JSON.stringify({
          username: data.get('username'),
          password: data.get('password')
        })
      });

      const page = document.body.dataset.page;
      if (payload.role === 'ADMIN' && page !== 'admin-login') {
        showMessage('#login-feedback', 'Please use the admin login page to sign in as an admin.', 'error');
        return;
      }

      if (payload.role === 'ADMIN') {
        window.location.href = '/admin/dashboard.html';
      } else {
        window.location.href = '/equipment.html';
      }
    } catch (error) {
      showMessage('#login-feedback', error.message, 'error');
    }
  });
}


async function initEquipmentPage() {
  const user = await initAuthenticatedPage(['USER', 'STUDENT']);
  if (!user) return;
  buildNavLogout();
  const feedback = '#equipment-feedback';
  const list = document.querySelector('#equipment-list');
  const searchInput = document.querySelector('#search-input');
  const button = document.querySelector('#search-button');

  if (!list) return;

  async function loadItems(search = '', category = '') {
    hideMessage(feedback);
    showSkeletonLoader('equipment-list', 3);
    try {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (category) params.append('category', category);
      const query = params.toString() ? `?${params.toString()}` : '';
      const payload = await requestJson(`/api/equipment${query}`);
      list.innerHTML = payload.equipment.map(item => `
        <article class="equipment-card">
          <div class="card-header">
            <h2>${item.name}</h2>
            <span class="status-badge status-${item.status.toLowerCase()}">${item.status}</span>
          </div>
          <p><strong>Tag:</strong> ${item.asset_tag}</p>
          <p><strong>Category:</strong> ${item.category}</p>
          <p>${item.description || 'No description provided.'}</p>
          <p><strong>Available:</strong> ${item.available_quantity}</p>
          <div class="reserve-row">
            <label>
              Quantity
              <input type="number" min="1" max="${item.available_quantity}" value="1" data-quantity-for="${item.equipment_id}" class="quantity-input">
            </label>
            <button data-equipment-id="${item.equipment_id}">Request Reservation</button>
          </div>
        </article>
      `).join('');

      list.querySelectorAll('button[data-equipment-id]').forEach(button => {
        button.addEventListener('click', async event => {
          const equipmentId = event.target.dataset.equipmentId;
          const qtyInput = list.querySelector(`input[data-quantity-for="${equipmentId}"]`);
          const requested = Number(qtyInput?.value || 1);
          const quantity = Math.max(1, Math.min(requested, Number(qtyInput?.max || 1)));
          try {
            await requestJson('/api/reservations', {
              method: 'POST',
              body: JSON.stringify({ equipmentId, quantity })
            });
            showMessage(feedback, 'Reservation request sent.', 'success');
          } catch (error) {
            showMessage(feedback, error.message, 'error');
          }
        });
      });
    } catch (error) {
      showMessage(feedback, error.message, 'error');
    }
  }

  const categoryFilter = document.querySelector('#category-filter');
  const clearFilters = document.querySelector('#clear-filters');

  button?.addEventListener('click', () => loadItems(searchInput.value, categoryFilter.value));
  clearFilters?.addEventListener('click', () => {
    searchInput.value = '';
    categoryFilter.value = '';
    loadItems();
  });
  
  // Allow Enter key to trigger search
  searchInput?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      loadItems(searchInput.value, categoryFilter.value);
    }
  });

  await loadItems();
}

async function initCurrentLoansPage() {
  const user = await initAuthenticatedPage(['USER', 'STUDENT']);
  if (!user) return;
  buildNavLogout();
  const list = document.querySelector('#current-loans-list');
  const feedback = '#current-loans-feedback';
  if (!list) return;

  try {
    const payload = await requestJson('/api/reservations/current');
    if (payload.reservations.length === 0) {
      list.innerHTML = '<p>No active borrowings found.</p>';
      return;
    }
    list.innerHTML = payload.reservations.map(item => `
      <article class="borrow-card">
        <h2>${item.name}</h2>
        <p><strong>Tag:</strong> ${item.asset_tag}</p>
        <p><strong>Status:</strong> ${item.status}</p>
        <p><strong>Due date:</strong> ${item.due_date || 'Not assigned yet'}</p>
      </article>
    `).join('');
  } catch (error) {
    showMessage(feedback, error.message, 'error');
  }
}

async function initHistoryPage() {
  const user = await initAuthenticatedPage(['USER', 'STUDENT']);
  if (!user) return;
  buildNavLogout();
  const list = document.querySelector('#history-list');
  const feedback = '#history-feedback';
  if (!list) return;

  async function loadHistory() {
    hideMessage(feedback);
    try {
      const payload = await requestJson('/api/reservations/history');
      if (payload.reservations.length === 0) {
        list.innerHTML = '<p>No reservation history found.</p>';
        return;
      }

      list.innerHTML = payload.reservations.map(item => `
        <article class="history-card">
          <h2>${item.name}</h2>
          <p><strong>Status:</strong> ${item.status}</p>
          <p><strong>Requested:</strong> ${new Date(item.request_date).toLocaleString()}</p>
          <p><strong>Due:</strong> ${item.due_date || 'N/A'}</p>
          <p><strong>Remarks:</strong> ${item.remarks || 'None'}</p>
          ${item.status === 'PENDING' ? `<button class="cancel-button" data-cancel-id="${item.reservation_id}">Cancel request</button>` : ''}
        </article>
      `).join('');

      list.querySelectorAll('button[data-cancel-id]').forEach(button => {
        button.addEventListener('click', async event => {
          const reservationId = event.target.dataset.cancelId;
          try {
            await requestJson(`/api/reservations/${reservationId}`, { method: 'DELETE' });
            showMessage(feedback, 'Reservation canceled.', 'success');
            await loadHistory();
          } catch (error) {
            showMessage(feedback, error.message, 'error');
          }
        });
      });
    } catch (error) {
      showMessage(feedback, error.message, 'error');
    }
  }

  await loadHistory();
}

async function initPendingRequestsPage() {
  const user = await initAuthenticatedPage(['USER', 'STUDENT']);
  if (!user) return;
  buildNavLogout();
  const list = document.querySelector('#pending-list');
  const feedback = '#pending-feedback';
  if (!list) return;

  async function loadPending() {
    hideMessage(feedback);
    try {
      const payload = await requestJson('/api/reservations/pending');
      if (payload.reservations.length === 0) {
        list.innerHTML = '<p>No pending requests found.</p>';
        return;
      }

      list.innerHTML = payload.reservations.map(item => `
        <article class="history-card">
          <div class="card-header">
            <h2>${item.name}</h2>
            <span class="status-badge status-pending">${item.status}</span>
          </div>
          <p><strong>Tag:</strong> ${item.asset_tag}</p>
          <p><strong>Quantity:</strong> ${item.quantity}</p>
          <p><strong>Requested:</strong> ${new Date(item.request_date).toLocaleString()}</p>
          <p><strong>Remarks:</strong> ${item.remarks || 'None'}</p>
          <button class="cancel-button" data-cancel-id="${item.reservation_id}">Cancel request</button>
        </article>
      `).join('');

      list.querySelectorAll('button[data-cancel-id]').forEach(button => {
        button.addEventListener('click', async event => {
          const reservationId = event.target.dataset.cancelId;
          try {
            await requestJson(`/api/reservations/${reservationId}`, { method: 'DELETE' });
            showMessage(feedback, 'Reservation canceled.', 'success');
            await loadPending();
          } catch (error) {
            showMessage(feedback, error.message, 'error');
          }
        });
      });
    } catch (error) {
      showMessage(feedback, error.message, 'error');
    }
  }

  await loadPending();
}

async function initAccountPage() {
  const user = await initAuthenticatedPage(['USER', 'STUDENT', 'ADMIN']);
  if (!user) return;
  const info = document.querySelector('#user-info');
  const form = document.querySelector('#change-password-form');
  const logoutButton = document.querySelector('#logout-account-button');
  const feedback = '#account-feedback';

  if (logoutButton) {
    logoutButton.addEventListener('click', event => {
      event.preventDefault();
      logout();
    });
  }

  try {
    const payload = await requestJson('/api/user');
    const currentUser = payload.user;
    if (!currentUser) {
      window.location.href = '/login.html';
      return;
    }
    info.innerHTML = `
      <p><strong>Name:</strong> ${currentUser.fullName}</p>
      <p><strong>Email:</strong> ${currentUser.email}</p>
      <p><strong>User ID:</strong> ${currentUser.username}</p>
      <p><strong>Role:</strong> ${currentUser.role}</p>
    `;
    // Load user-specific overdue incidents and show penalty info
    try {
      const overPayload = await requestJson('/api/overdues');
      if (overPayload.overdues && overPayload.overdues.length > 0) {
        const listHtml = overPayload.overdues.map(o => `
          <div class="overdue-item">
            <p><strong>Reservation:</strong> ${o.reservation_id} — <strong>Equipment:</strong> ${o.equipment_id}</p>
            <p><strong>Days late:</strong> ${o.days_late} — <strong>Penalty days:</strong> ${o.penalty_days}</p>
            <p><strong>Penalty end:</strong> ${o.penalty_end_date ? new Date(o.penalty_end_date).toLocaleDateString() : 'None'}</p>
            <p><strong>Settled:</strong> ${o.settled ? 'Yes' : 'No'}</p>
          </div>
        `).join('');
        const section = document.createElement('section');
        section.className = 'admin-card';
        section.innerHTML = `<h2>Overdue Records</h2>${listHtml}`;
        info.parentNode.insertBefore(section, info.nextSibling);
      }
    } catch (e) {
      // ignore if endpoint unavailable
    }
  } catch (error) {
    showMessage(feedback, error.message, 'error');
  }

  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage(feedback);
    const data = new FormData(form);
    try {
      await requestJson('/api/change-password', {
        method: 'POST',
        body: JSON.stringify({
          currentPassword: data.get('currentPassword'),
          newPassword: data.get('newPassword')
        })
      });
      showMessage(feedback, 'Password updated successfully.', 'success');
      form.reset();
    } catch (error) {
      showMessage(feedback, error.message, 'error');
    }
  });
}

async function initAdminPage() {
  const user = await initAuthenticatedPage(['ADMIN']);
  if (!user) return;
  buildNavLogout();
  const form = document.querySelector('#create-user-form');
  if (!form) return;

  form.addEventListener('submit', async event => {
    event.preventDefault();
    hideMessage('#admin-feedback');
    const data = new FormData(form);
    try {
      await requestJson('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify({
          firstName: data.get('firstName'),
          middleName: data.get('middleName'),
          lastName: data.get('lastName'),
          suffix: data.get('suffix'),
          userId: data.get('userId'),
          phoneNumber: data.get('phoneNumber'),
          department: data.get('department'),
          course: data.get('course'),
          yearLevel: data.get('yearLevel'),
          block: data.get('block'),
          password: data.get('password')
        })
      });
      showMessage('#admin-feedback', 'User account created successfully.', 'success');
      form.reset();
    } catch (error) {
      showMessage('#admin-feedback', error.message, 'error');
    }
  });
}

async function runPage() {
  const page = document.body.dataset.page;

  if (page === 'login' || page === 'admin-login') {
    return initLoginPage();
  }
  if (page === 'admin') {
    return initAdminPage();
  }

  if (['equipment', 'pending-requests', 'current-loans', 'history', 'account'].includes(page)) {
    await initAuthenticatedPage();
    if (page === 'equipment') return initEquipmentPage();
    if (page === 'pending-requests') return initPendingRequestsPage();
    if (page === 'current-loans') return initCurrentLoansPage();
    if (page === 'history') return initHistoryPage();
    if (page === 'account') return initAccountPage();
  }
}

runPage().catch(error => {
  console.error(error);
});
