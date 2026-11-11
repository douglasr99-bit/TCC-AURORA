# Atualizações do texto do TCC

> O texto completo das seções 14 a 18, já redigido, está em `docs/secoes-14-a-18.txt`. Nele, o quadro de casos de teste é o Quadro 15, e as referências do Ministério da Saúde de 2026 passam a ser BRASIL, 2026a (layout), 2026b (SIGTAP), 2026c (BDSIA) e 2026d (BPA Magnético).

Este documento reúne o que precisa entrar no texto do TCC (`TCC_Aurora_revisado-add-V1.txt`) depois da implementação. A parte 1 é a tabela da seção 16, pronta para copiar. A parte 2 lista, seção por seção, o que mudar, com o motivo detalhado em `docs/registro-implementacao.md`.

---

## Parte 1 — Seção 16 (Testes e validação)

### Texto sugerido

A validação do Aurora combinou três tipos de teste, todos com dados fictícios:

- **Testes unitários (JUnit)** das regras isoladas: validação do CNS e do dígito verificador do procedimento, regras do cadastro de paciente, do lançamento de atendimentos e de usuários (com os repositórios simulados), e a montagem do arquivo BPA, conferida campo a campo contra o layout de exportação do DATASUS.
- **Testes de integração**, executados contra um banco PostgreSQL 18 de teste, cobrindo a API REST com autenticação, perfis, CSRF, lançamento, competência e geração do arquivo. Cada teste é desfeito ao final (rollback), de modo que um não interfere no outro.
- **Conferência pela interface**, realizada no navegador ao fim de cada incremento, percorrendo os fluxos principais: cadastros, lançamento BPA-C e BPA-I, cadastro de paciente a partir do lançamento, correção, conferência, fechamento, geração do arquivo, reabertura e histórico.

Ao todo, foram executados **91 testes automatizados, todos aprovados**. A geração do arquivo de uma competência com 3.000 atendimentos levou **121 ms**, bem abaixo do limite de 10 segundos do RNF07. O Quadro X resume os casos de teste; a saída completa e as capturas de tela estão no repositório, em `docs/evidencias/`.

### Quadro X – Casos de teste

| ID | Funcionalidade | Procedimento | Resultado esperado | Resultado obtido | Status |
|---|---|---|---|---|---|
| T01 | RF01 Autenticação | Login com usuário ativo e senha correta | Acesso liberado, com nome e perfis do usuário | Acesso liberado (HTTP 200) com os perfis | Aprovado |
| T02 | RF01 Autenticação | Login com senha incorreta | Recusa com a mensagem genérica "Login ou senha incorretos." | Recusa (401) com a mensagem genérica | Aprovado |
| T03 | RF01 Autenticação | Login com usuário inexistente | Mesma mensagem genérica, sem revelar se o login existe | Mesma mensagem genérica | Aprovado |
| T04 | RF01/RF02 Autenticação | Login de usuário inativo | Acesso recusado com a mensagem genérica | Recusa (401) com a mensagem genérica | Aprovado |
| T05 | RNF04 Segurança | Usuário logado é desativado pelo administrador | Perde o acesso na próxima ação, com aviso | Próxima requisição recusada (401) com "Seu acesso foi alterado pelo administrador. Entre novamente." | Aprovado |
| T06 | RNF04 Autorização | Operador acessa o cadastro de usuários | Acesso negado | 403 com "Seu perfil não tem permissão para esta ação." | Aprovado |
| T07 | RNF04 Autorização | Operador consulta e tenta alterar o estabelecimento | Consulta permitida, alteração negada | Consulta 200, alteração 403 | Aprovado |
| T08 | RNF04 Autorização | Administrador (sem perfil Operador) acessa pacientes | Acesso negado | 403 | Aprovado |
| T09 | RNF04 Segurança | Requisição que altera dados sem token CSRF | Recusada | 403 | Aprovado |
| T10 | RF08 Lançamento | Procedimento BPA-I sem paciente | Erro no campo do paciente | Erro no campo, nada gravado | Aprovado |
| T11 | RF08 Lançamento | Procedimento BPA-C sem idade | Erro no campo idade | Erro "Informe a idade do paciente atendido (exigida no BPA-C)." | Aprovado |
| T12 | RF08 Lançamento | Procedimento que exige CID, sem CID | Erro no campo CID-10 | Erro "Este procedimento exige o CID-10…" | Aprovado |
| T13 | RF05/RF09 Validação | CNS do paciente com 14 dígitos | Erro no campo CNS, com orientação | 400 no campo `cns`: "O CNS deve ter 15 dígitos, só números. Confira o cartão do paciente." | Aprovado |
| T14 | RF08 Validação | CNS com dígito errado, tamanho errado, letras ou zeros | Recusado | Todos os 9 casos recusados | Aprovado |
| T15 | RF06/RF08 Validação | Código de procedimento com dígito verificador inválido | Erro no campo código | 400 no campo `codigo` | Aprovado |
| T15b | RF08 Validação | Validar os 5.024 códigos da Tabela Unificada do SIGTAP (09/2026) | Todos aceitos | 5.024 de 5.024 aceitos | Aprovado |
| T16 | RF08 Lançamento | Quantidade 0 e quantidade negativa | Erro no campo quantidade | "A quantidade deve ser maior que zero." nos dois casos | Aprovado |
| T17 | RF08 Lançamento | Data do atendimento no futuro | Erro no campo data | Erro "…não pode ser no futuro…" | Aprovado |
| T18 | RF07 Competência | Primeiro lançamento de um mês sem competência | Competência criada com situação ABERTA | Competência criada ABERTA; lançamento gravado com o usuário | Aprovado |
| T19 | RF07 Competência | Atendimento de novembro lançado em dezembro, novembro ABERTA | Entra na competência de novembro | Gravado em 202611 | Aprovado |
| T20 | RF07/RF12 Competência | Atendimento de novembro lançado em dezembro, novembro FECHADA | Recusado, orientando a reabrir | Recusado: "A competência 11/2026 está fechada… peça… que reabra a competência." | Aprovado |
| T21 | RF10/RF12 Competência | Lançar, corrigir e excluir em competência FECHADA | As três operações recusadas | As três recusadas; lançamento preservado | Aprovado |
| T22 | RF13 Arquivo | Gerar arquivo com a competência ABERTA | Recusado com mensagem | 400: "Feche a competência antes de gerar o arquivo." | Aprovado |
| T23 | RF13/RNF03 Arquivo | Conferir os tamanhos das linhas e a codificação | Cabeçalho 130, BPA-C 48 e BPA-I 351 posições + CR LF; só ASCII | Conforme o esperado | Aprovado |
| T24 | RF13/RNF03 Arquivo | Conferir o cabeçalho campo a campo | Dados do estabelecimento, competência, totais de linhas e folhas e campo de controle nas posições do layout | Todos os campos conferidos; controle igual a (soma dos códigos + quantidades) mod 1111 + 1111 | Aprovado |
| T25 | RF13/RNF03 Arquivo | Atendimentos BPA-C com mesmo CBO, procedimento e idade | Consolidados em uma linha, com as quantidades somadas | 2 + 5 consolidados em uma linha com quantidade 000007 | Aprovado |
| T26 | RF13/RNF03 Arquivo | Conferir o registro BPA-I campo a campo | Todos os campos nas posições do layout; textos em maiúsculas sem acento | Todos os campos conferidos ("JOSE DA CONCEICAO", número vazio "SN") | Aprovado |
| T27 | RF15 Reabertura | Reabrir sem motivo e depois com motivo | Sem motivo: erro; com motivo: reabertura registrada e competência ABERTA | Erro no campo `motivo`; depois reabertura gravada com data, usuário e motivo | Aprovado |
| T28 | RF11 Conferência | Resumo de competência com BPA-C e BPA-I | Totais por modalidade, por procedimento e por profissional | Totais corretos (12 = 10 BPA-C + 2 BPA-I) | Aprovado |
| T29 | RNF07/RF14 Desempenho | Gerar o arquivo de 3.000 atendimentos e baixá-lo de novo pelo histórico | Até 10 s; arquivo do histórico idêntico ao gerado | 121 ms; arquivo idêntico | Aprovado |
| T30 | RF10 Consulta | Consultar, corrigir e excluir na competência aberta | As três operações funcionam | Consulta, correção e exclusão realizadas | Aprovado |
| T31 | RF13 Arquivo | Paciente indígena com etnia da tabela do SIA (X411) | Etnia gravada no registro BPA-I | "X411" nas posições 153–156 | Aprovado |
| T32 | RF05/RNF03 Validação | Nome do paciente acima de 30 caracteres | Recusado (o sistema nunca trunca em silêncio) | 400 no campo `nome` | Aprovado |

Além dos casos do quadro, a suíte tem testes complementares (regras de etnia, raça/cor e nacionalidade, CID-10 inexistente, caráter de atendimento, profissional inativo, idade calculada no BPA-I, folhas do BPA-C e do BPA-I, limite de 999 folhas, transições inválidas de competência, regras do cadastro de usuários). Todos estão listados em `docs/evidencias/final/testes.txt`.

### Conferência pela interface (resumo para o texto)

Nos três incrementos, os fluxos principais foram percorridos no navegador com dados fictícios. Os pontos verificados estão no registro de implementação e nas capturas de `docs/evidencias/incremento-1..3/`. Dois problemas foram encontrados por essa conferência e corrigidos:

- **Perda dos dados digitados:** ao ir do lançamento para o cadastro de um paciente novo, o que já tinha sido digitado se perdia. Isso feria o RF09. Agora o lançamento é guardado e restaurado na volta.
- **Sessão de usuário desativado:** um usuário desativado continuava com a sessão aberta até ela expirar. Agora as sessões são encerradas na hora (T05).

---

## Parte 2 — O que mudar em cada seção

### 7.3 Requisitos funcionais
- **RF05:** acrescentar **nacionalidade** e **etnia (para indígenas)** aos dados do paciente.
- **RF07:** acrescentar o **caráter do atendimento** nos procedimentos BPA-I, e dizer que no BPA-I a idade é calculada pela data de nascimento.
- **RF08:** citar também a validação do **CNS** (algoritmo publicado pela ANS) e do **CID-10** contra a tabela do SIA.

### 8 Metodologia
- **Etapa 1:** acrescentar as fontes oficiais usadas na implementação: Tabela Unificada do SIGTAP, tabelas do SIA (pacote BDSIA), programa BPA Magnético e suas notas de versão, Portaria SAS nº 508/2010 e o documento de algoritmos da ANS.
- **Etapa 4:** o texto diz que o código "será hospedado no GitHub". **O repositório ainda não está no GitHub**, só local. Publique antes da entrega e informe o endereço na seção 15.
- **Etapa 5:** acrescentar os testes de integração com PostgreSQL e a conferência pela interface.
- Passar os verbos do futuro para o passado ("serão elaborados" → "foram elaborados").

### 9 Tecnologias e ferramentas (Quadro 2)
- **Versões efetivas:** Spring Boot **4.1.2** (Spring Framework 7.0, Spring Security 7.1, Hibernate 7.4, Jackson 3.1) e Java 25.0.5 LTS (Amazon Corretto).
- **JUnit:** citar que vem com o `spring-boot-starter-test`, junto com Mockito e AssertJ, usados nos testes.
- **keytool** (vem com o Java): geração do certificado HTTPS.
- *Opcional:* citar que, na máquina de desenvolvimento, o PostgreSQL 18 rodou em container Docker (o repositório oficial não publica pacotes para o Ubuntu 25.04). Não é tecnologia do sistema e não muda o Quadro 2.

### 11.2 Diagrama de classes (Figura 3) e 12.1/12.2 Modelos (Figuras 6 e 7)
- **Paciente:** acrescentar `nacionalidade` e `etnia`.
- **Atendimento:** acrescentar `caraterAtendimento`.
- Refazer as três figuras no draw.io.

### 11.3 Diagrama de sequência
- Os nomes `ExportacaoController` e `ExportacaoBpaService` agora são **os nomes reais** das classes. A frase "os nomes das classes de controle e de serviço são indicativos" pode sair.

### 12.3 Dicionário de dados
**Quadro 8 (paciente)**: os tamanhos passam a seguir o layout do BPA-I, e há dois campos novos:

| Campo | Tipo | Descrição | Restrição |
|---|---|---|---|
| nome | **VARCHAR(30)** | Nome do paciente (limite do layout do BPA-I) | NOT NULL |
| raca_cor | CHAR(2) | Código de raça/cor (01 a 05, 99) | NOT NULL, **CHECK (01, 02, 03, 04, 05, 99)** |
| **etnia** | **CHAR(4)** | Etnia, conforme a tabela do SIA; só para raça/cor indígena e nacionalidade brasileira | Obrigatório nesse caso (verificado no serviço) |
| **nacionalidade** | **CHAR(3)** | Código de nacionalidade conforme a tabela do SIA (010 = Brasil) | NOT NULL |
| logradouro | **VARCHAR(30)** | Logradouro de residência | — |
| numero | **VARCHAR(5)** | Número da residência | — |
| bairro | **VARCHAR(30)** | Bairro de residência | — |

**Quadro 11 (atendimento)**: um campo novo e uma descrição ajustada:

| Campo | Tipo | Descrição | Restrição |
|---|---|---|---|
| idade | INTEGER | Idade do paciente: informada no BPA-C, calculada pela data de nascimento no BPA-I | CHECK (>= 0) |
| **carater_atendimento** | **CHAR(2)** | Caráter do atendimento (01 a 06, tabela do SIA), obrigatório nos procedimentos BPA-I | — |

**Quadro 10 (competencia)**: em `data_fechamento`, trocar "Obrigatório quando FECHADA" por "Preenchido no fechamento e apagado na reabertura (controlado pelo serviço)".

**Frase nova em 12.2** (decisões do modelo lógico): as tabelas de códigos do SIA (nacionalidade, etnia, caráter de atendimento, CID-10) não foram modeladas no banco. Elas são carregadas de arquivos da aplicação, extraídos da tabela oficial do SIA, e usadas na validação.

### 13 Prototipação e interface
- **13.2 (lançamento):** acrescentar que o paciente é buscado **pelo CNS** e que, se não estiver cadastrado, o sistema oferece "Cadastrar novo paciente" e **restaura o lançamento** na volta. Também que, depois de salvar, data, profissional e procedimento continuam preenchidos para o próximo lançamento.
- **13.2 (competência):** a tela implementada também mostra a **produção por profissional**, exigida pelo RF11, e a lista de reaberturas.
- Se quiser, inclua capturas das telas implementadas ao lado dos wireframes (`docs/evidencias/`).

### 14 Desenvolvimento e implementação
O conteúdo está pronto em `docs/registro-implementacao.md`, organizado por incremento e por módulo (objetivo, tecnologias, implementação, resultado). Os destaques para o texto:
- **Dígito verificador do procedimento:** nenhum documento oficial descreve o cálculo. A regra foi **verificada contra os 5.024 procedimentos da Tabela Unificada do SIGTAP** e deve ser descrita assim, não como norma.
- **Tabela de etnias:** o SIA usa uma tabela **mais atual que a Portaria 508/2010**, com 473 etnias contra 264, incluindo códigos com letra. Use a frase sugerida no registro.
- **Arquivo BPA:** codificação ASCII, consolidação do BPA-C, folhas (BPA-C com 20 linhas; BPA-I com 99 linhas e uma folha por profissional) e campo de controle.

### 15 Versionamento e repositório (texto sugerido)
O código foi versionado com Git. A branch `main` recebe o trabalho concluído, e cada incremento foi desenvolvido em uma branch própria (`incremento-1`, `incremento-2`, `incremento-3`; correções em branches como `sessoes-usuario`), mesclada na `main` ao terminar, preservando o histórico de cada etapa. Os commits são pequenos, escritos no imperativo e em português (ex.: "Adiciona validação de CNS no lançamento"). O repositório contém o código, o `README.md` (como executar, variáveis de ambiente, banco, certificado e testes), o registro de implementação, o guia de implantação, as evidências dos testes e as fontes oficiais usadas. Senhas, o arquivo `.env` e o keystore do certificado ficam fora do repositório (`.gitignore`). *(Falta: publicar no GitHub e informar o endereço.)*

### 17 e 18 Resultados e discussão (pontos para desenvolver)
- **Implementação:** todos os requisitos funcionais (RF01 a RF15) foram implementados e todos os 91 testes passaram.
- **Desempenho:** o RNF07 foi atendido com folga (121 ms para 3.000 atendimentos).
- **Prevenção de erros:** a validação no momento do lançamento evita os principais erros de digitação apontados no problema: CNS, código do procedimento, CID inexistente, campos obrigatórios por modalidade e competência errada.
- **Documentação oficial:** ela é dispersa e em parte desatualizada (etnias, dígito verificador do procedimento, codificação do arquivo), o que exigiu buscar fontes complementares (SIGTAP, tabelas do SIA, BPA Magnético, ANS).
- **Validação:** o arquivo foi validado contra o layout, mas **não foi importado no SIA**. Isso limita a afirmação de conformidade (ver Limitações).

### 19 Segurança, privacidade e ética
- **19.1:** acrescentar que, ao desativar um usuário ou alterar seus perfis ou senha, as sessões abertas dele são **encerradas na hora**. Também que o sistema usa **proteção CSRF**, porque a autenticação é por cookie de sessão.
- **19.2:** o HTTPS fica **ligado por padrão** (porta 8443). O servidor envia o cabeçalho **HSTS**, e o cookie de sessão é `Secure`, `HttpOnly` e `SameSite=Strict`.
- **19.3:** está coerente com a implementação. Os logs não registram dados de pacientes.

### 20 Limitações (itens para acrescentar)
- O arquivo gerado foi conferido campo a campo contra o layout, mas **não foi importado no SIA nem no BPA Magnético** durante o trabalho. *(Remova este item se conseguir fazer a importação.)*
- O paciente é identificado **só pelo CNS**. O CPF, que alguns procedimentos exigem, não é registrado.
- Campos opcionais do BPA-I não tratados saem em branco: **INE** (equipe), serviço/classificação, número de autorização, complemento, e-mail e situação de rua.
- **CNES, CNPJ e código do município** são validados só no formato. O CNPJ alfanumérico (Receita Federal, 2026) não é aceito, porque o layout do BPA ainda prevê apenas números.
- As tabelas de códigos do SIA (CID-10, etnia, nacionalidade, caráter de atendimento) correspondem à **competência 10/2026**. Atualizá-las exige substituir os arquivos na aplicação.
- A regra do dígito verificador do procedimento foi **verificada empiricamente**, não obtida de documento oficial.
- A compatibilidade com **Firefox e Edge** (RNF06) precisa ser conferida. A conferência pela interface foi feita em navegador baseado em Chromium. *(Teste nos três navegadores antes da entrega e remova este item.)*
- Mudança de perfil do próprio usuário encerra a sessão dele, exigindo novo login.

### 21 Trabalhos futuros (itens para acrescentar)
- Atualização automática das tabelas do SIA a cada competência (pacote BDSIA).
- Registro do CPF do paciente para os procedimentos que o exigem.
- Validação do município de residência contra a tabela do IBGE (disponível no pacote BDSIA).
- Suporte ao CNPJ alfanumérico quando o layout do BPA for atualizado.
- *Remover* "Rotina automatizada de cópias de segurança": o guia de implantação já traz a rotina agendada com `pg_dump` (Apêndice A).

### 23 Cronograma
| Etapa | Data prevista | Data realizada | Status |
|---|---|---|---|
| Desenvolvimento | 10/11 a 26/11/2026 | 10/11 a 11/11/2026 | Concluída |
| Testes | 12/11 a 28/11/2026 | 11/11/2026 (automatizados e conferência pela interface) | Concluída *(faltam: navegadores Firefox/Edge e importação no SIA)* |
| Documentação | 09/11 a 01/12/2026 | — | Em andamento |

### 24 Referências (novas entradas, conferir a formatação ABNT)
- AGÊNCIA NACIONAL DE SAÚDE SUPLEMENTAR (ANS). *Algoritmos: aplicativo de carga SIB-XML*. Rio de Janeiro: ANS, 2026. Disponível em: https://www.gov.br/ans/pt-br/arquivos/assuntos/espaco-da-operadora-de-plano-de-saude/aplicativos-ans/sib/sibalgoritmosxml1002072026.pdf. Acesso em: 11 nov. 2026.
- BRASIL. Ministério da Saúde. Secretaria de Atenção à Saúde. Portaria nº 508, de 28 de setembro de 2010. Inclui o campo Etnia nos Sistemas de Informação Ambulatorial e Hospitalar (SIA/SUS e SIH/SUS). *Diário Oficial da União*: seção 1, Brasília, DF, 29 set. 2010.
- BRASIL. Ministério da Saúde. Departamento de Informática do SUS (DATASUS). *Tabela Unificada de Procedimentos, Medicamentos e OPM do SUS (SIGTAP)*: competência 09/2026. Brasília, DF: DATASUS, 2026. Disponível em: ftp://ftp2.datasus.gov.br/pub/sistemas/tup/downloads/. Acesso em: 11 nov. 2026.
- BRASIL. Ministério da Saúde. Departamento de Informática do SUS (DATASUS). *BDSIA: tabelas do Sistema de Informações Ambulatoriais*: competência 10/2026. Brasília, DF: DATASUS, 2026. Disponível em: ftp://arpoador.datasus.gov.br/siasus/SIA/. Acesso em: 11 nov. 2026.
- BRASIL. Ministério da Saúde. Departamento de Informática do SUS (DATASUS). *BPA Magnético*: versão 05.00 e notas de versão. Brasília, DF: DATASUS, 2026. Disponível em: ftp://arpoador.datasus.gov.br/siasus/BPA/. Acesso em: 11 nov. 2026.

### 25 Apêndices
- **Apêndice A – Guia de implantação:** pronto em `docs/guia-implantacao.md`.
- **Apêndice B – Manual do usuário:** pronto em `docs/apendice-b-manual-usuario.md`.
- **Apêndice C – Documentação da API REST:** pronto em `docs/apendice-c-api.md` (rotas conferidas contra os controladores e o `SecurityConfig`).

### 28 Checklist final
- [x] Os testes possuem evidências (`docs/evidencias/`).
- [x] O repositório está organizado. *(Falta publicar no GitHub.)*
- [ ] A implementação está documentada: falta passar o registro para a seção 14.
- [ ] A versão final corresponde ao sistema apresentado: depende das atualizações desta lista (diagramas e dicionário).
