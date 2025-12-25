// Protect route
redirectIfNotAuthenticated();

let currentTasks = [];
let currentFilter = 'all';

// ==================== LOAD USER INFO ====================
function loadUserInfo() {
    const user = getUser();
    if (user) {
        document.getElementById('userName').textContent = user.username;
        document.getElementById('userEmail').textContent = user.role;
    }
}

// ==================== LOGOUT ====================
function logout() {
    storage.clear();
    showNotification('Logged out successfully', 'success');
    setTimeout(() => {
        window.location.href = '../pages/login.html';
    }, 1000);
}

// ==================== TOGGLE SIDEBAR ====================
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('active');
}

// ==================== LOAD TASKS ====================
async function loadTasks() {
    try {
        const tasks = await taskAPI.getAllTasks();
        currentTasks = tasks;
        updateStats(tasks);
        displayTasks(tasks);
    } catch (error) {
        console.error('Error loading tasks:', error);
        showNotification('Error loading tasks', 'error');
    }
}

// ==================== UPDATE STATS ====================
function updateStats(tasks) {
    const total = tasks.length;
    const todo = tasks.filter(t => t.status === 'TODO').length;
    const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const done = tasks.filter(t => t.status === 'DONE').length;

    document.getElementById('totalTasks').textContent = total;
    document.getElementById('pendingTasks').textContent = todo;
    document.getElementById('inProgressTasks').textContent = inProgress;
    document.getElementById('completedTasks').textContent = done;

    document.getElementById('allCount').textContent = total;
    document.getElementById('todoCount').textContent = todo;
    document.getElementById('progressCount').textContent = inProgress;
    document.getElementById('doneCount').textContent = done;
}

// ==================== DISPLAY TASKS ====================
function displayTasks(tasks) {
    const tasksGrid = document.getElementById('tasksGrid');
    const emptyState = document.getElementById('emptyState');

    if (tasks.length === 0) {
        tasksGrid.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    tasksGrid.style.display = 'grid';
    emptyState.style.display = 'none';

    tasksGrid.innerHTML = tasks.map(task => `
        <div class="task-card status-${task.status}">
            <div class="task-header">
                <div>
                    <div class="task-title">${task.title}</div>
                    <span class="task-status ${task.status}">${task.status.replace('_', ' ')}</span>
                </div>
            </div>
            <div class="task-description">${task.description || 'No description'}</div>
            <div class="task-footer">
                <div class="task-date">
                    <i class="fas fa-clock"></i>
                    ${formatDate(task.createdAt)}
                </div>
                <div class="task-actions">
                    <button class="task-action-btn edit" onclick="editTask(${task.id})" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="task-action-btn delete" onclick="deleteTask(${task.id})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// ==================== FILTER TASKS ====================
function filterTasks(status) {
    currentFilter = status;

    // Update active nav item
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    event.target.closest('.nav-item').classList.add('active');

    applyFilter();
}

function applyFilter() {
    const selectFilter = document.getElementById('filterSelect').value;
    const filter = selectFilter || currentFilter;

    let filteredTasks = currentTasks;

    if (filter !== 'all') {
        filteredTasks = currentTasks.filter(task => task.status === filter);
    }

    displayTasks(filteredTasks);
}

// ==================== TASK MODAL ====================
function openTaskModal(taskId = null) {
    const modal = document.getElementById('taskModal');
    const modalTitle = document.getElementById('modalTitle');
    const form = document.getElementById('taskForm');

    form.reset();
    document.getElementById('taskId').value = '';

    if (taskId) {
        modalTitle.textContent = 'Edit Task';
        const task = currentTasks.find(t => t.id === taskId);
        if (task) {
            document.getElementById('taskId').value = task.id;
            document.getElementById('taskTitle').value = task.title;
            document.getElementById('taskDescription').value = task.description || '';
            document.getElementById('taskStatus').value = task.status;
        }
    } else {
        modalTitle.textContent = 'Create New Task';
    }

    modal.classList.add('active');
}

function closeTaskModal() {
    const modal = document.getElementById('taskModal');
    modal.classList.remove('active');
}

// Click outside to close
document.getElementById('taskModal')?.addEventListener('click', (e) => {
    if (e.target.id === 'taskModal') {
        closeTaskModal();
    }
});

// ==================== SAVE TASK ====================
document.getElementById('taskForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const taskId = document.getElementById('taskId').value;
    const title = document.getElementById('taskTitle').value;
    const description = document.getElementById('taskDescription').value;
    const status = document.getElementById('taskStatus').value;
    const submitBtn = document.getElementById('saveTaskBtn');

    const taskData = { title, description, status };

    setButtonLoading(submitBtn, true);

    try {
        if (taskId) {
            await taskAPI.updateTask(taskId, taskData);
            showNotification('Task updated successfully', 'success');
        } else {
            await taskAPI.createTask(taskData);
            showNotification('Task created successfully', 'success');
        }

        closeTaskModal();
        loadTasks();

    } catch (error) {
        console.error('Error saving task:', error);
        showNotification('Error saving task', 'error');
    } finally {
        setButtonLoading(submitBtn, false);
    }
});

// ==================== EDIT TASK ====================
function editTask(taskId) {
    openTaskModal(taskId);
}

// ==================== DELETE TASK ====================
async function deleteTask(taskId) {
    if (!confirm('Are you sure you want to delete this task?')) {
        return;
    }

    try {
        await taskAPI.deleteTask(taskId);
        showNotification('Task deleted successfully', 'success');
        loadTasks();
    } catch (error) {
        console.error('Error deleting task:', error);
        showNotification('Error deleting task', 'error');
    }
}

// ==================== INITIALIZE ====================
document.addEventListener('DOMContentLoaded', () => {
    loadUserInfo();
    loadTasks();
});