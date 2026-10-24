package com.estudo.cheques.conversa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pessoa dona da conversa. Neste estudo existe só o usuário 1, carregado pelo Flyway.
 * Não há senha: a tela usa esse registro fixo para o histórico ter um dono.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
