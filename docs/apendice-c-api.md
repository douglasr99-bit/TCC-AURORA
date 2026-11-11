# Apêndice C – Documentação da API REST do Aurora

A interface web do Aurora conversa com o servidor por uma API REST com JSON, servida pela própria aplicação (mesma origem, sem CORS). Este apêndice descreve as rotas, as permissões por perfil, o formato dos erros e exemplos das chamadas principais. Todos os exemplos usam dados fictícios.

## 1. Convenções

- **Endereço base:** `https://<servidor>:8443`. Todas as rotas de dados começam com `/api`.
- **Formato:** JSON em UTF-8. Datas no formato `AAAA-MM-DD`, data e hora em ISO 8601, competências no formato `AAAAMM` (ex.: `202611`).
- **Autenticação:** por **sessão no servidor**, com cookie de sessão. Não há token JWT. O login é feito em `POST /login` e a sessão vale por 8 horas sem uso.
- **CSRF:** toda requisição que altera dados (POST, PUT, DELETE, inclusive `/login` e `/logout`) precisa do cabeçalho `X-XSRF-TOKEN` com o valor do cookie `XSRF-TOKEN`. Se o cookie ainda não existir, `GET /api/csrf` o cria.
- **Autorização:** verificada no servidor, por rota e método HTTP (seção 3). Esconder o menu na interface não substitui essa verificação.

## 2. Respostas de erro

Erros de validação e de regra de negócio devolvem **HTTP 400** com a lista de problemas. Cada problema traz o campo do formulário e uma mensagem em linguagem simples que diz como corrigir. Erros que não são de um campo vêm com `campo` nulo.

```json
{
  "erros": [
    { "campo": "cns", "mensagem": "O CNS deve ter 15 dígitos, só números. Confira o cartão do paciente." },
    { "campo": "dataNascimento", "mensagem": "A data de nascimento não pode ser no futuro." }
  ]
}
```

| Código | Quando | Mensagem (exemplo) |
|---|---|---|
| 400 | Dados inválidos ou regra de negócio violada | uma por campo, como acima |
| 401 | Sem sessão, sessão expirada, ou acesso alterado pelo administrador | "Sua sessão expirou. Entre novamente." |
| 403 | Perfil sem permissão, ou requisição sem token CSRF | "Seu perfil não tem permissão para esta ação." |
| 404 | Registro inexistente | "Paciente não encontrado." |
| 500 | Erro inesperado (sem detalhes técnicos na resposta) | "Ocorreu um erro inesperado. Tente de novo; …" |

O servidor nunca devolve stack trace, e os logs não registram dados de pacientes.

## 3. Rotas e permissões

| Método e rota | Perfil | Uso | Requisito |
|---|---|---|---|
| `POST /login` | público | Entrar (formulário: `login`, `senha`) | RF01 |
| `POST /logout` | autenticado | Sair (204) | RF01 |
| `GET /api/csrf` | público | Cria o cookie `XSRF-TOKEN` (204) | — |
| `GET /api/me` | autenticado | Usuário logado e perfis (monta o menu) | RF01 |
| `GET /api/usuarios` · `GET /api/usuarios/{id}` | Administrador | Listar e obter usuários | RF02 |
| `POST /api/usuarios` · `PUT /api/usuarios/{id}` | Administrador | Criar e alterar usuários | RF02 |
| `GET /api/estabelecimento` | autenticado | Dados do estabelecimento (204 se ainda não cadastrado) | RF03 |
| `PUT /api/estabelecimento` | Administrador | Cadastrar ou alterar o estabelecimento | RF03 |
| `GET /api/profissionais?q=` · `GET /api/profissionais/{id}` | autenticado | Buscar por nome ou CNS (até 50) e obter | RF04 |
| `POST /api/profissionais` · `PUT /api/profissionais/{id}` | Administrador | Criar e alterar profissionais | RF04 |
| `GET /api/pacientes?q=` · `GET /api/pacientes/{id}` | Operador, Responsável | Buscar por nome ou CNS (até 50) e obter | RF05 |
| `POST /api/pacientes` · `PUT /api/pacientes/{id}` | Operador | Criar e alterar pacientes | RF05 |
| `GET /api/procedimentos?q=` · `GET /api/procedimentos/{id}` | autenticado | Buscar por código ou descrição (até 50) e obter | RF06 |
| `POST /api/procedimentos` · `PUT /api/procedimentos/{id}` | Administrador | Criar e alterar procedimentos | RF06 |
| `GET /api/tabelas/{nome}` | autenticado | Tabelas do SIA: `nacionalidade`, `etnia`, `raca_cor`, `carater_atendimento`, `cid10` | RF05, RF07 |
| `GET /api/atendimentos?competencia=AAAAMM` | Operador, Responsável | Lançamentos de uma competência | RF10 |
| `GET /api/atendimentos?data=AAAA-MM-DD` | Operador, Responsável | Lançamentos de um dia | RF10 |
| `GET /api/atendimentos/{id}` | Operador, Responsável | Obter um lançamento | RF10 |
| `POST /api/atendimentos` | Operador, Responsável | Lançar atendimento | RF07, RF08 |
| `PUT /api/atendimentos/{id}` · `DELETE /api/atendimentos/{id}` | Operador, Responsável | Corrigir e excluir (só em competência ABERTA) | RF10 |
| `GET /api/competencias/situacao?mesAno=AAAAMM` | autenticado | Situação de uma competência (barra superior) | — |
| `GET /api/competencias` | Responsável | Listar competências | RF11 |
| `GET /api/competencias/{id}/resumo` | Responsável | Conferência da produção | RF11 |
| `POST /api/competencias/{id}/fechamento` | Responsável | Fechar competência | RF12 |
| `POST /api/competencias/{id}/reabertura` | Responsável | Reabrir competência (corpo: motivo) | RF15 |
| `POST /api/competencias/{id}/exportacao` | Responsável | Gerar o arquivo BPA (devolve o arquivo) | RF13 |
| `GET /api/exportacoes` | Responsável | Histórico de exportações | RF14 |
| `GET /api/exportacoes/{id}/arquivo` | Responsável | Baixar de novo um arquivo gerado | RF14 |

## 4. Exemplos

### 4.1 Login

```http
POST /login
Content-Type: application/x-www-form-urlencoded
X-XSRF-TOKEN: <valor do cookie XSRF-TOKEN>

login=maria.operadora&senha=********
```

Sucesso (200):

```json
{ "nome": "Maria Fictícia", "perfis": ["OPERADOR"] }
```

Falha (401), com a mesma mensagem para login inexistente, senha errada ou usuário inativo:

```json
{ "erros": [ { "campo": null, "mensagem": "Login ou senha incorretos." } ] }
```

### 4.2 Cadastrar paciente

```http
POST /api/pacientes
Content-Type: application/json
X-XSRF-TOKEN: <token>
```

```json
{
  "cns": "800000000000001",
  "nome": "JOAO FICTICIO DA SILVA",
  "dataNascimento": "1985-04-20",
  "sexo": "M",
  "racaCor": "03",
  "etnia": null,
  "nacionalidade": "010",
  "codigoMunicipio": "355030",
  "cep": null, "logradouro": null, "numero": null, "bairro": null, "telefone": null
}
```

Resposta **201** com o paciente gravado (inclui `id`). Regras conferidas, além do formato:
- CNS válido pelo algoritmo publicado pela ANS;
- data de nascimento não futura e idade até 130 anos;
- raça/cor e nacionalidade presentes nas tabelas;
- etnia obrigatória só para raça/cor 05 com nacionalidade 010, e proibida nos outros casos;
- CNS ainda não cadastrado.

### 4.3 Lançar atendimento

BPA-I (paciente, caráter de atendimento e, se o procedimento exigir, CID):

```json
{
  "data": "2026-11-10",
  "profissionalId": 1,
  "procedimentoId": 2,
  "quantidade": 1,
  "pacienteId": 2,
  "cid": "J069",
  "caraterAtendimento": "01"
}
```

BPA-C (sem paciente, com idade):

```json
{ "data": "2026-11-10", "profissionalId": 1, "procedimentoId": 1, "quantidade": 5, "idade": 34 }
```

Resposta **201**:

```json
{
  "id": 3, "data": "2026-11-10", "competencia": "202611", "situacaoCompetencia": "ABERTA",
  "profissionalId": 1, "profissionalNome": "Ana Ficticia Souza", "profissionalCbo": "225142",
  "procedimentoId": 2, "procedimentoCodigo": "0301010072",
  "procedimentoDescricao": "CONSULTA MEDICA EM ATENCAO ESPECIALIZADA",
  "modalidade": "BPA_I", "exigeCid": true,
  "pacienteId": 2, "pacienteNome": "JOAO FICTICIO DA SILVA", "pacienteCns": "800000000000001",
  "pacienteDataNascimento": "1985-04-20",
  "quantidade": 1, "idade": 41, "cid": "J069", "caraterAtendimento": "01",
  "registradoPor": "Maria Fictícia"
}
```

A competência é a do mês da **data do atendimento**. Se ainda não existir, é criada com situação ABERTA. Todos os problemas encontrados voltam juntos, cada um no seu campo (`data`, `profissionalId`, `procedimentoId`, `quantidade`, `pacienteId`, `idade`, `cid`, `caraterAtendimento`). Em competência FECHADA, a resposta é 400 no campo `data`, orientando a reabrir a competência.

### 4.4 Conferência da competência

`GET /api/competencias/1/resumo` → 200:

```json
{
  "competencia": { "id": 1, "mesAno": "202611", "situacao": "ABERTA", "dataFechamento": null },
  "totalAtendimentos": 6, "totalBpaC": 5, "totalBpaI": 1, "profissionais": 1,
  "porProcedimento": [
    { "codigo": "0101010010", "descricao": "ATIVIDADE EDUCATIVA / ...", "modalidade": "BPA_C", "lancamentos": 1, "quantidade": 5 },
    { "codigo": "0301010072", "descricao": "CONSULTA MEDICA EM ...", "modalidade": "BPA_I", "lancamentos": 1, "quantidade": 1 }
  ],
  "porProfissional": [ { "nome": "Ana Ficticia Souza", "cbo": "225142", "lancamentos": 2, "quantidade": 6 } ],
  "reaberturas": []
}
```

### 4.5 Fechar, reabrir e gerar o arquivo

- `POST /api/competencias/1/fechamento` → 200 com a competência `FECHADA` e `dataFechamento`. Se já estiver fechada, responde 400.
- `POST /api/competencias/1/reabertura` com o corpo `{ "motivo": "Corrigir lançamentos glosados" }` → 200 com a competência `ABERTA`. Sem motivo, responde 400 no campo `motivo`. Se a competência não estiver fechada, responde 400.
- `POST /api/competencias/1/exportacao`:
  - **competência ABERTA:** 400 com "Feche a competência antes de gerar o arquivo.";
  - **competência FECHADA:** 200 com o arquivo.

  Cabeçalhos da resposta com o arquivo:

  ```http
  Content-Type: text/plain;charset=US-ASCII
  Content-Disposition: attachment; filename="bpa_202611.txt"
  X-Exportacao-Id: 1
  ```

  O corpo é o arquivo no layout de exportação do BPA (DATASUS): cabeçalho com 130 posições, registros BPA-C com 48 e BPA-I com 351, cada linha terminada em CR LF. Cada geração fica gravada no histórico.

### 4.6 Histórico

`GET /api/exportacoes` → 200:

```json
[ { "id": 1, "dataGeracao": "2026-11-11T10:31:05", "competencia": "202611",
    "nomeArquivo": "bpa_202611.txt", "totalRegistros": 2, "usuario": "Administrador" } ]
```

`GET /api/exportacoes/1/arquivo` devolve exatamente o mesmo arquivo gerado, com os mesmos cabeçalhos do item 4.5.
