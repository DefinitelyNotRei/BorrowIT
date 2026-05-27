async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    ...options
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.message || 'Request failed');
  }
  return body;
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

async function requireGuestPage() {
  const user = await getCurrentUser();
  if (user) {
    if (user.role === 'ADMIN') {
      window.location.href = '/admin.html';
    } else {
      window.location.href = '/equipment.html';
    }
  }
}

async function initAuthenticatedPage(allowedRoles = ['USER', 'STUDENT']) {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = '/login.html';
    return null;
  }
  if (!allowedRoles.includes(user.role)) {
    if (user.role === 'ADMIN') {
      window.location.href = '/admin.html';
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

  await requireGuestPage();

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
      if (payload.role === 'ADMIN') {
        window.location.href = '/admin.html';
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

  async function loadItems(search = '') {
    hideMessage(feedback);
    try {
      const query = search ? `?search=${encodeURIComponent(search)}` : '';
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

  button?.addEventListener('click', () => loadItems(searchInput.value));
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
      <article class="loan-card">
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
  buildNavLogout();
  const info = document.querySelector('#user-info');
  const form = document.querySelector('#change-password-form');
  const feedback = '#account-feedback';

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

  if (page === 'login') {
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
