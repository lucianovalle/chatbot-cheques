package com.estudo.cheques.service;

import com.estudo.cheques.config.OllamaProperties;
import com.estudo.cheques.conversa.HistoricoConversaService;
import com.estudo.cheques.conversa.MemoriaPorSessao;
import com.estudo.cheques.conversa.TipoMemoria;
import com.estudo.cheques.recebivel.ChequeConsulta;
import com.estudo.cheques.recebivel.ListaCalculada;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Quarto da fila e primeiro que decide de verdade.
 * A tela já mandou um modo. Este reply confere de novo, nesta ordem.
 * O primeiro if que servir responde. Os de baixo não rodam.
 * 1. abertura — texto fixo. Sem modelo.
 * 2. melhores ou ate-valor — ChequeConsulta. Sem modelo.
 * 3. produto — AssistenteProduto lê o manual. Memória termina em _manual.
 * 4. frase sem recebível — chatModel sozinho. Sem ferramenta e sem MySQL.
 * 5. recebível — AssistenteRecebiveis. O modelo escolhe RecebivelTools. Memória _dados.
 * No fim de cada caminho, HistoricoConversaService grava a frase que a tela mostra.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Resposta fixa de "quero antecipar". Não passa pelo modelo, mas fica no histórico. */
    static final String TEXTO_ABERTURA =
            "Para antecipar, marque cheque, boleto ou os dois e escolha se quer ver os melhores ou cobrir um valor.";

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
    private final MemoriaPorSessao memorias;
    private final HistoricoConversaService historico;
    private final OllamaProperties ollama;
    private final ChequeConsulta chequeConsulta;

    public ChatService(
            AssistenteRecebiveis assistente,
            AssistenteProduto assistenteProduto,
            ChatModel chatModel,
            MemoriaPorSessao memorias,
            HistoricoConversaService historico,
            OllamaProperties ollama,
            ChequeConsulta chequeConsulta) {
        this.assistente = assistente;
        this.assistenteProduto = assistenteProduto;
        this.chatModel = chatModel;
        this.memorias = memorias;
        this.historico = historico;
        this.ollama = ollama;
        this.chequeConsulta = chequeConsulta;
    }

    public String modelName() {
        return ollama.modelName();
    }

    public String baseUrl() {
        return ollama.baseUrl();
    }

    public synchronized RespostaChat reply(IncomingMessage pedido) {
        String userText = pedido.message().trim();
        String sessaoId = pedido.sessaoId();
        historico.conferirSessao(sessaoId);

        // 1. A tela já abriu cheque e boleto. Aqui só grava a frase.
        if ("abertura".equals(pedido.modo())) {
            gravarNaMemoriaDeDados(sessaoId, userText, TEXTO_ABERTURA);
            historico.registrarTurno(sessaoId, TipoMemoria.DADOS, userText, TEXTO_ABERTURA);
            log.info("Abertura do formulário gravada na sessão {}, sem chamar o modelo", sessaoId);
            return new RespostaChat(TEXTO_ABERTURA);
        }

        // 2. Veio do botão Consultar. ChequeConsulta lê o MySQL e faz a conta.
        // A lista de itens é a mesma conta: a tela Antecipar não interpreta o texto.
        if ("melhores".equals(pedido.modo()) || "ate-valor".equals(pedido.modo())) {
            ListaCalculada calculo = consultaDireta(pedido);
            gravarNaMemoriaDeDados(sessaoId, userText, calculo.texto());
            historico.registrarTurno(sessaoId, TipoMemoria.DADOS, userText, calculo.texto());
            log.info("Consulta do formulário respondida pelo Java, sem chamar o modelo");
            return new RespostaChat(calculo.texto(), calculo.itens());
        }

        try {
            // 3. Manual antes de recebível. "O que é a antecipação" tem a palavra recebível,
            // mas a resposta está no documento, não na tabela.
            if ("produto".equals(pedido.modo()) || falaDeProduto(userText)) {
                log.info("Pergunta de produto respondida pelo modelo {} com os manuais", ollama.modelName());
                String answer = textoOuVazio(assistenteProduto.explicar(MemoriaPorSessao.idManual(sessaoId), userText));
                historico.registrarTurno(sessaoId, TipoMemoria.MANUAL, userText, answer);
                return new RespostaChat(answer);
            }
            // 4. Pergunta geral. O modo "geral" da tela não basta: se houver recebível, cai no 5.
            if (!falaDeRecebivel(userText)) {
                log.info("Pergunta geral respondida pelo modelo {}, sem ferramenta e sem banco", ollama.modelName());
                String answer = responderDireto(sessaoId, userText);
                historico.registrarTurno(sessaoId, TipoMemoria.DADOS, userText, answer);
                return new RespostaChat(answer);
            }
            // 5. O modelo escolhe a ferramenta. Quem lê o MySQL é o ChequeConsulta.
            log.info("Pergunta sobre recebíveis para o modelo {} escolher a ferramenta", ollama.modelName());
            String answer = textoOuVazio(assistente.responder(MemoriaPorSessao.idDados(sessaoId), userText));
            historico.registrarTurno(sessaoId, TipoMemoria.DADOS, userText, answer);
            return new RespostaChat(answer);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ModeloIndisponivelException(
                    "Não consegui falar com o Ollama em " + ollama.baseUrl()
                            + " (modelo " + ollama.modelName() + "). "
                            + "Detalhe: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Manda a frase ao llama3.2 sem ferramenta e sem trecho de manual.
     * A instrução daqui não cita ferramenta. Se citasse, o modelo responderia
     * "vou chamar a ferramenta" sem ter como chamá-la.
     */
    private String responderDireto(String sessaoId, String userText) {
        ChatMemory memoria = memorias.get(MemoriaPorSessao.idDados(sessaoId));
        List<ChatMessage> turn = new ArrayList<>();
        turn.add(SystemMessage.from("""
                Responda em português, em poucas frases.
                Não invente quantidade, número, valor ou taxa de recebíveis.
                """));
        for (ChatMessage mensagem : memoria.messages()) {
            if (!(mensagem instanceof SystemMessage)) {
                turn.add(mensagem);
            }
        }
        turn.add(UserMessage.from(userText));
        ChatResponse response = chatModel.chat(turn);
        String answer = textoOuVazio(response.aiMessage().text());
        memoria.add(UserMessage.from(userText));
        memoria.add(AiMessage.from(answer));
        return answer;
    }

    /**
     * Formulário e abertura não passam pelo AiServices, então a frase é colocada
     * na memória de dados à mão. Assim a próxima pergunta geral ainda enxerga o turno.
     */
    private void gravarNaMemoriaDeDados(String sessaoId, String pergunta, String resposta) {
        ChatMemory memoria = memorias.get(MemoriaPorSessao.idDados(sessaoId));
        memoria.add(UserMessage.from(pergunta));
        memoria.add(AiMessage.from(resposta));
    }

    private static String textoOuVazio(String answer) {
        if (answer == null || answer.isBlank()) {
            return "(O modelo respondeu sem texto.)";
        }
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

    private ListaCalculada consultaDireta(IncomingMessage pedido) {
        List<String> modalidades = pedido.modalidades() == null || pedido.modalidades().isEmpty()
                ? List.of("cheque")
                : pedido.modalidades();
        if ("ate-valor".equals(pedido.modo())) {
            BigDecimal valor = pedido.valor();
            if (valor == null || valor.signum() <= 0) {
                throw new IllegalArgumentException("Informe um valor maior que zero.");
            }
            return chequeConsulta.calcularAteValor(valor, modalidades);
        }
        return chequeConsulta.calcularMelhores(modalidades);
    }
}
