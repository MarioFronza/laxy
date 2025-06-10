document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('themeToggle');
    const stored = localStorage.getItem('theme');
    if (stored === 'light') {
        document.body.classList.add('light');
    }
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            document.body.classList.toggle('light');
            const mode = document.body.classList.contains('light') ? 'light' : 'dark';
            localStorage.setItem('theme', mode);
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
