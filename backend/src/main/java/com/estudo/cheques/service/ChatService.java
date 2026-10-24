package com.estudo.cheques.service;

import com.estudo.cheques.config.ChatConfig;
import com.estudo.cheques.config.OllamaProperties;
import com.estudo.cheques.recebivel.ChequeConsulta;
import com.estudo.cheques.web.IncomingMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada de cada mensagem. A IA não entra em todo pedido.
 * O método reply escolhe um de quatro caminhos:
 * 1. Formulário (quero antecipar) — o Java calcula e responde na hora.
 * 2. Produto (o que é, benefícios, regra) — o llama3.2 lê os manuais da pasta docs.
 * 3. Pergunta geral (capital do Brasil) — o llama3.2 responde sozinho, sem banco.
 * 4. Dados de cheque ou boleto — o llama3.2 escolhe uma ferramenta e o Java lê o MySQL.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String[] ASSUNTOS_DO_PRODUTO = {
        "o que e o produto",
        "o que e a antecip",
        "o que e antecip",
        "manual",
        "beneficio",
        "capital de giro",
        "contrat",
        "cet",
        "formula",
        "regra",
        "como funciona",
        "como e calcul",
        "como calcula",
        "pessoa juridica",
        "titulos aceit",
        "titulo aceit",
        "previsibilidade",
        "produto de antecip"
    };

    private final AssistenteRecebiveis assistente;
    private final AssistenteProduto assistenteProduto;
    private final ChatModel chatModel;
    private final ChatMemory memory;
    private final ChatMemory memoriaProduto;
    private final OllamaProperties ollama;
    private final ChequeConsulta chequeConsulta;

    public ChatService(
            AssistenteRecebiveis assistente,
            AssistenteProduto assistenteProduto,
            ChatModel chatModel,
            @Qualifier("memoriaChat") ChatMemory memory,
            @Qualifier("memoriaProduto") ChatMemory memoriaProduto,
            OllamaProperties ollama,
            ChequeConsulta chequeConsulta) {
        this.assistente = assistente;
        this.assistenteProduto = assistenteProduto;
        this.chatModel = chatModel;
        this.memory = memory;
        this.memoriaProduto = memoriaProduto;
        this.ollama = ollama;
        this.chequeConsulta = chequeConsulta;
    }

    public String modelName() {
        return ollama.modelName();
    }

    public String baseUrl() {
        return ollama.baseUrl();
    }

    public synchronized String reply(IncomingMessage pedido) {
        String userText = pedido.message().trim();
        // O Angular manda "modo" para dizer qual caminho usar.
        // melhores e ate-valor vêm do botão Consultar. Não chamam o modelo.
        if ("melhores".equals(pedido.modo()) || "ate-valor".equals(pedido.modo())) {
            String answer = consultaDireta(pedido);
            log.info("Consulta do formulário respondida pelo Java, sem chamar o modelo");
            memory.add(UserMessage.from(userText));
            memory.add(AiMessage.from(answer));
            return answer;
        }

        try {
            // Manual primeiro. "O que é a antecipação" também contém a palavra recebível,
            // mas a explicação está no documento, não na tabela.
            if ("produto".equals(pedido.modo()) || falaDeProduto(userText)) {
                log.info("Pergunta de produto respondida pelo modelo {} com os manuais", ollama.modelName());
                String answer = assistenteProduto.explicar(userText);
                if (answer == null || answer.isBlank()) {
                    return "(O modelo respondeu sem texto.)";
                }
                return answer;
            }
            if ("geral".equals(pedido.modo()) || !falaDeRecebivel(userText)) {
                log.info("Pergunta geral respondida pelo modelo {}, sem ferramenta e sem banco", ollama.modelName());
                return responderDireto(userText);
            }
            log.info("Pergunta sobre recebíveis para o modelo {} escolher a ferramenta", ollama.modelName());
            String answer = assistente.responder(userText);
            if (answer == null || answer.isBlank()) {
                return "(O modelo respondeu sem texto.)";
            }
            return answer;
        } catch (RuntimeException ex) {
            throw new ModeloIndisponivelException(
                    "Não consegui falar com o Ollama em " + ollama.baseUrl()
                            + " (modelo " + ollama.modelName() + "). "
                            + "Detalhe: " + ex.getMessage(),
                    ex);
        }
    }

    /** Manda a frase ao llama3.2 sem ferramenta e sem trecho de manual. */
    private String responderDireto(String userText) {
        List<ChatMessage> turn = new ArrayList<>(memory.messages());
        turn.add(UserMessage.from(userText));
        ChatResponse response = chatModel.chat(turn);
        String answer = response.aiMessage().text();
        if (answer == null || answer.isBlank()) {
            answer = "(O modelo respondeu sem texto.)";
        }
        memory.add(UserMessage.from(userText));
        memory.add(AiMessage.from(answer));
        return answer;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Verdadeiro quando a pergunta pede número que está no MySQL. */
    private static boolean falaDeRecebivel(String texto) {
        String normalizado = normalizar(texto);
        return normalizado.contains("cheque")
                || normalizado.contains("boleto")
                || normalizado.contains("receb")
                || normalizado.contains("antecip")
                || normalizado.contains("taxa")
                || normalizado.contains("juro")
                || normalizado.contains("venc")
                || normalizado.contains("liquido")
                || normalizado.contains("desconto");
    }

    public synchronized void reset() {
        memory.clear();
        memory.add(SystemMessage.from(ChatConfig.SYSTEM_PROMPT));
        memoriaProduto.clear();
    }

    /**
     * Verdadeiro para assunto de manual: o que é o produto, benefício, regra, fórmula.
     * O acento é removido antes, então "benefício" e "beneficio" valem igual.
     */
    private static boolean falaDeProduto(String texto) {
        String normalizado = normalizar(texto);
        for (String assunto : ASSUNTOS_DO_PRODUTO) {
            if (normalizado.contains(assunto)) {
                return true;
            }
        }
        return false;
    }

    private String consultaDireta(IncomingMessage pedido) {
        List<String> modalidades = pedido.modalidades() == null || pedido.modalidades().isEmpty()
                ? List.of("cheque")
                : pedido.modalidades();
        if ("ate-valor".equals(pedido.modo())) {
            BigDecimal valor = pedido.valor();
            if (valor == null || valor.signum() <= 0) {
                throw new IllegalArgumentException("Informe um valor maior que zero.");
            }
            return chequeConsulta.resumoAteValor(valor, modalidades);
        }
        return chequeConsulta.resumoParaModelo(modalidades);
    }
}
