package com.estudo.cheques.service;

/**
 * Contrato do assistente de dados. O LangChain4j cria a implementação
 * e liga os métodos de RecebivelTools para o modelo poder chamá-los.
 */
public interface AssistenteRecebiveis {

    String responder(String pergunta);
}
