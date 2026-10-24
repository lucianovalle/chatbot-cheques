package com.estudo.cheques.web;

import com.estudo.cheques.conversa.HistoricoConversaService;
import com.estudo.cheques.service.ChatService;
import com.estudo.cheques.service.ModeloIndisponivelException;
import com.estudo.cheques.service.RespostaChat;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Terceiro da fila. Só recebe o HTTP e entrega para a classe seguinte.
 * POST /api/chat vai para ChatService.reply, que escolhe o caminho.
 * GET e POST /sessoes e GET /historico vão para HistoricoConversaService.
 * Nenhum cálculo e nenhuma chamada ao modelo acontecem aqui.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4201")
public class ChatController {

    private final ChatService chatService;
    private final HistoricoConversaService historico;

    public ChatController(ChatService chatService, HistoricoConversaService historico) {
        this.chatService = chatService;
        this.historico = historico;
    }

    @GetMapping("/info")
    public ChatInfo info() {
        return new ChatInfo(chatService.modelName(), chatService.baseUrl());
    }

    /** Cria outra sessão do usuário 1. A sessão anterior continua no MySQL. */
    @PostMapping("/sessoes")
    public SessaoResposta novaSessao() {
        return historico.novaSessao();
    }

    /** Combo: uma opção por sessão do usuário, da mais nova para a mais antiga. */
    @GetMapping("/sessoes")
    public List<SessaoOpcao> sessoes() {
        return historico.listarOpcoes();
    }

    /** Lê dados e manual da mesma sessão, na ordem em que foram falados. */
    @GetMapping("/historico")
    public HistoricoResposta historico(@RequestParam String sessaoId) {
        return historico.listar(sessaoId);
    }

    /** Quarto passo da mensagem. Daqui o reply decide quem responde. */
    @PostMapping
    public ChatReply chat(@RequestBody IncomingMessage body) {
        if (body == null || body.message() == null || body.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva uma mensagem.");
        }
        RespostaChat resposta = chatService.reply(body);
        return new ChatReply(resposta.texto(), resposta.itens());
    }

    @ExceptionHandler(ModeloIndisponivelException.class)
    public ResponseEntity<Map<String, String>> onModelDown(ModeloIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> onData(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> onBadAmount(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}
