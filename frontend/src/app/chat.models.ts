export interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
}

export interface ChatReply {
  answer: string;
}

export interface ChatInfo {
  modelName: string;
  baseUrl: string;
}

export interface PedidoConsulta {
  message: string;
  modo?: 'melhores' | 'ate-valor' | 'produto' | 'geral';
  valor?: number;
  modalidades?: Array<'cheque' | 'boleto'>;
}
