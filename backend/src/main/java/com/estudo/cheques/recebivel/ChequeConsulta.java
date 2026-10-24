package com.estudo.cheques.recebivel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Conta da antecipação. Esta classe não chama IA.
 * Custo = taxa diária da tabela taxas_juros × dias até o vencimento.
 * Desconto = valor de face × custo. Líquido = valor de face − desconto.
 * A lista sai do menor custo para o maior. Título vencido fica de fora.
 */
@Service
public class ChequeConsulta {

    private static final Set<String> MODALIDADES = Set.of("cheque", "boleto");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChequeRepository chequeRepository;
    private final BoletoRepository boletoRepository;
    private final TaxaRepository taxaRepository;

    public ChequeConsulta(
            ChequeRepository chequeRepository,
            BoletoRepository boletoRepository,
            TaxaRepository taxaRepository) {
        this.chequeRepository = chequeRepository;
        this.boletoRepository = boletoRepository;
        this.taxaRepository = taxaRepository;
    }

    public String contagemEmAberto() {
        Apuracao apuracao = apurar(List.of("cheque", "boleto"));
        long cheques = apuracao.abertos().stream().filter(linha -> "cheque".equals(linha.tipo())).count();
        long boletos = apuracao.abertos().stream().filter(linha -> "boleto".equals(linha.tipo())).count();
        if (cheques == 0 && boletos == 0) {
            return "Não há recebíveis em aberto para antecipar. Cheques: 0. Boletos: 0.";
        }
        return "Sim, há recebíveis em aberto para antecipar. Cheques: "
                + cheques
                + ". Boletos: "
                + boletos
                + ".";
    }

    public String resumoParaModelo(List<String> modalidades) {
        Apuracao apuracao = apurar(modalidades);
        StringBuilder texto = new StringBuilder();
        texto.append("Recebíveis em aberto de ")
                .append(apuracao.modalidades())
                .append(", do menor custo de antecipação para o maior.\n");
        texto.append("Custo = taxa diária × dias até o vencimento. Em ")
                .append(apuracao.hoje().format(DATA))
                .append(".\n\n");
        appendLinhas(texto, apuracao.abertos());
        return texto.toString();
    }

    public String resumoAteValor(BigDecimal alvo, List<String> modalidades) {
        if (alvo == null || alvo.signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero.");
        }
        Apuracao apuracao = apurar(modalidades);
        List<Linha> escolhidos = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;
        for (Linha linha : apuracao.abertos()) {
            if (soma.compareTo(alvo) >= 0) {
                break;
            }
            escolhidos.add(linha);
            soma = soma.add(linha.liquido());
        }

        StringBuilder texto = new StringBuilder();
        texto.append("Para cobrir ")
                .append(dinheiro(alvo))
                .append(" de líquido em ")
                .append(apuracao.modalidades())
                .append(", estes são os recebíveis, do menor custo para o maior.\n\n");
        appendLinhas(texto, escolhidos);
        texto.append("\nSoma dos líquidos: ").append(dinheiro(soma)).append('\n');
        if (soma.compareTo(alvo) < 0) {
            texto.append("A soma não cobre o valor pedido. Faltam ")
                    .append(dinheiro(alvo.subtract(soma)))
                    .append(".\n");
        }
        return texto.toString();
    }

    private Apuracao apurar(List<String> modalidades) {
        List<String> escolhidas = normalizar(modalidades);
        LocalDate hoje = LocalDate.now();
        List<Linha> abertos = new ArrayList<>();

        if (escolhidas.contains("cheque")) {
            abertos.addAll(linhas("cheque", hoje, carregarCheques()));
        }
        if (escolhidas.contains("boleto")) {
            abertos.addAll(linhas("boleto", hoje, carregarBoletos()));
        }

        // Menor custo primeiro. Se o custo empatar, fica o de maior líquido.
        abertos.sort(Comparator.comparing(Linha::custo).thenComparing(Linha::liquido, Comparator.reverseOrder()));
        return new Apuracao(hoje, String.join(" e ", escolhidas), abertos);
    }

    private List<Titulo> carregarCheques() {
        return chequeRepository.findAllByOrderByDataVencimentoAsc().stream()
                .map(cheque -> new Titulo(cheque.getNumero(), cheque.getValor(), cheque.getDataVencimento()))
                .toList();
    }

    private List<Titulo> carregarBoletos() {
        return boletoRepository.findAllByOrderByDataVencimentoAsc().stream()
                .map(boleto -> new Titulo(boleto.getNumero(), boleto.getValor(), boleto.getDataVencimento()))
                .toList();
    }

    /** Aplica a fórmula de um tipo só: cheque ou boleto, cada um com a sua taxa. */
    private List<Linha> linhas(String modalidade, LocalDate hoje, List<Titulo> titulos) {
        Taxa taxa = taxaRepository.findByModalidade(modalidade)
                .orElseThrow(() -> new IllegalStateException("Não há taxa com modalidade " + modalidade + " na tabela taxas_juros."));
        List<Linha> linhas = new ArrayList<>();
        for (Titulo titulo : titulos) {
            long dias = ChronoUnit.DAYS.between(hoje, titulo.vencimento());
            if (dias < 0) {
                continue;
            }
            // desconto = valor × taxa diária × dias; líquido = valor − desconto
            BigDecimal desconto = titulo.valor()
                    .multiply(taxa.getTaxaDiaria())
                    .multiply(BigDecimal.valueOf(dias))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal liquido = titulo.valor().subtract(desconto);
            BigDecimal custo = taxa.getTaxaDiaria().multiply(BigDecimal.valueOf(dias));
            linhas.add(new Linha(modalidade, titulo.numero(), titulo.valor(), titulo.vencimento(), dias, desconto, liquido, custo));
        }
        return linhas;
    }

    private static List<String> normalizar(List<String> modalidades) {
        if (modalidades == null || modalidades.isEmpty()) {
            throw new IllegalArgumentException("Marque cheque, boleto ou os dois.");
        }
        List<String> escolhidas = modalidades.stream()
                .filter(item -> item != null && MODALIDADES.contains(item.toLowerCase(Locale.ROOT)))
                .map(item -> item.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (escolhidas.isEmpty()) {
            throw new IllegalArgumentException("Marque cheque, boleto ou os dois.");
        }
        return escolhidas;
    }

    private static void appendLinhas(StringBuilder texto, List<Linha> linhas) {
        if (linhas.isEmpty()) {
            texto.append("Nenhum recebível em aberto.\n");
            return;
        }
        int ordem = 1;
        for (Linha linha : linhas) {
            String tipo = linha.tipo().substring(0, 1).toUpperCase(Locale.ROOT) + linha.tipo().substring(1);
            texto.append(ordem++)
                    .append(". ")
                    .append(tipo)
                    .append(' ')
                    .append(linha.numero())
                    .append(" — valor ")
                    .append(dinheiro(linha.valor()))
                    .append(", vence em ")
                    .append(linha.vencimento().format(DATA))
                    .append(" (")
                    .append(linha.dias())
                    .append(linha.dias() == 1 ? " dia" : " dias")
                    .append("), desconto ")
                    .append(dinheiro(linha.desconto()))
                    .append(", líquido ")
                    .append(dinheiro(linha.liquido()))
                    .append('\n');
        }
    }

    private static String dinheiro(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }

    private record Titulo(String numero, BigDecimal valor, LocalDate vencimento) {
    }

    private record Linha(
            String tipo,
            String numero,
            BigDecimal valor,
            LocalDate vencimento,
            long dias,
            BigDecimal desconto,
            BigDecimal liquido,
            BigDecimal custo) {
    }

    private record Apuracao(LocalDate hoje, String modalidades, List<Linha> abertos) {
    }
}
