# Cheques para antecipar

Projeto novo, ao lado de `chatbot-java-angular`. O chat antigo não foi alterado.

Este aqui lê cheques de um MySQL local e pede ao modelo para indicar os melhores para antecipar. A tela fica na porta 4201 e a API na porta 8081, para os dois projetos poderem ficar abertos juntos.

## Banco

O banco se chama `recebiveis`, separado do `algafood`. Na subida, o Flyway roda os scripts em `backend/src/main/resources/db/migration`, no mesmo estilo do algafood:

1. `V001` cria a tabela `taxas` (modalidade e taxa diária). Hoje há `cheque` e `duplicata`. Outros recebíveis entram como nova modalidade e nova tabela, sem misturar com o cheque.
2. `V002` cria a tabela `cheque` (número, valor, data de vencimento).
3. `V003` carrega as taxas e dez cheques com vencimentos diferentes.

O valor líquido é calculado no Java: `valor - (valor * taxa_diaria * dias até o vencimento)`. O modelo recebe essa lista já ordenada e só explica a recomendação.

## Como subir

O Ollama precisa estar ligado, como no outro chat:

```bash
~/.local/bin/ollama serve
```

Backend, na pasta `/home/luciano_valle/meu_projeto_python/chatbot-cheques/backend`:

```bash
export JAVA_HOME="$HOME/.local/share/chatbot-learn-tools/jdk/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$HOME/.local/share/chatbot-learn-tools/maven/bin:$PATH"
mvn spring-boot:run
```

Espere o Flyway aplicar as migrations e a linha `Started ChequesApplication`.

Angular, na pasta `/home/luciano_valle/meu_projeto_python/chatbot-cheques/frontend`:

```bash
export PATH="$HOME/.local/share/chatbot-learn-tools/node/bin:$PATH"
npm install
npm start
```

Abra `http://localhost:4201` e pergunte: "Quais cheques são melhores para antecipar?"

A senha do MySQL está em `backend/src/main/resources/application-local.properties`. Esse arquivo fica de fora do Git. O usuário `recebiveis` só acessa esse banco. O `root` do algafood continua só em `localhost`.
