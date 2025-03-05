document.addEventListener("DOMContentLoaded", function () {
    const forms = document.querySelectorAll("form");

    forms.forEach(form => {
        form.addEventListener("submit", function (event) {
            const inputs = form.querySelectorAll("input[required]");
            let isValid = true;

            inputs.forEach(input => {
                if (!input.value.trim()) {
                    isValid = false;
                    input.style.border = "1px solid red";
                } else {
                    input.style.border = "1px solid #ccc";
                }
            });

            if (!isValid) {
                event.preventDefault();
                alert("Please fill in all required fields.");
            }
        });
    });
});
