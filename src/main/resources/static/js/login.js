"use strict";

const form = document.getElementById("form-login");
form.login.focus();

form.addEventListener("submit", async evento => {
    evento.preventDefault();
    Formulario.limparErros(form);

    const erros = [];
    if (!form.login.value.trim()) erros.push({ campo: "login", mensagem: "Informe o seu login." });
    if (!form.senha.value) erros.push({ campo: "senha", mensagem: "Informe a sua senha." });
    if (erros.length) { Formulario.mostrarErros(form, erros); return; }

    const botao = form.querySelector("button[type=submit]");
    botao.disabled = true;
    try {
        await Api.post("/login", new URLSearchParams({ login: form.login.value.trim(), senha: form.senha.value }),
            { semRedirecionar: true });
        window.location.href = "/index.html";
    } catch (e) {
        // Mensagem genérica do servidor ("Login ou senha incorretos."), junto ao campo Senha,
        // como no wireframe. Nada do que foi digitado é apagado.
        Formulario.mostrarErros(form, e.erros.map(x => ({ campo: e.status === 401 ? "senha" : null, mensagem: x.mensagem })));
        const msgs = form.querySelector(".mensagens");
        if (e.status === 401) { msgs.textContent = ""; msgs.className = "mensagens"; }
    } finally {
        botao.disabled = false;
    }
});
