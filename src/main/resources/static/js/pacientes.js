// Aurora — tela de Pacientes (RF05).
"use strict";

const NACIONALIDADE_BRASILEIRA = "010";
const RACA_INDIGENA = "05";

const form = document.getElementById("form-pacientes");
const blocoEtnia = document.getElementById("bloco-etnia");

function formatarCns(cns) {
    return cns.replace(/(\d{3})(\d{4})(\d{4})(\d{4})/, "$1 $2 $3 $4");
}

function formatarData(iso) {
    const [a, m, d] = iso.split("-");
    return `${d}/${m}/${a}`;
}

/** Etnia só aparece (e só é enviada) para indígena de nacionalidade brasileira. */
function atualizarEtnia() {
    const exige = form.racaCor.value === RACA_INDIGENA && form.nacionalidade.value === NACIONALIDADE_BRASILEIRA;
    blocoEtnia.hidden = !exige;
    form.etnia.disabled = !exige;
}

async function preencherSelect(select, tabela) {
    const itens = await Api.get(`/api/tabelas/${tabela}`);
    select.insertAdjacentHTML("beforeend",
        itens.map(i => `<option value="${esc(i.codigo)}">${esc(i.codigo)} – ${esc(i.descricao)}</option>`).join(""));
}

const pacientes = CadastroLista({
    nome: "pacientes",
    url: "/api/pacientes",
    comBusca: true,
    titulos: { novo: "Novo paciente", editar: "Editar paciente" },
    nomeItem: p => p.nome,
    linha: p => `<td>${esc(p.nome)}</td><td>${formatarCns(p.cns)}</td>
        <td>${formatarData(p.dataNascimento)}</td><td>${p.sexo === "F" ? "Feminino" : "Masculino"}</td>`,
    aoAbrir: (f, item) => {
        if (!item) f.nacionalidade.value = NACIONALIDADE_BRASILEIRA;
        atualizarEtnia();
    },
});

form.racaCor.addEventListener("change", atualizarEtnia);
form.nacionalidade.addEventListener("change", atualizarEtnia);

(async () => {
    const usuario = await Layout.iniciar("pacientes", ["OPERADOR"]);
    if (!usuario) return;
    await Promise.all([
        preencherSelect(form.nacionalidade, "nacionalidade"),
        preencherSelect(form.racaCor, "raca_cor"),
        preencherSelect(form.etnia, "etnia"),
    ]);
    pacientes.mostrar();
})();
