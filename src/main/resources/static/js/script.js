document.addEventListener("DOMContentLoaded", () => {
    const menuToggle = document.getElementById("menuToggle");
    const navLinks = document.getElementById("navLinks");

    if(menuToggle) {
        menuToggle.addEventListener("click", () => {
            navLinks.classList.toggle("active");
        })
    }
})
