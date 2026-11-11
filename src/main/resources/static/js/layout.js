// Aurora — barra superior e menu lateral comuns a todas as telas.
// O menu mostra a união dos itens dos perfis do usuário. Esconder o menu não protege nada:
// a autorização é verificada no servidor.
"use strict";

const Layout = (() => {

    const ITENS = [
        { id: "inicio", rotulo: "Início", href: "/index.html",
          perfis: ["OPERADOR", "RESPONSAVEL_PRODUCAO", "ADMINISTRADOR"] },
        { id: "atendimentos", rotulo: "Atendimentos", href: "/atendimentos.html",
          perfis: ["OPERADOR", "RESPONSAVEL_PRODUCAO"], incremento: 2 },
        { id: "pacientes", rotulo: "Pacientes", href: "/pacientes.html", perfis: ["OPERADOR"] },
        { id: "competencias", rotulo: "Competências", href: "/competencias.html",
          perfis: ["RESPONSAVEL_PRODUCAO"], incremento: 3 },
        { id: "cadastros", rotulo: "Cadastros", href: "/cadastros.html", perfis: ["ADMINISTRADOR"] },
    ];
    // Telas dos próximos incrementos ainda não existem.
    const INCREMENTO_ATUAL = 2;

    const NOMES_PERFIL = {
        ADMINISTRADOR: "Administrador",
        OPERADOR: "Operador",
        RESPONSAVEL_PRODUCAO: "Responsável pela produção",
    };

    function mesAnoAtual() {
        const hoje = new Date();
        const mm = String(hoje.getMonth() + 1).padStart(2, "0");
        return { aaaamm: `${hoje.getFullYear()}${mm}`, texto: `${mm}/${hoje.getFullYear()}` };
    }

    const NOMES_SITUACAO = { ABERTA: "ABERTA", FECHADA: "FECHADA" };

    /** Atualiza a competência atual e a situação na barra superior (sempre visível). */
    async function atualizarCompetencia() {
        const atual = mesAnoAtual();
        const alvo = document.getElementById("competencia-atual");
        try {
            const c = await Api.get(`/api/competencias/situacao?mesAno=${atual.aaaamm}`);
            alvo.innerHTML = `Competência atual: <strong>${atual.texto}</strong> – ${c.situacao
                ? `<strong>${NOMES_SITUACAO[c.situacao]}</strong>` : "sem lançamentos"}`;
        } catch (e) {
            alvo.innerHTML = `Competência atual: <strong>${atual.texto}</strong>`;
        }
    }

    function montarBarra(usuario) {
        const barra = document.getElementById("barra");
        barra.className = "barra-superior";
        barra.innerHTML = `
            <button type="button" class="botao-menu" aria-expanded="false" aria-controls="menu">Menu</button>
            <span class="marca">AURORA</span>
            <span class="competencia" id="competencia-atual" aria-live="polite"></span>
            <span class="usuario">${esc(usuario.nome.split(" ")[0])}
                (${usuario.perfis.map(p => NOMES_PERFIL[p]).join(", ")})</span>
            <button type="button" id="sair">Sair</button>`;

        barra.querySelector("#sair").addEventListener("click", async () => {
            try { await Api.post("/logout"); } finally { window.location.href = "/login.html"; }
        });

        const botaoMenu = barra.querySelector(".botao-menu");
        botaoMenu.addEventListener("click", () => {
            const menu = document.getElementById("menu");
            const aberto = menu.classList.toggle("aberto");
            botaoMenu.setAttribute("aria-expanded", String(aberto));
        });
    }

    function montarMenu(usuario, paginaAtual) {
        const menu = document.getElementById("menu");
        menu.className = "menu-lateral";
        menu.setAttribute("aria-label", "Menu principal");
        const itens = ITENS.filter(i => (i.incremento || 1) <= INCREMENTO_ATUAL
            && i.perfis.some(p => usuario.perfis.includes(p)));
        menu.innerHTML = `<ul>${itens.map(i => `<li><a href="${i.href}"${i.id === paginaAtual
            ? ' aria-current="page"' : ""}>${i.rotulo}</a></li>`).join("")}</ul>`;
    }

    /**
     * Monta o layout. Se a tela exige perfis que o usuário não tem, mostra um aviso no lugar
     * do conteúdo e devolve null.
     */
    async function iniciar(paginaAtual, perfisDaTela) {
        const usuario = await Api.get("/api/me");
        montarBarra(usuario);
        montarMenu(usuario, paginaAtual);
        atualizarCompetencia();
        if (perfisDaTela && !perfisDaTela.some(p => usuario.perfis.includes(p))) {
            document.getElementById("conteudo").innerHTML =
                `<div class="painel"><h1>Acesso não permitido</h1>
                 <p>Seu perfil não tem acesso a esta tela. Use o menu ao lado.</p></div>`;
            return null;
        }
        return usuario;
    }

    return { iniciar, atualizarCompetencia, NOMES_PERFIL };
})();
