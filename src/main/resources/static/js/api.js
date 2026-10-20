export async function getServices() {
    const response = await fetch("/services");

    if (!response.ok) {
        throw new Error(`Erro ao buscar serviços: ${response.status} ${response.statusText}`);
    }

    return response.json();
}

export async function createAppointment(request, customerId) {

    const response = await fetch("/appointments", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Customer-Id": customerId
        },
        body: JSON.stringify(request)
    });
    if (!response.ok) {
        throw new Error(`Request failed with status ${response.status} ${response.statusText}`);
    }

    return response.json();
}

export async function getAppointments(from, to, customerId) {
    const params = new URLSearchParams({ from, to });

    const response = await fetch(`/appointments?${params}`, {
        headers: {
            "X-Customer-Id": String(customerId)
        }
    });

    if (!response.ok) {
        throw new Error(
            `Erro ao buscar agendamentos: ${response.status} ${response.statusText}`
        );
    }

    return response.json();
}

export async function updateAppointment(appointmentId, request, customerId) {
    const response = await fetch(`/appointments/${appointmentId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "X-Customer-Id": String(customerId)
        },
        body: JSON.stringify(request)
    });

    if (!response.ok) {
        throw new Error(
            `Erro ao alterar agendamento: ${response.status} ${response.statusText}`
        );
    }

    return response.json();
}

export async function getCsrfToken() {
    const response = await fetch("/api/csrf");

    if (!response.ok) {
        throw new Error(`Não foi possível obter o token CSRF: ${response.status}`);
    }

    const cookie = document.cookie
        .split("; ")
        .find(value => value.startsWith("XSRF-TOKEN="));

    if (!cookie) {
        throw new Error("Cookie CSRF não encontrado.");
    }

    return decodeURIComponent(cookie.substring("XSRF-TOKEN=".length));
}

export async function confirmAppointment(appointmentId) {
    const csrfToken = await getCsrfToken();
    const response = await fetch(
        `/api/admin/appointments/${appointmentId}/confirm`,
        {
            method: "PUT",
            headers: { "X-XSRF-TOKEN": csrfToken }
        }
    );

    if (!response.ok) {
        throw new Error(`Falha ao confirmar: ${response.status}`);
    }

    return response.json();
}

export async function updateAppointmentServiceStatus(appointmentId, itemId, status) {
    const csrfToken = await getCsrfToken();
    const params = new URLSearchParams({ status });
    const response = await fetch(
        `/api/admin/appointments/${appointmentId}/services/${itemId}/status?${params}`,
        {
            method: "PUT",
            headers: { "X-XSRF-TOKEN": csrfToken }
        }
    );

    if (!response.ok) {
        throw new Error(`Falha ao atualizar o serviço: ${response.status}`);
    }

    return response.json();
}

export async function updateAdminAppointment(appointmentId, request) {
    const csrfToken = await getCsrfToken();
    const response = await fetch(`/api/admin/appointments/${appointmentId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken
        },
        body: JSON.stringify(request)
    });

    if (!response.ok) {
        throw new Error(`Falha ao alterar o agendamento: ${response.status}`);
    }

    return response.json();
}

export async function cancelAppointment(appointmentId) {
    const csrfToken = await getCsrfToken();
    const response = await fetch(`/api/admin/appointments/${appointmentId}/cancel`, {
        method: "PUT",
        headers: { "X-XSRF-TOKEN": csrfToken }
    });

    if (!response.ok) {
        throw new Error(`Falha ao cancelar o agendamento: ${response.status}`);
    }

    return response.json();
}

export async function getAdminAppointments() {
    const response = await fetch("/api/admin/appointments");

    if (!response.ok) {
        throw new Error(`Falha ao buscar agendamentos: ${response.status}`);
    }

    return response.json();
}

export async function getWeeklyDashboard() {
    const response = await fetch("/api/admin/appointments/dashboard");

    if (!response.ok) {
        throw new Error(`Falha ao carregar o painel semanal: ${response.status}`);
    }

    return response.json();
}

export async function logoutAdmin() {
    const csrfToken = await getCsrfToken();
    const response = await fetch("/logout", {
        method: "POST",
        headers: { "X-XSRF-TOKEN": csrfToken }
    });

    if (!response.ok) {
        throw new Error(`Falha ao sair: ${response.status}`);
    }
}