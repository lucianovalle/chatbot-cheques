import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChatInfo, ChatReply, HistoricoChat, PedidoConsulta, SessaoChatInfo, SessaoOpcao } from './chat.models';

/**
 * Segundo da fila. Não escolhe caminho.
 * A tela chama send, listarSessoes, novaSessao ou historico.
 * O próximo é o ChatController, em /api/chat.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  info(): Observable<ChatInfo> {
    return this.http.get<ChatInfo>('/api/chat/info');
  }

  send(pedido: PedidoConsulta): Observable<ChatReply> {
    return this.http.post<ChatReply>('/api/chat', pedido);
  }

  /** Sessões do usuário, da mais nova para a mais antiga. */
  listarSessoes(): Observable<SessaoOpcao[]> {
    return this.http.get<SessaoOpcao[]>('/api/chat/sessoes');
  }

  /** Abre outra conversa do usuário mockado. A anterior permanece no MySQL. */
  novaSessao(): Observable<SessaoChatInfo> {
    return this.http.post<SessaoChatInfo>('/api/chat/sessoes', {});
  }

  /** Traz dados e manual da sessão, na ordem em que foram falados. */
  historico(sessaoId: string): Observable<HistoricoChat> {
    return this.http.get<HistoricoChat>('/api/chat/historico', { params: { sessaoId } });
  }
}
