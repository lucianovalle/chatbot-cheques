package com.estudo.cheques.web;

import com.estudo.cheques.recebivel.RecebivelItem;
import java.util.List;

/** answer enche o balão. itens alimenta a tela que abre no botão Antecipar. */
public record ChatReply(String answer, List<RecebivelItem> itens) {
}
