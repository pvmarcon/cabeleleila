import { getServices, createAppointment } from "./api.js";

const servicesList = document.getElementById("lista-servicos");
const data = document.getElementById("data");
const time = document.getElementById("hora");
const appointmentForm = document.getElementById("form-agendamento");
const totalOutput = document.getElementById("total");
const customerId = 1;
const suggestionPanel = document.getElementById("sugestao-semana");
const suggestionText = document.getElementById("sugestao-texto");
const acceptSuggestionButton = document.getElementById("aceitar-sugestao");
const declineSuggestionButton = document.getElementById("recusar-sugestao");

let pendingAppointmentRequest = null;

const priceFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

function updateTotal() {
    if (!servicesList || !totalOutput) return;

    const checkedServices = servicesList.querySelectorAll(
        'input[name="servico"]:checked'
    );

    let total = 0;

    for (const checkbox of checkedServices) {
        total += Number(checkbox.dataset.preco);
    }

    totalOutput.textContent = priceFormatter.format(total);
}

async function loadServices() {

    try {
        const services = await getServices();

        if (!servicesList) {
            throw new Error("Elemento #lista-servicos não encontrado.");
        }

        for (const service of services) {
            const label = document.createElement("label");
            label.className = "card servico";

            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "servico";
            checkbox.value = String(service.id);
            checkbox.dataset.preco = String(service.price);
            checkbox.addEventListener("change", updateTotal);
            checkbox.dataset.duracao = String(service.durationMinutes);

            const info = document.createElement("span");
            info.className = "info";

            const name = document.createElement("strong");
            name.textContent = service.name;

            const duration = document.createElement("span");
            duration.className = "detalhe";
            duration.textContent = `${service.durationMinutes} min`;

            const price = document.createElement("strong");
            price.textContent = priceFormatter.format(service.price);

            info.append(name, duration);
            label.append(checkbox, info, price);
            servicesList.append(label);
        }
    } catch (error) {
        const message = document.getElementById("mensagem");
        if (message) {
            message.textContent = error instanceof Error
                ? error.message
                : "Não foi possível carregar os serviços.";
            message.hidden = false;
            message.classList.add("erro");
        }
    }
}

function populateTimes() {
    if (!data || !time) return;

    time.replaceChildren(new Option("Selecione", ""));

    if (!data.value) return;

    const [year, month, day] = data.value.split("-").map(Number);
    const weekday = new Date(year, month - 1, day).getDay();

    const message = document.getElementById("mensagem");

    if (weekday === 0) {
        if (message) {
            message.textContent = "Agendamento não permitido para domingo.";
            message.hidden = false;
            message.classList.add("erro");
        }
        return;
    }

    if (message && message.textContent === "Agendamento não permitido para domingo.") {
        message.hidden = true;
        message.textContent = "";
        message.classList.remove("erro");
    }

    for (let hour = 9; hour <= 16; hour++) {
        const hourText = `${String(hour).padStart(2, "0")}:00`;
        const option = document.createElement("option");

        option.value = hourText;
        option.textContent = hourText;
        time.append(option);
    }
}

if (data) {
    data.addEventListener("change", populateTimes);
}

async function sendSuggestionDecision(accepted) {
    if (!pendingAppointmentRequest) return;

    const message = document.getElementById("mensagem");
    if (!message) return;

    pendingAppointmentRequest.suggestionAccepted = accepted;

    try {
        const result = await createAppointment(pendingAppointmentRequest, customerId);

        if (result.outcome !== "CREATED") {
            throw new Error("O agendamento não foi criado.");
        }

        message.textContent = accepted
            ? "Agendamento criado na data sugerida."
            : "Agendamento criado na data escolhida.";
        message.hidden = false;
        message.classList.remove("erro");

        if (suggestionPanel) {
            suggestionPanel.hidden = true;
        }

        pendingAppointmentRequest = null;
    } catch (error) {
        message.textContent = error instanceof Error
            ? error.message
            : "Não foi possível criar o agendamento.";
        message.hidden = false;
        message.classList.add("erro");
    }
}

if (acceptSuggestionButton) {
    acceptSuggestionButton.addEventListener("click", () => sendSuggestionDecision(true));
}

if (declineSuggestionButton) {
    declineSuggestionButton.addEventListener("click", () => sendSuggestionDecision(false));
}

if (appointmentForm && data && time) {
    appointmentForm.addEventListener("submit", async event => {
        event.preventDefault();

        const message = document.getElementById("mensagem");
        if (!message) return;

        const checkedServices = document.querySelectorAll(
            '#lista-servicos input[name="servico"]:checked'
        );

        const salonServiceIds = Array.from(checkedServices)
            .map(checkbox => Number(checkbox.value));

        if (salonServiceIds.length === 0) {
            message.textContent = "Selecione pelo menos um serviço."
            message.hidden = false;
            message.classList.add("erro");
            return;
        }

        const request = {
            dateTime: `${data.value}T${time.value}`,
            salonServiceIds: salonServiceIds,
            suggestionAccepted: null
        };

        try {
            const result = await createAppointment(request, customerId);

            if (result.outcome === "CREATED") {
                pendingAppointmentRequest = null;
                message.textContent = "Agendamento criado com sucesso.";
                message.hidden = false;
                message.classList.remove("erro");

                if (suggestionPanel) {
                    suggestionPanel.hidden = true;
                }
            } else {
                pendingAppointmentRequest = request;

                const suggestedDate = new Date(result.suggestedDateTime);
                const formattedDate = suggestedDate.toLocaleString("pt-BR", {
                    dateStyle: "short",
                    timeStyle: "short"
                });

                if (!suggestionPanel || !suggestionText) {
                    throw new Error("Painel de sugestão não encontrado.");
                }

                suggestionText.textContent =
                    `O horário sugerido é ${formattedDate}. Deseja usar esta data?`;
                suggestionPanel.hidden = false;
                message.hidden = true;
            }
        } catch (error) {
            message.textContent = error instanceof Error
                ? error.message
                : "Não foi possível criar o agendamento.";
            message.hidden = false;
            message.classList.add("erro");
        }
    });
}

loadServices();