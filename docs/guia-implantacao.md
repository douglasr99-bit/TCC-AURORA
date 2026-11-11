# Guia de implantação do Aurora

Este guia vira o **Apêndice A** do TCC. Ele descreve como instalar o Aurora no computador servidor da unidade de saúde, gerar o certificado próprio para HTTPS (RNF09), instalar o certificado nas estações e fazer a cópia de segurança do banco (RNF08).

## 1. Visão geral

- Um computador da unidade é o **servidor**: roda o Aurora (Java) e o PostgreSQL.
- As **estações** acessam pelo navegador (Chrome, Firefox ou Edge) em `https://<servidor>:8443`.
- Tudo funciona na rede local, **sem internet** (RNF02).
- O servidor precisa de **nome ou IP fixo** na rede local (configurado no roteador, por reserva de DHCP, ou no próprio sistema operacional). O certificado é emitido para esse nome/IP.

Nos exemplos abaixo, o servidor se chama `aurora-servidor` e tem o IP fixo `192.168.0.10`. Troque pelos valores da unidade.

## 2. Requisitos do servidor

| Item | Versão |
|---|---|
| Java (JDK ou JRE) | 25 |
| PostgreSQL | 18 |
| Memória | 4 GB ou mais |
| Disco | 10 GB livres (banco + cópias de segurança) |

O `keytool`, usado na seção 4, vem junto com o Java.

## 3. Banco de dados

Crie o usuário e o banco (no Linux, como usuário `postgres`; no Windows, pelo `psql` instalado com o PostgreSQL):

```sql
CREATE USER aurora WITH PASSWORD '<senha-forte-do-banco>';
CREATE DATABASE aurora OWNER aurora;
```

As tabelas são criadas pelo próprio Aurora na primeira execução (`schema.sql`).

## 4. Certificado próprio (HTTPS)

### 4.1 Gerar o keystore no servidor

O certificado precisa ter **SAN** (Subject Alternative Name) com o nome **e** o IP do servidor; sem isso, os navegadores atuais recusam o certificado mesmo depois de instalado.

```bash
keytool -genkeypair -alias aurora -keyalg RSA -keysize 2048 -validity 825 \
  -storetype PKCS12 -keystore aurora.p12 \
  -dname "CN=aurora-servidor, O=Unidade de Saude, C=BR" \
  -ext "SAN=dns:aurora-servidor,ip:192.168.0.10"
```

O `keytool` pede uma senha para o keystore. Guarde-a: ela vai na variável `AURORA_KEYSTORE_SENHA`.

- Validade de 825 dias (cerca de 2 anos e 3 meses). Anote a data para renovar antes de vencer (seção 4.4).
- Guarde o `aurora.p12` numa pasta que só o administrador do servidor acessa. **Ele nunca vai para o repositório** (o `.gitignore` exclui `*.p12`).

### 4.2 Exportar o certificado para as estações

```bash
keytool -exportcert -rfc -alias aurora -keystore aurora.p12 -file aurora.crt
```

O arquivo `aurora.crt` contém **só o certificado público** (sem a chave privada) e pode ser copiado para as estações por pendrive ou pasta compartilhada.

### 4.3 Instalar o certificado nas estações

**Windows (Chrome e Edge)** — usam o repositório de certificados do Windows:

1. Dê dois cliques em `aurora.crt` → **Instalar Certificado…**
2. Local do repositório: **Máquina Local** (precisa de administrador) → Avançar.
3. Marque **Colocar todos os certificados no repositório a seguir** → Procurar → **Autoridades de Certificação Raiz Confiáveis** → OK → Avançar → Concluir.
4. Feche e abra o navegador.

**Firefox** — tem repositório próprio. Escolha uma das opções:

- *Opção A (recomendada no Windows):* faça o passo anterior e, no Firefox, abra `about:config` e confirme que `security.enterprise_roots.enabled` está `true`. O Firefox passa a confiar nos certificados instalados no Windows.
- *Opção B:* Configurações → Privacidade e Segurança → Certificados → **Ver certificados…** → aba **Autoridades** → **Importar…** → escolha `aurora.crt` → marque **Confiar nesta CA para identificar sites** → OK.

**Conferência:** acesse `https://aurora-servidor:8443` (ou `https://192.168.0.10:8443`). O navegador deve abrir a tela de login **sem aviso de segurança**, com o cadeado fechado.

### 4.4 Renovar o certificado

Antes do vencimento, repita 4.1 e 4.2 com o mesmo nome e IP, troque o `aurora.p12` no servidor, reinicie o Aurora e reinstale o novo `aurora.crt` nas estações (4.3).

## 5. Variáveis de ambiente

| Variável | Exemplo | Uso |
|---|---|---|
| `AURORA_DB_URL` | `jdbc:postgresql://localhost:5432/aurora` | banco |
| `AURORA_DB_USUARIO` | `aurora` | banco |
| `AURORA_DB_SENHA` | (a senha da seção 3) | banco |
| `AURORA_KEYSTORE` | `/opt/aurora/aurora.p12` ou `C:\Aurora\aurora.p12` | certificado |
| `AURORA_KEYSTORE_SENHA` | (a senha da seção 4.1) | certificado |
| `AURORA_ADMIN_LOGIN` | `admin` | só no primeiro acesso: cria o administrador se não houver usuários |
| `AURORA_ADMIN_SENHA` | (mínimo 8 caracteres) | idem; depois do primeiro acesso pode ser removida |

O HTTPS fica **ligado por padrão** na porta **8443**. As variáveis `AURORA_HTTPS=false` e `AURORA_PORTA` existem só para desenvolvimento e **não devem ser usadas na unidade**.

## 6. Executar

Gere o pacote no computador de desenvolvimento (`mvn package`) e copie `target/aurora-0.0.1-SNAPSHOT.jar` para o servidor (ex.: `/opt/aurora/aurora.jar` ou `C:\Aurora\aurora.jar`).

**Linux (serviço systemd)** — arquivo `/etc/systemd/system/aurora.service`:

```ini
[Unit]
Description=Aurora - registro da produção ambulatorial
After=network.target postgresql.service

[Service]
User=aurora
WorkingDirectory=/opt/aurora
EnvironmentFile=/opt/aurora/aurora.env
ExecStart=/usr/bin/java -jar /opt/aurora/aurora.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

O `aurora.env` tem as variáveis da seção 5 (`NOME=valor`, uma por linha) e deve ser legível só pelo usuário `aurora` (`chmod 600`). Depois: `sudo systemctl enable --now aurora`.

**Windows** — crie `C:\Aurora\iniciar.bat`:

```bat
@echo off
set AURORA_DB_URL=jdbc:postgresql://localhost:5432/aurora
set AURORA_DB_USUARIO=aurora
set AURORA_DB_SENHA=<senha-do-banco>
set AURORA_KEYSTORE=C:\Aurora\aurora.p12
set AURORA_KEYSTORE_SENHA=<senha-do-keystore>
java -jar C:\Aurora\aurora.jar
```

Proteja a pasta `C:\Aurora` (só administradores) e agende o `iniciar.bat` no **Agendador de Tarefas** com o gatilho "Ao iniciar o computador".

**Firewall:** libere a porta **8443/TCP** no servidor, apenas para a rede local. A porta do PostgreSQL (5432) **não** precisa ser liberada: só o Aurora, no próprio servidor, acessa o banco.

## 7. Primeiro acesso

1. Abra `https://aurora-servidor:8443` e entre com `AURORA_ADMIN_LOGIN` / `AURORA_ADMIN_SENHA`.
2. Em **Cadastros**: preencha o **Estabelecimento** (CNES, nome, CNPJ, órgão de destino), cadastre **profissionais**, **procedimentos** e os **usuários** (Operador, Responsável pela produção).
3. Remova `AURORA_ADMIN_SENHA` da configuração do servidor.

## 8. Cópia de segurança (RNF08)

Todos os dados ficam no PostgreSQL, **inclusive os arquivos BPA gerados** (tabela `exportacao_bpa`): um único backup do banco preserva tudo.

**Linux** — cópia diária às 22h, mantendo 30 dias (`crontab -e` do usuário `postgres`):

```cron
0 22 * * * pg_dump -Fc aurora > /var/backups/aurora/aurora_$(date +\%Y\%m\%d).dump && find /var/backups/aurora -name 'aurora_*.dump' -mtime +30 -delete
```

**Windows** — agende no Agendador de Tarefas um `.bat` com:

```bat
"C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -U aurora -Fc aurora -f "D:\Backup\aurora_%date:~6,4%%date:~3,2%%date:~0,2%.dump"
```

(use um arquivo `pgpass.conf` para não deixar a senha no `.bat`).

Copie as cópias para **fora do servidor** (HD externo ou outro computador) pelo menos uma vez por semana. A cópia contém dados pessoais e de saúde (LGPD): guarde-a em local de acesso restrito.

**Restaurar:**

```bash
createdb -O aurora aurora_restaurado
pg_restore -d aurora_restaurado aurora_20261130.dump
```

Teste a restauração periodicamente; backup que nunca foi restaurado não é garantia.

## 9. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Aurora não inicia: `Unable to create key store: Location must not be empty or null` | `AURORA_KEYSTORE` não definida | Defina a variável com o caminho do `aurora.p12` (seção 5). |
| Aurora não inicia: senha do keystore incorreta | `AURORA_KEYSTORE_SENHA` errada | Corrija a variável. |
| Navegador mostra "Sua conexão não é particular" | certificado não instalado na estação, ou acesso por nome/IP que não está no SAN | Instale o `aurora.crt` (4.3) e acesse pelo nome ou IP usados no certificado. |
| Firefox avisa, Chrome não | Firefox não usa o repositório do Windows | Seção 4.3, Firefox. |
| "Nenhum usuário cadastrado. Defina AURORA_ADMIN_LOGIN…" | primeiro acesso sem as variáveis do administrador | Defina `AURORA_ADMIN_LOGIN` e `AURORA_ADMIN_SENHA` e reinicie. |

## 10. Conferência feita no desenvolvimento (11/11/2026)

Keystore PKCS12 gerado com o comando da seção 4.1 (SAN `dns:localhost,ip:127.0.0.1`), Aurora iniciado com `AURORA_KEYSTORE`/`AURORA_KEYSTORE_SENHA`:

- Conexão HTTPS na porta 8443 validando o certificado exportado (`curl --cacert aurora.crt`): **200**, com os cabeçalhos `Strict-Transport-Security`, `X-Frame-Options: DENY` e `X-Content-Type-Options: nosniff`.
- Sem confiar no certificado: conexão recusada (`self-signed certificate`), como esperado antes da instalação na estação.
- HTTP simples na porta 8443: recusado (**400**).
- Sem `AURORA_KEYSTORE`: o Aurora não inicia (`Unable to create key store: Location must not be empty or null`).
