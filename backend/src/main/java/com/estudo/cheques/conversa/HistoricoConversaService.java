package com.estudo.cheques.conversa;

import com.estudo.cheques.web.HistoricoResposta;
import com.estudo.cheques.web.MensagemVisivel;
import com.estudo.cheques.web.SessaoOpcao;
import com.estudo.cheques.web.SessaoResposta;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Histórico que a tela mostra. Roda depois da resposta, não antes do modelo.
 * Quem chama: a combo e o F5 usam listar e listarOpcoes.
 * O ChatService.reply chama registrarTurno no fim de cada caminho.
 * O usuário é o registro 1. Cada "Nova conversa" cria outra sessão e não apaga a antiga.
 */
@Service
public class HistoricoConversaService {

    /** Id fixo do usuário de estudo. Não vem de login. */
    public static final long USUARIO_MOCADO = 1L;

    private static final DateTimeFormatter DATA_DA_COMBO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Logger log = LoggerFactory.getLogger(HistoricoConversaService.class);

    private final UsuarioRepository usuarios;
    private final SessaoChatRepository sessoes;
    private final HistoricoMensagemRepository mensagens;

    public HistoricoConversaService(
            UsuarioRepository usuarios,
            SessaoChatRepository sessoes,
            HistoricoMensagemRepository mensagens) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.mensagens = mensagens;
    }

    @Transactional
    public SessaoResposta novaSessao() {
        Usuario usuario = usuarios.findById(USUARIO_MOCADO)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário de estudo não encontrado. A migração V007 precisa ter rodado."));
        SessaoChat sessao = new SessaoChat();
        sessao.setId(UUID.randomUUID().toString());
        sessao.setUsuario(usuario);
        sessoes.save(sessao);
        log.info("Sessão {} aberta para o usuário {}", sessao.getId(), usuario.getNome());
        return new SessaoResposta(sessao.getId(), usuario.getId(), usuario.getNome());
    }

    /**
     * Lista as sessões do usuário 1 para a combo.
     * A SQL filtra sessao_chat e historico_mensagens pelo mesmo usuario_id.
     */
    @Transactional(readOnly = true)
    public List<SessaoOpcao> listarOpcoes() {
        List<SessaoOpcao> opcoes = new ArrayList<>();
        for (Object[] linha : sessoes.listarDoUsuario(USUARIO_MOCADO)) {
            String sessaoId = String.valueOf(linha[0]);
            long usuarioId = ((Number) linha[1]).longValue();
            String nome = String.valueOf(linha[2]);
            LocalDateTime criadaEm = paraData(linha[3]);
            String resumo = linha[4] == null ? null : String.valueOf(linha[4]);
            boolean vazia = resumo == null || resumo.isBlank();
            opcoes.add(new SessaoOpcao(sessaoId, usuarioId, nome, rotulo(criadaEm, resumo), vazia));
        }
        return opcoes;
    }

    /** Confere se o id que o navegador mandou existe, antes de chamar o modelo. */
    @Transactional(readOnly = true)
    public void conferirSessao(String sessaoId) {
        exigir(sessaoId);
    }

    @Transactional(readOnly = true)
    public HistoricoResposta listar(String sessaoId) {
        SessaoChat sessao = exigir(sessaoId);
        List<MensagemVisivel> visiveis = mensagens
                .findByUsuario_IdAndSessao_IdOrderByIdAsc(sessao.getUsuario().getId(), sessao.getId())
                .stream()
                .map(msg -> new MensagemVisivel(papelNaTela(msg.getRole()), msg.getConteudo()))
                .toList();
        Usuario usuario = sessao.getUsuario();
        return new HistoricoResposta(sessao.getId(), usuario.getId(), usuario.getNome(), visiveis);
    }

    /**
     * Acrescenta a pergunta e a resposta no fim da sessão.
     * Não apaga as frases antigas. DADOS e MANUAL ficam na mesma sessão, com tipos diferentes.
     */
    @Transactional
    public void registrarTurno(String sessaoId, TipoMemoria tipo, String pergunta, String resposta) {
        SessaoChat sessao = exigir(sessaoId);
        mensagens.save(linha(sessao, tipo, PapelMensagem.USER, pergunta));
        mensagens.save(linha(sessao, tipo, PapelMensagem.ASSISTANT, resposta));
        log.info("Turno gravado na sessão {} com memória {}", sessaoId, tipo);
    }

    private SessaoChat exigir(String sessaoId) {
        if (sessaoId == null || sessaoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A conversa precisa de uma sessão.");
        }
        return sessoes.findById(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));
    }

    private static HistoricoMensagem linha(SessaoChat sessao, TipoMemoria tipo, PapelMensagem papel, String texto) {
        HistoricoMensagem mensagem = new HistoricoMensagem();
        mensagem.setSessao(sessao);
        mensagem.setUsuario(sessao.getUsuario());
        mensagem.setTipoMemoria(tipo);
        mensagem.setRole(papel);
        mensagem.setConteudo(texto);
        return mensagem;
    }

    private static LocalDateTime paraData(Object valor) {
        if (valor instanceof LocalDateTime data) {
            return data;
        }
        if (valor instanceof Timestamp marca) {
            return marca.toLocalDateTime();
        }
        throw new IllegalStateException("Data da sessão em formato inesperado.");
    }

    /** Data da sessão e o começo da primeira pergunta. Sessão sem frase fica "conversa nova". */
    private static String rotulo(LocalDateTime criadaEm, String resumo) {
        String titulo = resumo == null || resumo.isBlank()
                ? "conversa nova"
                : resumo.replaceAll("\\s+", " ").trim();
        if (titulo.length() > 48) {
            titulo = titulo.substring(0, 48).trim() + "…";
        }
        return criadaEm.format(DATA_DA_COMBO) + " — " + titulo;
    }

    /** O Angular chama o assistente de "bot". No banco o papel fica ASSISTANT. */
    private static String papelNaTela(PapelMensagem papel) {
        return papel == PapelMensagem.USER ? "user" : "bot";
    }
}
