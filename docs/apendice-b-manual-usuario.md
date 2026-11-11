# Apêndice B – Manual do usuário do Aurora

Este manual explica como usar o Aurora no dia a dia. Ele está organizado por perfil: cada usuário vê no menu apenas as telas do seu perfil, e quem tem mais de um perfil vê a união delas.

| Perfil | Telas | O que faz |
|---|---|---|
| Operador | Início, Atendimentos, Pacientes | Lança os atendimentos do dia e cadastra pacientes |
| Responsável pela produção | Início, Atendimentos, Competências | Confere a produção, fecha a competência, gera o arquivo BPA e reabre quando preciso |
| Administrador | Início, Cadastros | Cadastra usuários, estabelecimento, profissionais e procedimentos |

## 1. Entrar e sair

1. No navegador da estação, abra o endereço do Aurora informado pela unidade (ex.: `https://aurora-servidor:8443`).
2. Digite o **login** e a **senha** e clique em **Entrar** (ou tecle Enter).
3. Se aparecer "Login ou senha incorretos.", confira os dois e tente de novo. Por segurança, o sistema não diz qual deles está errado.
4. Para sair, clique em **Sair**, no canto superior direito. Faça isso sempre que deixar a estação.

Se o administrador alterar o seu acesso, ou se a sessão expirar (8 horas sem uso), o sistema volta para a tela de login e mostra o motivo.

**Barra superior:** mostra sempre a **competência atual** (o mês corrente) e a situação dela: ABERTA, FECHADA ou "sem lançamentos". Ao lado aparecem o seu nome e o seu perfil.

## 2. Como o sistema mostra os erros

- O erro aparece **junto ao campo**, com o ícone ⚠, o texto e a borda vermelha, dizendo como corrigir.
- No alto do formulário, um aviso informa quantos campos precisam de correção, e o cursor vai para o primeiro deles.
- **Nada do que você digitou é apagado.** Corrija o campo indicado e salve de novo.
- Campos que não se aplicam ficam escondidos, e botões que ainda não podem ser usados ficam desabilitados, com a explicação ao lado.

## 3. Operador

### 3.1 Lançar um atendimento (menu Atendimentos → aba "Lançar atendimento")

1. **Data do atendimento:** já vem com a data de hoje. Para um atendimento de outro dia, troque a data. Abaixo do campo aparece "Define a competência: MM/AAAA", o mês em que o atendimento será contado. A data não pode ser no futuro.
2. **Profissional:** digite parte do nome ou do CNS e escolha na lista. Pelo teclado: seta para baixo e Enter, ou Enter direto para o primeiro resultado.
3. **Procedimento:** digite parte do código ou da descrição e escolha na lista. Abaixo aparece a modalidade (BPA-C ou BPA-I) e se o procedimento exige CID-10.
4. **Os campos mudam conforme o procedimento:**
   - **BPA-C (consolidado):** informe a **idade** do paciente atendido. Não se identifica o paciente.
   - **BPA-I (individualizado):** aparece a área **"Dados do paciente (BPA-I)"**. Digite o **CNS do paciente** (15 dígitos). O sistema busca o cadastro sozinho e mostra o nome e a data de nascimento. Escolha também o **caráter do atendimento** (ex.: 01 – Eletivo).
   - **Procedimento que exige CID:** aparece o campo **CID-10**. Digite o código sem ponto (ex.: J069).
5. **Quantidade:** quantas vezes o procedimento foi realizado (padrão 1).
6. Clique em **Salvar lançamento** ou tecle **Enter**.

Depois de salvar, aparece "Lançamento salvo na competência MM/AAAA". Data, profissional e procedimento **continuam preenchidos**, para agilizar o próximo lançamento do mesmo profissional. O botão **Limpar** apaga tudo para começar do zero.

**Paciente sem cadastro:** se o CNS não estiver cadastrado, aparece o link **"Cadastrar novo paciente"**. Ele abre o cadastro com o CNS já preenchido. Salve o paciente e clique em **"volte ao lançamento"**: o lançamento que você estava digitando volta preenchido, com o paciente já encontrado.

### 3.2 Corrigir ou excluir um lançamento

- **Lançamentos de hoje:** ficam na lista **"Últimos lançamentos de hoje"**, abaixo do formulário.
- **Lançamentos de outros dias:** use a aba **"Consultar e corrigir"**. Digite a competência (MM/AAAA) e clique em **Consultar**.
- **Editar:** carrega o lançamento no formulário ("Corrigir lançamento"). Corrija e clique em **Salvar correção**, ou em **Cancelar correção** para desistir.
- **Excluir:** pede confirmação, porque a exclusão não pode ser desfeita.
- **Competência FECHADA:** os botões ficam desabilitados. Para corrigir, peça ao responsável pela produção que reabra a competência.

### 3.3 Cadastrar ou alterar pacientes (menu Pacientes)

1. Para procurar um paciente, digite parte do nome ou do CNS e clique em **Buscar**. Para alterar um cadastro, clique em **Editar** na linha do paciente.
2. Para um paciente novo, clique em **Novo paciente** e preencha os campos:
   - **CNS** (15 dígitos);
   - **Nome:** até 30 caracteres, o limite do BPA. Abrevie os sobrenomes do meio se precisar;
   - **Data de nascimento**;
   - **Sexo**;
   - **Nacionalidade:** já vem como Brasil;
   - **Raça/cor**;
   - **Código IBGE do município** de residência (6 dígitos).
3. **Etnia:** só aparece quando a raça/cor é **Indígena** e a nacionalidade é brasileira. Nesse caso é obrigatória.
4. CEP, endereço (até 30 caracteres), número, bairro e telefone com DDD são opcionais.
5. Clique em **Salvar paciente**.

## 4. Responsável pela produção

O responsável também pode lançar e corrigir atendimentos (seção 3.1 e 3.2) e buscar pacientes durante o lançamento.

### 4.1 Conferir a produção (menu Competências)

1. Escolha a competência no seletor, no alto da tela.
2. Confira:
   - os **totais**: atendimentos, BPA consolidado, BPA individualizado e profissionais;
   - a **produção por procedimento** e a **produção por profissional**;
   - a quantidade de lançamentos de cada linha.
3. Se algo estiver errado, corrija na tela de Atendimentos antes de fechar.

### 4.2 Fechar a competência e gerar o arquivo BPA

1. Clique em **Fechar competência** e confirme em **Sim, fechar**. A competência fechada não aceita lançamentos nem correções.
2. O botão **Gerar arquivo BPA**, desabilitado enquanto a competência estava aberta, fica disponível. Clique nele.
3. O navegador baixa o arquivo `bpa_AAAAMM.txt` (ex.: `bpa_202611.txt`).
4. **Importe esse arquivo no SIA**, conforme o processo do gestor de saúde. O Aurora não envia o arquivo sozinho.

O arquivo pode ser gerado mais de uma vez. Cada geração fica registrada no **Histórico de exportações** (data, competência, arquivo, registros e usuário), e o botão **Baixar** recupera exatamente o mesmo arquivo depois.

### 4.3 Reabrir a competência

Se o SIA rejeitar registros, ou se for preciso corrigir algo depois do fechamento:

1. Na tela Competências, clique em **Reabrir competência**.
2. Escreva o **motivo**, que é obrigatório (ex.: "Corrigir lançamentos glosados").
3. Clique em **Reabrir competência**.

A reabertura fica registrada com data, usuário e motivo, na lista "Reaberturas desta competência". Depois de corrigir os lançamentos, feche a competência e gere o arquivo de novo.

## 5. Administrador

Menu **Cadastros**. Use as abas, que também podem ser trocadas pelas setas do teclado.

| Aba | O que cadastrar | Cuidados |
|---|---|---|
| **Usuários** | Nome, login, senha (mínimo 8 caracteres), perfis e situação | Um usuário pode ter mais de um perfil. Na edição, senha em branco mantém a atual. Desativar um usuário, mudar seus perfis ou sua senha o desconecta na hora. Você não pode desativar a si mesmo nem retirar de si o perfil de Administrador. |
| **Estabelecimento** | CNES, CNPJ, nome (até 30), sigla (até 6), órgão de destino (até 40), destino municipal ou estadual | É o cabeçalho do arquivo BPA. Cadastre antes de gerar o primeiro arquivo. |
| **Profissionais** | Nome, CNS, CBO (6 caracteres, como no CNES), situação | Profissional inativo não pode receber lançamentos. |
| **Procedimentos** | Código da Tabela SUS (10 dígitos), descrição, instrumento (BPA-C ou BPA-I), se exige CID-10 | O sistema confere o dígito verificador do código. Use o instrumento indicado no SIGTAP para o procedimento. |

**Primeiro acesso da unidade:**
1. Cadastre o **estabelecimento**.
2. Cadastre os **procedimentos** que a unidade realiza e os **profissionais**.
3. Crie os **usuários** com seus perfis.

## 6. Dúvidas frequentes

| Situação | O que fazer |
|---|---|
| "A competência MM/AAAA está fechada e não aceita lançamentos." | Peça ao responsável pela produção que reabra a competência. |
| "Feche a competência antes de gerar o arquivo." | Confira a produção e clique em Fechar competência. |
| "Este profissional está inativo." | Escolha outro profissional ou peça ao administrador que o reative. |
| "CID-10 não encontrado na tabela do SIA." | Confira o código (sem ponto). O sistema aceita só códigos vigentes. |
| "A página ficou aberta por muito tempo." | Recarregue a página (F5) e repita a operação. |
| O nome do paciente não cabe | O BPA aceita até 30 caracteres. Abrevie os sobrenomes do meio. |
