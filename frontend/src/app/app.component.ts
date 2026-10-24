import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChatMessage,
  ChatReply,
  HistoricoChat,
  PedidoConsulta,
  RecebivelItem,
  SessaoChatInfo,
  SessaoOpcao,
  prepararItens,
} from './chat.models';
import { ChatService } from './chat.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
/**
 * Primeiro da fila: esta tela.
 * 1. Ao abrir, pede as sessões e o histórico.
 * 2. Enviar separa a frase aqui e chama o ChatService do Angular.
 * 3. Esse serviço só faz HTTP para o ChatController.
 * 4. O ChatService do Java confirma o caminho no método reply.
 */
export class AppComponent implements OnInit {
  private readonly chat = inject(ChatService);
  /** Sobrevive ao F5. A combo usa este id para reabrir a mesma sessão. */
  private readonly chaveSessao = 'chatbot-cheques.sessaoId';

  readonly messages = signal<ChatMessage[]>([]);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly modelName = signal('...');
  readonly usuarioNome = signal('...');
  readonly sessoes = signal<SessaoOpcao[]>([]);
  readonly sessaoAtual = signal<string | null>(null);
  /** Tela que abre no botão Antecipar. Null significa que o chat está na frente. */
  readonly listaTela = signal<RecebivelItem[] | null>(null);

  /**
   * Frases de estudo ao lado do chat. O clique só preenche o campo.
   * O envio continua no botão Enviar, para a pessoa ver o caminho antes.
   * caminho é o texto que aparece ao deixar o mouse sobre a frase.
   */
  readonly exemplos: Array<{ titulo: string; itens: Array<{ texto: string; caminho: string }> }> = [
    {
      titulo: 'Manual',
      itens: [
        {
          texto: 'O que é o produto de antecipação de recebíveis?',
          caminho: 'Manual. A tela mostra Consultando o manual…',
        },
        {
          texto: 'Quais são os títulos aceitos para antecipação?',
          caminho: 'Manual. A resposta sai dos arquivos da pasta docs.',
        },
        {
          texto: 'Como é calculado o custo de desconto de um título?',
          caminho: 'Manual. A fórmula está no documento, não no MySQL.',
        },
      ],
    },
    {
      titulo: 'Banco',
      itens: [
        {
          texto: 'Tenho recebíveis para antecipar?',
          caminho: 'Ferramenta. O Java conta cheques e boletos no MySQL.',
        },
        {
          texto: 'Quais são as melhores opções de títulos para antecipar e otimizar meu caixa?',
          caminho: 'Ferramenta. A lista sai do menor custo para o maior.',
        },
        {
          texto: 'Explique qual é o critério para priorizar um recebível em relação ao outro.',
          caminho: 'Ferramenta. O critério é taxa diária vezes os dias.',
        },
      ],
    },
    {
      titulo: 'Formulário',
      itens: [
        {
          texto: 'quero antecipar',
          caminho: 'Abre cheque, boleto e valor. O modelo não entra.',
        },
      ],
    },
    {
      titulo: 'Geral',
      itens: [
        {
          texto: 'Qual é a capital do Brasil?',
          caminho: 'Modelo sozinho, sem manual e sem banco.',
        },
        {
          texto: 'Me dê uma dica rápida de como organizar o fluxo de caixa de uma empresa.',
          caminho: 'Modelo sozinho. Não usa a tabela de recebíveis.',
        },
      ],
    },
  ];

  mensagem = '';
  perguntasVisiveis = false;
  cheque = true;
  boleto = false;
  modo: 'melhores' | 'ate-valor' = 'melhores';
  valor: number | null = null;
  private pendente = 'Respondendo…';
  private sessaoId: string | null = null;

  /**
   * Ao abrir, busca as sessões do usuário e reabre a que estava no navegador.
   * Se essa sessão não existir mais, abre a mais recente. Se não houver nenhuma, cria uma.
   */
  ngOnInit(): void {
    this.chat.info().subscribe({
      next: (info) => this.modelName.set(info.modelName),
      error: () => this.modelName.set('backend parado'),
    });
    this.carregarSessoes(true);
  }

  /** Sessão vazia só aparece se for a que está aberta. As outras mostram data e a primeira frase. */
  sessoesVisiveis(): SessaoOpcao[] {
    const atual = this.sessaoAtual();
    return this.sessoes().filter((sessao) => !sessao.vazia || sessao.sessaoId === atual);
  }

  /**
   * Segunda decisão, ainda na tela, antes do modelo.
   * 1. "quero antecipar" manda modo abertura. O formulário abre aqui. O Java só grava.
   * 2. Frase de manual manda modo produto.
   * 3. O resto manda modo geral. Se a frase falar de recebível, o Java troca para a ferramenta.
   */
  enviar(): void {
    const texto = this.mensagem.trim();
    if (!texto || this.sending() || !this.sessaoId) {
      return;
    }
    this.mensagem = '';
    this.error.set(null);
    this.messages.update((list) => [...list, { role: 'user', text: texto }]);

    if (this.querAntecipar(texto)) {
      this.perguntasVisiveis = true;
      this.pendente = 'Salvando a conversa…';
      this.sending.set(true);
      this.chat.send(this.comSessao({ message: texto, modo: 'abertura' })).subscribe({
        next: (reply) => {
          this.messages.update((list) => [...list, this.balao(reply)]);
          this.sending.set(false);
          this.carregarSessoes(false);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.describe(err));
          this.sending.set(false);
        },
      });
      return;
    }

    const produto = this.falaDeProduto(texto);
    this.pendente = produto ? 'Consultando o manual…' : 'Respondendo…';
    this.sending.set(true);
    this.chat.send(this.comSessao({ message: texto, modo: produto ? 'produto' : 'geral' })).subscribe({
      next: (reply) => {
        this.messages.update((list) => [...list, this.balao(reply)]);
        this.sending.set(false);
        this.carregarSessoes(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.sending.set(false);
      },
    });
  }

  podeConsultar(): boolean {
    if ((!this.cheque && !this.boleto) || this.sending() || !this.sessaoId) {
      return false;
    }
    if (this.modo === 'melhores') {
      return true;
    }
    return this.valor != null && this.valor > 0;
  }

  /**
   * Terceiro passo do formulário, depois que a pessoa clica em Consultar.
   * Manda modo melhores ou ate-valor. O Java chama ChequeConsulta e não chama o modelo.
   */
  consultar(): void {
    if (!this.podeConsultar()) {
      return;
    }

    const pedido = this.comSessao(this.montarPedido());
    this.error.set(null);
    this.messages.update((list) => [...list, { role: 'user', text: pedido.message }]);
    this.pendente = 'Consultando os recebíveis…';
    this.sending.set(true);

    this.chat.send(pedido).subscribe({
      next: (reply) => {
        this.messages.update((list) => [...list, this.balao(reply)]);
        this.sending.set(false);
        this.carregarSessoes(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.sending.set(false);
      },
    });
  }

  /**
   * A lista já foi calculada. Este clique só troca o chat pela tela dos títulos.
   * Não chama o modelo de novo.
   */
  abrirLista(msg: ChatMessage): void {
    const itens = msg.itens ?? [];
    if (itens.length === 0) {
      return;
    }
    this.listaTela.set(itens);
  }

  fecharLista(): void {
    this.listaTela.set(null);
  }

  somaLista(): number {
    return (this.listaTela() ?? []).reduce((soma, item) => soma + Number(item.liquido), 0);
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
  }

  /**
   * Caminho da combo, separado do Enviar.
   * A tela pede o histórico ao Java e só então troca os balões. O modelo não entra.
   */
  abrirSessao(sessaoId: string): void {
    if (!sessaoId || this.sending()) {
      return;
    }
    if (sessaoId === this.sessaoId && this.messages().length > 0) {
      return;
    }
    this.pendente = 'Carregando o histórico…';
    this.sending.set(true);
    this.error.set(null);
    this.chat.historico(sessaoId).subscribe({
      next: (historico) => {
        this.aplicarHistorico(historico);
        this.sending.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.sending.set(false);
      },
    });
  }

  /** Outra sessão do mesmo usuário. A anterior continua na combo. */
  clear(): void {
    if (this.sending()) {
      return;
    }
    this.criarSessao(true);
  }

  textoPendente(): string {
    return this.pendente;
  }

  /** Coloca a frase de estudo no campo. Não envia sozinha. */
  usarExemplo(texto: string): void {
    if (this.sending()) {
      return;
    }
    this.mensagem = texto;
  }

  private querAntecipar(texto: string): boolean {
    return this.normalizar(texto).includes('quero antecipar');
  }

  private falaDeProduto(texto: string): boolean {
    const normalizado = this.normalizar(texto);
    return [
      'o que e o produto',
      'o que e a antecip',
      'o que e antecip',
      'manual',
      'beneficio',
      'capital de giro',
      'contrat',
      'cet',
      'formula',
      'regra',
      'como funciona',
      'como e calcul',
      'como calcula',
      'pessoa juridica',
      'titulos aceit',
      'titulo aceit',
      'previsibilidade',
      'produto de antecip',
    ].some((assunto) => normalizado.includes(assunto));
  }

  private normalizar(texto: string): string {
    return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
  }

  private criarSessao(limparTela = false): void {
    this.chat.novaSessao().subscribe({
      next: (sessao) => {
        this.guardarSessao(sessao);
        this.messages.set([]);
        this.listaTela.set(null);
        this.perguntasVisiveis = false;
        if (limparTela) {
          this.error.set(null);
        }
        this.carregarSessoes(false);
      },
      error: (err: HttpErrorResponse) => this.error.set(this.describe(err)),
    });
  }

  private aplicarHistorico(historico: HistoricoChat): void {
    this.guardarSessao(historico);
    this.messages.set(
      historico.mensagens.map((msg) =>
        msg.role === 'bot' ? { ...msg, itens: prepararItens(msg.itens, msg.text) } : msg,
      ),
    );
    this.listaTela.set(null);
    this.perguntasVisiveis = historico.mensagens.some(
      (msg) => msg.role === 'user' && this.querAntecipar(msg.text),
    );
  }

  private guardarSessao(sessao: SessaoChatInfo): void {
    this.sessaoId = sessao.sessaoId;
    this.sessaoAtual.set(sessao.sessaoId);
    localStorage.setItem(this.chaveSessao, sessao.sessaoId);
    this.usuarioNome.set(sessao.usuarioNome);
  }

  /**
   * Atualiza a combo. Na abertura da página, também escolhe qual sessão mostrar.
   * A mais nova vem primeiro. A sessão vazia só entra se for a atual.
   */
  private carregarSessoes(abrir: boolean): void {
    this.chat.listarSessoes().subscribe({
      next: (lista) => {
        this.sessoes.set(lista);
        if (!abrir) {
          return;
        }
        const salva = localStorage.getItem(this.chaveSessao);
        const existe = lista.some((sessao) => sessao.sessaoId === salva);
        if (salva && existe) {
          this.abrirSessao(salva);
          return;
        }
        if (lista.length > 0) {
          this.abrirSessao(lista[0].sessaoId);
          return;
        }
        this.criarSessao();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.describe(err)),
    });
  }

  /** O balão guarda o texto e, se houver lista, os títulos do botão Antecipar. */
  private balao(reply: ChatReply): ChatMessage {
    return { role: 'bot', text: reply.answer, itens: prepararItens(reply.itens, reply.answer) };
  }

  private comSessao(pedido: PedidoConsulta): PedidoConsulta {
    return { ...pedido, sessaoId: this.sessaoId ?? undefined };
  }

  private montarPedido(): PedidoConsulta {
    const modalidades: Array<'cheque' | 'boleto'> = [];
    if (this.cheque) {
      modalidades.push('cheque');
    }
    if (this.boleto) {
      modalidades.push('boleto');
    }
    const tipos = modalidades.join(' e ');
    if (this.modo === 'ate-valor') {
      const valor = Number(this.valor);
      return {
        message: `Quero antecipar ${valor} em ${tipos}. Traga os melhores até cobrir esse valor.`,
        modo: 'ate-valor',
        valor,
        modalidades,
      };
    }
    return {
      message: `Quais ${tipos} são melhores para antecipar?`,
      modo: 'melhores',
      modalidades,
    };
  }

  private describe(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'O backend Java não respondeu. Ele precisa estar em http://localhost:8081.';
    }
    const body = err.error as { message?: string } | null;
    return body?.message ?? 'A chamada para /api/chat falhou.';
  }
}
