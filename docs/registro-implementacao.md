# Registro de implementação — Aurora

Este registro alimenta a seção 14 (Implementação) do TCC. Para cada módulo: objetivo, tecnologias utilizadas, descrição da implementação e resultado obtido.

## Ambiente de desenvolvimento

- **Sistema operacional:** Ubuntu 25.04 (Linux 6.14).
- **Java:** 25.0.5 LTS (Amazon Corretto, distribuição do OpenJDK).
- **Maven:** 3.9.16.
- **PostgreSQL:** 18.6, executado em um container Docker (imagem oficial `postgres:18`) apenas na máquina de desenvolvimento. Porta local 5433.

- **Versões efetivas:** Spring Boot 4.1.2 (Spring Framework 7.0.10, Spring Security 7.1.2, Hibernate ORM 7.4.13, Jackson 3.1.7), driver PostgreSQL 42.7.14. Projeto criado pelo Spring Initializr com Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Security e Validation.

## Módulos

### Incremento 1 (11/11/2026) — base do projeto, segurança e cadastros

**Persistência**
- *Objetivo:* criar o banco conforme o dicionário de dados, com as restrições no próprio banco.
- *Tecnologias:* PostgreSQL 18, Spring Data JPA, Hibernate 7.
- *Implementação:* `schema.sql` com `CREATE TABLE IF NOT EXISTS`, `CHECK` e `UNIQUE`, executado na inicialização; `ddl-auto=validate` (o Hibernate só confere o mapeamento). Entidades do diagrama de classes no pacote `domain`; `AttributeConverter` para `YearMonth` ↔ `CHAR(6)` (AAAAMM) e para `Sexo` ↔ `CHAR(1)`; colunas `CHAR(n)` mapeadas com `@JdbcTypeCode(SqlTypes.CHAR)`.
- *Resultado:* esquema criado e validado pelo Hibernate na inicialização.

**Segurança (RF01, RNF04)**
- *Objetivo:* login com perfis, senhas protegidas e autorização no servidor.
- *Tecnologias:* Spring Security 7, BCrypt.
- *Implementação:* login por formulário com sessão no servidor (sem JWT); respostas JSON para a interface; mensagem genérica "Login ou senha incorretos." também para login inexistente e usuário inativo; perfis viram papéis `ROLE_*`; autorização por rota e método HTTP em `SecurityConfig`; CSRF ativo com token em cookie (`XSRF-TOKEN`) enviado no cabeçalho `X-XSRF-TOKEN`; administrador inicial criado a partir de `AURORA_ADMIN_LOGIN`/`AURORA_ADMIN_SENHA` quando não há usuários.
- *Resultado:* testes T01–T04 passando (login válido, senha errada, usuário inativo, 403 por perfil, 403 sem CSRF).

**Cadastros (RF02, RF03, RF04, RF05, RF06)**
- *Objetivo:* manter usuários, estabelecimento, profissionais, pacientes e procedimentos.
- *Tecnologias:* Spring Web MVC (API REST/JSON), Bean Validation, records do Java.
- *Implementação:* camadas controle → serviço → repositório. Formatos validados nos DTOs (Bean Validation, mensagens em português dizendo como corrigir); regras que dependem de tabelas ou de outros campos nos serviços. Validadores próprios: CNS (algoritmo da ANS) e dígito verificador do procedimento (regra conferida no SIGTAP), como anotações `@Cns` e `@CodigoProcedimento`. Tabelas do SIA (nacionalidade, etnia, caráter de atendimento) e de raça/cor carregadas de CSV e expostas em `/api/tabelas/{nome}`. Erros centralizados em `@RestControllerAdvice`, no formato `{"erros":[{"campo","mensagem"}]}`, sem stack trace.
- *Resultado:* cadastros funcionando pela API e pela interface; testes de validação passando.

**Interface**
- *Objetivo:* telas simples, acessíveis e sem dependência de internet.
- *Tecnologias:* HTML5, CSS3, JavaScript puro (sem bibliotecas externas).
- *Implementação:* telas de login, início, cadastros (abas: estabelecimento, profissionais, procedimentos, usuários) e pacientes. Barra superior com AURORA, competência atual, usuário e perfis, botão Sair; menu lateral conforme a união dos perfis. Erros mostrados junto ao campo com ícone, texto e borda, ligados por `aria-describedby`/`aria-invalid`; área de mensagens com `aria-live`; o que foi digitado nunca é apagado. Campos não aplicáveis ocultos (etnia só para indígena brasileiro). Menu recolhível em telas menores.
- *Resultado:* tela de login conferida no navegador contra o wireframe (erro junto ao campo Senha, nada apagado; print em `docs/evidencias/incremento-1/login-senha-incorreta.jpg`). Abas de Cadastros na ordem do fluxo de navegação (Usuários, Estabelecimento, Profissionais, Procedimentos). Telas internas cobertas pelos testes da API; conferência visual pendente. O rodapé "Conexão segura (HTTPS) na rede local da unidade" do wireframe de login entra junto com o HTTPS, no incremento 3.

**Testes do incremento 1:** 51 testes, todos passando (`docs/evidencias/incremento-1/testes.txt`): validação do CNS (13), dígito verificador com os 5.024 códigos do SIGTAP (12), regras do paciente (8), regras de usuário (4) e integração de segurança e cadastros no PostgreSQL de teste (14).

### Incremento 2 (11/11/2026) — lançamento de atendimentos

**Lançamento com validação (RF07, RF08, RF09)**
- *Objetivo:* registrar os atendimentos no dia a dia, com a validação feita no momento do lançamento.
- *Tecnologias:* Spring Web MVC, Bean Validation, Spring Data JPA, PostgreSQL.
- *Implementação:* `AtendimentoService` reúne todas as regras e devolve **todos** os erros do lançamento de uma vez, cada um com o seu campo:
  - data obrigatória e não futura;
  - profissional existente e ativo; procedimento existente;
  - quantidade de 1 a 999999;
  - **BPA-I:** paciente obrigatório (buscado pelo CNS), idade calculada pela data de nascimento na data do atendimento, caráter de atendimento obrigatório (tabela do SIA);
  - **BPA-C:** sem paciente, idade obrigatória (0 a 130);
  - CID-10 obrigatório quando o procedimento exige, no formato do layout (letra + 2 ou 3 números) e existente na tabela de CID-10 vigente do SIA.
  A competência é a do mês da **data do atendimento**; se não existe, é criada ABERTA no primeiro lançamento (`INSERT ... ON CONFLICT DO NOTHING`, seguro com lançamentos simultâneos). Competência FECHADA recusa o lançamento com mensagem orientando a reabrir. O lançamento grava o usuário que registrou.
- *Resultado:* testes T05–T12 passando, incluindo "atendimento de novembro lançado em dezembro" (entra em novembro se ABERTA; recusado se FECHADA).

**Consulta, correção e exclusão (RF10)**
- *Implementação:* `GET /api/atendimentos?competencia=AAAAMM` e `?data=AAAA-MM-DD` buscam os lançamentos com uma única consulta (`join fetch`, sem N+1). Edição e exclusão só em competência ABERTA.
- *Resultado:* testes de consulta, correção e exclusão passando; competência fechada bloqueia as três operações.

**Interface do lançamento**
- *Implementação:* tela `atendimentos.html` seguindo o wireframe: data com a indicação "Define a competência: MM/AAAA"; profissional e procedimento escolhidos por busca (nome/CNS, código/descrição), com teclado; a área "Dados do paciente (BPA-I)" só aparece para procedimento BPA-I, com busca pelo CNS e nome/nascimento preenchidos do cadastro; idade só no BPA-C; CID só quando o procedimento exige. Campos ocultos não são enviados. Enter salva. Depois de salvar, data, profissional e procedimento continuam preenchidos para o próximo lançamento. Lista "Últimos lançamentos de hoje" com Editar/Excluir (exclusão pede confirmação) e aba "Consultar e corrigir" por competência. Barra superior mostra "Competência atual: MM/AAAA – ABERTA/FECHADA". Paciente sem cadastro: link para o cadastro com o CNS já preenchido (perfil Operador).

**Conferência no navegador (11/11/2026):** fluxo completo feito pela interface, com dados fictícios: cadastro de profissional (CNS inválido recusado junto ao campo), procedimentos BPA-C e BPA-I; lançamento BPA-C sem idade (erro no campo) e salvo (competência 11/2026 criada ABERTA, barra superior atualizada); lançamento BPA-I com CNS de 14 dígitos (mensagem do wireframe), paciente sem cadastro (link para cadastrar), cadastro do paciente indígena sem etnia (erro no campo), volta ao lançamento com tudo restaurado, caráter obrigatório e salvamento; correção com quantidade 0 (recusada) e 5 (salva); consulta da competência. Prints em `docs/evidencias/incremento-2/`.

**Testes do incremento 2:** 70 testes no total (74 após a correção das sessões: T03b e 3 testes de serviço), todos passando (`docs/evidencias/incremento-2/testes.txt`): 12 de regras do lançamento e 7 de integração do lançamento no PostgreSQL de teste, além dos 51 do incremento 1.

## Desvios do projeto

| Data | O que mudou | Motivo | Impacto no TCC |
|---|---|---|---|
| 11/11/2026 | O PostgreSQL 18 de desenvolvimento roda em container Docker, e não instalado direto no sistema. | O repositório oficial do PostgreSQL (PGDG) não publica pacotes para o Ubuntu 25.04. | Nenhum no Quadro 2: a tecnologia continua sendo o PostgreSQL 18. Pode ser citado na seção de ambiente de desenvolvimento. A implantação na unidade (Apêndice A) não depende de Docker. |
| 11/11/2026 | `paciente.nome` VARCHAR(100) → VARCHAR(30); `logradouro` VARCHAR(100) → VARCHAR(30); `bairro` VARCHAR(60) → VARCHAR(30); `numero` VARCHAR(10) → VARCHAR(5). | O layout do BPA-I (DATASUS, 2026) reserva 30, 30, 30 e 5 posições para esses campos (prd-nmpac, prd_end_pcnte, prd_bairro_pcnte, prd_num_pcnte), e o sistema não pode truncar em silêncio. | Atualizar o dicionário de dados e o modelo lógico. |
| 11/11/2026 | **Paciente** ganha `nacionalidade` (CHAR(3), obrigatório) e `etnia` (CHAR(4), obrigatório só quando raça/cor = 05 Indígena e nacionalidade brasileira; senão nulo). | O Manual Operacional do BPA define os dois como campos do paciente no BPA-I (Nacionalidade obrigatória; Etnia só para indígenas brasileiros), e o layout reserva prd-nac e prd-etnia. | Atualizar diagrama de classes, modelo conceitual, modelo lógico e dicionário de dados. |
| 11/11/2026 | **Atendimento** ganha `caraterAtendimento` / `carater_atendimento` (CHAR(2)), obrigatório nos procedimentos BPA-I. | O Manual Operacional define o Caráter de Atendimento como obrigatório no BPA-I (campo Prd-caten do layout). | Atualizar diagrama de classes, modelos conceitual e lógico, dicionário de dados e wireframe do lançamento. |
| 11/11/2026 | O INE (identificação nacional de equipes, prd_ine) sai sempre em branco. | Só se aplica a atendimentos feitos no contexto de uma equipe cadastrada no CNES (ex.: ESF), o que exige o cadastro de equipes. | Incluir nas Limitações (seção 20) e nos Trabalhos futuros (seção 21). |
| 11/11/2026 | A tabela de etnias usada é a vigente do SIA (473 códigos, incluindo códigos alfanuméricos `X999`), e não só o Anexo I da Portaria SAS 508/2010 (264 códigos numéricos). O campo etnia aceita letra na primeira posição. | A documentação oficial está desatualizada em relação ao próprio sistema: o layout descreve prd-etnia como NUM e a portaria lista 264 etnias, mas a tabela que o SIA usa nas críticas (S_CDN, tabela 58, competência 10/2026) tem 473. | **Incluir no texto do TCC** (Implementação e/ou Dificuldades encontradas) uma frase sobre a defasagem da documentação oficial, por exemplo: "Durante a implementação, constatou-se que a documentação oficial não acompanha as tabelas efetivamente usadas pelo SIA: a Portaria SAS nº 508/2010 lista 264 etnias com códigos numéricos, enquanto a tabela vigente do SIA (competência 10/2026) contém 473, das quais 140 com código alfanumérico. Optou-se pela tabela vigente, por ser a utilizada na crítica do arquivo." Atualizar o dicionário de dados (etnia CHAR(4) alfanumérico). |

| 11/11/2026 | Leitura de pacientes (`GET /api/pacientes`) liberada também para o perfil Responsável pela produção; escrita continua só do Operador. | O Responsável também lança atendimentos (RF10) e precisa buscar o paciente nos procedimentos BPA-I. | Ajustar a linha de pacientes na tabela da API (seção 10) e, se houver, a matriz de perfis × funcionalidades. |
| 11/11/2026 | Novas rotas `GET /api/tabelas/{nome}` (nacionalidade, etnia, raça/cor, caráter de atendimento) e `GET /api/csrf`. As tabelas ficam em arquivos CSV na aplicação, não no banco. | Preencher as listas dos formulários e validar os códigos; o CSRF precisa existir antes do primeiro POST. | Acrescentar as rotas na tabela da API. As tabelas do SIA não entram no modelo de dados. |
| 11/11/2026 | Usuário Administrador não pode se desativar nem retirar de si o perfil de Administrador. | Evita que a instalação fique sem nenhum administrador. | Pode ser citado como regra de negócio do RF02. |
| 11/11/2026 | CNES e CNPJ do estabelecimento e código IBGE do município são validados só no formato (quantidade de dígitos). | Não há nos documentos oficiais obtidos o algoritmo do dígito verificador do CNES, e o CNPJ passou a ter formato alfanumérico em 2026, o que o layout do BPA (NUM 14) ainda não prevê. A validação do município contra a tabela do IBGE não está no escopo. | Citar nas Limitações. |
| 11/11/2026 | O Maven Wrapper gerado pelo Initializr foi fixado na versão 3.9.16 (o padrão era 3.10.0). | Manter a versão do Quadro 2 (Maven 3.9). | Nenhum. |
| 11/11/2026 | O lançamento valida o CID-10 contra a tabela de CID-10 vigente do SIA (14.274 códigos, `S_CID` do pacote BDSIA 10/2026), além do formato. | Evita lançar CID inexistente, que seria glosado. A tabela é a mesma usada pelo SIA. | Citar no RF08. Fonte em `docs/referencias/sia_202610a_cid10.csv`. As críticas de compatibilidade (CID × sexo, procedimento × CID) continuam fora do escopo. |
| 11/11/2026 | Nova rota `GET /api/competencias/situacao?mesAno=AAAAMM`, liberada para todos os perfis. | A barra superior mostra a competência atual e a situação para todos, mas `GET /api/competencias` é só do Responsável. | Acrescentar a rota na tabela da API. |
| 11/11/2026 | Na tela de lançamento, depois de salvar, data, profissional e procedimento ficam preenchidos. | Agiliza a digitação em sequência (vários pacientes do mesmo profissional no mesmo dia). | Pode ser citado na descrição da interface. |
| 11/11/2026 | No BPA-I, a idade gravada no atendimento é calculada pela data de nascimento do paciente na data do atendimento; no BPA-C, é informada. | O layout do BPA-I também tem o campo idade; calcular evita digitação e divergência com o cadastro. | Citar na descrição do lançamento. |
| 11/11/2026 | Ao ir do lançamento para "Cadastrar novo paciente", o lançamento em andamento é guardado no navegador (`sessionStorage`, só durante a sessão da aba) e restaurado na volta, com o paciente já buscado pelo CNS. | RF09: não perder o que foi digitado. Sem isso, o usuário redigitava data, profissional, procedimento e CID. | Pode ser citado na descrição da interface. |
| 11/11/2026 | Sessões registradas (`SessionRegistry`): ao desativar um usuário, alterar seus perfis ou trocar sua senha, as sessões abertas dele são encerradas na hora; a próxima ação leva ao login com o aviso "Seu acesso foi alterado pelo administrador. Entre novamente." | Sem isso, perfis e situação só eram lidos no login: um usuário desativado continuava usando o sistema até a sessão expirar (até 8 horas). Problema encontrado na conferência pela interface. | Citar na seção de Segurança (RNF04). Teste T03b. |

## Conferência do layout oficial (11/11/2026)

Comparação de `docs/Layout_Exportacao_BPA.pdf` com o CLAUDE.md:

- **Conferem:** CNES 7, CNS 15, CBO 6, procedimento 10, IBGE 6, CEP 8, CNPJ 14, CID 4, raça/cor 2, telefone 11, sexo 1; cabeçalho: nome do órgão de origem 30, sigla 6, órgão destino 40, indicador M/E.
- **Domínios a validar no lançamento:** idade de 0 a 130; quantidade até 6 dígitos (máx. 999999); raça/cor 01, 02, 03, 04, 05, 99.
- **Arquivo:** largura fixa, fim de linha CR+LF; cabeçalho 130 posições + CR LF, BPA-C 48 + CR LF, BPA-I 351 + CR LF. Campo de controle = (soma dos códigos de procedimento + quantidades) mod 1111 + 1111. Folha 001–999; sequência 01–20 no BPA-C e 01–99 no BPA-I. Origem "BPA".
- **Paciente no BPA-I:** identificado por um documento só (CNS ou CPF). O Aurora usa sempre o CNS e deixa o CPF em branco.
- **Não constam nos documentos:** algoritmo do dígito verificador do procedimento, algoritmo de validação do CNS, tabelas de nacionalidade, etnia e caráter de atendimento. Pendentes de fonte oficial.

## Dígito verificador do procedimento (decidido em 11/11/2026)

Nenhum documento oficial encontrado descreve o cálculo. A regra foi **verificada empiricamente** contra a Tabela Unificada do SIGTAP, competência 09/2026 (`TabelaUnificada_202609_v2610261311.zip`, FTP `ftp2.datasus.gov.br/pub/sistemas/tup/downloads/`), e confere com **5.024 de 5.024** códigos oficiais:

> DV = (d1×1 + d2×2 + … + d9×9) mod 11; se o resultado for 10, DV = 0.

Os restos aparecem distribuídos entre 0 e 10 (459 códigos com resto 10 → DV 0), então todos os casos da regra estão cobertos pela amostra. A lista de códigos está em `docs/referencias/sigtap_202609_codigos_procedimento.txt` e será usada por um teste JUnit que valida todos eles (evidência para a seção 16).

**Para o TCC:** descrever a regra como "verificada contra os 5.024 procedimentos da Tabela Unificada do SIGTAP (competência 09/2026)", e não como norma publicada.

## Tabela de etnias (fonte obtida em 11/11/2026)

Fonte: Portaria SAS/MS nº 508, de 28/09/2010 (DOU de 29/09/2010, seção 1), Anexo I — Tabela de Etnias Indígenas Brasileiras. Cópia em `docs/referencias/Portaria_SAS_508_2010_Etnias.pdf`.

- 264 etnias, códigos de 4 dígitos de 0001 a 0264, sem lacunas (confere com o campo prd-etnia, 4 posições).
- Art. 2º: a etnia só é exigida quando a raça/cor é Indígena (05). O Manual do BPA acrescenta: só para nacionalidade brasileira.
- Efeitos a partir da competência 10/2010.

## Caráter de atendimento (fonte obtida em 11/11/2026)

Fonte: programa oficial BPA Magnético versão 05.00 (`BPAMAG0500.exe`, DATASUS, 09/07/2026, FTP `arpoador.datasus.gov.br/siasus/BPA/`), lista de opções da tela de registro do BPA-I. O instalador foi apenas extraído (innoextract 1.9), não executado.

| Código | Descrição |
|---|---|
| 01 | Eletivo |
| 02 | Urgência |
| 03 | Acidente no local de trabalho ou a serviço da empresa |
| 04 | Acidente no trajeto para o trabalho |
| 05 | Outros tipos de acidente de trânsito |
| 06 | Outros tipos de lesões e envenenamentos por agentes químicos ou físicos |

O programa mostra os códigos com 1 dígito (1 a 6); no arquivo, o campo Prd-caten tem 2 posições com zeros à esquerda (01 a 06). O programa também tem a opção "Sem informação" sem código; o Aurora não a oferece, porque o Manual define o campo como obrigatório.

## Tabelas de códigos do SIA (fonte obtida em 11/11/2026)

Fonte: pacote oficial de tabelas do SIA, competência 10/2026 (`BDSIA202610a.exe`, DATASUS, FTP `arpoador.datasus.gov.br/siasus/SIA/`), arquivo `S_CDN.DBF` (tabela genérica de códigos que o SIA e o BPA Magnético usam nas críticas). O pacote foi apenas extraído (7z), não executado. Exportado para CSV em `docs/referencias/`:

| Tabela S_CDN | Conteúdo | Registros | Arquivo |
|---|---|---|---|
| 31 | Nacionalidade (3 dígitos; 010 = Brasil) | 328 | `sia_202610a_nacionalidade.csv` |
| 53 | Caráter de atendimento (01 a 06) | 6 | `sia_202610a_carater_atendimento.csv` |
| 54 | Raça/cor (01 a 05) | 5 | `sia_202610a_raca_cor.csv` |
| 58 | Etnias indígenas brasileiras | 473 | `sia_202610a_etnia.csv` |

- **Caráter de atendimento:** a tabela 53 confirma os mesmos 6 códigos encontrados no BPA Magnético (seção acima).
- **Raça/cor:** a tabela 54 não tem o código 99 ("sem informação") que o layout aceita. O Aurora oferece 01 a 05 e 99, como diz o layout.
- **Etnia — divergência com a Portaria 508/2010:** a tabela vigente do SIA tem **473** etnias, e não 264. São 333 códigos numéricos (0001 a 0334) e 140 códigos alfanuméricos no formato `X999` (ex.: `X411`), incluídos depois da portaria. Decisão: o Aurora usa a **tabela vigente do SIA**, porque é contra ela que o arquivo é criticado; a Portaria 508/2010 continua citada como origem do campo. Consequência: o campo etnia aceita letra na primeira posição, embora o layout o descreva como NUM.

## Validação do CNS (fonte obtida em 11/11/2026)

Fonte: Agência Nacional de Saúde Suplementar (ANS), "Algoritmos — Aplicativo de carga SIB-XML", seção 4 (CNS), PDF de 02/07/2026, publicado em gov.br (`https://www.gov.br/ans/pt-br/arquivos/assuntos/espaco-da-operadora-de-plano-de-saude/aplicativos-ans/sib/sibalgoritmosxml1002072026.pdf`). Cópia em `docs/referencias/ANS_SIB_Algoritmos_2026-07.pdf`.

O CNS é válido se tiver 15 dígitos, não for `000000000000000` e passar em **uma** das duas rotinas:

1. **CNS definitivo (`validaCns`)** — os 11 primeiros dígitos (base PIS) são multiplicados pelos pesos 15, 14, …, 5 e somados. DV = 11 − (soma mod 11); se DV = 11, DV = 0. Se DV = 10, soma-se 2 à soma, recalcula-se DV = 11 − (soma mod 11) e o CNS esperado é base + `001` + DV; senão, base + `000` + DV. O CNS informado tem de ser igual ao esperado.
2. **CNS provisório (`validaCnsProv`)** — os 15 dígitos são multiplicados pelos pesos 15, 14, …, 1 e somados; a soma tem de ser múltipla de 11.

Observação: a rotina da ANS não restringe o primeiro dígito (a literatura costuma associar 1/2 ao definitivo e 7/8/9 ao provisório). O Aurora segue a rotina da ANS como está, por ser a fonte oficial encontrada.

Os testes JUnit usarão apenas números de CNS fictícios gerados por essa regra.

## Pendências de fonte oficial

Nenhuma.
