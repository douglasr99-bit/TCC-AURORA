# Aurora — contexto para implementação

Este arquivo reúne todas as decisões do TCC "Desenvolvimento de um sistema para registro da produção ambulatorial em unidades de saúde: Aurora" (Douglas Rodrigues Santos, Ciência da Computação, UNIFRAN, orientador Prof. Euller da Silva Ferreira). **O sistema implementado precisa corresponder ao que está documentado no TCC.** Se alguma decisão abaixo precisar mudar durante a implementação, não mude em silêncio: registre a mudança em `docs/registro-implementacao.md` (seção "Desvios do projeto") para que o texto e os diagramas do TCC sejam atualizados.

Prazo: apresentação em **03/12/2026**. Desenvolvimento previsto de 10/11 a 26/11/2026.

---

## 1. O que o sistema faz (e o que não faz)

**Problema:** unidades de saúde anotam os atendimentos do mês em planilhas e, no fim de cada competência, alguém digita tudo manualmente no BPA (Boletim de Produção Ambulatorial) para gerar o arquivo que é validado no SIA/SUS. Isso gera retrabalho e erros de digitação, que podem virar glosas (perda de recursos públicos).

**Solução:** a unidade registra os atendimentos no dia a dia, com validação no momento do lançamento e mensagens de erro claras. Ao fim da competência, o sistema gera o arquivo de produção **no formato BPA** (BPA-C consolidado e BPA-I individualizado), que o usuário importa no SIA.

**Fora do escopo (não implementar):**
- Comunicação direta com sistemas do Ministério da Saúde (SIA, CNES, SIGTAP). O arquivo é baixado e importado manualmente pelo usuário.
- Cadastro de estabelecimentos múltiplos: **uma instalação = um estabelecimento**.
- Importação da tabela SIGTAP: procedimentos são cadastrados manualmente.
- Críticas de compatibilidade do SIA (procedimento × CBO × idade × sexo).
- Inteligência Artificial de qualquer tipo.
- Uso em smartphone.

---

## 2. Stack (fechada — não adicionar tecnologias sem perguntar)

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 (LTS) | Linguagem |
| Spring Boot | 4.1.x | Backend |
| Spring Web MVC | gerenciada pelo Boot | API REST + servir o frontend estático |
| Spring Security | gerenciada pelo Boot | Login, perfis, BCrypt |
| Spring Data JPA + Hibernate | gerenciadas pelo Boot | Persistência |
| Bean Validation | gerenciada pelo Boot | Validação de formato nos DTOs |
| PostgreSQL | 18 | Banco de dados |
| Maven | 3.9 | Build |
| JUnit | gerenciada pelo Boot (spring-boot-starter-test) | Testes |
| HTML5, CSS3, JavaScript puro | — | Interface |

Regras:
- **Não adicionar** dependências ou ferramentas que não estejam na tabela (Lombok, MapStruct, Flyway, Thymeleaf, React/Vue/Angular, jQuery, Bootstrap, JWT, H2, Testcontainers etc.). Se algo parecer realmente necessário, **pare e pergunte**, porque o Quadro 2 do TCC lista as tecnologias usadas.
- O projeto deve ser criado pelo Spring Initializr (Boot 4.1.x, Java 25, Maven) com: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Security, Validation. Confira os nomes atuais dos starters no Initializr.
- **Spring Boot 4 mudou várias coisas em relação ao 3** (Jackson 3, Hibernate 7, Spring Security 7, reorganização de starters e módulos). Use a documentação da versão 4.1, não padrões de tutoriais antigos do Boot 2/3.
- DTOs como `record` do Java.
- **Nenhum recurso externo no frontend** (sem CDN, sem Google Fonts): o sistema roda em rede local, possivelmente sem internet. Tudo que a interface usa deve estar em `src/main/resources/static`.

---

## 3. Arquitetura

Cliente-servidor na **rede local** da unidade. Um computador é o servidor: roda a aplicação Spring Boot e o PostgreSQL. As estações acessam pelo navegador, via **HTTPS**.

- O frontend (HTML/CSS/JS) é servido pela própria aplicação Spring Boot (`src/main/resources/static`). Mesma origem: **não configurar CORS**.
- O frontend conversa com o backend por **API REST com JSON** (`fetch`).
- **Autenticação por sessão no servidor** (cookie de sessão do Spring Security). **Não usar JWT.**
- Backend em camadas; cada camada só fala com a vizinha:
  1. **Segurança:** Spring Security intercepta todas as requisições e verifica autenticação e perfil.
  2. **Controle:** controladores REST recebem a requisição, chamam o serviço e devolvem a resposta (incluindo os erros de validação por campo).
  3. **Serviço:** regras de negócio, validação dos lançamentos, competência, geração do arquivo BPA.
  4. **Persistência:** repositórios Spring Data JPA.
- Todos os dados ficam no PostgreSQL, **inclusive o conteúdo dos arquivos BPA gerados** (tabela `exportacao_bpa`). Um único backup do banco preserva tudo.

### Estrutura de pacotes sugerida

```
br.com.aurora
├── config        # SecurityConfig, conversores, inicialização do admin
├── controller    # controladores REST
├── service       # regras de negócio
├── repository    # interfaces Spring Data JPA
├── domain        # entidades JPA e enums
├── dto           # records de entrada e saída
├── bpa           # montagem do arquivo BPA (cabeçalho, BPA-C, BPA-I)
└── exception     # exceções de negócio e @RestControllerAdvice
```

Nomes usados no diagrama de sequência do TCC (manter): `ExportacaoController`, `ExportacaoBpaService`.

---

## 4. Modelo de domínio (diagrama de classes do TCC)

Classes e atributos em português, sem acentos, exatamente como abaixo.

- **Usuario**: id, nome, login, senhaHash, perfis (`Set<Perfil>`), ativo
- **Estabelecimento**: id, cnes, nome, sigla, cnpj, orgaoDestino, indicadorDestino (`char`: M ou E). Instância única.
- **Profissional**: id, nome, cns, cbo, ativo
- **Paciente**: id, cns, nome, dataNascimento, sexo (`Sexo`), racaCor, codigoMunicipio, cep, logradouro, numero, bairro, telefone
- **Procedimento**: id, codigo, descricao, modalidade (`Modalidade`), exigeCid
- **Competencia**: id, mesAno (`YearMonth`), situacao (`SituacaoCompetencia`), dataFechamento
- **Atendimento**: id, data (`LocalDate`), quantidade, idade (`Integer`), cid. Associações: competencia (1), profissional (1), procedimento (1), paciente (0..1), usuario que registrou (1)
- **ExportacaoBpa**: id, dataGeracao, nomeArquivo, conteudo, totalRegistros. Associações: competencia (1), usuario que gerou (1)
- **ReaberturaCompetencia**: id, dataReabertura, motivo. Associações: competencia (1), usuario que reabriu (1)

Enums: `Perfil` (ADMINISTRADOR, OPERADOR, RESPONSAVEL_PRODUCAO), `Modalidade` (BPA_C, BPA_I), `SituacaoCompetencia` (ABERTA, FECHADA), `Sexo` (M, F).

---

## 5. Banco de dados (dicionário de dados do TCC)

Esquema criado por `src/main/resources/schema.sql` (com `CREATE TABLE IF NOT EXISTS`, executado pelo Spring na inicialização) e `spring.jpa.hibernate.ddl-auto=validate`. **Não** deixar o Hibernate criar o esquema: as restrições `CHECK` e `UNIQUE` abaixo precisam existir no banco.

Chaves primárias: `BIGSERIAL`. Nomes de tabelas e colunas em snake_case.

**usuario**: id PK · nome VARCHAR(100) NOT NULL · login VARCHAR(50) NOT NULL UNIQUE · senha_hash VARCHAR(100) NOT NULL · ativo BOOLEAN NOT NULL DEFAULT TRUE

**usuario_perfil**: usuario_id BIGINT FK→usuario · perfil VARCHAR(30) CHECK (ADMINISTRADOR, OPERADOR, RESPONSAVEL_PRODUCAO) · PK (usuario_id, perfil)

**estabelecimento**: id PK · cnes CHAR(7) NOT NULL UNIQUE · nome VARCHAR(30) NOT NULL · sigla VARCHAR(6) NOT NULL · cnpj CHAR(14) NOT NULL · orgao_destino VARCHAR(40) NOT NULL · indicador_destino CHAR(1) NOT NULL CHECK (M, E)

**profissional**: id PK · nome VARCHAR(100) NOT NULL · cns CHAR(15) NOT NULL UNIQUE · cbo CHAR(6) NOT NULL · ativo BOOLEAN NOT NULL DEFAULT TRUE

**paciente**: id PK · cns CHAR(15) NOT NULL UNIQUE · nome VARCHAR(100) NOT NULL · data_nascimento DATE NOT NULL · sexo CHAR(1) NOT NULL CHECK (M, F) · raca_cor CHAR(2) NOT NULL · codigo_municipio CHAR(6) NOT NULL · cep CHAR(8) · logradouro VARCHAR(100) · numero VARCHAR(10) · bairro VARCHAR(60) · telefone VARCHAR(11)

**procedimento**: id PK · codigo CHAR(10) NOT NULL UNIQUE · descricao VARCHAR(250) NOT NULL · modalidade VARCHAR(5) NOT NULL CHECK (BPA_C, BPA_I) · exige_cid BOOLEAN NOT NULL DEFAULT FALSE

**competencia**: id PK · mes_ano CHAR(6) NOT NULL UNIQUE (formato AAAAMM) · situacao VARCHAR(10) NOT NULL CHECK (ABERTA, FECHADA) · data_fechamento TIMESTAMP

**atendimento**: id PK · competencia_id FK NOT NULL · profissional_id FK NOT NULL · procedimento_id FK NOT NULL · paciente_id FK (nulo nos procedimentos BPA-C) · usuario_id FK NOT NULL · data_atendimento DATE NOT NULL · quantidade INTEGER NOT NULL CHECK (> 0) · idade INTEGER CHECK (>= 0) · cid VARCHAR(4)

**exportacao_bpa**: id PK · competencia_id FK NOT NULL · usuario_id FK NOT NULL · data_geracao TIMESTAMP NOT NULL · nome_arquivo VARCHAR(50) NOT NULL · conteudo TEXT NOT NULL · total_registros INTEGER NOT NULL

**reabertura_competencia**: id PK · competencia_id FK NOT NULL · usuario_id FK NOT NULL · data_reabertura TIMESTAMP NOT NULL · motivo VARCHAR(255) NOT NULL

Observações:
- `YearMonth` (Java) ↔ `CHAR(6)` AAAAMM (banco): usar um `AttributeConverter`.
- Enums gravados como texto (`@Enumerated(EnumType.STRING)`).
- Os perfis do usuário: `@ElementCollection` mapeado para `usuario_perfil`.
- A obrigatoriedade de paciente e CID depende do procedimento e é verificada **no serviço**, não no banco.
- Os tamanhos de CHAR (CNES 7, CNS 15, CBO 6, procedimento 10, IBGE 6, CEP 8, CNPJ 14) devem ser conferidos contra o layout oficial (seção 8). Se divergirem, o layout manda; registre o desvio.

---

## 6. Requisitos

### Funcionais

| Código | Requisito | Perfil | Prioridade |
|---|---|---|---|
| RF01 | Autenticar usuário (login e senha) | todos | Alta |
| RF02 | Gerenciar usuários e perfis (um usuário pode ter vários perfis) | Administrador | Média |
| RF03 | Cadastrar estabelecimento (dados do cabeçalho do BPA) | Administrador | Alta |
| RF04 | Cadastrar profissionais (nome, CNS, CBO) | Administrador | Alta |
| RF05 | Cadastrar pacientes (dados exigidos pelo BPA-I) | Operador | Alta |
| RF06 | Manter tabela de procedimentos (cadastro manual: código, descrição, modalidade, exige CID) | Administrador | Alta |
| RF07 | Lançar atendimento (data, profissional, procedimento, quantidade; paciente e CID quando exigidos). A competência é a do mês da data e é criada automaticamente, ABERTA, no primeiro lançamento do mês | Operador | Alta |
| RF08 | Validar o lançamento no momento do registro (obrigatórios, formatos, regras do layout do BPA, dígito verificador do procedimento) | — | Alta |
| RF09 | Exibir mensagens de erro claras junto ao campo, sem perder o que foi digitado | — | Alta |
| RF10 | Consultar, editar e excluir atendimentos da competência aberta | Operador, Responsável | Alta |
| RF11 | Conferir a produção da competência (totais por procedimento e por profissional) | Responsável | Média |
| RF12 | Fechar competência (bloqueia lançamentos e alterações) | Responsável | Média |
| RF13 | Gerar o arquivo BPA da competência (BPA-C e BPA-I, layout DATASUS) | Responsável | Alta |
| RF14 | Manter histórico de exportações (data, competência, usuário; permitir baixar de novo) | Responsável | Baixa |
| RF15 | Reabrir competência fechada, registrando data, usuário e motivo | Responsável | Média |

### Não funcionais

| Código | Requisito |
|---|---|
| RNF01 | Operável por usuários sem conhecimento técnico; mensagens de erro em linguagem simples |
| RNF02 | Funciona na rede local, sem depender de internet |
| RNF03 | Arquivo gerado segue integralmente o layout de exportação do BPA vigente |
| RNF04 | Senhas com hash (BCrypt); cada funcionalidade respeita o perfil do usuário |
| RNF05 | Dados pessoais tratados conforme a LGPD (Lei 13.709/2018) |
| RNF06 | Funciona nas versões atuais de Chrome, Firefox e Edge |
| RNF07 | Gerar o arquivo de uma competência com até 3.000 atendimentos em até 10 segundos |
| RNF08 | Dados em banco relacional, com rotina de cópia de segurança |
| RNF09 | Comunicação por HTTPS, com certificado próprio instalado nas estações |

### Ordem de corte, se o prazo apertar

O mínimo para a apresentação é: **login, cadastros, lançamento com validação e geração do arquivo** (cumpre o objetivo geral). Se precisar cortar, cortar nesta ordem: RF14 → RF02 (criar usuários por script inicial) → RF15 → RF11. Todo corte deve ser registrado em `docs/registro-implementacao.md`, porque vira item das seções 20 (Limitações) e 21 (Trabalhos futuros) do TCC.

---

## 7. Regras de negócio

### Competência
- A competência de um atendimento é definida pela **data do atendimento** (não pela data de digitação).
- Se a competência daquele mês não existe, é **criada automaticamente** com situação ABERTA no primeiro lançamento.
- Lançar, editar ou excluir atendimento em competência **FECHADA** é recusado, com mensagem orientando a reabrir a competência.
- Data do atendimento no futuro é recusada.
- **Fechar:** só se ABERTA → FECHADA, grava `data_fechamento`.
- **Reabrir (RF15):** só se FECHADA; motivo obrigatório; cria `ReaberturaCompetencia` (data, motivo, usuário) e volta para ABERTA.
- **Gerar arquivo (RF13):** só se FECHADA. Se ABERTA, responder erro com mensagem "Feche a competência antes de gerar o arquivo." (ver diagrama de sequência). Uma competência pode ter várias exportações.

### Lançamento (RF07, RF08)
- Profissional e procedimento obrigatórios; profissional precisa estar ativo.
- Quantidade > 0.
- **Procedimento BPA-I:** paciente obrigatório.
- **Procedimento BPA-C:** sem paciente; **idade obrigatória** (exigida no BPA-C).
- **Procedimento com `exigeCid`:** CID-10 obrigatório.
- Formatos: CNS com 15 dígitos; CBO com 6 caracteres; código do procedimento com 10 dígitos e dígito verificador válido; CID-10 no formato do layout.
- Textos que vão para o arquivo não podem passar do tamanho definido no layout (o sistema recusa ou avisa; nunca trunca em silêncio).
- **Não invente algoritmos de validação** (dígito verificador do procedimento, validação do CNS): implemente a partir da documentação oficial. Se a regra não estiver nos documentos em `docs/`, pare e pergunte.

### Geração do arquivo BPA (RF13)
- **Fonte de verdade: `docs/Layout_Exportacao_BPA.pdf` (DATASUS, 2026).** Leia o PDF antes de implementar. Posições, tamanhos, preenchimento (zeros/espaços), cabeçalho, campo de controle, separadores de linha e codificação vêm dele. **Não invente posições de campos.** Se o arquivo não estiver na pasta, pare e peça.
- Estrutura geral: um registro de cabeçalho, registros BPA-C (atendimentos consolidados) e registros BPA-I (um por atendimento individualizado), em texto de largura fixa.
- O cabeçalho usa os dados do `Estabelecimento` e da competência.
- Ao gerar: montar o conteúdo, gravar `ExportacaoBpa` (com `conteudo`, `nome_arquivo`, `total_registros`, usuário e data) e devolver o arquivo para download.
- Isolar a montagem no pacote `bpa` (uma classe por tipo de registro ajuda a testar cada linha).
- Desempenho: 3.000 atendimentos em até 10 s (RNF07). Buscar os atendimentos da competência em uma consulta, sem N+1.
- Formulários de apoio para conferir os campos: `docs/Formulario_BPA_I.pdf` e `docs/Formulario_BPA_C.pdf`; manual: `docs/Manual_Operacional_BPA.pdf`.

### Erros
- Erros de validação e de negócio devolvem **HTTP 400** com JSON contendo, para cada problema, o campo e a mensagem. Exemplo de formato: `{"erros": [{"campo": "cnsPaciente", "mensagem": "O CNS deve ter 15 dígitos. Confira o cartão do paciente."}]}`.
- Mensagens em português, linguagem simples, sem jargão nem códigos técnicos, **dizendo como corrigir**.
- Centralizar em um `@RestControllerAdvice`. Nunca devolver stack trace ao cliente.

---

## 8. Documentos de referência (colocar em `docs/`)

- `Layout_Exportacao_BPA.pdf`: layout do arquivo (obrigatório para RF13).
- `Formulario_BPA_I.pdf`, `Formulario_BPA_C.pdf`, `Manual_Operacional_BPA.pdf`.
- `diagramas/`: os PNG do TCC (arquitetura, casos de uso, classes, sequência, atividades, modelo conceitual, modelo lógico, fluxo de navegação, wireframes). Use-os como referência visual.
- `TCC_Aurora_revisado.txt`: o texto do TCC.

---

## 9. Segurança (seção 19 do TCC)

- **Spring Security** com login por formulário e sessão no servidor. Senhas com `BCryptPasswordEncoder`.
- Perfis viram papéis: `ROLE_ADMINISTRADOR`, `ROLE_OPERADOR`, `ROLE_RESPONSAVEL_PRODUCAO`. Um usuário pode ter vários.
- **A autorização é verificada no servidor**, por rota/método. Esconder o menu no frontend não basta.
- Usuário com `ativo = false` não entra.
- Falha de login: mensagem genérica "Login ou senha incorretos." (não revelar se o login existe).
- Manter **CSRF ativo** (autenticação por cookie de sessão). O JavaScript envia o token nas requisições que alteram dados.
- **Primeiro acesso:** se não existir nenhum usuário, criar um administrador na inicialização, com login e senha lidos de variáveis de ambiente (`AURORA_ADMIN_LOGIN`, `AURORA_ADMIN_SENHA`). Nunca deixar senha fixa no código.
- **HTTPS (RNF09):** `server.ssl.*` (ou SSL bundle) apontando para um keystore **PKCS12** gerado com `keytool`. O certificado precisa ter **SAN** com o nome ou IP fixo do servidor na rede local. Porta 8443. Caminho e senha do keystore vêm de variáveis de ambiente. Documentar a geração e a instalação do certificado nas estações (Windows e Firefox) em `docs/guia-implantacao.md` (vira o Apêndice A do TCC).
- Credenciais do banco por variáveis de ambiente (`AURORA_DB_URL`, `AURORA_DB_USUARIO`, `AURORA_DB_SENHA`). **Nada de segredo commitado**; `.gitignore` deve excluir keystore e arquivos `.env`.

### LGPD
- O sistema trata dados pessoais e **dados sensíveis** (CID-10, procedimentos, raça/cor).
- Coletar só os campos exigidos pelo BPA (minimização).
- **Logs nunca podem conter dados de pacientes** (nome, CNS, CID, endereço).
- Lançamentos, exportações e reaberturas registram o usuário responsável (rastreabilidade).
- **Testes e dados de exemplo só com dados fictícios.** Nunca usar dados reais.

---

## 10. API REST (proposta)

Prefixo `/api`. Ajuste se necessário, mas mantenha `POST /api/competencias/{id}/exportacao` (aparece no diagrama de sequência).

| Método e rota | Perfil | Uso |
|---|---|---|
| `POST /login`, `POST /logout` | — | Spring Security |
| `GET /api/me` | autenticado | usuário logado e perfis (para montar o menu) |
| `GET/POST/PUT /api/usuarios` | ADMINISTRADOR | RF02 |
| `GET/PUT /api/estabelecimento` | ADMINISTRADOR (GET: autenticado) | RF03 |
| `GET/POST/PUT /api/profissionais` (`?q=` busca nome/CNS) | escrita: ADMINISTRADOR; leitura: autenticado | RF04 |
| `GET/POST/PUT /api/pacientes` (`?q=`) | OPERADOR | RF05 |
| `GET/POST/PUT /api/procedimentos` (`?q=` código/descrição) | escrita: ADMINISTRADOR; leitura: autenticado | RF06 |
| `GET /api/atendimentos?competencia=AAAAMM` | OPERADOR, RESPONSAVEL_PRODUCAO | RF10 |
| `POST/PUT/DELETE /api/atendimentos` | OPERADOR, RESPONSAVEL_PRODUCAO | RF07, RF10 |
| `GET /api/competencias` | RESPONSAVEL_PRODUCAO | lista |
| `GET /api/competencias/{id}/resumo` | RESPONSAVEL_PRODUCAO | RF11 |
| `POST /api/competencias/{id}/fechamento` | RESPONSAVEL_PRODUCAO | RF12 |
| `POST /api/competencias/{id}/reabertura` (corpo: motivo) | RESPONSAVEL_PRODUCAO | RF15 |
| `POST /api/competencias/{id}/exportacao` | RESPONSAVEL_PRODUCAO | RF13 (devolve o arquivo) |
| `GET /api/exportacoes`, `GET /api/exportacoes/{id}/arquivo` | RESPONSAVEL_PRODUCAO | RF14 |

---

## 11. Interface (seção 13 do TCC — seguir os wireframes)

Páginas em `static/`, uma por tela, com CSS e JS próprios (sem framework):
- **Login:** login, senha, botão Entrar, mensagem genérica de erro.
- **Layout comum:** barra superior com "AURORA", **competência atual e situação sempre visíveis**, nome e perfil do usuário, botão Sair. Menu lateral **conforme o perfil** (união dos perfis): Operador → Início, Atendimentos, Pacientes; Responsável → Início, Atendimentos, Competências; Administrador → Início, Cadastros.
- **Lançar atendimento** (tela principal): data (abaixo dela: "Define a competência: MM/AAAA"), profissional e procedimento **por busca** (nome ou código), quantidade. Os **campos do paciente só aparecem quando o procedimento é BPA-I**; o **CID só aparece quando o procedimento exige**; a **idade aparece nos procedimentos BPA-C**. Botões "Salvar lançamento" e "Limpar". Abaixo, a lista dos lançamentos do dia. Link "Paciente sem cadastro? Cadastrar novo paciente".
- **Competências:** totais (atendimentos, BPA-C, BPA-I, profissionais), produção por procedimento, botões Fechar / Reabrir (pede motivo) / Gerar arquivo BPA. **"Gerar arquivo BPA" fica desabilitado enquanto ABERTA**, com a explicação ao lado. Histórico de exportações com "Baixar".
- **Cadastros:** usuários, estabelecimento, profissionais, procedimentos. **Pacientes:** cadastro e edição.

Erros (seção 6.1.4 do TCC):
- Mostrar a mensagem **junto ao campo**, com ícone e texto (não só cor), borda destacada.
- **Nunca apagar o que o usuário digitou** quando há erro.
- Prevenir erros: campos não aplicáveis ocultos, ações indisponíveis desabilitadas com explicação.

Acessibilidade (WCAG 2.2, nível AA):
- `<label for>` em todos os campos; erro ligado ao campo com `aria-describedby` e `aria-invalid`; área de mensagens com `aria-live`.
- Contraste mínimo 4,5:1.
- Tudo operável por teclado, com foco visível; Enter salva o lançamento.

Responsividade: pensado para ≥ 1280 px. Em telas menores (tablet), menu lateral recolhe e o formulário vira uma coluna. Smartphone fora do escopo.

---

## 12. Testes (seção 16 do TCC)

- **JUnit** para as regras de negócio, com foco em validação dos lançamentos e geração do arquivo. Testar a camada de serviço e o pacote `bpa` isolados (mocks dos repositórios onde fizer sentido).
- **Testes de integração**, se houver, rodam contra um banco PostgreSQL de teste local (não usar H2).
- **Só dados fictícios.**
- Casos obrigatórios (cada um vira uma linha da tabela da seção 16: ID, funcionalidade, procedimento, resultado esperado, resultado obtido, status):
  - Login válido; senha errada (mensagem genérica); usuário inativo.
  - Perfil sem permissão acessando rota protegida → 403.
  - Lançamento BPA-I sem paciente → erro no campo.
  - Lançamento BPA-C sem idade → erro no campo.
  - Procedimento com `exigeCid` sem CID → erro no campo.
  - CNS com quantidade errada de dígitos → erro no campo.
  - Código de procedimento com dígito verificador inválido → erro.
  - Quantidade 0 ou negativa → erro.
  - Data no futuro → erro.
  - Primeiro lançamento do mês cria a competência ABERTA.
  - **Atendimento de novembro lançado em dezembro:** entra em novembro se ABERTA; recusado se FECHADA.
  - Gerar arquivo com competência ABERTA → recusado com mensagem.
  - Arquivo gerado: cada linha com o tamanho exato do layout; cabeçalho com os dados do estabelecimento; totais corretos; BPA-C consolidado corretamente.
  - Reabrir sem motivo → erro; reabrir com motivo → registra reabertura e volta para ABERTA.
  - Desempenho: 3.000 atendimentos geram o arquivo em até 10 s (RNF07).
- Guardar evidências (saída dos testes, prints) em `docs/evidencias/`.

---

## 13. Plano de trabalho (Metodologia do TCC: modelo incremental)

| Período | Incremento | Conteúdo |
|---|---|---|
| 10–14/11 | 1 | Projeto, `schema.sql`, entidades, Spring Security (login, perfis, admin inicial), cadastros: estabelecimento, profissional, paciente, procedimento (e usuários, se der tempo) |
| 15–20/11 | 2 | Lançamento de atendimento com todas as validações, criação automática da competência, mensagens de erro, consulta/edição/exclusão |
| 21–26/11 | 3 | Conferência, fechamento, reabertura, geração do arquivo BPA, histórico, HTTPS |
| 12–28/11 | — | Testes ao fim de cada incremento |
| até 01/12 | — | Documentação (seções 14 a 18 e 22 do TCC) |
| 03/12 | — | Apresentação |

Ao terminar cada incremento, o sistema deve **rodar de ponta a ponta** com o que já existe.

---

## 14. Convenções

- Código de domínio em português, sem acentos nos identificadores (`Atendimento`, `gerarArquivo`). Mensagens ao usuário em português correto, com acentos.
- Commits pequenos, no imperativo, em português (ex.: "Adiciona validação de CNS no lançamento"). Uma branch por funcionalidade, mesclada na `main` ao terminar.
- `README.md` com: requisitos para rodar, variáveis de ambiente, como criar o banco, como gerar o certificado e como executar os testes.
- Nada de dados reais, senhas ou keystore no repositório.

---

## 15. Registro para o TCC (manter atualizado)

Manter `docs/registro-implementacao.md` com, para cada módulo (vira a seção 14 do TCC):
- **Objetivo**, **tecnologias utilizadas**, **descrição da implementação**, **resultado obtido**.

E uma seção **"Desvios do projeto"** listando tudo que mudou em relação a este documento (nomes de classes, campos, rotas, requisitos cortados, tamanhos do layout diferentes etc.), para que os diagramas e o texto do TCC sejam atualizados. O último item do checklist do TCC é: "A versão final corresponde ao sistema apresentado".

---

## 16. Pronto quando

- [ ] Requisitos de prioridade Alta funcionando (RF01, RF03–RF10, RF13).
- [ ] Arquivo BPA conferido campo a campo contra o layout oficial.
- [ ] Testes da seção 12 passando, com evidências em `docs/evidencias/`.
- [ ] HTTPS funcionando com o certificado próprio, e guia em `docs/guia-implantacao.md`.
- [ ] `README.md` e `docs/registro-implementacao.md` atualizados.
- [ ] Nenhum dado real, senha ou keystore no repositório.
