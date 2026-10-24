package com.estudo.cheques.service;

public class ModeloIndisponivelException extends RuntimeException {

    public ModeloIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
