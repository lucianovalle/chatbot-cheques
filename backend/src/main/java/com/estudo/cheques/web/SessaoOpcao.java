package com.estudo.cheques.web;

/** Uma linha da combo. rotulo já vem com data e o começo da primeira frase. */
public record SessaoOpcao(String sessaoId, long usuarioId, String usuarioNome, String rotulo, boolean vazia) {
}
