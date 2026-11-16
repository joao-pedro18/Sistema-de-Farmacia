package tests;

import static tests.TestKit.*;

import model.Customer;
import model.CustomerType;
import model.Employee;
import model.ItemSale;
import model.Profile;
import model.Sale;
import strategy.CardPayment;
import strategy.CashPayment;
import strategy.DiscountSeniors;
import strategy.DiscountStrategy;
import strategy.LoyaltyDiscount;
import strategy.NoDiscount;
import strategy.PartnershipDiscount;
import strategy.PaymentStrategy;
import strategy.PixPayment;

/** Pessoa 2: Strategy de desconto e de pagamento. */
public class StrategyTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        discounts();
        payments();
    }

    private static final Customer CLIENT = new Customer("Cliente", "123", CustomerType.REGULAR);

    // ------------------------------------------------------------------
    private static void discounts() {
        suite("Strategy de desconto");

        test("NoDiscount: sempre 0", () -> {
            DiscountStrategy d = new NoDiscount();
            near("100", 0, d.calculate(100, CLIENT));
            near("0", 0, d.calculate(0, CLIENT));
            near("1234,56", 0, d.calculate(1234.56, CLIENT));
        });

        test("DiscountSeniors: 10% do subtotal", () -> {
            DiscountStrategy d = new DiscountSeniors();
            near("100", 10, d.calculate(100, CLIENT));
            near("59,90", 5.99, d.calculate(59.90, CLIENT));
            near("0", 0, d.calculate(0, CLIENT));
        });

        test("PartnershipDiscount (convênio): 15% do subtotal", () -> {
            DiscountStrategy d = new PartnershipDiscount();
            near("100", 15, d.calculate(100, CLIENT));
            near("80", 12, d.calculate(80, CLIENT));
            near("0", 0, d.calculate(0, CLIENT));
        });

        test("LoyaltyDiscount (fidelidade): 5% do subtotal", () -> {
            DiscountStrategy d = new LoyaltyDiscount();
            near("100", 5, d.calculate(100, CLIENT));
            near("250", 12.5, d.calculate(250, CLIENT));
            near("0", 0, d.calculate(0, CLIENT));
        });

        test("devolve o VALOR do desconto, não o total final", () -> {
            double discount = new DiscountSeniors().calculate(200, CLIENT);
            isTrue("desconto menor que o subtotal", discount < 200);
            near("valor do desconto", 20, discount);
        });

        test("ordem dos descontos: nenhum < fidelidade < idoso < convênio", () -> {
            double none = new NoDiscount().calculate(100, CLIENT);
            double loyalty = new LoyaltyDiscount().calculate(100, CLIENT);
            double senior = new DiscountSeniors().calculate(100, CLIENT);
            double insurance = new PartnershipDiscount().calculate(100, CLIENT);
            isTrue("nenhum < fidelidade", none < loyalty);
            isTrue("fidelidade < idoso", loyalty < senior);
            isTrue("idoso < convênio", senior < insurance);
        });

        test("todas as regras funcionam pela interface DiscountStrategy", () -> {
            DiscountStrategy[] all = {new NoDiscount(), new DiscountSeniors(), new PartnershipDiscount(), new LoyaltyDiscount()};
            double[] expected = {0, 10, 15, 5};
            for (int i = 0; i < all.length; i++) {
                near(all[i].getClass().getSimpleName(), expected[i], all[i].calculate(100, CLIENT));
            }
        });
    }

    // ------------------------------------------------------------------
    private static void payments() {
        suite("Strategy de pagamento");

        test("nomes das formas de pagamento", () -> {
            eq("dinheiro", "Dinheiro", new CashPayment(10).getName());
            eq("cartão", "Cartão", new CardPayment().getName());
            eq("pix", "Pix", new PixPayment().getName());
        });

        test("cartão: mostra confirmação de aprovação", () -> {
            new CardPayment().pay(100);
            contains("confirmação", out(), "aprovado");
        });

        test("pix: mostra confirmação do pix", () -> {
            new PixPayment().pay(100);
            contains("confirmação", out(), "Pix realizado");
        });

        test("dinheiro: valor exato, sem troco", () -> {
            new CashPayment(100).pay(100);
            contains("mensagem", out(), "sem nenhum troco");
        });

        test("dinheiro: valor maior mostra o troco certo", () -> {
            new CashPayment(100).pay(59.70);
            contains("troco", out(), "Troco");
            contains("valor do troco", out(), String.format("%.2f", 40.30));
        });

        test("dinheiro: valor menor é recusado e informa quanto falta", () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, "insuficiente",
                    () -> new CashPayment(50).pay(80));
            contains("quanto falta", e.getMessage(), String.format("%.2f", 30.0));
        });

        test("dinheiro: quando recusa, não imprime confirmação de pagamento", () -> {
            try {
                new CashPayment(10).pay(50);
            } catch (IllegalArgumentException ignored) {
            }
            notContains("sem confirmação", out(), "Valor total pago");
        });

        test("dinheiro: pagar 0 com 0 recebido é aceito", () -> new CashPayment(0).pay(0));

        test("dinheiro: cliente que paga o valor EXATO de 3 x R$ 1,10 (R$ 3,30) é aceito", () -> {
            // 1.10 * 3 vira 3.3000000000000003 em double; o valor digitado, 3.30, tem que cobrir a conta
            Sale sale = new Sale(CLIENT, new Employee("Ana", Profile.CLERK));
            sale.addItem(new ItemSale(regular("1", 1.10, 50, 1), 3));
            sale.setDiscountStrategy(new NoDiscount());
            sale.setPaymentStrategy(new CashPayment(3.30));
            sale.applyDiscount();
            sale.pay();
        });

        test("dinheiro: valor exato de compra com desconto de 10% (R$ 1,89) é aceito", () -> {
            // 2 x 1.05 = 2.10, menos 10% = 1.89 (em double: 1.8900000000000001)
            Sale sale = new Sale(CLIENT, new Employee("Ana", Profile.CLERK));
            sale.addItem(new ItemSale(regular("1", 1.05, 50, 1), 2));
            sale.setDiscountStrategy(new DiscountSeniors());
            sale.setPaymentStrategy(new CashPayment(1.89));
            sale.applyDiscount();
            sale.pay();
        });

        test("todas as formas de pagamento funcionam pela interface PaymentStrategy", () -> {
            PaymentStrategy[] all = {new CashPayment(100), new CardPayment(), new PixPayment()};
            for (PaymentStrategy p : all) {
                p.pay(100);
                notNull("nome de " + p.getClass().getSimpleName(), p.getName());
            }
        });
    }
}
