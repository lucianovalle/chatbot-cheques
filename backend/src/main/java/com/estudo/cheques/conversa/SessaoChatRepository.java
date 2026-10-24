package com.estudo.cheques.conversa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SessaoChatRepository extends JpaRepository<SessaoChat, String> {

    /**
     * Combo de conversas de um usuário só.
     * O subselect da primeira frase também filtra usuario_id, para usar o mesmo índice.
     */
    @Query(value = """
            SELECT s.id,
                   s.usuario_id,
                   u.nome,
                   s.criada_em,
                   (SELECT h.conteudo
                      FROM historico_mensagens h
                     WHERE h.usuario_id = s.usuario_id
                       AND h.sessao_id = s.id
                       AND h.role = 'USER'
                     ORDER BY h.id
                     LIMIT 1)
              FROM sessao_chat s
              JOIN usuario u ON u.id = s.usuario_id
             WHERE s.usuario_id = :usuarioId
             ORDER BY s.criada_em DESC, s.id DESC
            """, nativeQuery = true)
    List<Object[]> listarDoUsuario(long usuarioId);
}
