package com.estudo.cheques.config;

import com.estudo.cheques.recebivel.RecebivelTools;
import com.estudo.cheques.service.AssistenteRecebiveis;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o modelo de conversa. Ollama roda na máquina, em localhost:11434.
 * O modelo configurado é o llama3.2. Temperatura 0 deixa a resposta mais estável.
 * AssistenteRecebiveis é a interface que o LangChain4j implementa sozinho:
 * ele recebe a pergunta, pode chamar RecebivelTools e devolve o texto.
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

    @Bean(name = "memoriaChat")
    ChatMemory memoriaChat() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .alwaysKeepSystemMessageFirst(true)
                .build();
        memory.add(SystemMessage.from(SYSTEM_PROMPT));
        return memory;
    }

    @Bean
    AssistenteRecebiveis assistente(
            ChatModel chatModel,
            @Qualifier("memoriaChat") ChatMemory chatMemory,
            RecebivelTools tools) {
        return AiServices.builder(AssistenteRecebiveis.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(tools)
                .maxToolCallingRoundTrips(3)
                .build();
    }
}
