-- O id do usuário já estava em sessao_chat.
-- Aqui ele também entra em historico_mensagens e memoria_modelo.
-- A consulta da combo filtra por usuario_id direto, sem varrer a sessão de outra pessoa.

ALTER TABLE historico_mensagens
    ADD COLUMN usuario_id BIGINT NULL AFTER sessao_id;

UPDATE historico_mensagens h
JOIN sessao_chat s ON s.id = h.sessao_id
SET h.usuario_id = s.usuario_id;

ALTER TABLE historico_mensagens
    MODIFY usuario_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_hist_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

CREATE INDEX idx_hist_usuario_sessao ON historico_mensagens (usuario_id, sessao_id, id);

ALTER TABLE memoria_modelo
    ADD COLUMN usuario_id BIGINT NULL,
    ADD COLUMN sessao_id VARCHAR(36) NULL;

UPDATE memoria_modelo
SET sessao_id = LEFT(memory_id, 36);

UPDATE memoria_modelo m
JOIN sessao_chat s ON s.id = m.sessao_id
SET m.usuario_id = s.usuario_id;

UPDATE memoria_modelo
SET usuario_id = 1
WHERE usuario_id IS NULL;

ALTER TABLE memoria_modelo
    MODIFY usuario_id BIGINT NOT NULL,
    MODIFY sessao_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_memoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    ADD CONSTRAINT fk_memoria_sessao FOREIGN KEY (sessao_id) REFERENCES sessao_chat (id);

CREATE INDEX idx_memoria_usuario ON memoria_modelo (usuario_id, sessao_id);
CREATE INDEX idx_sessao_usuario_data ON sessao_chat (usuario_id, criada_em);
