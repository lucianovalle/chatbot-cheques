export interface RecebivelItem {
  ordem: number;
  tipo: string;
  numero: string;
  valor: number;
  vencimento: string;
  dias: number;
  desconto: number;
  liquido: number;
}

export interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
  itens?: RecebivelItem[];
}

export interface ChatReply {
  answer: string;
  itens?: RecebivelItem[];
}

/** A lista do Java vem junto da resposta. No F5 o texto do balão é lido de novo. */
export function prepararItens(itens: RecebivelItem[] | undefined, texto: string): RecebivelItem[] {
  if (itens && itens.length > 0) {
    return itens.map((item) => ({
      ...item,
      tipo: item.tipo.charAt(0).toUpperCase() + item.tipo.slice(1),
      vencimento: dataBr(item.vencimento),
    }));
  }
  return extrairItens(texto);
}

function dataBr(valor: string): string {
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(valor);
  if (!iso) {
    return valor;
  }
  return `${iso[3]}/${iso[2]}/${iso[1]}`;
}

function reais(texto: string): number {
  const limpo = texto.replace(/\s/g, '').replace('R$', '').replace(/\./g, '').replace(',', '.');
  return Number(limpo);
}

function extrairItens(texto: string): RecebivelItem[] {
  const linha =
    /^(\d+)\.\s+(Cheque|Boleto)\s+(\S+)\s+[—–-]\s+valor\s+(R\$\s*[\d.]+,\d{2}),\s+vence em\s+(\d{2}\/\d{2}\/\d{4})\s+\((\d+)\s+dias?\),\s+desconto\s+(R\$\s*[\d.]+,\d{2}),\s+líquido\s+(R\$\s*[\d.]+,\d{2})/gm;
  const itens: RecebivelItem[] = [];
  for (const encontrado of texto.matchAll(linha)) {
    itens.push({
      ordem: Number(encontrado[1]),
      tipo: encontrado[2],
      numero: encontrado[3],
      valor: reais(encontrado[4]),
      vencimento: encontrado[5],
      dias: Number(encontrado[6]),
      desconto: reais(encontrado[7]),
      liquido: reais(encontrado[8]),
    });
  }
  return itens;
}

export interface ChatInfo {
  modelName: string;
  baseUrl: string;
}

export interface SessaoChatInfo {
  sessaoId: string;
  usuarioId: number;
  usuarioNome: string;
}

export interface SessaoOpcao extends SessaoChatInfo {
  rotulo: string;
  vazia: boolean;
}

export interface HistoricoChat extends SessaoChatInfo {
  mensagens: ChatMessage[];
}

export interface PedidoConsulta {
  message: string;
  modo?: 'melhores' | 'ate-valor' | 'produto' | 'geral' | 'abertura';
  valor?: number;
  modalidades?: Array<'cheque' | 'boleto'>;
  sessaoId?: string;
}
