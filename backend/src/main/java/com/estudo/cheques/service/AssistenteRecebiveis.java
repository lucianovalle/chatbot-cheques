package com.estudo.cheques.service;

import com.estudo.cheques.config.ChatConfig;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Caminho 5. Quem chama: ChatService.reply.
 * O LangChain4j cria a implementação. A ordem dentro dela é:
 * 1. lê a memória _dados;
 * 2. manda a pergunta ao llama3.2 com as ferramentas;
 * 3. se o modelo escolher uma, executa RecebivelTools;
 * 4. o texto do Java volta para a tela, por causa do IMMEDIATE.
 */
public interface AssistenteRecebiveis {

    @SystemMessage(ChatConfig.SYSTEM_PROMPT)
    String responder(@MemoryId String memoryId, @UserMessage String pergunta);
}
