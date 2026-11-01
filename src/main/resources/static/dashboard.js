async function showDashboard() {
    try {
        const user = await loadCurrentUser();
        document.getElementById("welcome").textContent =
            "Welcome, " + user.username + "! Your role is " + user.role + ".";
    } catch (error) {
        showMessage(error.message);
    }
}

showDashboard();
