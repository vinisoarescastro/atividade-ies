# Relatório Técnico: Refatoração do SaaS Billing Co.

**Disciplina:** Implantação e Entrega de Software (2026/2)
**Professora:** Sofia Costa Paiva
**Aluno:** Vinicius Soares Castro (matrícula 202206163)

## 1. Diagnóstico do débito técnico

As linhas se referem ao código legado original (commit 1).

| # | Linha / trecho | Code Smell ou princípio violado | Impacto ("juros") |
|---|---|---|---|
| 1 | 18 a 105 (`processMonthlyBilling`) | Long Method e violação do SRP | Um único método calcula preço, acessa o banco, cobra o cartão e envia e-mail. Qualquer mudança arrisca quebrar outra parte e nada pode ser testado isoladamente. |
| 2 | 18 a 20 (8 parâmetros) | Long Parameter List e Primitive Obsession | Vários `String` seguidos podem ser trocados de posição sem erro de compilação. O parâmetro `planType` nem é usado. |
| 3 | 29 a 57 e 85 (`double`) | Primitive Obsession em valores monetários | `double` gera erros de arredondamento em centavos, o que causa divergência entre fatura, banco e e-mail. |
| 4 | 14 a 16 (`DB_URL`, `DB_USER`, `DB_PASS`) | Credenciais fixas no código | A senha fica exposta no repositório e o mesmo build não pode ser usado em ambientes diferentes. |
| 5 | 25 (`LocalDate.now()`) | Dependência do relógio do sistema | Os testes mudam de resultado conforme a data (por exemplo, o cupom BLACKFRIDAY só vale em novembro). |
| 6 | 59 a 95 (JDBC e `SimpleEmail`) | Dependência direta de I/O (violação do DIP) | Não é possível rodar testes sem PostgreSQL e servidor SMTP, o que impede a CI de funcionar sem infraestrutura. |
| 7 | 100 a 102 (`catch (Exception e)`) | Erro mascarado | Falhas de banco viram apenas uma linha de log e ninguém é avisado do problema. |
| 8 | 96 (`// emailSender.send();`) | Código morto | O log diz que o e-mail foi enviado, mas ele nunca é enviado. |

## 2. Refatorações aplicadas

1. **Money** (record imutável com `BigDecimal`) substituiu o `double`.
2. **BillingCommand** agrupou os parâmetros do método. O `planType`, que não era usado, foi removido.
3. **Interfaces** `CustomerRepository`, `InvoiceRepository`, `PaymentGateway` e `NotificationService` separaram o serviço da infraestrutura. As implementações reais ficam no pacote `adapter`.
4. **Clock** injetado no lugar de `LocalDate.now()`.
5. **PricingCalculator** concentra as regras de preço em uma classe pura, sem banco nem rede.
6. **Tratamento de erros:** o `catch (Exception e)` foi removido. Falhas de banco agora geram exceção. Falha no envio de e-mail é registrada no log sem cancelar a cobrança já feita.
7. **Credenciais** passaram a ser lidas de variáveis de ambiente.

## 3. Métricas antes e depois

| Métrica | Antes | Depois |
|---|---|---|
| Linhas do método principal | 88 | 11 |
| Complexidade ciclomática do método principal | 18 | 3 |
| Parâmetros do método principal | 8 | 1 |
| Classes de produção | 1 | 18 |
| `catch (Exception e)` genéricos | 1 | 0 |
| Credenciais no código | 3 | 0 |
| Testes automatizados | 0 | 37 |
| Infraestrutura necessária para os testes | PostgreSQL e SMTP | Nenhuma |

Para garantir que nenhuma regra de negócio mudou, o teste `LegacyPricingParityTest` compara o cálculo novo com o cálculo original em 4.480 combinações de entrada. Todas dão o mesmo resultado.

## 4. Como executar os testes

```bash
mvn test
```

É necessário Java 17 ou superior.
