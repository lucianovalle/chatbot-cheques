package com.estudo.cheques.config;

import com.estudo.cheques.conversa.MemoriaPorSessao;
import com.estudo.cheques.recebivel.RecebivelTools;
import com.estudo.cheques.service.AssistenteRecebiveis;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta o caminho 5 na subida da API, antes de qualquer pergunta.
 * 1. chatModel aponta para o llama3.2 no Ollama.
 * 2. assistente junta esse modelo, a MemoriaPorSessao e as RecebivelTools.
 * Quem usa o assistente pronto é o ChatService.reply.
 */
@Configuration
public class ChatConfig {

    /** Instrução fixa que vai em toda conversa de dados. O modelo não escolhe título por conta própria. */
    public static final String SYSTEM_PROMPT = """
            Você é um assistente de antecipação de recebíveis.
            Para saber se existem recebíveis, chame a ferramenta contarRecebiveis.
            Para listar os melhores, chame listarMelhores.
            Para cobrir um valor, chame cobrirValor.
            Quando a pergunta for sobre recebíveis, chame a ferramenta em vez de responder sozinho.
            Não invente quantidade, número, valor ou taxa.
            Para outros assuntos, responda em português, em poucas frases, sem chamar ferramenta.
            """;

    @Bean
    ChatModel chatModel(OllamaProperties properties) {
        return OllamaChatModel.builder()
                .baseUrl(properties.baseUrl())
                .modelName(properties.modelName())
                .temperature(0.0)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
    }

    /**
     * chatMemoryProvider, e não uma memória única: cada sessão tem a própria chave.
     * O texto de sistema vai na anotação de AssistenteRecebiveis.
     */
    @Bean
    AssistenteRecebiveis assistente(
            ChatModel chatModel,
            MemoriaPorSessao memorias,
            RecebivelTools tools) {
        return AiServices.builder(AssistenteRecebiveis.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memorias)
                .tools(tools)
                .maxToolCallingRoundTrips(3)
                .build();
    }
}
