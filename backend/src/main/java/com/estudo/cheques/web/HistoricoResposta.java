package com.estudo.cheques.web;

import java.util.List;

public record HistoricoResposta(
        String sessaoId, long usuarioId, String usuarioNome, List<MensagemVisivel> mensagens) {
}
