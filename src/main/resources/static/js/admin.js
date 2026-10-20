import {
    cancelAppointment,
    confirmAppointment,
    getAdminAppointments,
    getServices,
    getWeeklyDashboard,
    logoutAdmin,
    updateAdminAppointment,
    updateAppointmentServiceStatus
} from "./api.js";

const appointmentsTableBody = document.getElementById("lista-admin-agendamentos");
const popularServicesList = document.getElementById("lista-servicos-populares");
const message = document.getElementById("mensagem");
const weekTitle = document.getElementById("titulo-semana");
const logoutButton = document.getElementById("btn-sair");

const statusLabels = {
    PENDING: "Pendente",
    CONFIRMED: "Confirmado",
    CANCELED: "Cancelado",
    COMPLETED: "Concluído"
};

const statusClasses = {
    PENDING: "badge-pendente",
    CONFIRMED: "badge-ok",
    COMPLETED: "badge-concluido"
};

const itemStatusOptions = [
    { value: "PENDING", label: "Pendente" },
    { value: "IN_PROGRESS", label: "Em andamento" },
    { value: "COMPLETED", label: "Concluído" }
];

const priceFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

function showMessage(text, isError = false) {
    if (!message) return;

    message.textContent = text;
    message.hidden = false;
    message.classList.toggle("erro", isError);
}

function createButton(text, className, onClick) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.textContent = text;
    button.addEventListener("click", () => onClick(button));
    return button;
}

async function runAppointmentAction(button, action, successMessage) {
    button.disabled = true;

    try {
        await action();
        showMessage(successMessage);
        await loadAdminData();
    } catch (error) {
        button.disabled = false;
        showMessage(
            error instanceof Error ? error.message : "Não foi possível concluir a ação.",
            true
        );
        return false;
    }
    return true;
}

function renderDashboard(dashboard) {
    const weekStart = new Date(`${dashboard.weekStart}T12:00:00`);
    const weekEnd = new Date(`${dashboard.weekEnd}T12:00:00`);
    if (weekTitle) {
        const dateFormatter = new Intl.DateTimeFormat("pt-BR", {
            day: "2-digit",
            month: "2-digit"
        });
        weekTitle.textContent =
            `Desempenho semanal · ${dateFormatter.format(weekStart)} a ${dateFormatter.format(weekEnd)}`;
    }

    document.getElementById("kpi-total").textContent =
        String(dashboard.totalAppointments);
    document.getElementById("kpi-concluidos").textContent =
        String(dashboard.completedAppointments);
    document.getElementById("kpi-cancelados").textContent =
        String(dashboard.canceledAppointments);
    document.getElementById("kpi-faturamento").textContent =
        priceFormatter.format(dashboard.revenue);

    if (!popularServicesList) return;
    popularServicesList.replaceChildren();

    if (dashboard.mostRequestedServices.length === 0) {
        const item = document.createElement("li");
        item.textContent = "Nenhum serviço solicitado nesta semana.";
        popularServicesList.append(item);
        return;
    }

    for (const service of dashboard.mostRequestedServices) {
        const item = document.createElement("li");
        const name = document.createElement("span");
        name.textContent = service.salonServiceName;
        const count = document.createElement("span");
        count.textContent = `${service.requestCount} pedido(s)`;
        item.append(name, count);
        popularServicesList.append(item);
    }
}

function renderAppointments(appointments) {
    if (!appointmentsTableBody) return;
    appointmentsTableBody.replaceChildren();

    if (appointments.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 5;
        cell.textContent = "Nenhum agendamento recebido.";
        row.append(cell);
        appointmentsTableBody.append(row);
        return;
    }

    for (const appointment of appointments) {
        const row = document.createElement("tr");
        const customerCell = document.createElement("td");
        customerCell.textContent = appointment.customerName;

        const dateCell = document.createElement("td");
        dateCell.textContent = new Date(appointment.dateTime).toLocaleString("pt-BR", {
            dateStyle: "short",
            timeStyle: "short"
        });

        const servicesCell = document.createElement("td");
        const servicesList = document.createElement("ul");
        servicesList.className = "itens";

        for (const item of appointment.items) {
            const serviceRow = document.createElement("li");
            const serviceName = document.createElement("span");
            serviceName.textContent = item.salonServiceName;

            const statusSelect = document.createElement("select");
            statusSelect.setAttribute("aria-label", `Status de ${item.salonServiceName}`);
            statusSelect.disabled =
                appointment.status === "CANCELED" || appointment.status === "COMPLETED";

            for (const optionData of itemStatusOptions) {
                const option = document.createElement("option");
                option.value = optionData.value;
                option.textContent = optionData.label;
                statusSelect.append(option);
            }
            statusSelect.value = item.status;

            statusSelect.addEventListener("change", () => {
                const previousStatus = item.status;
                statusSelect.disabled = true;
                runAppointmentAction(
                    statusSelect,
                    () => updateAppointmentServiceStatus(
                        appointment.id,
                        item.id,
                        statusSelect.value
                    ),
                    `Status de ${item.salonServiceName} atualizado.`
                ).then(succeeded => {
                    if (succeeded) return;
                    statusSelect.value = previousStatus;
                });
            });

            serviceRow.append(serviceName, statusSelect);
            servicesList.append(serviceRow);
        }
        servicesCell.append(servicesList);

        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");
        statusBadge.className = "badge";
        statusBadge.textContent = statusLabels[appointment.status] ?? appointment.status;
        const statusClass = statusClasses[appointment.status];
        if (statusClass) statusBadge.classList.add(statusClass);
        statusCell.append(statusBadge);

        const actionsCell = document.createElement("td");
        const actions = document.createElement("div");
        actions.className = "acoes";

        if (appointment.status === "PENDING") {
            const confirmButton = createButton("Confirmar", "btn", button => {
                runAppointmentAction(
                    button,
                    () => confirmAppointment(appointment.id),
                    "Agendamento confirmado."
                );
            });
            actions.append(confirmButton);
        }

        if (appointment.status === "PENDING" || appointment.status === "CONFIRMED") {
            const editButton = createButton("Alterar", "btn-secondary", async button => {
                button.disabled = true;
                try {
                    const serviceRow = document.createElement("tr");
                    const cell = document.createElement("td");
                    cell.colSpan = 5;
                    serviceRow.append(cell);
                    const editForm = await createAdminEditForm(appointment);
                    cell.append(editForm);
                    row.after(serviceRow);
                } catch (error) {
                    showMessage(
                        error instanceof Error ? error.message : "Não foi possível abrir a edição.",
                        true
                    );
                } finally {
                    button.disabled = false;
                }
            });
            const cancelButton = createButton("Cancelar", "btn-secondary", button => {
                runAppointmentAction(
                    button,
                    () => cancelAppointment(appointment.id),
                    "Agendamento cancelado."
                );
            });
            actions.append(editButton, cancelButton);
        }

        if (actions.childElementCount === 0) actions.textContent = "—";
        actionsCell.append(actions);
        row.append(customerCell, dateCell, servicesCell, statusCell, actionsCell);
        appointmentsTableBody.append(row);
    }
}

async function createAdminEditForm(appointment) {
    const services = await getServices();
    const form = document.createElement("form");
    form.className = "painel edit-form";
    const dateField = document.createElement("div");
    dateField.className = "field";
    const dateLabel = document.createElement("label");
    dateLabel.textContent = "Data e horário";
    const dateInput = document.createElement("input");
    dateInput.type = "datetime-local";
    dateInput.step = "3600";
    dateInput.required = true;
    dateInput.value = appointment.dateTime.slice(0, 16);
    dateField.append(dateLabel, dateInput);

    const servicesField = document.createElement("div");
    servicesField.className = "stack";
    const title = document.createElement("strong");
    title.textContent = "Selecione os serviços";
    servicesField.append(title);
    const currentIds = new Set(appointment.items.map(item => Number(item.salonServiceId)));
    const serviceInputs = [];

    for (const service of services) {
        const label = document.createElement("label");
        label.className = "card servico";
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = String(service.id);
        checkbox.checked = currentIds.has(Number(service.id));
        serviceInputs.push(checkbox);
        const details = document.createElement("span");
        details.className = "info";
        details.textContent =
            `${service.name} · ${service.durationMinutes} min · ${priceFormatter.format(service.price)}`;
        label.append(checkbox, details);
        servicesField.append(label);
    }

    const actions = document.createElement("div");
    actions.className = "acoes";
    const saveButton = createButton("Salvar alterações", "btn", () => {});
    saveButton.type = "submit";
    const closeButton = createButton("Fechar", "btn-secondary", () => form.closest("tr")?.remove());
    actions.append(saveButton, closeButton);
    form.append(dateField, servicesField, actions);

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const salonServiceIds = serviceInputs
            .filter(input => input.checked)
            .map(input => Number(input.value));
        if (salonServiceIds.length === 0) {
            showMessage("Selecione pelo menos um serviço.", true);
            return;
        }

        saveButton.disabled = true;
        try {
            await updateAdminAppointment(appointment.id, {
                dateTime: dateInput.value,
                salonServiceIds
            });
            showMessage("Agendamento alterado com sucesso.");
            await loadAdminData();
        } catch (error) {
            saveButton.disabled = false;
            showMessage(
                error instanceof Error ? error.message : "Não foi possível alterar o agendamento.",
                true
            );
        }
    });

    return form;
}

async function loadAdminData() {
    const results = await Promise.allSettled([
        getAdminAppointments(),
        getWeeklyDashboard()
    ]);

    if (results[0].status === "fulfilled") {
        renderAppointments(results[0].value);
    } else {
        showMessage(
            results[0].reason instanceof Error
                ? results[0].reason.message
                : "Não foi possível carregar os agendamentos.",
            true
        );
    }

    if (results[1].status === "fulfilled") {
        renderDashboard(results[1].value);
    } else {
        showMessage(
            results[1].reason instanceof Error
                ? results[1].reason.message
                : "Não foi possível carregar o painel semanal.",
            true
        );
    }
}

loadAdminData();

if (logoutButton) {
    logoutButton.addEventListener("click", async () => {
        logoutButton.disabled = true;
        try {
            await logoutAdmin();
            window.location.href = "/login?logout";
        } catch (error) {
            logoutButton.disabled = false;
            showMessage(
                error instanceof Error ? error.message : "Não foi possível sair.",
                true
            );
        }
    });
}
