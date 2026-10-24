package com.estudo.cheques.recebivel;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ferramentas que o llama3.2 pode chamar. O @Tool descreve a função para o modelo.
 * O modelo só escolhe qual chamar. Quem lê o MySQL e faz a conta é o ChequeConsulta.
 * IMMEDIATE devolve o texto do Java direto ao usuário, sem o modelo reescrever número.
 */
@Component
public class RecebivelTools {

    private static final Logger log = LoggerFactory.getLogger(RecebivelTools.class);

    private final ChequeConsulta chequeConsulta;

    public RecebivelTools(ChequeConsulta chequeConsulta) {
        this.chequeConsulta = chequeConsulta;
    }

    /** "Tenho recebíveis para antecipar?" cai aqui. */
    @Tool(
            name = "contarRecebiveis",
            value = "Consulta o MySQL e conta cheques e boletos em aberto que ainda podem ser antecipados. Use quando perguntarem se há recebíveis.",
            returnBehavior = ReturnBehavior.IMMEDIATE)
    public String contarRecebiveis() {
        log.info("Ferramenta contarRecebiveis");
        return chequeConsulta.contagemEmAberto();
    }

    /** "Quais são os melhores cheques?" cai aqui. */
    @Tool(
            name = "listarMelhores",
            value = "Lista os recebíveis em aberto do menor custo de antecipação para o maior.",
            returnBehavior = ReturnBehavior.IMMEDIATE)
    public String listarMelhores(@P("cheque, boleto ou ambos") String modalidades) {
        log.info("Ferramenta listarMelhores modalidades={}", modalidades);
        try {
            return chequeConsulta.resumoParaModelo(interpretar(modalidades));
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    /** "Quero cobrir 50000" cai aqui. O valor vem do que o modelo entendeu na frase. */
    @Tool(
            name = "cobrirValor",
            value = "Escolhe os recebíveis de menor custo até cobrir um valor líquido.",
            returnBehavior = ReturnBehavior.IMMEDIATE)
    public String cobrirValor(
            @P("valor em reais a cobrir") double valor,
            @P("cheque, boleto ou ambos") String modalidades) {
        log.info("Ferramenta cobrirValor valor={} modalidades={}", valor, modalidades);
        try {
            return chequeConsulta.resumoAteValor(BigDecimal.valueOf(valor), interpretar(modalidades));
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    private static List<String> interpretar(String texto) {
        String normalizado = texto == null
                ? ""
                : Normalizer.normalize(texto, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT);
        boolean todos = normalizado.isBlank()
                || normalizado.contains("ambos")
                || normalizado.contains("dois")
                || normalizado.contains("tudo");
        List<String> modalidades = new ArrayList<>();
        if (todos || normalizado.contains("cheque")) {
            modalidades.add("cheque");
        }
        if (todos || normalizado.contains("boleto")) {
            modalidades.add("boleto");
        }
        if (modalidades.isEmpty()) {
            modalidades.add("cheque");
            modalidades.add("boleto");
        }
        return modalidades;
    }
}
