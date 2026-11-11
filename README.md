# Aurora

Sistema para registro da produção ambulatorial em unidades de saúde e geração do arquivo no formato BPA (BPA-C e BPA-I, layout DATASUS) para importação no SIA/SUS.

TCC de Ciência da Computação (UNIFRAN) — Douglas Rodrigues Santos. Orientador: Prof. Euller da Silva Ferreira.

## Requisitos para rodar

- Java 25
- Maven 3.9
- PostgreSQL 18

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `AURORA_DB_URL` | URL JDBC do banco, ex.: `jdbc:postgresql://localhost:5432/aurora` |
| `AURORA_DB_USUARIO` | usuário do banco |
| `AURORA_DB_SENHA` | senha do banco |
| `AURORA_ADMIN_LOGIN` | login do administrador criado no primeiro acesso (só é usado se não houver nenhum usuário) |
| `AURORA_ADMIN_SENHA` | senha desse administrador (mínimo 8 caracteres) |
| `AURORA_KEYSTORE` | caminho do keystore PKCS12 do certificado HTTPS |
| `AURORA_KEYSTORE_SENHA` | senha do keystore |
| `AURORA_HTTPS` | (só desenvolvimento) `false` desliga o HTTPS; padrão `true` |
| `AURORA_PORTA` | (só desenvolvimento) porta; padrão `8443` |
| `AURORA_TEST_DB_URL` | (opcional) banco dos testes de integração; padrão `jdbc:postgresql://localhost:5433/aurora_teste` |

Em desenvolvimento, as variáveis podem ficar num arquivo `.env` na pasta do projeto, no formato `NOME=valor`, uma por linha. O `.env` está no `.gitignore` e **nunca deve ser commitado**.

## Criar o banco

```bash
sudo -u postgres createuser --pwprompt aurora
sudo -u postgres createdb --owner aurora aurora
```

As tabelas são criadas pelo `src/main/resources/schema.sql` quando a aplicação inicia. O Hibernate só confere o esquema (`ddl-auto=validate`).

Para os testes de integração, crie também o banco `aurora_teste` com o mesmo dono.

### Desenvolvimento com Docker (opcional)

Na máquina de desenvolvimento do TCC o PostgreSQL 18 roda num container (veja `docs/registro-implementacao.md`):

```bash
docker run -d --name aurora-db --restart unless-stopped \
  -e POSTGRES_DB=aurora -e POSTGRES_USER=aurora -e POSTGRES_PASSWORD=<senha> \
  -p 127.0.0.1:5433:5432 -v aurora-pgdata:/var/lib/postgresql postgres:18
```

## Executar

```bash
mvn spring-boot:run
```

Por padrão o Aurora sobe em **HTTPS na porta 8443** e exige o keystore (`AURORA_KEYSTORE`, `AURORA_KEYSTORE_SENHA`). Acesse `https://<servidor>:8443`. No primeiro acesso, entre com `AURORA_ADMIN_LOGIN` e `AURORA_ADMIN_SENHA` e cadastre o estabelecimento.

Em desenvolvimento, sem certificado: `AURORA_HTTPS=false` e `AURORA_PORTA=8080` no `.env`, e acesse `http://localhost:8080`.

## Gerar o certificado

```bash
keytool -genkeypair -alias aurora -keyalg RSA -keysize 2048 -validity 825 -storetype PKCS12 \
  -keystore aurora.p12 -dname "CN=aurora-servidor, O=Unidade de Saude, C=BR" \
  -ext "SAN=dns:aurora-servidor,ip:192.168.0.10"
keytool -exportcert -rfc -alias aurora -keystore aurora.p12 -file aurora.crt
```

O `aurora.crt` é instalado nas estações. Passo a passo (Windows, Chrome/Edge e Firefox), execução como serviço e cópia de segurança em `docs/guia-implantacao.md`.

## Testes

```bash
mvn test
```

Os testes de integração usam o banco PostgreSQL de teste (`aurora_teste`) e desfazem as alterações ao final. Só dados fictícios. Evidências em `docs/evidencias/`.

## Documentação

- `CLAUDE.md`: decisões do projeto (requisitos, arquitetura, regras).
- `docs/registro-implementacao.md`: registro da implementação e desvios em relação ao projeto do TCC.
- `docs/referencias/`: fontes oficiais (layout do BPA, tabelas do SIA, algoritmos).
