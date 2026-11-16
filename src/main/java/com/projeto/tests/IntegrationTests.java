package tests;

import static tests.TestKit.*;

import model.Customer;
import model.CustomerType;
import model.Employee;
import model.ItemSale;
import model.Prescription;
import model.Product;
import model.ProductType;
import model.Profile;
import model.Sale;
import observer.ExpirationAlert;
import observer.LowStockAlert;
import observer.RestockOrder;
import proxy.ProxySalesService;
import proxy.RealSalesService;
import proxy.SalesService;
import service.CustomerService;
import service.EmployeeService;
import service.Inventory;
import service.ProductService;
import service.SalesHistory;
import strategy.CardPayment;
import strategy.CashPayment;
import strategy.DiscountSeniors;
import strategy.DiscountStrategy;
import strategy.LoyaltyDiscount;
import strategy.NoDiscount;
import strategy.PartnershipDiscount;
import strategy.PaymentStrategy;

/**
 * Juntos: monta o sistema do mesmo jeito que a classe principal (Main/Client) vai montar
 * e testa fluxos completos entre a Parte 1 e a Parte 2.
 */
public class IntegrationTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        suite("Integração (Parte 1 + Parte 2 funcionando juntas)");

        test("fluxo completo: login, cadastro, venda com controlado, estoque, histórico e impressão", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 50, inDays(300), 5);
            app.products.registerProduct(ProductType.CONTROLLED, "C1", "Clonazepam", 40.0, 10, inDays(300), 2);
            app.customers.registerCustomer("Seu José", "222", CustomerType.SENIOR);
            app.employees.registerEmployee("Carlos", Profile.PHARMACEUTICAL);

            Employee logged = app.employees.login("Carlos");
            Customer client = app.customers.findByCpf("222");

            Sale sale = new Sale(client, logged);
            sale.addItem(new ItemSale(app.inventory.findProduct("R1"), 2));   // 20
            sale.addItem(new ItemSale(app.inventory.findProduct("C1"), 1));   // 40
            sale.setPrescription(new Prescription("001", "Dr. Silva"));
            sale.setDiscountStrategy(new DiscountSeniors());                  // -10% de 60
            sale.setPaymentStrategy(new CardPayment());

            app.proxyFor(logged).finalizeSale(sale);

            near("subtotal", 60, sale.calculateSubtotal());
            near("desconto", 6, sale.getDiscount());
            near("total", 54, sale.getTotal());
            eq("estoque do comum", 48, app.inventory.findProduct("R1").getQuantity());
            eq("estoque do controlado", 9, app.inventory.findProduct("C1").getQuantity());
            eq("histórico", 1, app.history.getSales().size());

            clearOut();
            app.history.printSales();
            contains("cliente", out(), "Seu José");
            contains("funcionário", out(), "Carlos");
            contains("pagamento", out(), "Cartão");
        });

        test("os 4 tipos de cliente recebem o desconto certo ponta a ponta (subtotal 200)", () -> {
            Object[][] cases = {
                    {CustomerType.REGULAR, new NoDiscount(), 0.0},
                    {CustomerType.SENIOR, new DiscountSeniors(), 20.0},
                    {CustomerType.INSURANCE, new PartnershipDiscount(), 30.0},
                    {CustomerType.LOYALTY, new LoyaltyDiscount(), 10.0},
            };
            for (Object[] c : cases) {
                App app = new App();
                app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 100, inDays(300), 5);
                Customer client = app.customers.registerCustomer("Cliente", "1", (CustomerType) c[0]);
                Employee clerk = new Employee("Ana", Profile.CLERK);
                Sale sale = new Sale(client, clerk);
                sale.addItem(new ItemSale(app.inventory.findProduct("R1"), 20));
                sale.setDiscountStrategy((DiscountStrategy) c[1]);
                sale.setPaymentStrategy(new CardPayment());
                app.proxyFor(clerk).finalizeSale(sale);
                near("desconto " + c[0], (Double) c[2], sale.getDiscount());
                near("total " + c[0], 200 - (Double) c[2], sale.getTotal());
            }
        });

        test("venda que deixa o estoque abaixo do mínimo dispara alerta e pedido de reposição", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 10, inDays(300), 5);
            Employee clerk = new Employee("Ana", Profile.CLERK);

            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 3)); // 10 -> 7 (acima do mínimo)
            notContains("ainda sem alerta", out(), "estoque baixo");
            eq("sem pedido", 0, app.restock.getOrders().size());

            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 3)); // 7 -> 4 (abaixo)
            contains("alerta", out(), "estoque baixo");
            eq("pedido aberto", 1, app.restock.getOrders().size());
            eq("produto do pedido", "R1", app.restock.getOrders().get(0).getCode());
        });

        test("depois do pedido, a entrada de mercadoria fecha o pedido de reposição", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 10, inDays(300), 5);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 8));
            eq("pedido aberto", 1, app.restock.getOrders().size());
            app.inventory.increaseStock("R1", 30);
            eq("pedido fechado", 0, app.restock.getOrders().size());
        });

        test("vender produto perto de vencer é permitido e dispara o alerta de validade", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Xarope", 10.0, 10, inDays(15), 1);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 1));
            contains("alerta", out(), "perto de vencer");
            eq("histórico", 1, app.history.getSales().size());
        });

        test("balconista tentando controlado: negado, nada muda e nenhum alerta dispara", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.CONTROLLED, "C1", "Clonazepam", 40.0, 3, inDays(300), 2);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            ProxySalesService proxy = app.proxyFor(clerk);
            Sale sale = app.simpleSale(clerk, "C1", 1);
            sale.setPrescription(new Prescription("001", "Dr. Silva"));
            assertThrows(RuntimeException.class, "farmac", () -> proxy.finalizeSale(sale));
            eq("estoque", 3, app.inventory.findProduct("C1").getQuantity());
            eq("histórico", 0, app.history.getSales().size());
            eq("tentativas negadas", 1, proxy.getDeniedAttempts().size());
            eq("nenhum alerta", "", out().trim());
            eq("nenhum pedido", 0, app.restock.getOrders().size());
        });

        test("dinheiro insuficiente: venda cancelada, estoque e alertas intactos", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 6, inDays(300), 5);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            Sale sale = app.simpleSale(clerk, "R1", 3); // total 30, abaixo do mínimo se vendesse
            sale.setPaymentStrategy(new CashPayment(10));
            assertThrows(IllegalArgumentException.class, "insuficiente", () -> app.proxyFor(clerk).finalizeSale(sale));
            eq("estoque", 6, app.inventory.findProduct("R1").getQuantity());
            eq("histórico", 0, app.history.getSales().size());
            notContains("sem alerta", out(), "estoque baixo");
        });

        test("vendas seguidas acumulam estoque e histórico; quando acaba, a próxima é recusada", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 10, inDays(300), 1);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 4));
            app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 6));
            eq("estoque zerado", 0, app.inventory.findProduct("R1").getQuantity());
            eq("histórico", 2, app.history.getSales().size());
            assertThrows(IllegalArgumentException.class, "estoque",
                    () -> app.proxyFor(clerk).finalizeSale(app.simpleSale(clerk, "R1", 1)));
            eq("histórico continua", 2, app.history.getSales().size());
        });

        test("login define o acesso: o mesmo carrinho de controlado nega para balconista e libera para farmacêutico", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.CONTROLLED, "C1", "Clonazepam", 40.0, 10, inDays(300), 2);
            app.employees.registerEmployee("Ana", Profile.CLERK);
            app.employees.registerEmployee("Carlos", Profile.PHARMACEUTICAL);

            Employee ana = app.employees.login("Ana");
            Sale deniedSale = app.simpleSale(ana, "C1", 1);
            deniedSale.setPrescription(new Prescription("1", "Dr. A"));
            assertThrows(RuntimeException.class, "farmac", () -> app.proxyFor(ana).finalizeSale(deniedSale));

            Employee carlos = app.employees.login("Carlos");
            Sale okSale = app.simpleSale(carlos, "C1", 1);
            okSale.setPrescription(new Prescription("1", "Dr. A"));
            app.proxyFor(carlos).finalizeSale(okSale);

            eq("histórico", 1, app.history.getSales().size());
            eq("quem vendeu", "Carlos", app.history.getSales().get(0).getEmployee().getName());
        });

        test("remover o produto ou o cliente depois da venda não apaga o histórico", () -> {
            App app = new App();
            app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 10, inDays(300), 1);
            app.customers.registerCustomer("Maria", "111", CustomerType.REGULAR);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            Sale sale = new Sale(app.customers.findByCpf("111"), clerk);
            sale.addItem(new ItemSale(app.inventory.findProduct("R1"), 2));
            sale.setDiscountStrategy(new NoDiscount());
            sale.setPaymentStrategy(new CardPayment());
            app.proxyFor(clerk).finalizeSale(sale);

            app.products.removeProduct("R1");
            app.customers.removeCustomer("111");

            eq("histórico", 1, app.history.getSales().size());
            clearOut();
            app.history.printSales();
            contains("cliente ainda aparece", out(), "Maria");
        });

        test("produto removido do estoque não pode mais ser vendido", () -> {
            App app = new App();
            Product p = app.products.registerProduct(ProductType.REGULAR, "R1", "Dipirona", 10.0, 10, inDays(300), 1);
            Employee clerk = new Employee("Ana", Profile.CLERK);
            Sale sale = app.simpleSale(clerk, "R1", 1);
            app.products.removeProduct("R1");
            // o proxy valida o produto em memória, mas o estoque real já não o conhece
            assertThrows(IllegalArgumentException.class, "não encontrado", () -> app.proxyFor(clerk).finalizeSale(sale));
            eq("histórico", 0, app.history.getSales().size());
            eq("objeto do produto não foi alterado", 10, p.getQuantity());
        });
    }

    /** Monta o sistema como o Main fará: estoque + 3 observadores + serviços + histórico. */
    private static class App {
        final Inventory inventory = new Inventory();
        final RestockOrder restock = new RestockOrder();
        final ProductService products = new ProductService(inventory);
        final CustomerService customers = new CustomerService();
        final EmployeeService employees = new EmployeeService();
        final SalesHistory history = new SalesHistory();
        final SalesService real = new RealSalesService(inventory, history);

        App() {
            inventory.addObserver(new LowStockAlert());
            inventory.addObserver(new ExpirationAlert());
            inventory.addObserver(restock);
        }

        ProxySalesService proxyFor(Employee who) {
            return new ProxySalesService(real, who);
        }

        /** Venda de 1 produto, cliente comum, sem desconto, no cartão. */
        Sale simpleSale(Employee who, String code, int amount) {
            Sale s = new Sale(new Customer("Cliente", "000", CustomerType.REGULAR), who);
            s.addItem(new ItemSale(inventory.findProduct(code), amount));
            s.setDiscountStrategy(new NoDiscount());
            PaymentStrategy card = new CardPayment();
            s.setPaymentStrategy(card);
            return s;
        }
    }
}
