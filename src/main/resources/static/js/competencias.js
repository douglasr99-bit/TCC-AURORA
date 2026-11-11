// Aurora — conferência, fechamento, reabertura, geração do arquivo BPA e histórico
// (RF11, RF12, RF13, RF14, RF15). Perfil: Responsável pela produção.
"use strict";

const escolha = document.getElementById("escolha-competencia");
const msg = document.getElementById("msg");
const botaoFechar = document.getElementById("botao-fechar");
const botaoReabrir = document.getElementById("botao-reabrir");
const botaoGerar = document.getElementById("botao-gerar");
const explicaGerar = document.getElementById("explica-gerar");
const confirmaFechar = document.getElementById("confirma-fechar");
const formReabrir = document.getElementById("form-reabrir");

let competencias = [];
let atual = null;   // CompetenciaDto selecionada

const numero = n => Number(n).toLocaleString("pt-BR");
const mesAnoBr = aaaamm => `${aaaamm.slice(4)}/${aaaamm.slice(0, 4)}`;
function dataHoraBr(iso) {
    const d = new Date(iso);
    return `${d.toLocaleDateString("pt-BR")} ${d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}`;
}

function mostrar(el, tipo, texto) {
    el.className = `mensagens ${tipo}`;
    el.textContent = texto;
}

// ---------- carregar ----------

async function carregarLista(selecionarId) {
    competencias = await Api.get("/api/competencias");
    const vazia = competencias.length === 0;
    document.getElementById("sem-competencias").hidden = !vazia;
    document.getElementById("detalhe").hidden = vazia;
    escolha.disabled = vazia;
    escolha.innerHTML = competencias.map(c =>
        `<option value="${c.id}">${mesAnoBr(c.mesAno)} – ${c.situacao}</option>`).join("");
    if (vazia) return;
    escolha.value = selecionarId ?? competencias[0].id;
    await carregarResumo();
}

async function carregarResumo() {
    const r = await Api.get(`/api/competencias/${escolha.value}/resumo`);
    atual = r.competencia;
    const aberta = atual.situacao === "ABERTA";
    const texto = mesAnoBr(atual.mesAno);

    document.getElementById("titulo").textContent = `Competência ${texto}`;
    const selo = document.getElementById("selo-situacao");
    selo.textContent = atual.situacao;
    selo.className = `selo ${aberta ? "aberta" : "fechada"}`;

    document.getElementById("total-atendimentos").textContent = numero(r.totalAtendimentos);
    document.getElementById("total-bpac").textContent = numero(r.totalBpaC);
    document.getElementById("total-bpai").textContent = numero(r.totalBpaI);
    document.getElementById("total-profissionais").textContent = numero(r.profissionais);

    document.getElementById("lista-procedimentos").innerHTML = r.porProcedimento.length
        ? r.porProcedimento.map(p => `<tr><td>${p.codigo}</td><td>${esc(p.descricao)}</td>
            <td>${p.modalidade === "BPA_I" ? "BPA-I" : "BPA-C"}</td><td class="numero">${numero(p.lancamentos)}</td>
            <td class="numero">${numero(p.quantidade)}</td></tr>`).join("")
        : `<tr><td colspan="5" class="vazio">Nenhum atendimento lançado.</td></tr>`;

    document.getElementById("lista-profissionais").innerHTML = r.porProfissional.length
        ? r.porProfissional.map(p => `<tr><td>${esc(p.nome)}</td><td>${esc(p.cbo)}</td>
            <td class="numero">${numero(p.lancamentos)}</td><td class="numero">${numero(p.quantidade)}</td></tr>`).join("")
        : `<tr><td colspan="4" class="vazio">Nenhum atendimento lançado.</td></tr>`;

    document.getElementById("secao-reaberturas").hidden = r.reaberturas.length === 0;
    document.getElementById("lista-reaberturas").innerHTML = r.reaberturas.map(x =>
        `<li>${dataHoraBr(x.data)} – ${esc(x.usuario)}: ${esc(x.motivo)}</li>`).join("");

    // Ações indisponíveis ficam desabilitadas, com a explicação ao lado.
    botaoFechar.hidden = !aberta;
    botaoReabrir.hidden = aberta;
    botaoGerar.disabled = aberta;
    explicaGerar.textContent = aberta ? "Feche a competência para gerar o arquivo." : "";
    confirmaFechar.hidden = true;
    formReabrir.hidden = true;
    confirmaFechar.querySelector("[data-mes]").textContent = texto;
}

async function carregarHistorico() {
    const itens = await Api.get("/api/exportacoes");
    document.getElementById("lista-exportacoes").innerHTML = itens.length
        ? itens.map(e => `<tr><td>${dataHoraBr(e.dataGeracao)}</td><td>${mesAnoBr(e.competencia)}</td>
            <td>${esc(e.nomeArquivo)}</td><td class="numero">${numero(e.totalRegistros)}</td><td>${esc(e.usuario)}</td>
            <td class="acoes"><button type="button" class="link" data-baixar="${e.id}">Baixar<span class="visualmente-oculto">
            ${esc(e.nomeArquivo)} gerado em ${dataHoraBr(e.dataGeracao)}</span></button></td></tr>`).join("")
        : `<tr><td colspan="6" class="vazio">Nenhum arquivo gerado ainda.</td></tr>`;
}

// ---------- fechar (RF12) ----------

botaoFechar.addEventListener("click", () => {
    confirmaFechar.hidden = false;
    confirmaFechar.querySelector("button[type=submit]").focus();
});
confirmaFechar.querySelector("[data-cancelar]").addEventListener("click", () => {
    confirmaFechar.hidden = true;
    botaoFechar.focus();
});
confirmaFechar.addEventListener("submit", async e => {
    e.preventDefault();
    try {
        await Api.post(`/api/competencias/${atual.id}/fechamento`);
        await carregarLista(atual.id);
        Layout.atualizarCompetencia();
        mostrar(msg, "sucesso", `Competência ${mesAnoBr(atual.mesAno)} fechada. Agora você pode gerar o arquivo BPA.`);
        botaoGerar.focus();
    } catch (erro) {
        mostrar(msg, "erro", erro.erros.map(x => x.mensagem).join(" "));
    }
});

// ---------- reabrir (RF15) ----------

botaoReabrir.addEventListener("click", () => {
    Formulario.limparErros(formReabrir);
    formReabrir.hidden = false;
    formReabrir.motivo.focus();
});
formReabrir.querySelector("[data-cancelar]").addEventListener("click", () => {
    formReabrir.hidden = true;
    botaoReabrir.focus();
});
formReabrir.addEventListener("submit", async e => {
    e.preventDefault();
    const motivo = formReabrir.motivo.value.trim();
    if (!motivo) {
        Formulario.mostrarErros(formReabrir, [{ campo: "motivo", mensagem: "Informe o motivo da reabertura." }]);
        return;
    }
    try {
        await Api.post(`/api/competencias/${atual.id}/reabertura`, { motivo });
        formReabrir.motivo.value = "";
        await carregarLista(atual.id);
        Layout.atualizarCompetencia();
        mostrar(msg, "sucesso", `Competência ${mesAnoBr(atual.mesAno)} reaberta. Lançamentos e correções estão liberados.`);
    } catch (erro) {
        Formulario.mostrarErros(formReabrir, erro.erros);   // o motivo digitado não é apagado
    }
});

// ---------- gerar arquivo (RF13) e baixar de novo (RF14) ----------

botaoGerar.addEventListener("click", async () => {
    botaoGerar.disabled = true;
    try {
        const nome = await Api.baixar("POST", `/api/competencias/${atual.id}/exportacao`);
        mostrar(msg, "sucesso", `Arquivo ${nome} gerado. Importe-o no SIA da competência ${mesAnoBr(atual.mesAno)}.`);
        await carregarHistorico();
    } catch (erro) {
        mostrar(msg, "erro", erro.erros.map(x => x.mensagem).join(" "));
    } finally {
        botaoGerar.disabled = atual.situacao === "ABERTA";
    }
});

document.getElementById("lista-exportacoes").addEventListener("click", async e => {
    const botao = e.target.closest("[data-baixar]");
    if (!botao) return;
    try {
        await Api.baixar("GET", `/api/exportacoes/${botao.dataset.baixar}/arquivo`);
    } catch (erro) {
        mostrar(document.getElementById("msg-historico"), "erro", erro.erros.map(x => x.mensagem).join(" "));
    }
});

escolha.addEventListener("change", () => { msg.textContent = ""; msg.className = "mensagens"; carregarResumo(); });

(async () => {
    const usuario = await Layout.iniciar("competencias", ["RESPONSAVEL_PRODUCAO"]);
    if (!usuario) return;
    await Promise.all([carregarLista(), carregarHistorico()]);
})();
