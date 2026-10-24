-- Estudo: a conversa deixa de morar só na memória do Java.
-- usuario: pessoa mockada, sem senha e sem login.
-- sessao_chat: uma conversa. Nova conversa cria outra linha; a antiga fica no banco.
-- historico_mensagens: o que a tela mostra depois do F5. Cada turno entra e não é apagado.
-- memoria_modelo: o que o llama3.2 relembra. É a janela curta, separada em dados e manual.

CREATE TABLE usuario (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL
);

INSERT INTO usuario (id, nome) VALUES (1, 'Luciano');

CREATE TABLE sessao_chat (
    id VARCHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sessao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE historico_mensagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessao_id VARCHAR(36) NOT NULL,
    tipo_memoria VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_sessao FOREIGN KEY (sessao_id) REFERENCES sessao_chat (id)
);

CREATE INDEX idx_hist_sessao ON historico_mensagens (sessao_id, id);

-- memory_id junta a sessão e o tipo: "<sessao>_dados" ou "<sessao>_manual".
-- mensagens_json é a lista que o LangChain4j devolve, não o texto da tela.
CREATE TABLE memoria_modelo (
    memory_id VARCHAR(80) PRIMARY KEY,
    mensagens_json MEDIUMTEXT NOT NULL
);
