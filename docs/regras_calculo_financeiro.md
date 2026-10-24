# Regras Técnicas de Cálculo Financeiro - Antecipação

## 1. Premissa Fundamental
Para evitar erros matemáticos ("alucinações" de IA), nenhum valor financeiro ou taxa é calculado ou inventado pelo modelo de IA. Todo o cálculo numérico é executado rigorosamente pelo backend em Java.

## 2. Fórmulas de Desconto
O custo da operação de antecipação de um título (cheque ou boleto) é determinado pelas seguintes fórmulas:
- Dias até o Vencimento (D) = Data de Vencimento - Data da Operação.
- Custo de Desconto (CD) = Valor de Face * (Taxa Diária * D).
- Valor Líquido Creditado (VL) = Valor de Face - CD.

## 3. Critério de Recomendação e Priorização
Ao sugerir quais títulos devem ser antecipados primeiro:
1. O sistema ordena os recebíveis do menor custo de desconto para o maior.
2. Dá-se preferência a títulos com menor prazo e taxas mais atrativas para otimizar o Custo Efetivo Total (CET).
