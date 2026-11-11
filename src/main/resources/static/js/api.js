// Aurora — comunicação com a API REST e exibição de erros junto aos campos.
"use strict";

const Api = (() => {

    function lerCookie(nome) {
        const item = document.cookie.split("; ").find(c => c.startsWith(nome + "="));
        return item ? decodeURIComponent(item.split("=")[1]) : null;
    }

    /** Garante o cookie XSRF-TOKEN (o Spring Security o cria sob demanda). */
    async function tokenCsrf() {
        let token = lerCookie("XSRF-TOKEN");
        if (!token) {
            await fetch("/api/csrf", { credentials: "same-origin" });
            token = lerCookie("XSRF-TOKEN");
        }
        return token;
    }

    /**
     * Faz a requisição. Em erro, lança um objeto { status, erros: [{campo, mensagem}] }.
     * Sessão expirada (401) volta para a tela de login.
     */
    async function requisitar(metodo, url, corpo, opcoes = {}) {
        const cabecalhos = { "Accept": "application/json" };
        if (metodo !== "GET") {
            cabecalhos["X-XSRF-TOKEN"] = await tokenCsrf();
        }
        let body;
        if (corpo instanceof URLSearchParams) {
            body = corpo;
        } else if (corpo !== undefined) {
            cabecalhos["Content-Type"] = "application/json";
            body = JSON.stringify(corpo);
        }

        let resposta;
        try {
            resposta = await fetch(url, { method: metodo, headers: cabecalhos, body, credentials: "same-origin" });
        } catch (e) {
            throw { status: 0, erros: [{ campo: null,
                mensagem: "Não foi possível falar com o servidor. Verifique a rede e tente de novo." }] };
        }

        if (resposta.status === 401 && !opcoes.semRedirecionar) {
            window.location.href = "/login.html";
            throw { status: 401, erros: [] };
        }
        if (resposta.status === 204) {
            return null;
        }
        const tipo = resposta.headers.get("Content-Type") || "";
        const dados = tipo.includes("application/json") ? await resposta.json() : null;
        if (!resposta.ok) {
            throw { status: resposta.status, erros: (dados && dados.erros) || [{ campo: null,
                mensagem: "Ocorreu um erro inesperado. Tente de novo." }] };
        }
        return dados;
    }

    return {
        get: (url, opcoes) => requisitar("GET", url, undefined, opcoes),
        post: (url, corpo, opcoes) => requisitar("POST", url, corpo, opcoes),
        put: (url, corpo, opcoes) => requisitar("PUT", url, corpo, opcoes),
        del: (url, opcoes) => requisitar("DELETE", url, undefined, opcoes),
    };
})();

const Formulario = (() => {

    /**
     * Lê os campos do formulário em um objeto. Campos com data-digitos têm a máscara removida
     * (só para enviar; o que o usuário digitou continua na tela). Texto vazio vira null.
     */
    function ler(form) {
        const dados = {};
        for (const el of form.elements) {
            if (!el.name || el.disabled) continue;
            if (el.type === "checkbox") {
                if (el.dataset.lista !== undefined) {
                    dados[el.name] = dados[el.name] || [];
                    if (el.checked) dados[el.name].push(el.value);
                } else {
                    dados[el.name] = el.checked;
                }
            } else if (el.type === "radio") {
                if (el.checked) dados[el.name] = el.value;
                else if (!(el.name in dados)) dados[el.name] = null;
            } else {
                let valor = el.value.trim();
                if (el.dataset.digitos !== undefined) valor = valor.replace(/\D/g, "");
                if (el.dataset.maiusculas !== undefined) valor = valor.toUpperCase();
                dados[el.name] = valor === "" ? null : valor;
            }
        }
        return dados;
    }

    function preencher(form, dados) {
        for (const el of form.elements) {
            if (!el.name || !(el.name in dados)) continue;
            const valor = dados[el.name];
            if (el.type === "checkbox") {
                el.checked = el.dataset.lista !== undefined
                    ? Array.isArray(valor) && valor.includes(el.value)
                    : Boolean(valor);
            } else if (el.type === "radio") {
                el.checked = el.value === valor;
            } else {
                el.value = valor ?? "";
            }
        }
    }

    function regiaoMensagens(form) {
        return form.querySelector(".mensagens");
    }

    /** Remove erros anteriores (o conteúdo digitado nunca é apagado). */
    function limparErros(form) {
        form.querySelectorAll("[aria-invalid]").forEach(el => el.removeAttribute("aria-invalid"));
        form.querySelectorAll(".erro-campo").forEach(el => {
            const alvo = el.dataset.alvo && form.querySelector(`#${el.dataset.alvo}`);
            if (alvo) {
                const ids = (alvo.getAttribute("aria-describedby") || "").split(" ").filter(i => i && i !== el.id);
                ids.length ? alvo.setAttribute("aria-describedby", ids.join(" ")) : alvo.removeAttribute("aria-describedby");
            }
            el.remove();
        });
        const msgs = regiaoMensagens(form);
        if (msgs) { msgs.textContent = ""; msgs.className = "mensagens"; }
    }

    /** Mostra cada erro junto ao seu campo; erros gerais vão para a área de mensagens. */
    function mostrarErros(form, erros) {
        limparErros(form);
        let primeiro = null;
        const gerais = [];
        for (const erro of erros) {
            const alvo = erro.campo ? (form.elements.namedItem(erro.campo)) : null;
            const el = alvo instanceof RadioNodeList ? alvo[0] : alvo;
            if (!el) { gerais.push(erro.mensagem); continue; }

            const container = el.closest(".campo") || el.parentElement;
            const id = `erro-${erro.campo}-${Math.random().toString(36).slice(2, 7)}`;
            const p = document.createElement("p");
            p.className = "erro-campo";
            p.id = id;
            p.textContent = erro.mensagem;
            const controle = el.type === "radio" || el.type === "checkbox" ? el.closest("fieldset") || el : el;
            if (!controle.id) controle.id = `campo-${erro.campo}`;
            p.dataset.alvo = controle.id;
            container.appendChild(p);
            controle.setAttribute("aria-invalid", "true");
            controle.setAttribute("aria-describedby",
                [controle.getAttribute("aria-describedby"), id].filter(Boolean).join(" "));
            primeiro = primeiro || el;
        }
        const msgs = regiaoMensagens(form);
        if (msgs) {
            msgs.className = "mensagens erro";
            msgs.textContent = gerais.length
                ? gerais.join(" ")
                : (erros.length === 1 ? "Corrija o campo destacado." : `Corrija os ${erros.length} campos destacados.`);
        }
        if (primeiro) primeiro.focus();
        else if (msgs) msgs.focus();
    }

    function sucesso(form, texto) {
        limparErros(form);
        const msgs = regiaoMensagens(form);
        if (msgs) { msgs.className = "mensagens sucesso"; msgs.textContent = texto; }
    }

    return { ler, preencher, limparErros, mostrarErros, sucesso };
})();

/** Escapa texto para inserir em HTML. */
function esc(texto) {
    return String(texto ?? "").replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
