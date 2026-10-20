import { getAppointments, getServices, updateAppointment } from "./api.js";

const historyForm = document.getElementById("form-filtro");
const fromInput = document.getElementById("de");
const toInput = document.getElementById("ate");
const appointmentsList = document.getElementById("lista-agendamentos");
const customerId = 1;

const statusLabels = {
    PENDING: "Pendente",
    CONFIRMED: "Confirmado",
    CANCELED: "Cancelado",
    COMPLETED: "Concluído"
};

const itemStatusLabels = {
    PENDING: "Pendente",
    IN_PROGRESS: "Em andamento",
    COMPLETED: "Concluído"
};

const priceFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

function canEditOnline(dateTime) {
    const appointmentTime = new Date(dateTime).getTime();
    const limit = Date.now() + 2 * 24 * 60 * 60 * 1000;

    return appointmentTime >= limit;
}

function isAllowedBusinessSlot(value) {
    if (!value) return false;

    const [dateValue, timeValue] = value.split("T");
    if (!dateValue || !timeValue) return false;

    const [year, month, day] = dateValue.split("-").map(Number);
    const [hour, minute] = timeValue.split(":").map(Number);
    const dayOfWeek = new Date(year, month - 1, day).getDay();

    return dayOfWeek !== 0 && hour >= 9 && hour <= 16 && minute === 0;
}

function showMessage(text, isError) {
    const message = document.getElementById("mensagem");
    if (!message) return;

    message.textContent = text;
    message.hidden = false;
    message.classList.toggle("erro", isError);
}

function renderAppointments(appointments) {
    if (!appointmentsList) return;

    appointmentsList.replaceChildren();

    if (appointments.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.textContent = "Nenhum agendamento nesse período.";
        appointmentsList.append(emptyMessage);
        return;
    }

    for (const appointment of appointments) {
        const card = document.createElement("article");
        card.className = "card agendamento";

        const info = document.createElement("div");
        info.className = "info";

        const date = document.createElement("strong");
        date.textContent = new Date(appointment.dateTime).toLocaleString("pt-BR", {
            dateStyle: "full",
            timeStyle: "short"
        });

        const status = document.createElement("span");
        status.className = "badge";
        status.textContent = statusLabels[appointment.status] ?? appointment.status;
        let total = 0;
        const serviceNames = [];

        for (const item of appointment.items) {
            serviceNames.push(item.salonServiceName);
            total += Number(item.priceCharged);
        }

        const summary = document.createElement("p");
        summary.className = "muted";
        summary.textContent =
            `${serviceNames.join(", ")} · ${priceFormatter.format(total)}`;

        const details = document.createElement("details");

        const detailsTitle = document.createElement("summary");
        detailsTitle.textContent = "Detalhes dos serviços";

        const itemsList = document.createElement("ul");
        itemsList.className = "itens";

        for (const item of appointment.items) {
            const listItem = document.createElement("li");

            const itemInfo = document.createElement("span");
            itemInfo.textContent =
                `${item.salonServiceName} · ${priceFormatter.format(item.priceCharged)}`;

            const itemStatus = document.createElement("span");
            itemStatus.className = "badge";
            itemStatus.textContent = itemStatusLabels[item.status] ?? item.status;

            if (item.status === "PENDING") itemStatus.classList.add("badge-pendente");
            if (item.status === "IN_PROGRESS") itemStatus.classList.add("badge-ok");
            if (item.status === "COMPLETED") itemStatus.classList.add("badge-concluido");

            listItem.append(itemInfo, itemStatus);
            itemsList.append(listItem);
        }

        details.append(detailsTitle, itemsList);

        info.append(date, summary);

        if (appointment.status === "CONFIRMED") status.classList.add("badge-ok");
        if (appointment.status === "PENDING") status.classList.add("badge-pendente");
        if (appointment.status === "COMPLETED") status.classList.add("badge-concluido");

        card.append(info, status, details);

        if (canEditOnline(appointment.dateTime)) {
            const editButton = document.createElement("button");
            editButton.type = "button";
            editButton.className = "btn";
            editButton.textContent = "Alterar";

            editButton.addEventListener("click", async () => {
                editButton.disabled = true;
                await openEditForm(appointment, card);
                editButton.disabled = false;
            });

            card.append(editButton);
        } else {
            const warning = document.createElement("p");
            warning.className = "aviso-prazo";
            warning.textContent =
                "Faltam menos de 2 dias. Para alterar, entre em contato com o salão.";
            card.append(warning);
        }

        appointmentsList.append(card);
    }
}

async function openEditForm(appointment, card) {
    if (card.querySelector(".edit-form")) return;

    try {
        const services = await getServices();
        const form = document.createElement("form");
        form.className = "painel edit-form";

        const dateTimeField = document.createElement("div");
        dateTimeField.className = "field";

        const dateTimeLabel = document.createElement("label");
        const dateTimeInput = document.createElement("input");
        dateTimeInput.id = `edit-date-time-${appointment.id}`;
        dateTimeInput.type = "datetime-local";
        dateTimeInput.step = "3600";
        dateTimeInput.required = true;
        dateTimeInput.value = appointment.dateTime.slice(0, 16);
        dateTimeLabel.htmlFor = dateTimeInput.id;
        dateTimeLabel.textContent = "Nova data e horário";
        dateTimeField.append(dateTimeLabel, dateTimeInput);
        form.append(dateTimeField);

        const servicesField = document.createElement("div");
        servicesField.className = "stack";

        const servicesTitle = document.createElement("strong");
        servicesTitle.textContent = "Serviços";
        servicesField.append(servicesTitle);

        const currentServiceIds = new Set(
            appointment.items.map(item => Number(item.salonServiceId))
        );

        for (const service of services) {
            const label = document.createElement("label");
            label.className = "card servico";

            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "salonServiceIds";
            checkbox.value = String(service.id);
            checkbox.checked = currentServiceIds.has(Number(service.id));

            const serviceInfo = document.createElement("span");
            serviceInfo.className = "info";

            const serviceName = document.createElement("strong");
            serviceName.textContent = service.name;

            const duration = document.createElement("span");
            duration.className = "detalhe";
            duration.textContent = `${service.durationMinutes} min`;

            const price = document.createElement("strong");
            price.textContent = priceFormatter.format(service.price);

            serviceInfo.append(serviceName, duration);
            label.append(checkbox, serviceInfo, price);
            servicesField.append(label);
        }

        form.append(servicesField);

        const actions = document.createElement("div");
        actions.className = "acoes";

        const saveButton = document.createElement("button");
        saveButton.type = "submit";
        saveButton.className = "btn";
        saveButton.textContent = "Salvar alterações";

        const cancelButton = document.createElement("button");
        cancelButton.type = "button";
        cancelButton.className = "btn-secondary";
        cancelButton.textContent = "Cancelar";
        cancelButton.addEventListener("click", () => form.remove());

        actions.append(saveButton, cancelButton);
        form.append(actions);

        form.addEventListener("submit", async event => {
            event.preventDefault();

            const message = document.getElementById("mensagem");
            if (!message) return;

            if (!isAllowedBusinessSlot(dateTimeInput.value)) {
                showMessage(
                    "Escolha um dia de segunda a sábado e um horário entre 09:00 e 16:00.",
                    true
                );
                return;
            }

            const salonServiceIds = Array.from(
                form.querySelectorAll('input[name="salonServiceIds"]:checked')
            ).map(checkbox => Number(checkbox.value));

            if (salonServiceIds.length === 0) {
                showMessage("Selecione pelo menos um serviço.", true);
                return;
            }

            saveButton.disabled = true;

            try {
                await updateAppointment(appointment.id, {
                    dateTime: dateTimeInput.value,
                    salonServiceIds
                }, customerId);
            } catch (error) {
                showMessage(
                    error instanceof Error
                        ? error.message
                        : "Não foi possível alterar o agendamento.",
                    true
                );
                saveButton.disabled = false;
                return;
            }

            form.remove();
            showMessage("Agendamento alterado com sucesso.", false);

            try {
                const appointments = await getAppointments(
                    fromInput.value,
                    toInput.value,
                    customerId
                );
                renderAppointments(appointments);
            } catch (error) {
                showMessage(
                    error instanceof Error
                        ? `Agendamento alterado, mas a lista não foi atualizada: ${error.message}`
                        : "Agendamento alterado, mas não foi possível atualizar a lista.",
                    true
                );
            }
        });

        card.append(form);
    } catch (error) {
        showMessage(
            error instanceof Error
                ? error.message
                : "Não foi possível abrir o formulário de alteração.",
            true
        );
    }
}

async function searchHistory(event) {
    event.preventDefault();

    const message = document.getElementById("mensagem");

    if (!message || !fromInput || !toInput || !appointmentsList) return;

    if (!fromInput.value || !toInput.value) {
        message.textContent = "Escolha as duas datas.";
        message.hidden = false;
        message.classList.add("erro");
        return;
    }

    try {
        const appointments = await getAppointments(
            fromInput.value,
            toInput.value,
            customerId
        );

        renderAppointments(appointments);
        message.hidden = true;

    } catch (error) {
        message.textContent = error instanceof Error
            ? error.message
            : "Não foi possível buscar os agendamentos.";
        message.hidden = false;
        message.classList.add("erro");
    }
}

if (historyForm) {
    historyForm.addEventListener("submit", searchHistory);
}