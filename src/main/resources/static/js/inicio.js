"use strict";

(async () => {
    const usuario = await Layout.iniciar("inicio");
    if (!usuario) return;
    document.getElementById("saudacao").textContent = `Olá, ${usuario.nome.split(" ")[0]}`;

    const atalhos = [];
    if (usuario.perfis.some(p => p === "OPERADOR" || p === "RESPONSAVEL_PRODUCAO")) atalhos.push(["/atendimentos.html", "Lançar atendimento"]);
    if (usuario.perfis.includes("OPERADOR")) atalhos.push(["/pacientes.html", "Cadastrar ou buscar pacientes"]);
    if (usuario.perfis.includes("ADMINISTRADOR")) atalhos.push(["/cadastros.html", "Cadastros: estabelecimento, profissionais, procedimentos e usuários"]);
    document.getElementById("atalhos").innerHTML =
        atalhos.map(([href, texto]) => `<li><a href="${href}">${texto}</a></li>`).join("");

    if (usuario.perfis.includes("ADMINISTRADOR")) {
        const estabelecimento = await Api.get("/api/estabelecimento");
        document.getElementById("aviso-estabelecimento").hidden = estabelecimento !== null;
    }
})();
