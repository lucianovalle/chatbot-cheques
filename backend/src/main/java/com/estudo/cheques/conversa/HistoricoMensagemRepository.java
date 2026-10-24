package com.estudo.cheques.conversa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoMensagemRepository extends JpaRepository<HistoricoMensagem, Long> {

    /** Usa o índice (usuario_id, sessao_id, id). As duas memórias da sessão vêm juntas. */
    List<HistoricoMensagem> findByUsuario_IdAndSessao_IdOrderByIdAsc(Long usuarioId, String sessaoId);
}
