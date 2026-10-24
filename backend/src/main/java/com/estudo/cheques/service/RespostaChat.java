package com.estudo.cheques.service;

import com.estudo.cheques.recebivel.RecebivelItem;
import java.util.List;

/** Texto do chat e, quando a conta foi do Java, os títulos da tela Antecipar. */
public record RespostaChat(String texto, List<RecebivelItem> itens) {

    public RespostaChat(String texto) {
        this(texto, List.of());
    }
}
