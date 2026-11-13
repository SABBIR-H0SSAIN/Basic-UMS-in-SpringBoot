document.getElementById("login-form").addEventListener("submit", async function (event) {
    event.preventDefault();
    const button = this.querySelector("button");
    button.disabled = true;
    showMessage("");
    try {
        await apiRequest("/api/auth/login", "POST", {
            username: this.elements.username.value,
            password: this.elements.password.value
        });
        this.reset();
        window.location.replace("/dashboard");
    } catch (error) {
        showMessage(error.message);
    } finally {
        button.disabled = false;
    }
});
