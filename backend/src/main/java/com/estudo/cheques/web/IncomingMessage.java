package com.estudo.cheques.web;

import java.math.BigDecimal;
import java.util.List;

public record IncomingMessage(String message, String modo, BigDecimal valor, List<String> modalidades) {
}
