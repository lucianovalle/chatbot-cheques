package com.estudo.cheques.conversa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Janela que o modelo relê na próxima pergunta. Uma linha por chave.
 * A chave é sessaoId + "_dados" ou sessaoId + "_manual".
 * O JSON pode ter mensagem de sistema e chamada de ferramenta.
 * A tela não lê esta tabela. Ela lê historico_mensagens.
 * usuarioId e sessaoId repetem a chave, para achar a janela deste usuário sem ler o JSON.
 */
@Entity
@Table(name = "memoria_modelo")
public class MemoriaModelo {

    @Id
    @Column(name = "memory_id", length = 80)
    private String memoryId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "sessao_id", length = 36, nullable = false)
    private String sessaoId;

    @Column(name = "mensagens_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String mensagensJson;

    protected MemoriaModelo() {}

    public MemoriaModelo(String memoryId, long usuarioId, String sessaoId, String mensagensJson) {
        this.memoryId = memoryId;
        this.usuarioId = usuarioId;
        this.sessaoId = sessaoId;
        this.mensagensJson = mensagensJson;
    }

    public String getMemoryId() {
        return memoryId;
    }

    public String getMensagensJson() {
        return mensagensJson;
    }

    public void setMensagensJson(String mensagensJson) {
        this.mensagensJson = mensagensJson;
    }
}
