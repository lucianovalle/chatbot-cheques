import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatMessage, PedidoConsulta } from './chat.models';
import { ChatService } from './chat.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private readonly chat = inject(ChatService);

  readonly messages = signal<ChatMessage[]>([]);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly modelName = signal('...');

  mensagem = '';
  perguntasVisiveis = false;
  cheque = true;
  boleto = false;
  modo: 'melhores' | 'ate-valor' = 'melhores';
  valor: number | null = null;
  private pendente = 'Respondendo…';

  ngOnInit(): void {
    this.chat.info().subscribe({
      next: (info) => this.modelName.set(info.modelName),
      error: () => this.modelName.set('backend parado'),
    });
  }

  /**
   * Primeira separação, ainda no navegador:
   * "quero antecipar" abre o formulário e não chama o backend.
   * Assunto de manual vai com modo "produto". O resto vai com modo "geral".
   * O Java confirma essa escolha de novo.
   */
  enviar(): void {
    const texto = this.mensagem.trim();
    if (!texto || this.sending()) {
      return;
    }
    this.mensagem = '';
    this.error.set(null);
    this.messages.update((list) => [...list, { role: 'user', text: texto }]);

    if (this.querAntecipar(texto)) {
      this.perguntasVisiveis = true;
      this.messages.update((list) => [
        ...list,
        {
          role: 'bot',
          text: 'Para antecipar, marque cheque, boleto ou os dois e escolha se quer ver os melhores ou cobrir um valor.',
        },
      ]);
      return;
    }

    const produto = this.falaDeProduto(texto);
    this.pendente = produto ? 'Consultando o manual…' : 'Respondendo…';
    this.sending.set(true);
    this.chat.send({ message: texto, modo: produto ? 'produto' : 'geral' }).subscribe({
      next: (reply) => {
        this.messages.update((list) => [...list, { role: 'bot', text: reply.answer }]);
        this.sending.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.sending.set(false);
      },
    });
  }

  podeConsultar(): boolean {
    if ((!this.cheque && !this.boleto) || this.sending()) {
      return false;
    }
    if (this.modo === 'melhores') {
      return true;
    }
    return this.valor != null && this.valor > 0;
  }

  /** Envia cheque, boleto e valor. O backend calcula sem chamar o modelo. */
  consultar(): void {
    if (!this.podeConsultar()) {
      return;
    }

    const pedido = this.montarPedido();
    this.error.set(null);
    this.messages.update((list) => [...list, { role: 'user', text: pedido.message }]);
    this.pendente = 'Consultando os recebíveis…';
    this.sending.set(true);

    this.chat.send(pedido).subscribe({
      next: (reply) => {
        this.messages.update((list) => [...list, { role: 'bot', text: reply.answer }]);
        this.sending.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.sending.set(false);
      },
    });
  }

  clear(): void {
    this.chat.reset().subscribe({
      next: () => {
        this.messages.set([]);
        this.perguntasVisiveis = false;
        this.error.set(null);
      },
      error: (err: HttpErrorResponse) => this.error.set(this.describe(err)),
    });
  }

  textoPendente(): string {
    return this.pendente;
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
      'fluxo de caixa',
      'previsibilidade',
      'produto de antecip',
    ].some((assunto) => normalizado.includes(assunto));
  }

  private normalizar(texto: string): string {
    return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
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
