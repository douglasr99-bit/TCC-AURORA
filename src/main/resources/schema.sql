-- Aurora: esquema do banco (dicionário de dados do TCC, com os desvios
-- registrados em docs/registro-implementacao.md).
-- Executado pelo Spring a cada inicialização; idempotente.

CREATE TABLE IF NOT EXISTS usuario (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    login       VARCHAR(50)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuario_perfil (
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id),
    perfil      VARCHAR(30) NOT NULL
                CHECK (perfil IN ('ADMINISTRADOR', 'OPERADOR', 'RESPONSAVEL_PRODUCAO')),
    PRIMARY KEY (usuario_id, perfil)
);

CREATE TABLE IF NOT EXISTS estabelecimento (
    id                 BIGSERIAL   PRIMARY KEY,
    cnes               CHAR(7)     NOT NULL UNIQUE,
    nome               VARCHAR(30) NOT NULL,
    sigla              VARCHAR(6)  NOT NULL,
    cnpj               CHAR(14)    NOT NULL,
    orgao_destino      VARCHAR(40) NOT NULL,
    indicador_destino  CHAR(1)     NOT NULL CHECK (indicador_destino IN ('M', 'E'))
);

CREATE TABLE IF NOT EXISTS profissional (
    id     BIGSERIAL    PRIMARY KEY,
    nome   VARCHAR(100) NOT NULL,
    cns    CHAR(15)     NOT NULL UNIQUE,
    cbo    CHAR(6)      NOT NULL,
    ativo  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tamanhos de nome, logradouro, numero e bairro seguem o layout do BPA-I.
CREATE TABLE IF NOT EXISTS paciente (
    id                BIGSERIAL   PRIMARY KEY,
    cns               CHAR(15)    NOT NULL UNIQUE,
    nome              VARCHAR(30) NOT NULL,
    data_nascimento   DATE        NOT NULL,
    sexo              CHAR(1)     NOT NULL CHECK (sexo IN ('M', 'F')),
    raca_cor          CHAR(2)     NOT NULL CHECK (raca_cor IN ('01', '02', '03', '04', '05', '99')),
    etnia             CHAR(4),
    nacionalidade     CHAR(3)     NOT NULL,
    codigo_municipio  CHAR(6)     NOT NULL,
    cep               CHAR(8),
    logradouro        VARCHAR(30),
    numero            VARCHAR(5),
    bairro            VARCHAR(30),
    telefone          VARCHAR(11)
);

CREATE TABLE IF NOT EXISTS procedimento (
    id          BIGSERIAL    PRIMARY KEY,
    codigo      CHAR(10)     NOT NULL UNIQUE,
    descricao   VARCHAR(250) NOT NULL,
    modalidade  VARCHAR(5)   NOT NULL CHECK (modalidade IN ('BPA_C', 'BPA_I')),
    exige_cid   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS competencia (
    id               BIGSERIAL   PRIMARY KEY,
    mes_ano          CHAR(6)     NOT NULL UNIQUE,
    situacao         VARCHAR(10) NOT NULL CHECK (situacao IN ('ABERTA', 'FECHADA')),
    data_fechamento  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS atendimento (
    id                   BIGSERIAL  PRIMARY KEY,
    competencia_id       BIGINT     NOT NULL REFERENCES competencia (id),
    profissional_id      BIGINT     NOT NULL REFERENCES profissional (id),
    procedimento_id      BIGINT     NOT NULL REFERENCES procedimento (id),
    paciente_id          BIGINT     REFERENCES paciente (id),
    usuario_id           BIGINT     NOT NULL REFERENCES usuario (id),
    data_atendimento     DATE       NOT NULL,
    quantidade           INTEGER    NOT NULL CHECK (quantidade > 0),
    idade                INTEGER    CHECK (idade >= 0),
    cid                  VARCHAR(4),
    carater_atendimento  CHAR(2)
);

CREATE INDEX IF NOT EXISTS idx_atendimento_competencia ON atendimento (competencia_id);

CREATE TABLE IF NOT EXISTS exportacao_bpa (
    id               BIGSERIAL   PRIMARY KEY,
    competencia_id   BIGINT      NOT NULL REFERENCES competencia (id),
    usuario_id       BIGINT      NOT NULL REFERENCES usuario (id),
    data_geracao     TIMESTAMP   NOT NULL,
    nome_arquivo     VARCHAR(50) NOT NULL,
    conteudo         TEXT        NOT NULL,
    total_registros  INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS reabertura_competencia (
    id               BIGSERIAL    PRIMARY KEY,
    competencia_id   BIGINT       NOT NULL REFERENCES competencia (id),
    usuario_id       BIGINT       NOT NULL REFERENCES usuario (id),
    data_reabertura  TIMESTAMP    NOT NULL,
    motivo           VARCHAR(255) NOT NULL
);
