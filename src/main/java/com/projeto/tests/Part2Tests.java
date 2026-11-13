package tests;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.*;
import proxy.*;
import service.*;
import strategy.*;

public class Part2Tests {

    // ===== Stubs (versões provisórias da Parte 1) =====

    // Produto de mentira: só diz se exige receita
    static class StubProduct extends Product {
        private boolean controlled;

        StubProduct(String code, double price, boolean controlled) {
            super(code, "Produto " + code, price, 100, LocalDate.now().plusYears(1), 10);
            this.controlled = controlled;
        }

        public boolean requiresPrescription() {
            return controlled;
        }

        public void validateSale(int quantity, Prescription prescription) {
            // Sem regras: o foco do teste é a Parte 2
        }
    }

    // Estoque de mentira: guarda os produtos numa lista
    static class StubInventory extends Inventory {
        private List<Product> products = new ArrayList<>();

        void add(Product product) {
            products.add(product);
        }

        public Product findProduct(String code) {
            for (Product p : products) {
                if (p.getCode().equals(code)) {
                    return p;
                }
            }
            return null;
        }

        public void decreaseStock(String code, int quantity) {
            Product p = findProduct(code);
            p.setQuantity(p.getQuantity() - quantity);
        }
    }

    // ===== Testes =====

    public static void main(String[] args) {
        testDiscounts();
        testPayments();
        testProxyAndHistory();
    }

    // Mostra se o teste passou ou falhou
    private static void check(String description, boolean condition) {
        System.out.println((condition ? "[OK] " : "[FALHOU] ") + description);
    }

    // 1. Vender com cada tipo de cliente e conferir o desconto
    private static void testDiscounts() {
        System.out.println("\n--- Teste de descontos ---");

        Product product = new StubProduct("R1", 50.0, false);
        Employee cashier = new Employee("Ana", Profile.CLERK);

        // Subtotal da venda: 2 x 50 = 100
        checkDiscount("Cliente comum: sem desconto", CustomerType.REGULAR, new NoDiscount(), 0.0, product, cashier);
        checkDiscount("Cliente idoso: 10%", CustomerType.SENIOR, new DiscountSeniors(), 10.0, product, cashier);
        checkDiscount("Cliente de convênio: 15%", CustomerType.INSURANCE, new PartnershipDiscount(), 15.0, product, cashier);
        checkDiscount("Cliente fidelidade: 5%", CustomerType.LOYALTY, new LoyaltyDiscount(), 5.0, product, cashier);
    }

    private static void checkDiscount(String description, CustomerType type, DiscountStrategy strategy,
                                      double expected, Product product, Employee employee) {
        Customer customer = new Customer("Cliente", "123", type);
        Sale sale = new Sale(customer, employee);
        sale.addItem(new ItemSale(product, 2));
        sale.setDiscountStrategy(strategy);
        sale.applyDiscount();

        check(description, Math.abs(sale.getDiscount() - expected) < 0.001);
    }

    // 2. Pagar com cada forma de pagamento
    private static void testPayments() {
        System.out.println("\n--- Teste de pagamentos ---");

        PaymentStrategy cash = new CashPayment(100.0);
        cash.pay(100.0);
        check("Pagamento em dinheiro", cash.getName().equals("Dinheiro"));

        PaymentStrategy card = new CardPayment();
        card.pay(100.0);
        check("Pagamento em cartão", card.getName().equals("Cartão"));

        PaymentStrategy pix = new PixPayment();
        pix.pay(100.0);
        check("Pagamento em pix", pix.getName().equals("Pix"));
    }

    // 3 e 4. Regra do proxy e envio ao histórico
    private static void testProxyAndHistory() {
        System.out.println("\n--- Teste do proxy e do histórico ---");

        StubInventory inventory = new StubInventory();
        Product regular = new StubProduct("R1", 50.0, false);
        Product controlled = new StubProduct("C1", 50.0, true);
        inventory.add(regular);
        inventory.add(controlled);

        SalesHistory history = new SalesHistory();
        SalesService realService = new RealSalesService(inventory, history);

        Employee cashier = new Employee("Ana", Profile.CLERK);
        Employee pharmacist = new Employee("Carlos", Profile.PHARMACEUTICAL);

        ProxySalesService cashierProxy = new ProxySalesService(realService, cashier);
        ProxySalesService pharmacistProxy = new ProxySalesService(realService, pharmacist);

        // Balconista vendendo controlado: deve ser negado
        boolean denied = false;
        try {
            cashierProxy.finalizeSale(buildSale(controlled, cashier));
        } catch (Exception e) {
            denied = true;
            System.out.println("Mensagem do proxy: " + e.getMessage());
        }
        check("Balconista vendendo controlado: negado", denied);
        check("Tentativa negada foi registrada", cashierProxy.getDeniedAttempts().size() == 1);

        // Farmacêutico vendendo controlado: deve ser liberado
        boolean allowed = true;
        try {
            pharmacistProxy.finalizeSale(buildSale(controlled, pharmacist));
        } catch (Exception e) {
            allowed = false;
            System.out.println("Erro inesperado: " + e.getMessage());
        }
        check("Farmacêutico vendendo controlado: liberado", allowed);

        // Balconista vendendo produto comum: deve ser liberado
        boolean regularAllowed = true;
        try {
            cashierProxy.finalizeSale(buildSale(regular, cashier));
        } catch (Exception e) {
            regularAllowed = false;
            System.out.println("Erro inesperado: " + e.getMessage());
        }
        check("Balconista vendendo produto comum: liberado", regularAllowed);

        // Só as duas vendas liberadas devem estar no histórico
        check("Vendas liberadas foram enviadas ao histórico", history.getSales().size() == 2);
    }

    // Monta uma venda simples de 1 unidade, com receita, desconto e pagamento
    private static Sale buildSale(Product product, Employee employee) {
        Customer customer = new Customer("Cliente", "123", CustomerType.REGULAR);
        Sale sale = new Sale(customer, employee);
        sale.addItem(new ItemSale(product, 1));
        sale.setPrescription(new Prescription("001", "Dr. Silva"));
        sale.setDiscountStrategy(new NoDiscount());
        sale.setPaymentStrategy(new CardPayment());
        return sale;
    }
}