import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChatInfo, ChatReply, PedidoConsulta } from './chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  info(): Observable<ChatInfo> {
    return this.http.get<ChatInfo>('/api/chat/info');
  }

  send(pedido: PedidoConsulta): Observable<ChatReply> {
    return this.http.post<ChatReply>('/api/chat', pedido);
  }

  reset(): Observable<void> {
    return this.http.post<void>('/api/chat/reset', {});
  }
}
