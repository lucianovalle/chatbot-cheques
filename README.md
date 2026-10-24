# Cheques para antecipar

Projeto de estudo ao lado de `chatbot-java-angular`. O chat antigo não foi alterado.

A tela fica em `http://localhost:4201` e a API na porta 8081. O modelo é o `llama3.2`, no Ollama. Os manuais ficam em `docs/`. Os cheques e boletos ficam no MySQL, banco `recebiveis`.

## Como funciona

Cada frase segue um caminho só.

1. **Quero antecipar.** A tela abre as opções de cheque, boleto e valor. O Java calcula na hora e a IA não entra.
2. **Pergunta sobre o produto.** O LangChain4j procura um trecho nos manuais da pasta `docs` e o `llama3.2` responde com esse texto. É o RAG. Exemplos: o que é o produto, títulos aceitos, como o desconto é calculado.
3. **Pergunta sobre os títulos do banco.** O `llama3.2` escolhe uma ferramenta. O Java lê o MySQL e faz a conta. O modelo não inventa número.
4. **Pergunta geral.** O `llama3.2` responde sozinho, sem manual e sem banco.

A conta, quando existe, é sempre esta:

`custo = taxa diária × dias até o vencimento`

`desconto = valor × custo`

`líquido = valor − desconto`

A lista sai do menor custo para o maior. Título vencido fica de fora.

## Como subir

Ollama:

```bash
~/.local/bin/ollama serve
```

O manual usa também o modelo de embedding `nomic-embed-text`. Se ele ainda não estiver baixado:

```bash
~/.local/bin/ollama pull nomic-embed-text
```

Backend, na pasta `chatbot-cheques/backend`:

```bash
export JAVA_HOME="$HOME/.local/share/chatbot-learn-tools/jdk/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$HOME/.local/share/chatbot-learn-tools/maven/bin:$PATH"
mvn spring-boot:run
```

Espere a linha `Started ChequesApplication`.

Tela, na pasta `chatbot-cheques/frontend`:

```bash
export PATH="$HOME/.local/share/chatbot-learn-tools/node/bin:$PATH"
npm install
npm start
```

Abra `http://localhost:4201`. A senha do MySQL fica em `backend/src/main/resources/application-local.properties` e não entra no Git.

## Perguntas de teste

Copie a frase no campo da tela e envie.

### 1. Base de conhecimento (RAG, lendo os manuais)

Estas frases devem ser respondidas com o texto de `docs/`, não com um número inventado.

- O que é o produto de antecipação de recebíveis?
- Quais são os títulos aceitos para antecipação?
- Como é calculado o custo de desconto de um título?

O que observar: a resposta fala de pessoa jurídica, cheque, boleto e da fórmula taxa diária vezes os dias. A tela mostra "Consultando o manual…".

### 2. Regra de negócio e cálculo (Java)

Estas frases pedem os títulos de verdade. O modelo pode chamar uma ferramenta, mas a ordem e os valores saem do Java.

- Quais são as melhores opções de títulos para antecipar e otimizar meu caixa?
- Explique qual é o critério para priorizar um recebível em relação ao outro.

O que observar: a primeira traz uma lista, do menor custo para o maior, com número, valor, vencimento, desconto e líquido. A segunda deve dizer que o menor custo vem primeiro. Se vier uma lista, a ordem dessa lista é o critério: taxa diária vezes os dias até o vencimento.

### 3. Conversas gerais

Estas frases não são sobre o manual nem sobre a tabela.

- Qual é a capital do Brasil?
- Me dê uma dica rápida de como organizar o fluxo de caixa de uma empresa.

O que observar: a primeira responde Brasília. A segunda dá uma dica comum de empresa, sem citar cheque, boleto ou taxa da base. A tela mostra "Respondendo…".
