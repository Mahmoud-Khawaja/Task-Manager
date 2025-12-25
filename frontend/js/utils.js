// ==================== LOCAL STORAGE ====================
const storage = {
    set: (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.error('Error saving to localStorage:', error);
        }
    },

    get: (key) => {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : null;
        } catch (error) {
            console.error('Error reading from localStorage:', error);
            return null;
        }
    },

    remove: (key) => {
        try {
            localStorage.removeItem(key);
        } catch (error) {
            console.error('Error removing from localStorage:', error);
        }
    },

    clear: () => {
        try {
            localStorage.clear();
        } catch (error) {
            console.error('Error clearing localStorage:', error);
        }
    }
};

// ==================== AUTH HELPERS ====================
function isAuthenticated() {
    return !!storage.get('token');
}

function getUser() {
    return storage.get('user');
}

function getToken() {
    return storage.get('token');
}

function redirectIfNotAuthenticated() {
    if (!isAuthenticated()) {
        window.location.href = '../pages/login.html';
    }
}

function redirectIfAuthenticated() {
    if (isAuthenticated()) {
        window.location.href = '../pages/tasks.html';
    }
}

// ==================== NOTIFICATIONS ====================
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
    `;

    // Add to body
    document.body.appendChild(notification);

    // Trigger animation
    setTimeout(() => notification.classList.add('show'), 10);

    // Remove after 3 seconds
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// ==================== PASSWORD VISIBILITY TOGGLE ====================
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.toggle-password');
    const icon = button.querySelector('i');

    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}

// ==================== PASSWORD STRENGTH ====================
function checkPasswordStrength(password) {
    let strength = 0;

    if (password.length >= 8) strength++;
    if (password.match(/[a-z]/)) strength++;
    if (password.match(/[A-Z]/)) strength++;
    if (password.match(/[0-9]/)) strength++;
    if (password.match(/[^a-zA-Z0-9]/)) strength++;

    return strength;
}

function updatePasswordStrength(password) {
    const strengthBar = document.getElementById('passwordStrength');
    if (!strengthBar) return;

    const strength = checkPasswordStrength(password);

    strengthBar.className = 'password-strength';

    if (strength <= 2) {
        strengthBar.classList.add('strength-weak');
    } else if (strength <= 4) {
        strengthBar.classList.add('strength-medium');
    } else {
        strengthBar.classList.add('strength-strong');
    }
}

// ==================== DATE FORMATTING ====================
function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
        return 'Today';
    } else if (diffDays === 1) {
        return 'Yesterday';
    } else if (diffDays < 7) {
        return `${diffDays} days ago`;
    } else {
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }
}

// ==================== LOADING STATES ====================
function setButtonLoading(button, loading) {
    const textSpan = button.querySelector('.btn-text');
    const loadingSpan = button.querySelector('.btn-loading');

    if (loading) {
        textSpan.style.display = 'none';
        loadingSpan.style.display = 'flex';
        button.disabled = true;
    } else {
        textSpan.style.display = 'block';
        loadingSpan.style.display = 'none';
        button.disabled = false;
    }
}

// ==================== ERROR DISPLAY ====================
function displayError(message, elementId = 'errorMessage') {
    const errorElement = document.getElementById(elementId);
    if (!errorElement) return;

    if (typeof message === 'object') {
        // Handle validation errors
        let errorHTML = '<ul>';
        for (const [field, error] of Object.entries(message)) {
            errorHTML += `<li>${error}</li>`;
        }
        errorHTML += '</ul>';
        errorElement.innerHTML = errorHTML;
    } else {
        errorElement.textContent = message;
    }

    errorElement.style.display = 'block';

    // Scroll to error
    errorElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideError(elementId = 'errorMessage') {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.style.display = 'none';
    }
}