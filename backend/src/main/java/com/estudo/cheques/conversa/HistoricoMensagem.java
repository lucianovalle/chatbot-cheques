package com.estudo.cheques.conversa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Uma frase visível no chat. Esta tabela não é cortada em 20 mensagens.
 * tipoMemoria diz se a frase entrou pela memória de dados ou pela do manual.
 * A tela, no F5, lê as duas juntas, em ordem de id.
 * usuarioId repete o dono da sessão de propósito: a busca filtra por ele
 * e não precisa descobrir o dono só pela sessão.
 */
@Entity
@Table(name = "historico_mensagens")
public class HistoricoMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoChat sessao;

    /** Mesmo id de sessao.usuario. Índice (usuario_id, sessao_id, id). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_memoria", nullable = false, length = 20)
    private TipoMemoria tipoMemoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelMensagem role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void marcarCriacao() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public SessaoChat getSessao() {
        return sessao;
    }

    public void setSessao(SessaoChat sessao) {
        this.sessao = sessao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoMemoria getTipoMemoria() {
        return tipoMemoria;
    }

    public void setTipoMemoria(TipoMemoria tipoMemoria) {
        this.tipoMemoria = tipoMemoria;
    }

    public PapelMensagem getRole() {
        return role;
    }

    public void setRole(PapelMensagem role) {
        this.role = role;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
}
