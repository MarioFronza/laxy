document.addEventListener("DOMContentLoaded", () => {
    const menuToggle = document.getElementById("menuToggle");
    const navLinks = document.getElementById("navLinks");
    const themeToggle = document.getElementById("themeToggle");

    if (menuToggle && navLinks) {
        menuToggle.addEventListener("click", () => navLinks.classList.toggle("active"));
    }

    function setTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        try { localStorage.setItem('theme', theme); } catch (_) { /* noop */ }
        if (themeToggle) themeToggle.textContent = theme === 'dark' ? '☀️' : '🌙';
    }

    // Initialize toggle label based on current theme
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    if (themeToggle) themeToggle.textContent = current === 'dark' ? '☀️' : '🌙';

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const now = document.documentElement.getAttribute('data-theme') || 'light';
            setTheme(now === 'dark' ? 'light' : 'dark');
        });
    }
});

function showAlert(message, type = 'success') {
    const alertBox = document.getElementById('alertBox');
    alertBox.textContent = message;
    alertBox.className = `alert-box ${type} show`;
    setTimeout(() => {
        alertBox.classList.remove('show');
        alertBox.classList.add('hidden');
    }, 3000);
}
