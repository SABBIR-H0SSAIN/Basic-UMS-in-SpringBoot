// Shared Fetch helper. The browser sends the session cookie with each request.
async function apiRequest(url, method = "GET", body = null) {
    const options = { method: method, credentials: "same-origin", headers: {} };

    // Spring Security requires a CSRF token for requests that change data.
    // Fetching a fresh token also handles token rotation after login.
    if (method !== "GET") {
        const csrfResponse = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!csrfResponse.ok) {
            throw new Error("Could not obtain a security token. Please reload the page.");
        }
        const csrf = await csrfResponse.json();
        options.headers[csrf.headerName] = csrf.token;
    }
    if (body !== null) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);
    const data = response.status === 204 ? null : await response.json();
    if (!response.ok) {
        if (response.status === 401 && window.location.pathname !== "/login") {
            window.location.replace("/login");
        }
        throw new Error(data.message || "The request failed. Please try again.");
    }
    return data;
}

function showMessage(message) {
    document.getElementById("message").textContent = message;
}

// Read the role from the server session, rather than trusting browser storage.
async function loadCurrentUser() {
    const user = await apiRequest("/api/auth/me");
    const navigation = document.getElementById("navigation");
    addNavigationLink(navigation, "Dashboard", "/dashboard");
    if (user.role === "ADMIN") {
        addNavigationLink(navigation, "Admins", "/admins");
    }
    if (user.role === "ADMIN" || user.role === "TEACHER") {
        addNavigationLink(navigation, "Teachers", "/teachers");
    }
    if (user.role === "ADMIN" || user.role === "STUDENT") {
        addNavigationLink(navigation, "Students", "/students");
    }

    const logoutButton = document.createElement("button");
    logoutButton.type = "button";
    logoutButton.textContent = "Logout";
    logoutButton.addEventListener("click", async function () {
        try {
            await apiRequest("/api/auth/logout", "POST");
            window.location.replace("/login");
        } catch (error) {
            showMessage(error.message);
        }
    });
    navigation.appendChild(logoutButton);
    return user;
}

function addNavigationLink(navigation, text, url) {
    const link = document.createElement("a");
    link.textContent = text;
    link.href = url;
    if (window.location.pathname === url) {
        link.setAttribute("aria-current", "page");
    }
    navigation.appendChild(link);
}
