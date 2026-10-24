package com.estudo.cheques.service;

import dev.langchain4j.service.SystemMessage;

/**
 * Contrato do assistente de manual. O LangChain4j cria a implementação.
 * explicar busca pedaços em docs/ e o llama3.2 responde com esse texto.
 */
public interface AssistenteProduto {

    @SystemMessage("""
            Você explica o produto de antecipação de recebíveis para pessoas jurídicas.
            Use somente o trecho dos manuais recuperado para esta pergunta.
            Se os manuais não cobrirem a resposta, diga que o manual não trata disso.
            Não invente taxa, valor, quantidade de títulos nem número de cheque ou boleto.
            Responda em português, em poucas frases.
            """)
    String explicar(String pergunta);
}
