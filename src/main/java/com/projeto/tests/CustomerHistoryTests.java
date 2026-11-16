package tests;

import static tests.TestKit.*;

import java.util.List;

import model.Customer;
import model.CustomerType;
import model.Employee;
import model.ItemSale;
import model.Profile;
import model.Sale;
import service.CustomerService;
import service.SalesHistory;
import strategy.CardPayment;
import strategy.NoDiscount;
import strategy.PixPayment;

/** Pessoa 1: CustomerService e SalesHistory. */
public class CustomerHistoryTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        customerService();
        salesHistory();
    }

    // ------------------------------------------------------------------
    private static void customerService() {
        suite("CustomerService");

        test("começa sem clientes", () -> eq("tamanho", 0, new CustomerService().listCustomers().size()));

        test("registerCustomer devolve o cliente com os dados certos", () -> {
            Customer c = new CustomerService().registerCustomer("Maria", "123", CustomerType.SENIOR);
            eq("name", "Maria", c.getName());
            eq("cpf", "123", c.getCpf());
            eq("type", CustomerType.SENIOR, c.getType());
        });

        test("aceita os 4 tipos de cliente", () -> {
            CustomerService s = new CustomerService();
            int i = 0;
            for (CustomerType t : CustomerType.values()) {
                Customer c = s.registerCustomer("Cliente " + t, "cpf" + (i++), t);
                eq("tipo " + t, t, c.getType());
            }
            eq("total", 4, s.listCustomers().size());
        });

        test("findByCpf encontra o cliente cadastrado", () -> {
            CustomerService s = new CustomerService();
            Customer c = s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            same("mesma instância", c, s.findByCpf("123"));
        });

        test("findByCpf devolve null se não existir", () -> isNull("busca", new CustomerService().findByCpf("999")));

        test("CPF repetido é recusado e não duplica", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            assertThrows(IllegalArgumentException.class, "já cadastrado",
                    () -> s.registerCustomer("Outra Maria", "123", CustomerType.SENIOR));
            eq("tamanho", 1, s.listCustomers().size());
            eq("o original continua", "Maria", s.findByCpf("123").getName());
        });

        test("mesmo nome com CPF diferente é permitido", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            s.registerCustomer("Maria", "456", CustomerType.REGULAR);
            eq("tamanho", 2, s.listCustomers().size());
        });

        test("nome nulo ou em branco é recusado", () -> {
            CustomerService s = new CustomerService();
            assertThrows(IllegalArgumentException.class, "nome", () -> s.registerCustomer(null, "1", CustomerType.REGULAR));
            assertThrows(IllegalArgumentException.class, "nome", () -> s.registerCustomer("", "1", CustomerType.REGULAR));
            assertThrows(IllegalArgumentException.class, "nome", () -> s.registerCustomer("   ", "1", CustomerType.REGULAR));
            eq("nada cadastrado", 0, s.listCustomers().size());
        });

        test("CPF nulo ou em branco é recusado", () -> {
            CustomerService s = new CustomerService();
            assertThrows(IllegalArgumentException.class, "cpf", () -> s.registerCustomer("Ana", null, CustomerType.REGULAR));
            assertThrows(IllegalArgumentException.class, "cpf", () -> s.registerCustomer("Ana", "", CustomerType.REGULAR));
            assertThrows(IllegalArgumentException.class, "cpf", () -> s.registerCustomer("Ana", "  ", CustomerType.REGULAR));
            eq("nada cadastrado", 0, s.listCustomers().size());
        });

        test("tipo nulo é recusado", () -> {
            CustomerService s = new CustomerService();
            assertThrows(IllegalArgumentException.class, "tipo", () -> s.registerCustomer("Ana", "1", null));
            eq("nada cadastrado", 0, s.listCustomers().size());
        });

        test("removeCustomer: true se removeu, false se não existe", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            isTrue("primeira vez", s.removeCustomer("123"));
            isFalse("segunda vez", s.removeCustomer("123"));
            isNull("não está mais", s.findByCpf("123"));
        });

        test("depois de remover, o CPF pode ser cadastrado de novo", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            s.removeCustomer("123");
            Customer novo = s.registerCustomer("Maria Nova", "123", CustomerType.LOYALTY);
            eq("type", CustomerType.LOYALTY, novo.getType());
        });

        test("remover um cliente não afeta os outros", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("A", "1", CustomerType.REGULAR);
            s.registerCustomer("B", "2", CustomerType.REGULAR);
            s.removeCustomer("1");
            notNull("B continua", s.findByCpf("2"));
            eq("tamanho", 1, s.listCustomers().size());
        });

        test("listCustomers mantém a ordem e devolve uma cópia", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("A", "1", CustomerType.REGULAR);
            s.registerCustomer("B", "2", CustomerType.REGULAR);
            List<Customer> list = s.listCustomers();
            eq("1º", "A", list.get(0).getName());
            eq("2º", "B", list.get(1).getName());
            list.clear();
            eq("lista interna intacta", 2, s.listCustomers().size());
        });
    }

    // ------------------------------------------------------------------
    private static Sale sampleSale(String customerName, String employeeName, boolean pix) {
        Customer c = new Customer(customerName, "111", CustomerType.REGULAR);
        Employee e = new Employee(employeeName, Profile.CLERK);
        Sale sale = new Sale(c, e);
        sale.addItem(new ItemSale(regular("1", 10.0, 50, 1), 2));
        sale.setDiscountStrategy(new NoDiscount());
        sale.setPaymentStrategy(pix ? new PixPayment() : new CardPayment());
        sale.applyDiscount();
        return sale;
    }

    private static void salesHistory() {
        suite("SalesHistory");

        test("começa vazio", () -> eq("tamanho", 0, new SalesHistory().getSales().size()));

        test("addSale registra a venda", () -> {
            SalesHistory h = new SalesHistory();
            Sale s = sampleSale("Maria", "Ana", false);
            h.addSale(s);
            eq("tamanho", 1, h.getSales().size());
            same("mesma venda", s, h.getSales().get(0));
        });

        test("mantém a ordem das vendas", () -> {
            SalesHistory h = new SalesHistory();
            Sale a = sampleSale("A", "Ana", false);
            Sale b = sampleSale("B", "Ana", false);
            h.addSale(a);
            h.addSale(b);
            same("1ª", a, h.getSales().get(0));
            same("2ª", b, h.getSales().get(1));
        });

        test("addSale(null) é recusado e não registra nada", () -> {
            SalesHistory h = new SalesHistory();
            assertThrows(IllegalArgumentException.class, "inexistente", () -> h.addSale(null));
            eq("tamanho", 0, h.getSales().size());
        });

        test("getSales devolve uma cópia", () -> {
            SalesHistory h = new SalesHistory();
            h.addSale(sampleSale("A", "Ana", false));
            h.getSales().clear();
            eq("tamanho", 1, h.getSales().size());
        });

        test("printSales sem vendas avisa que não há nenhuma", () -> {
            new SalesHistory().printSales();
            contains("mensagem", out(), "Nenhuma venda");
        });

        test("printSales mostra cliente, funcionário, pagamento, desconto e total", () -> {
            SalesHistory h = new SalesHistory();
            h.addSale(sampleSale("Maria Souza", "Carlos", true));
            h.printSales();
            contains("cliente", out(), "Maria Souza");
            contains("funcionário", out(), "Carlos");
            contains("pagamento", out(), "Pix");
            contains("desconto", out(), "Desconto");
            contains("total", out(), "20");
        });

        test("printSales lista todas as vendas", () -> {
            SalesHistory h = new SalesHistory();
            h.addSale(sampleSale("Cliente Um", "Ana", false));
            h.addSale(sampleSale("Cliente Dois", "Ana", true));
            h.printSales();
            contains("1ª venda", out(), "Cliente Um");
            contains("2ª venda", out(), "Cliente Dois");
        });
    }
}
