// Aurora — lançamento, consulta e correção de atendimentos (RF07, RF08, RF09, RF10).
"use strict";

const form = document.getElementById("form-atendimento");
const caixaPaciente = document.getElementById("caixa-paciente");
const blocoIdade = document.getElementById("bloco-idade");
const blocoCid = document.getElementById("bloco-cid");
const dicaProcedimento = document.getElementById("dica-procedimento");
const dicaCompetencia = document.getElementById("dica-competencia");
const linkNovoPaciente = document.getElementById("link-novo-paciente");
const listaHoje = document.getElementById("lista-hoje");

// Nomes dos campos da API → campos do formulário onde o erro aparece
const CAMPO_NA_TELA = { profissionalId: "buscaProfissional", procedimentoId: "buscaProcedimento", pacienteId: "cnsPaciente" };

let usuario = null;
let procedimento = null;   // procedimento escolhido (modalidade, exigeCid)
let competenciaConsultada = null;

// ---------- utilidades ----------

function hojeIso() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function dataBr(iso) {
    if (!iso) return "";
    const [a, m, d] = iso.split("-");
    return `${d}/${m}/${a}`;
}

function procedimentoFormatado(codigo) {
    return codigo.replace(/(\d{2})(\d{2})(\d{2})(\d{3})(\d)/, "$1.$2.$3.$4-$5");
}

function mostrarMensagem(el, tipo, texto) {
    el.className = `mensagens ${tipo}`;
    el.textContent = texto;
}

// ---------- campos condicionais (prevenção de erros: só aparece o que se aplica) ----------

function atualizarCamposDoProcedimento() {
    const bpaI = procedimento && procedimento.modalidade === "BPA_I";
    const bpaC = procedimento && procedimento.modalidade === "BPA_C";
    const exigeCid = procedimento && procedimento.exigeCid;

    caixaPaciente.hidden = !bpaI;
    caixaPaciente.disabled = !bpaI;          // campos ocultos não são enviados
    blocoIdade.hidden = !bpaC;
    form.idade.disabled = !bpaC;
    blocoCid.hidden = !exigeCid;
    form.cid.disabled = !exigeCid;

    dicaProcedimento.textContent = procedimento
        ? `Modalidade: ${bpaI ? "BPA-I" : "BPA-C"}${exigeCid ? " · exige CID-10" : ""}`
        : "Busque por código ou descrição.";
}

function atualizarDicaCompetencia() {
    const v = form.data.value;
    dicaCompetencia.textContent = v ? `Define a competência: ${v.slice(5, 7)}/${v.slice(0, 4)}` : "";
}

// ---------- buscas ----------

const buscaProfissional = CampoBusca({
    campo: form.buscaProfissional,
    oculto: form.profissionalId,
    url: "/api/profissionais",
    rotulo: p => `${p.nome} – CBO ${p.cbo}`,
    linha: p => `${esc(p.nome)} <small>– CBO ${esc(p.cbo)}${p.ativo ? "" : " · inativo"}</small>`,
    aoEscolher: () => {},
});

const buscaProcedimento = CampoBusca({
    campo: form.buscaProcedimento,
    oculto: form.procedimentoId,
    url: "/api/procedimentos",
    rotulo: p => `${p.codigo} – ${p.descricao}`,
    linha: p => `${procedimentoFormatado(p.codigo)} – ${esc(p.descricao)} <small>(${p.modalidade === "BPA_I" ? "BPA-I" : "BPA-C"})</small>`,
    aoEscolher: p => { procedimento = p; atualizarCamposDoProcedimento(); },
});

// ---------- paciente (BPA-I) ----------

function limparPaciente() {
    form.pacienteId.value = "";
    document.getElementById("at-paciente-nome").value = "";
    document.getElementById("at-paciente-nascimento").value = "";
}

function definirPaciente(p) {
    form.pacienteId.value = p.id;
    form.cnsPaciente.value = p.cns;
    document.getElementById("at-paciente-nome").value = p.nome;
    document.getElementById("at-paciente-nascimento").value = dataBr(p.dataNascimento);
    linkNovoPaciente.hidden = true;
}

/** Busca o paciente pelo CNS. Devolve true se encontrou. */
async function buscarPaciente() {
    Formulario.limparErros(form);
    limparPaciente();
    const cns = form.cnsPaciente.value.replace(/\D/g, "");
    if (cns.length !== 15) {
        Formulario.mostrarErros(form, [{ campo: "cnsPaciente", mensagem: "O CNS deve ter 15 dígitos. Confira o cartão do paciente." }]);
        return false;
    }
    try {
        const encontrados = await Api.get(`/api/pacientes?q=${cns}`);
        const p = encontrados.find(x => x.cns === cns);
        if (p) { definirPaciente(p); return true; }
        Formulario.mostrarErros(form, [{ campo: "cnsPaciente",
            mensagem: "Nenhum paciente cadastrado com este CNS. Confira o número ou cadastre o paciente." }]);
        if (usuario.perfis.includes("OPERADOR")) {
            document.getElementById("a-novo-paciente").href = `/pacientes.html?novo&cns=${cns}`;
            linkNovoPaciente.hidden = false;
        }
    } catch (e) {
        Formulario.mostrarErros(form, e.erros);
    }
    return false;
}

// ---------- rascunho: ir ao cadastro de paciente sem perder o lançamento ----------

const CHAVE_RASCUNHO = "aurora.rascunhoLancamento";

function guardarRascunho() {
    const rascunho = {
        data: form.data.value,
        profissional: buscaProfissional.escolhido,
        procedimento: buscaProcedimento.escolhido,
        cns: form.cnsPaciente.value.replace(/\D/g, ""),
        quantidade: form.quantidade.value,
        cid: form.cid.value,
        carater: form.caraterAtendimento.value,
    };
    try { sessionStorage.setItem(CHAVE_RASCUNHO, JSON.stringify(rascunho)); } catch (e) { /* sem armazenamento: segue sem rascunho */ }
}

/** Restaura o lançamento que estava sendo digitado antes de cadastrar o paciente. */
async function restaurarRascunho() {
    let rascunho = null;
    try {
        rascunho = JSON.parse(sessionStorage.getItem(CHAVE_RASCUNHO));
        sessionStorage.removeItem(CHAVE_RASCUNHO);
    } catch (e) { return; }
    if (!rascunho) return;
    form.data.value = rascunho.data || hojeIso();
    if (rascunho.profissional) buscaProfissional.definir(rascunho.profissional);
    if (rascunho.procedimento) buscaProcedimento.definir(rascunho.procedimento);
    form.quantidade.value = rascunho.quantidade || "1";
    form.cid.value = rascunho.cid || "";
    form.caraterAtendimento.value = rascunho.carater || "";
    atualizarDicaCompetencia();
    if (rascunho.cns) {
        form.cnsPaciente.value = rascunho.cns;
        if (await buscarPaciente()) {
            Formulario.sucesso(form, "Paciente encontrado. Confira os dados e salve o lançamento.");
        }
    }
}

document.getElementById("a-novo-paciente").addEventListener("click", guardarRascunho);
document.getElementById("buscar-paciente").addEventListener("click", buscarPaciente);
form.cnsPaciente.addEventListener("input", () => {
    limparPaciente();
    if (form.cnsPaciente.value.replace(/\D/g, "").length === 15) buscarPaciente();
});
form.cnsPaciente.addEventListener("keydown", e => {
    if (e.key === "Enter" && !form.pacienteId.value) { e.preventDefault(); buscarPaciente(); }
});

// ---------- salvar / limpar / corrigir ----------

function modoEdicao(ativo) {
    document.getElementById("titulo-lancamento").textContent = ativo ? "Corrigir lançamento" : "Lançar atendimento";
    document.getElementById("botao-salvar").textContent = ativo ? "Salvar correção" : "Salvar lançamento";
    document.getElementById("botao-limpar").textContent = ativo ? "Cancelar correção" : "Limpar";
}

/** Limpa o formulário. Por padrão mantém data, profissional e procedimento (lançamentos em sequência). */
function limpar(tudo) {
    Formulario.limparErros(form);
    form.id.value = "";
    form.quantidade.value = "1";
    form.idade.value = "";
    form.cid.value = "";
    form.caraterAtendimento.value = "";
    form.cnsPaciente.value = "";
    limparPaciente();
    linkNovoPaciente.hidden = true;
    if (tudo) {
        form.data.value = hojeIso();
        buscaProfissional.limpar();
        buscaProcedimento.limpar();
        procedimento = null;
    }
    atualizarDicaCompetencia();
    atualizarCamposDoProcedimento();
    modoEdicao(false);
}

document.getElementById("botao-limpar").addEventListener("click", () => {
    limpar(true);
    form.data.focus();
});

form.addEventListener("submit", async e => {
    e.preventDefault();
    const dados = Formulario.ler(form);
    const id = dados.id;

    // Paciente digitado mas ainda não buscado: busca antes de salvar.
    if (procedimento && procedimento.modalidade === "BPA_I" && dados.cnsPaciente && !dados.pacienteId) {
        if (!(await buscarPaciente())) return;
        dados.pacienteId = form.pacienteId.value;
    }

    const corpo = {
        data: dados.data,
        profissionalId: dados.profissionalId ? Number(dados.profissionalId) : null,
        procedimentoId: dados.procedimentoId ? Number(dados.procedimentoId) : null,
        quantidade: dados.quantidade !== null ? Number(dados.quantidade) : null,
        pacienteId: dados.pacienteId ? Number(dados.pacienteId) : null,
        idade: dados.idade !== null && dados.idade !== undefined ? Number(dados.idade) : null,
        cid: dados.cid ?? null,
        caraterAtendimento: dados.caraterAtendimento ?? null,
    };

    const botao = document.getElementById("botao-salvar");
    botao.disabled = true;
    try {
        const salvo = id ? await Api.put(`/api/atendimentos/${id}`, corpo) : await Api.post("/api/atendimentos", corpo);
        const comp = `${salvo.competencia.slice(4)}/${salvo.competencia.slice(0, 4)}`;
        limpar(false);
        Formulario.sucesso(form, `${id ? "Correção salva" : "Lançamento salvo"} na competência ${comp}.`);
        await carregarHoje();
        Layout.atualizarCompetencia();
        if (id && competenciaConsultada) carregarConsulta();
        (procedimento && procedimento.modalidade === "BPA_I" ? form.cnsPaciente : form.quantidade).focus();
    } catch (erro) {
        // Erros junto aos campos; nada do que foi digitado é apagado.
        Formulario.mostrarErros(form, erro.erros.map(x => ({ ...x, campo: CAMPO_NA_TELA[x.campo] || x.campo })));
    } finally {
        botao.disabled = false;
    }
});

async function editar(id) {
    try {
        const a = await Api.get(`/api/atendimentos/${id}`);
        limpar(true);
        form.id.value = a.id;
        form.data.value = a.data;
        buscaProfissional.definir({ id: a.profissionalId, nome: a.profissionalNome, cbo: a.profissionalCbo });
        buscaProcedimento.definir({ id: a.procedimentoId, codigo: a.procedimentoCodigo, descricao: a.procedimentoDescricao,
            modalidade: a.modalidade, exigeCid: a.exigeCid });
        if (a.pacienteId) definirPaciente({ id: a.pacienteId, cns: a.pacienteCns, nome: a.pacienteNome,
            dataNascimento: a.pacienteDataNascimento });
        form.quantidade.value = a.quantidade;
        if (a.modalidade === "BPA_C") form.idade.value = a.idade ?? "";
        form.cid.value = a.cid ?? "";
        form.caraterAtendimento.value = a.caraterAtendimento ?? "";
        atualizarDicaCompetencia();
        modoEdicao(true);
        selecionarAba(document.getElementById("aba-lancar"));
        form.data.focus();
    } catch (e) {
        mostrarMensagem(document.getElementById("msg-hoje"), "erro", e.erros.map(x => x.mensagem).join(" "));
    }
}

async function excluir(a, msgEl) {
    const descricao = `${dataBr(a.data)} – ${a.procedimentoCodigo} – ${a.profissionalNome}`;
    if (!window.confirm(`Excluir o lançamento ${descricao}?\n\nEsta ação não pode ser desfeita.`)) return;
    try {
        await Api.del(`/api/atendimentos/${a.id}`);
        mostrarMensagem(msgEl, "sucesso", `Lançamento ${descricao} excluído.`);
        await carregarHoje();
        if (competenciaConsultada) await carregarConsulta();
    } catch (e) {
        mostrarMensagem(msgEl, "erro", e.erros.map(x => x.mensagem).join(" "));
    }
}

// ---------- listas ----------

function celulas(a) {
    return `<td>${dataBr(a.data)}</td><td>${esc(a.profissionalNome)}</td>
        <td>${a.procedimentoCodigo} – ${esc(a.procedimentoDescricao)}</td>
        <td>${a.pacienteNome ? esc(a.pacienteNome) : "—"}</td><td>${a.quantidade}</td>`;
}

function acoes(a, bloqueado) {
    const rotulo = `<span class="visualmente-oculto"> lançamento de ${dataBr(a.data)}, ${esc(a.procedimentoCodigo)}</span>`;
    return `<td class="acoes"><div class="botoes-linha">
        <button type="button" class="link" data-editar="${a.id}"${bloqueado ? " disabled" : ""}>Editar${rotulo}</button>
        <button type="button" class="link" data-excluir="${a.id}"${bloqueado ? " disabled" : ""}>Excluir${rotulo}</button>
    </div></td>`;
}

let itensHoje = [];
async function carregarHoje() {
    itensHoje = await Api.get(`/api/atendimentos?data=${hojeIso()}`);
    listaHoje.innerHTML = itensHoje.length
        ? itensHoje.slice(0, 20).map(a => `<tr>${celulas(a)}${acoes(a, a.situacaoCompetencia === "FECHADA")}</tr>`).join("")
        : `<tr><td colspan="6" class="vazio">Nenhum lançamento hoje.</td></tr>`;
}

listaHoje.addEventListener("click", e => {
    const ed = e.target.closest("[data-editar]");
    const ex = e.target.closest("[data-excluir]");
    if (ed) editar(ed.dataset.editar);
    if (ex) excluir(itensHoje.find(a => String(a.id) === ex.dataset.excluir), document.getElementById("msg-hoje"));
});

// ---------- consulta por competência (RF10) ----------

const formConsulta = document.getElementById("form-consulta");
const listaConsulta = document.getElementById("lista-consulta");
let itensConsulta = [];

async function carregarConsulta() {
    const msg = document.getElementById("msg-consulta");
    const resumo = document.getElementById("resumo-consulta");
    msg.textContent = ""; msg.className = "mensagens";
    try {
        const [lista, situacao] = await Promise.all([
            Api.get(`/api/atendimentos?competencia=${competenciaConsultada}`),
            Api.get(`/api/competencias/situacao?mesAno=${competenciaConsultada}`),
        ]);
        itensConsulta = lista;
        const fechada = situacao.situacao === "FECHADA";
        const texto = `${competenciaConsultada.slice(4)}/${competenciaConsultada.slice(0, 4)}`;
        resumo.innerHTML = situacao.situacao
            ? `Competência ${texto} – <strong>${situacao.situacao}</strong> · ${lista.length} lançamento(s).`
              + (fechada ? " Competência fechada: para corrigir, peça ao responsável pela produção que a reabra." : "")
            : `Competência ${texto}: nenhum lançamento.`;
        listaConsulta.innerHTML = lista.length
            ? lista.map(a => `<tr>${celulas(a)}<td>${esc(a.registradoPor)}</td>${acoes(a, fechada)}</tr>`).join("")
            : `<tr><td colspan="7" class="vazio">Nenhum lançamento nesta competência.</td></tr>`;
    } catch (e) {
        mostrarMensagem(msg, "erro", e.erros.map(x => x.mensagem).join(" "));
    }
}

formConsulta.addEventListener("submit", e => {
    e.preventDefault();
    Formulario.limparErros(formConsulta);
    const v = formConsulta.competencia.value.trim();
    const m = v.match(/^(0[1-9]|1[0-2])\/(\d{4})$/);
    if (!m) {
        Formulario.mostrarErros(formConsulta, [{ campo: "competencia", mensagem: "Use o formato MM/AAAA (ex.: 11/2026)." }]);
        return;
    }
    competenciaConsultada = `${m[2]}${m[1]}`;
    carregarConsulta();
});

listaConsulta.addEventListener("click", e => {
    const ed = e.target.closest("[data-editar]");
    const ex = e.target.closest("[data-excluir]");
    if (ed) editar(ed.dataset.editar);
    if (ex) excluir(itensConsulta.find(a => String(a.id) === ex.dataset.excluir), document.getElementById("msg-consulta"));
});

// ---------- abas ----------

const abas = [...document.querySelectorAll('[role="tab"]')];
function selecionarAba(aba) {
    for (const a of abas) {
        const ativa = a === aba;
        a.setAttribute("aria-selected", String(ativa));
        a.tabIndex = ativa ? 0 : -1;
        document.getElementById(a.getAttribute("aria-controls")).hidden = !ativa;
    }
}
abas.forEach((aba, i) => {
    aba.addEventListener("click", () => selecionarAba(aba));
    aba.addEventListener("keydown", e => {
        const alvo = e.key === "ArrowRight" ? abas[(i + 1) % abas.length]
            : e.key === "ArrowLeft" ? abas[(i - 1 + abas.length) % abas.length] : null;
        if (alvo) { e.preventDefault(); alvo.focus(); selecionarAba(alvo); }
    });
});

// ---------- início ----------

form.data.addEventListener("input", atualizarDicaCompetencia);

(async () => {
    usuario = await Layout.iniciar("atendimentos", ["OPERADOR", "RESPONSAVEL_PRODUCAO"]);
    if (!usuario) return;
    form.data.value = hojeIso();
    form.data.max = hojeIso();
    const hoje = new Date();
    formConsulta.competencia.value = `${String(hoje.getMonth() + 1).padStart(2, "0")}/${hoje.getFullYear()}`;
    const carater = await Api.get("/api/tabelas/carater_atendimento");
    form.caraterAtendimento.insertAdjacentHTML("beforeend",
        carater.map(c => `<option value="${c.codigo}">${c.codigo} – ${esc(c.descricao)}</option>`).join(""));
    limpar(true);
    await restaurarRascunho();
    await carregarHoje();
    form.data.focus();
})();
