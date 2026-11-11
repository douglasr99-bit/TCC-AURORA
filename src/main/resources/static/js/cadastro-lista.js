// Aurora — componente reutilizável de cadastro com lista, busca e formulário de inclusão/edição.
"use strict";

/**
 * @param {object} cfg
 *   nome: sufixo dos ids (lista-<nome>, form-<nome>, msg-<nome>)
 *   url: rota da API
 *   linha: função (item) => HTML das células (sem <tr>)
 *   titulos: { novo, editar }
 *   comBusca: se a API aceita ?q=
 *   aoAbrir: função (form, item|null) chamada ao abrir o formulário
 *   nomeItem: função (item) => texto usado na mensagem de sucesso
 */
function CadastroLista(cfg) {
    const lista = document.getElementById(`lista-${cfg.nome}`);
    const form = document.getElementById(`form-${cfg.nome}`);
    const mensagem = document.getElementById(`msg-${cfg.nome}`);
    const busca = document.querySelector(`[data-busca="${cfg.nome}"]`);
    let termo = "";
    let carregado = false;

    async function carregar() {
        const url = cfg.comBusca && termo ? `${cfg.url}?q=${encodeURIComponent(termo)}` : cfg.url;
        try {
            const itens = await Api.get(url);
            lista.innerHTML = itens.length
                ? itens.map(i => `<tr>${cfg.linha(i)}<td class="acoes">
                    <button type="button" class="link" data-editar="${i.id}">Editar<span class="visualmente-oculto"> ${esc(cfg.nomeItem(i))}</span></button></td></tr>`).join("")
                : `<tr><td colspan="5" class="vazio">${termo ? "Nada encontrado para esta busca." : "Nenhum registro cadastrado ainda."}</td></tr>`;
            carregado = true;
        } catch (e) {
            mensagem.className = "mensagens erro";
            mensagem.textContent = e.erros.map(x => x.mensagem).join(" ");
        }
    }

    function abrir(item) {
        Formulario.limparErros(form);
        form.reset();
        form.elements.id.value = "";
        if (item) Formulario.preencher(form, item);
        form.querySelector("[data-titulo]").textContent = item ? cfg.titulos.editar : cfg.titulos.novo;
        if (cfg.aoAbrir) cfg.aoAbrir(form, item);
        form.hidden = false;
        mensagem.textContent = "";
        mensagem.className = "mensagens";
        const primeiro = [...form.elements].find(el => el.type !== "hidden" && !el.disabled);
        primeiro.focus();
    }

    function fechar() {
        form.hidden = true;
    }

    lista.addEventListener("click", async e => {
        const botao = e.target.closest("[data-editar]");
        if (!botao) return;
        try {
            abrir(await Api.get(`${cfg.url}/${botao.dataset.editar}`));
        } catch (erro) {
            mensagem.className = "mensagens erro";
            mensagem.textContent = erro.erros.map(x => x.mensagem).join(" ");
        }
    });

    document.querySelector(`[data-novo="${cfg.nome}"]`).addEventListener("click", () => abrir(null));
    form.querySelector("[data-cancelar]").addEventListener("click", fechar);

    if (busca) {
        busca.addEventListener("submit", e => {
            e.preventDefault();
            termo = busca.elements.q.value.trim();
            carregar();
        });
    }

    form.addEventListener("submit", async e => {
        e.preventDefault();
        const dados = Formulario.ler(form);
        const id = dados.id;
        delete dados.id;
        const botao = form.querySelector("button[type=submit]");
        botao.disabled = true;
        try {
            const salvo = id ? await Api.put(`${cfg.url}/${id}`, dados) : await Api.post(cfg.url, dados);
            fechar();
            await carregar();
            mensagem.className = "mensagens sucesso";
            mensagem.textContent = `${cfg.nomeItem(salvo)} ${id ? "atualizado" : "cadastrado"} com sucesso.`;
        } catch (erro) {
            // O que foi digitado continua no formulário.
            Formulario.mostrarErros(form, erro.erros);
        } finally {
            botao.disabled = false;
        }
    });

    return {
        mostrar() { if (!carregado) carregar(); },
    };
}
