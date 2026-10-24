package com.estudo.cheques.recebivel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um título já calculado, pronto para a tela de antecipação.
 * A ordem é a do menor custo para o maior. O modelo não redefine essa ordem.
 */
public record RecebivelItem(
        int ordem,
        String tipo,
        String numero,
        BigDecimal valor,
        LocalDate vencimento,
        long dias,
        BigDecimal desconto,
        BigDecimal liquido) {
}
