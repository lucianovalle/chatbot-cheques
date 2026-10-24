package com.estudo.cheques.web;

import com.estudo.cheques.service.ChatService;
import com.estudo.cheques.service.ModeloIndisponivelException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Porta HTTP da conversa. O Angular chama POST /api/chat. A decisão fica no ChatService. */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4201")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/info")
    public ChatInfo info() {
        return new ChatInfo(chatService.modelName(), chatService.baseUrl());
    }

    @PostMapping
    public ChatReply chat(@RequestBody IncomingMessage body) {
        if (body == null || body.message() == null || body.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva uma mensagem.");
        }
        return new ChatReply(chatService.reply(body));
    }

    @PostMapping("/reset")
    public void reset() {
        chatService.reset();
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
