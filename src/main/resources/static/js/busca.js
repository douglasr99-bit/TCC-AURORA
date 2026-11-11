// Aurora — campo de busca com lista de resultados (profissional, procedimento).
// Teclado: digitar busca; seta para baixo entra na lista; Enter escolhe; Esc fecha.
"use strict";

/**
 * @param {object} cfg
 *   campo: input de texto onde se digita a busca
 *   oculto: input hidden que recebe o id escolhido
 *   url: rota da API (recebe ?q=)
 *   rotulo: (item) => texto mostrado no campo depois da escolha
 *   linha: (item) => HTML de cada resultado
 *   aoEscolher: (item|null) => void
 */
function CampoBusca(cfg) {
    const { campo, oculto } = cfg;
    const lista = document.createElement("ul");
    lista.className = "resultados-busca";
    lista.id = `${campo.id}-resultados`;
    lista.hidden = true;
    const aviso = document.createElement("p");
    aviso.className = "visualmente-oculto";
    aviso.setAttribute("aria-live", "polite");
    campo.insertAdjacentElement("afterend", lista);
    lista.insertAdjacentElement("afterend", aviso);
    campo.setAttribute("autocomplete", "off");
    campo.setAttribute("aria-controls", lista.id);
    campo.setAttribute("aria-expanded", "false");

    let itens = [];
    let escolhido = null;
    let espera = null;
    let ultimaBusca = "";

    function fechar() {
        lista.hidden = true;
        campo.setAttribute("aria-expanded", "false");
    }

    function mostrar(resultados) {
        itens = resultados;
        lista.innerHTML = resultados.length
            ? resultados.map((item, i) => `<li><button type="button" data-indice="${i}">${cfg.linha(item)}</button></li>`).join("")
            : `<li class="vazio">Nada encontrado.</li>`;
        lista.hidden = false;
        campo.setAttribute("aria-expanded", "true");
        aviso.textContent = resultados.length
            ? `${resultados.length} resultado(s). Use a seta para baixo para escolher.`
            : "Nada encontrado.";
    }

    async function buscar() {
        const termo = campo.value.trim();
        if (termo.length < 2 || termo === ultimaBusca) return;
        ultimaBusca = termo;
        try {
            mostrar(await Api.get(`${cfg.url}?q=${encodeURIComponent(termo)}`));
        } catch (e) {
            aviso.textContent = "Não foi possível buscar agora.";
        }
    }

    function escolher(item) {
        escolhido = item;
        oculto.value = item ? item.id : "";
        if (item) campo.value = cfg.rotulo(item);
        ultimaBusca = campo.value.trim();
        fechar();
        cfg.aoEscolher(item);
    }

    campo.addEventListener("input", () => {
        if (escolhido) { escolhido = null; oculto.value = ""; cfg.aoEscolher(null); }
        clearTimeout(espera);
        espera = setTimeout(buscar, 300);
    });

    campo.addEventListener("keydown", e => {
        if (e.key === "ArrowDown" && !lista.hidden) {
            e.preventDefault();
            const primeiro = lista.querySelector("button");
            if (primeiro) primeiro.focus();
        } else if (e.key === "Escape") {
            fechar();
        } else if (e.key === "Enter" && !lista.hidden && itens.length) {
            // Com a lista aberta, Enter escolhe o primeiro resultado em vez de salvar o formulário.
            e.preventDefault();
            escolher(itens[0]);
        }
    });

    lista.addEventListener("click", e => {
        const botao = e.target.closest("button[data-indice]");
        if (botao) { escolher(itens[Number(botao.dataset.indice)]); campo.focus(); }
    });

    lista.addEventListener("keydown", e => {
        const botoes = [...lista.querySelectorAll("button")];
        const i = botoes.indexOf(document.activeElement);
        if (e.key === "ArrowDown" && i < botoes.length - 1) { e.preventDefault(); botoes[i + 1].focus(); }
        if (e.key === "ArrowUp") { e.preventDefault(); (i > 0 ? botoes[i - 1] : campo).focus(); }
        if (e.key === "Escape") { fechar(); campo.focus(); }
    });

    document.addEventListener("click", e => {
        if (!lista.contains(e.target) && e.target !== campo) fechar();
    });

    return {
        get escolhido() { return escolhido; },
        definir(item) { escolher(item); },
        limpar() { escolhido = null; oculto.value = ""; campo.value = ""; ultimaBusca = ""; fechar(); },
    };
}
