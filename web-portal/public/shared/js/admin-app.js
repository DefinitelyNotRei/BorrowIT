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

function formatDate(value) {
  if (!value) return 'Not assigned';
  return new Date(value).toLocaleDateString();
}

async function initAdminDashboardPage() {
  const data = await requestJson('/api/admin/equipment');
  const approvals = await requestJson('/api/admin/approvals');

  document.querySelector('#summary-equipment-count').textContent = data.equipment.length;
  document.querySelector('#summary-pending-count').textContent = approvals.approvals.length;
  document.querySelector('#summary-active-count').textContent = 'N/A';
  document.querySelector('#summary-overdue-count').textContent = 'N/A';

  const container = document.querySelector('#recent-approvals');
  if (!container) return;
  container.innerHTML = approvals.approvals.length === 0
    ? '<p>No pending approvals at the moment.</p>'
    : `<table class="admin-table"><thead><tr><th>Equipment</th><th>User</th><th>Quantity</th><th>Requested</th><th>Action</th></tr></thead><tbody>${approvals.approvals.slice(0, 5).map(item => `
      <tr>
        <td>${item.equipment_name} (${item.asset_tag})</td>
        <td>${item.user_name} (${item.user_id})</td>
        <td>${item.quantity}</td>
        <td>${formatDate(item.request_date)}</td>
        <td><a href="/admin/approvals.html" class="link-button">Review</a></td>
      </tr>
    `).join('')}</tbody></table>`;
}

async function initAdminEquipmentPage() {
  const list = document.querySelector('#equipment-list');
  const form = document.querySelector('#equipment-form');

  async function refreshEquipment() {
    if (!list) return;
    const payload = await requestJson('/api/admin/equipment');
    if (payload.equipment.length === 0) {
      list.innerHTML = '<p>No equipment records found.</p>';
      return;
    }
    list.innerHTML = `
      <table class="admin-table"><thead><tr><th>Name</th><th>Tag</th><th>Category</th><th>Status</th><th>Available</th></tr></thead><tbody>${payload.equipment.map(item => `
        <tr>
          <td>${item.name}</td>
          <td>${item.asset_tag}</td>
          <td>${item.category}</td>
          <td>${item.status}</td>
          <td>${item.available_quantity}/${item.total_quantity}</td>
        </tr>
      `).join('')}</tbody></table>`;
  }

  form?.addEventListener('submit', async event => {
    event.preventDefault();
    const feedback = '#equipment-feedback';
    const element = document.querySelector(feedback);
    if (!element) return;
    element.textContent = '';
    element.classList.remove('visible', 'error', 'success');
    const data = new FormData(form);
    try {
      await requestJson('/api/admin/equipment', {
        method: 'POST',
        body: JSON.stringify({
          name: data.get('name'),
          assetTag: data.get('assetTag'),
          category: data.get('category'),
          description: data.get('description'),
          totalQuantity: data.get('totalQuantity'),
          status: data.get('status')
        })
      });
      element.textContent = 'Equipment item created successfully.';
      element.classList.add('visible', 'success');
      form.reset();
      await refreshEquipment();
    } catch (error) {
      element.textContent = error.message;
      element.classList.add('visible', 'error');
    }
  });

  await refreshEquipment();
}

async function initAdminUsersPage() {
  const container = document.querySelector('#user-list');
  if (!container) return;
  const payload = await requestJson('/api/admin/users');
  if (payload.users.length === 0) {
    container.innerHTML = '<p>No user profiles available.</p>';
    return;
  }
  container.innerHTML = `
    <table class="admin-table"><thead><tr><th>Name</th><th>ID</th><th>Email</th><th>Status</th><th>Role</th></tr></thead>
    <tbody>${payload.users.map(user => `
      <tr>
        <td>${user.full_name}</td>
        <td>${user.username}</td>
        <td>${user.email}</td>
        <td>${user.is_active ? 'Active' : 'Inactive'}</td>
        <td>${user.role}</td>
      </tr>
    `).join('')}</tbody></table>`;
}

async function initAdminApprovalsPage() {
  const container = document.querySelector('#approval-list');
  if (!container) return;
  const payload = await requestJson('/api/admin/approvals');
  if (payload.approvals.length === 0) {
    container.innerHTML = '<p>No pending requests to review.</p>';
    return;
  }
  container.innerHTML = `
    <table class="admin-table"><thead><tr><th>Equipment</th><th>User</th><th>Quantity</th><th>Requested</th><th>Due Date</th><th>Actions</th></tr></thead>
    <tbody>${payload.approvals.map(item => `
      <tr>
        <td>${item.equipment_name} (${item.asset_tag})</td>
        <td>${item.user_name}</td>
        <td>${item.quantity}</td>
        <td>${formatDate(item.request_date)}</td>
        <td>${formatDate(item.due_date)}</td>
        <td class="admin-actions">
          <button data-action="approve" data-id="${item.reservation_id}">Approve</button>
          <button data-action="decline" data-id="${item.reservation_id}">Decline</button>
        </td>
      </tr>
    `).join('')}</tbody></table>`;

  container.querySelectorAll('button[data-id]').forEach(button => {
    button.addEventListener('click', async event => {
      const reservationId = event.target.dataset.id;
      const action = event.target.dataset.action;
      try {
        await requestJson(`/api/admin/approvals/${reservationId}`, {
          method: 'PUT',
          body: JSON.stringify({ action })
        });
        await initAdminApprovalsPage();
      } catch (error) {
        alert(error.message);
      }
    });
  });
}

async function initAdminReportsPage() {
  const active = document.querySelector('#report-active');
  const approved = document.querySelector('#report-approved');
  const overdue = document.querySelector('#report-overdue');
  if (!active || !approved || !overdue) return;

  const pending = await requestJson('/api/admin/approvals');
  active.textContent = `${pending.approvals.length} pending requests`;
  approved.textContent = 'Use the approvals page to view approved reservations.';
  overdue.textContent = 'Overdue reporting is pending implementation.';
}

async function runAdminPage() {
  const page = document.body.dataset.page;
  if (page === 'admin-dashboard') {
    return initAdminDashboardPage();
  }
  if (page === 'admin-equipment') {
    return initAdminEquipmentPage();
  }
  if (page === 'admin-users') {
    return initAdminUsersPage();
  }
  if (page === 'admin-approvals') {
    return initAdminApprovalsPage();
  }
  if (page === 'admin-reports') {
    return initAdminReportsPage();
  }
}

runAdminPage().catch(error => {
  console.error(error);
});
