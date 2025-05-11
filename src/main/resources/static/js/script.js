document.addEventListener("DOMContentLoaded", () => {
    const menuToggle = document.getElementById("menuToggle");
    const navLinks = document.getElementById("navLinks");

    if(menuToggle) {
        menuToggle.addEventListener("click", () => {
            navLinks.classList.toggle("active");
        })
    }
})

function showAlert(message, type = 'success') {
    const alertBox = document.getElementById('alertBox');
    alertBox.textContent = message;
    alertBox.className = `alert-box ${type} show`;

    setTimeout(() => {
        alertBox.classList.remove('show');
        alertBox.classList.add('hidden');
    }, 3000);
}
