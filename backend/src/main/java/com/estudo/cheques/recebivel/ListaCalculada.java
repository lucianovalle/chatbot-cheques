package com.estudo.cheques.recebivel;

import java.util.List;

/**
 * O texto que vai para o chat e a mesma lista, em objetos, para a tela Antecipar.
 * Os dois nascem da mesma conta. A tela não recalcula.
 */
public record ListaCalculada(String texto, List<RecebivelItem> itens) {
}
