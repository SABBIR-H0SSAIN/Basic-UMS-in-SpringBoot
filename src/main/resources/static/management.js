const resource = document.body.dataset.resource;
const apiUrl = "/api/" + resource;
const form = document.getElementById("account-form");
let editingId = null;

async function startManagementPage() {
    try {
        const user = await loadCurrentUser();
        const ownResource = user.role.toLowerCase() + "s";
        if (user.role !== "ADMIN" && resource !== ownResource) {
            showMessage("You do not have permission to manage these records.");
            return;
        }
        document.getElementById("management").hidden = false;
        await loadRecords();
    } catch (error) {
        showMessage(error.message);
    }
}

async function loadRecords() {
    const records = await apiRequest(apiUrl);
    const tableBody = document.getElementById("records");
    tableBody.replaceChildren();
    if (records.length === 0) {
        const row = tableBody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = resource === "students" ? 8 : (resource === "teachers" ? 6 : 5);
        cell.textContent = "No records found.";
    }
    for (const record of records) {
        const row = tableBody.insertRow();
        for (const value of [record.id, record.username, record.name, record.email]) {
            row.insertCell().textContent = value;
        }
        if (resource === "students") {
            row.insertCell().textContent = record.year ?? "—";
            row.insertCell().textContent = record.semester == null ? "—" : (record.semester === 1 ? "1st" : "2nd");
            row.insertCell().textContent = record.cgpa == null ? "—" : Number(record.cgpa).toFixed(2);
        }
        if (resource === "teachers") {
            row.insertCell().textContent = record.department ?? "—";
        }
        const actions = row.insertCell();
        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.textContent = "Edit";
        editButton.addEventListener("click", function () { editRecord(record); });
        actions.appendChild(editButton);

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.textContent = "Delete";
        deleteButton.addEventListener("click", function () { deleteRecord(record); });
        actions.appendChild(deleteButton);
    }
}

function editRecord(record) {
    editingId = record.id;
    form.elements.username.value = record.username;
    form.elements.name.value = record.name;
    form.elements.email.value = record.email;
    if (resource === "students") {
        form.elements.year.value = record.year ?? "";
        form.elements.semester.value = record.semester ?? "";
        form.elements.cgpa.value = record.cgpa ?? "";
    }
    if (resource === "teachers") {
        form.elements.department.value = record.department ?? "";
    }
    form.elements.password.value = "";
    document.getElementById("form-heading").textContent = "Edit record #" + record.id;
    document.getElementById("save-button").textContent = "Save changes";
    document.getElementById("cancel-button").hidden = false;
    showMessage("");
    form.elements.username.focus();
}

function resetForm() {
    editingId = null;
    form.reset();
    document.getElementById("form-heading").textContent = "Add record";
    document.getElementById("save-button").textContent = "Add";
    document.getElementById("cancel-button").hidden = true;
}

document.getElementById("cancel-button").addEventListener("click", resetForm);

form.addEventListener("submit", async function (event) {
    event.preventDefault();
    const button = document.getElementById("save-button");
    button.disabled = true;
    showMessage("");
    const body = {
        username: form.elements.username.value,
        name: form.elements.name.value,
        email: form.elements.email.value,
        password: form.elements.password.value
    };
    if (resource === "students") {
        body.year = form.elements.year.value === "" ? null : Number(form.elements.year.value);
        body.semester = form.elements.semester.value === "" ? null : Number(form.elements.semester.value);
        body.cgpa = form.elements.cgpa.value === "" ? null : Number(form.elements.cgpa.value);
    }
    if (resource === "teachers") {
        body.department = form.elements.department.value;
    }
    try {
        if (editingId === null) {
            await apiRequest(apiUrl, "POST", body);
        } else {
            await apiRequest(apiUrl + "/" + editingId, "PUT", body);
        }
        resetForm();
        await loadRecords();
        showMessage("Record saved successfully.");
    } catch (error) {
        showMessage(error.message);
    } finally {
        button.disabled = false;
    }
});

async function deleteRecord(record) {
    if (!window.confirm("Delete " + record.username + "?")) {
        return;
    }
    try {
        await apiRequest(apiUrl + "/" + record.id, "DELETE");
        if (editingId === record.id) {
            resetForm();
        }
        await loadRecords();
        showMessage("Record deleted successfully.");
    } catch (error) {
        showMessage(error.message);
    }
}

startManagementPage();
