package com.estudo.cheques.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Caminho 3. Quem chama: ChatService.reply, antes do caminho de recebível.
 * A ordem dentro do explicar é:
 * 1. lê a memória _manual;
 * 2. busca pedaços parecidos na pasta docs;
 * 3. o llama3.2 escreve a resposta com esse texto.
 * Não chama RecebivelTools e não lê o MySQL.
 */
public interface AssistenteProduto {

    @SystemMessage("""
            Você explica o produto de antecipação de recebíveis para pessoas jurídicas.
            Use somente o trecho dos manuais recuperado para esta pergunta.
            Se os manuais não cobrirem a resposta, diga que o manual não trata disso.
            Não invente taxa, valor, quantidade de títulos nem número de cheque ou boleto.
            Responda em português, em poucas frases.
            """)
    String explicar(@MemoryId String memoryId, @UserMessage String pergunta);
}
