package com.estudo.cheques.conversa;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Memória que o modelo relê. Não é o histórico da tela.
 * Quem chama: AssistenteRecebiveis, AssistenteProduto e o ChatService nos caminhos sem AiServices.
 * A chave _dados junta formulário, pergunta geral e recebível.
 * A chave _manual fica só com o produto.
 * O ModeloMemoriaStore grava no MySQL só as últimas 20 frases desta chave.
 */
@Component
public class MemoriaPorSessao implements ChatMemoryProvider {

    /** O modelo só reenxerga este tanto. O histórico da tela não usa este limite. */
    static final int JANELA = 20;

    private final ModeloMemoriaStore store;
    private final ConcurrentHashMap<Object, ChatMemory> abertas = new ConcurrentHashMap<>();

    public MemoriaPorSessao(ModeloMemoriaStore store) {
        this.store = store;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return abertas.computeIfAbsent(memoryId, id -> MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(JANELA)
                .alwaysKeepSystemMessageFirst(true)
                .chatMemoryStore(store)
                .build());
    }

    /** sessaoId + este sufixo é a chave da memória de dados. */
    public static String idDados(String sessaoId) {
        return sessaoId + "_dados";
    }

    /** sessaoId + este sufixo é a chave da memória do manual. */
    public static String idManual(String sessaoId) {
        return sessaoId + "_manual";
    }
}
