package com.estudo.cheques.conversa;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tabela memoria_modelo. Quem chama: MemoriaPorSessao, quando o modelo lê ou grava a janela.
 * getMessages devolve o JSON da chave. updateMessages troca a linha inteira.
 * Por isso esta tabela não é a combo nem o F5: passando de 20 frases, a lista cortada substitui a anterior.
 * A frase visível fica em historico_mensagens, gravada pelo HistoricoConversaService.
 */
@Component
public class ModeloMemoriaStore implements ChatMemoryStore {

    private final MemoriaModeloRepository repository;
    private final SessaoChatRepository sessoes;

    public ModeloMemoriaStore(MemoriaModeloRepository repository, SessaoChatRepository sessoes) {
        this.repository = repository;
        this.sessoes = sessoes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Object memoryId) {
        return repository.findById(chave(memoryId))
                .map(linha -> ChatMessageDeserializer.messagesFromJson(linha.getMensagensJson()))
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String chave = chave(memoryId);
        if (messages == null || messages.isEmpty()) {
            repository.deleteById(chave);
            return;
        }
        String json = ChatMessageSerializer.messagesToJson(messages);
        repository.findById(chave).ifPresentOrElse(
                linha -> linha.setMensagensJson(json),
                () -> {
                    String sessaoId = sessaoDaChave(chave);
                    long usuarioId = sessoes.findById(sessaoId)
                            .map(sessao -> sessao.getUsuario().getId())
                            .orElse(HistoricoConversaService.USUARIO_MOCADO);
                    repository.save(new MemoriaModelo(chave, usuarioId, sessaoId, json));
                });
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        repository.deleteById(chave(memoryId));
    }

    private static String chave(Object memoryId) {
        return String.valueOf(memoryId);
    }

    /** A chave é o UUID da sessão mais "_dados" ou "_manual". O UUID não tem _. */
    private static String sessaoDaChave(String memoryId) {
        int corte = memoryId.lastIndexOf('_');
        if (corte <= 0) {
            throw new IllegalArgumentException("Chave de memória sem sessão: " + memoryId);
        }
        return memoryId.substring(0, corte);
    }
}
