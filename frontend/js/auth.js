// Check if already authenticated
if (window.location.pathname.includes('login') || window.location.pathname.includes('register')) {
    redirectIfAuthenticated();
}

// ==================== LOGIN ====================
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const submitBtn = document.getElementById('loginBtn');

        setButtonLoading(submitBtn, true);

        try {
            const response = await authAPI.login({ username, password });

            // Save token and user info
            storage.set('token', response.token);
            storage.set('user', {
                username: response.username,
                role: response.role
            });

            showNotification('Login successful!', 'success');

            // Redirect to tasks page
            setTimeout(() => {
                window.location.href = 'tasks.html';
            }, 1000);

        } catch (error) {
            displayError(error.message || 'Login failed. Please try again.');
            setButtonLoading(submitBtn, false);
        }
    });
}

// ==================== REGISTER ====================
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    const passwordInput = document.getElementById('password');

    // Password strength indicator
    if (passwordInput) {
        passwordInput.addEventListener('input', (e) => {
            updatePasswordStrength(e.target.value);
        });
    }

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();

        const username = document.getElementById('username').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const submitBtn = document.getElementById('registerBtn');

        setButtonLoading(submitBtn, true);

        try {
            const response = await authAPI.register({
                username,
                email,
                password
            });

            // Save token and user info
            storage.set('token', response.token);
            storage.set('user', {
                username: response.username,
                role: response.role
            });

            showNotification('Registration successful!', 'success');

            // Redirect to tasks page
            setTimeout(() => {
                window.location.href = 'tasks.html';
            }, 1000);

        } catch (error) {
            if (error.errors) {
                displayError(error.errors);
            } else {
                displayError(error.message || 'Registration failed. Please try again.');
            }
            setButtonLoading(submitBtn, false);
        }
    });
}