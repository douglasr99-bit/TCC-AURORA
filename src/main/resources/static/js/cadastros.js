// Aurora — tela de Cadastros (Administrador): RF02, RF03, RF04, RF06.
"use strict";

const NOMES_MODALIDADE = { BPA_C: "BPA-C", BPA_I: "BPA-I" };

function formatarCns(cns) {
    return cns.replace(/(\d{3})(\d{4})(\d{4})(\d{4})/, "$1 $2 $3 $4");
}

function formatarProcedimento(c) {
    return c.replace(/(\d{2})(\d{2})(\d{2})(\d{3})(\d)/, "$1.$2.$3.$4-$5");
}

const cadastros = {
    profissionais: CadastroLista({
        nome: "profissionais",
        url: "/api/profissionais",
        comBusca: true,
        titulos: { novo: "Novo profissional", editar: "Editar profissional" },
        nomeItem: p => p.nome,
        linha: p => `<td>${esc(p.nome)}</td><td>${formatarCns(p.cns)}</td><td>${esc(p.cbo)}</td>
            <td${p.ativo ? "" : ' class="situacao-inativo"'}>${p.ativo ? "Ativo" : "Inativo"}</td>`,
    }),
    procedimentos: CadastroLista({
        nome: "procedimentos",
        url: "/api/procedimentos",
        comBusca: true,
        titulos: { novo: "Novo procedimento", editar: "Editar procedimento" },
        nomeItem: p => `Procedimento ${formatarProcedimento(p.codigo)}`,
        linha: p => `<td>${formatarProcedimento(p.codigo)}</td><td>${esc(p.descricao)}</td>
            <td>${NOMES_MODALIDADE[p.modalidade]}</td><td>${p.exigeCid ? "Sim" : "Não"}</td>`,
    }),
    usuarios: CadastroLista({
        nome: "usuarios",
        url: "/api/usuarios",
        comBusca: false,
        titulos: { novo: "Novo usuário", editar: "Editar usuário" },
        nomeItem: u => `Usuário ${u.login}`,
        linha: u => `<td>${esc(u.nome)}</td><td>${esc(u.login)}</td>
            <td>${u.perfis.map(p => Layout.NOMES_PERFIL[p]).join(", ")}</td>
            <td${u.ativo ? "" : ' class="situacao-inativo"'}>${u.ativo ? "Ativo" : "Inativo"}</td>`,
        aoAbrir: (form, item) => {
            document.getElementById("dica-senha").textContent = item
                ? "Deixe em branco para manter a senha atual."
                : "Pelo menos 8 caracteres.";
        },
    }),
};

// ---------- Estabelecimento (RF03) ----------

const formEstabelecimento = document.getElementById("form-estabelecimento");

async function carregarEstabelecimento() {
    const dados = await Api.get("/api/estabelecimento");
    if (dados) Formulario.preencher(formEstabelecimento, dados);
}

formEstabelecimento.addEventListener("submit", async e => {
    e.preventDefault();
    const botao = formEstabelecimento.querySelector("button[type=submit]");
    botao.disabled = true;
    try {
        await Api.put("/api/estabelecimento", Formulario.ler(formEstabelecimento));
        Formulario.sucesso(formEstabelecimento, "Dados do estabelecimento salvos.");
    } catch (erro) {
        Formulario.mostrarErros(formEstabelecimento, erro.erros);
    } finally {
        botao.disabled = false;
    }
});

// ---------- Abas (teclado: setas esquerda/direita, Home, End) ----------

const abas = [...document.querySelectorAll('[role="tab"]')];

function selecionarAba(aba) {
    for (const a of abas) {
        const ativa = a === aba;
        a.setAttribute("aria-selected", String(ativa));
        a.tabIndex = ativa ? 0 : -1;
        document.getElementById(a.getAttribute("aria-controls")).hidden = !ativa;
    }
    const nome = aba.id.replace("aba-", "");
    if (cadastros[nome]) cadastros[nome].mostrar();
}

abas.forEach((aba, i) => {
    aba.addEventListener("click", () => selecionarAba(aba));
    aba.addEventListener("keydown", e => {
        let alvo = null;
        if (e.key === "ArrowRight") alvo = abas[(i + 1) % abas.length];
        if (e.key === "ArrowLeft") alvo = abas[(i - 1 + abas.length) % abas.length];
        if (e.key === "Home") alvo = abas[0];
        if (e.key === "End") alvo = abas[abas.length - 1];
        if (alvo) { e.preventDefault(); alvo.focus(); selecionarAba(alvo); }
    });
});

(async () => {
    const usuario = await Layout.iniciar("cadastros", ["ADMINISTRADOR"]);
    if (!usuario) return;
    cadastros.usuarios.mostrar();
    await carregarEstabelecimento();
})();
