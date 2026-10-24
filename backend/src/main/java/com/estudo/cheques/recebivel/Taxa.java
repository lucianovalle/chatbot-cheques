package com.estudo.cheques.recebivel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "taxas_juros")
public class Taxa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String modalidade;

    @Column(name = "taxa_diaria", nullable = false, precision = 10, scale = 6)
    private BigDecimal taxaDiaria;

    public Long getId() {
        return id;
    }

    public String getModalidade() {
        return modalidade;
    }

    public BigDecimal getTaxaDiaria() {
        return taxaDiaria;
    }
}
