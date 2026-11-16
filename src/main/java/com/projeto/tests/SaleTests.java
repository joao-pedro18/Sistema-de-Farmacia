package tests;

import static tests.TestKit.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import model.Customer;
import model.CustomerType;
import model.Employee;
import model.ItemSale;
import model.Prescription;
import model.Product;
import model.Profile;
import model.Sale;
import service.EmployeeService;
import strategy.CardPayment;
import strategy.DiscountSeniors;
import strategy.NoDiscount;
import strategy.PartnershipDiscount;
import strategy.PaymentStrategy;
import strategy.PixPayment;

/** Pessoa 2: ItemSale, Sale (contexto do Strategy), Employee e EmployeeService. */
public class SaleTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        itemSale();
        sale();
        employees();
    }

    private static Customer customer() {
        return new Customer("Maria", "123", CustomerType.REGULAR);
    }

    private static Employee clerk() {
        return new Employee("Ana", Profile.CLERK);
    }

    // ------------------------------------------------------------------
    private static void itemSale() {
        suite("ItemSale (item da venda)");

        test("guarda produto e quantidade", () -> {
            Product p = regular("1", 8.5, 20, 2);
            ItemSale item = new ItemSale(p, 3);
            same("product", p, item.getProduct());
            eq("amount", 3, item.getAmount());
        });

        test("subtotal = preço x quantidade", () -> near("subtotal", 25.5, new ItemSale(regular("1", 8.5, 20, 2), 3).getSubtotal()));

        test("subtotal de 1 unidade é o preço", () -> near("subtotal", 8.5, new ItemSale(regular("1", 8.5, 20, 2), 1).getSubtotal()));

        test("subtotal acompanha mudança de quantidade", () -> {
            ItemSale item = new ItemSale(regular("1", 10, 20, 2), 1);
            item.setAmount(4);
            near("subtotal", 40, item.getSubtotal());
        });

        test("subtotal acompanha troca de produto", () -> {
            ItemSale item = new ItemSale(regular("1", 10, 20, 2), 2);
            item.setProduct(regular("2", 3, 20, 2));
            near("subtotal", 6, item.getSubtotal());
        });
    }

    // ------------------------------------------------------------------
    private static void sale() {
        suite("Sale (venda)");

        test("nova venda: cliente e funcionário guardados, carrinho vazio, sem desconto", () -> {
            Customer c = customer();
            Employee e = clerk();
            Sale s = new Sale(c, e);
            same("customer", c, s.getCustomer());
            same("employee", e, s.getEmployee());
            eq("itens", 0, s.getItems().size());
            near("desconto", 0, s.getDiscount());
            near("subtotal", 0, s.calculateSubtotal());
            near("total", 0, s.getTotal());
            isNull("receita", s.getPrescription());
        });

        test("a data da venda é a de agora", () -> {
            LocalDateTime before = LocalDateTime.now();
            Sale s = new Sale(customer(), clerk());
            LocalDateTime after = LocalDateTime.now();
            notNull("date", s.getDate());
            isFalse("não pode ser antes de criar", s.getDate().isBefore(before.truncatedTo(ChronoUnit.MILLIS)));
            isFalse("não pode ser depois de criar", s.getDate().isAfter(after));
        });

        test("addItem coloca no carrinho", () -> {
            Sale s = new Sale(customer(), clerk());
            ItemSale item = new ItemSale(regular("1", 10, 20, 2), 1);
            s.addItem(item);
            eq("itens", 1, s.getItems().size());
            same("item", item, s.getItems().get(0));
        });

        test("calculateSubtotal soma todos os itens", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 10, 20, 2), 2)); // 20
            s.addItem(new ItemSale(regular("2", 7.5, 20, 2), 4)); // 30
            s.addItem(new ItemSale(regular("3", 1.25, 20, 2), 8)); // 10
            near("subtotal", 60, s.calculateSubtotal());
        });

        test("removeItem remove pelo código e atualiza o subtotal", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 10, 20, 2), 2));
            s.addItem(new ItemSale(regular("2", 5, 20, 2), 2));
            isTrue("removeu", s.removeItem("1"));
            eq("itens", 1, s.getItems().size());
            eq("sobrou o certo", "2", s.getItems().get(0).getProduct().getCode());
            near("subtotal", 10, s.calculateSubtotal());
        });

        test("removeItem de código que não está no carrinho devolve false", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 10, 20, 2), 2));
            isFalse("removeu", s.removeItem("999"));
            eq("itens", 1, s.getItems().size());
        });

        test("removeItem em carrinho vazio devolve false", () -> isFalse("removeu", new Sale(customer(), clerk()).removeItem("1")));

        test("removeItem do último item deixa o carrinho vazio", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 10, 20, 2), 2));
            isTrue("removeu", s.removeItem("1"));
            eq("itens", 0, s.getItems().size());
            near("subtotal", 0, s.calculateSubtotal());
        });

        test("applyDiscount guarda o desconto da estratégia escolhida", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2)); // 100
            s.setDiscountStrategy(new DiscountSeniors());
            s.applyDiscount();
            near("desconto", 10, s.getDiscount());
        });

        test("getTotal = subtotal - desconto", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2)); // 100
            s.setDiscountStrategy(new PartnershipDiscount());
            s.applyDiscount();
            near("total", 85, s.getTotal());
        });

        test("antes de applyDiscount, o total é o subtotal", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2));
            s.setDiscountStrategy(new DiscountSeniors());
            near("total", 100, s.getTotal());
        });

        test("trocar a estratégia muda o desconto (Strategy)", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2));
            s.setDiscountStrategy(new NoDiscount());
            s.applyDiscount();
            near("sem desconto", 0, s.getDiscount());
            s.setDiscountStrategy(new DiscountSeniors());
            s.applyDiscount();
            near("com desconto de idoso", 10, s.getDiscount());
        });

        test("applyDiscount de novo recalcula (não acumula)", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2));
            s.setDiscountStrategy(new DiscountSeniors());
            s.applyDiscount();
            s.applyDiscount();
            near("desconto", 10, s.getDiscount());
        });

        test("o desconto acompanha itens adicionados antes de applyDiscount", () -> {
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 1));
            s.addItem(new ItemSale(regular("2", 50, 20, 2), 1));
            s.setDiscountStrategy(new DiscountSeniors());
            s.applyDiscount();
            near("desconto", 10, s.getDiscount());
            near("total", 90, s.getTotal());
        });

        test("pay() cobra o TOTAL (já com desconto) na forma de pagamento escolhida", () -> {
            double[] charged = {-1};
            PaymentStrategy recorder = new PaymentStrategy() {
                @Override
                public void pay(double amount) {
                    charged[0] = amount;
                }

                @Override
                public String getName() {
                    return "Teste";
                }
            };
            Sale s = new Sale(customer(), clerk());
            s.addItem(new ItemSale(regular("1", 50, 20, 2), 2)); // 100
            s.setDiscountStrategy(new DiscountSeniors());
            s.setPaymentStrategy(recorder);
            s.applyDiscount();
            s.pay();
            near("valor cobrado", 90, charged[0]);
        });

        test("getPaymentMethod devolve o nome da forma de pagamento", () -> {
            Sale s = new Sale(customer(), clerk());
            s.setPaymentStrategy(new PixPayment());
            eq("pix", "Pix", s.getPaymentMethod());
            s.setPaymentStrategy(new CardPayment());
            eq("cartão", "Cartão", s.getPaymentMethod());
        });

        test("receita: guarda e devolve", () -> {
            Sale s = new Sale(customer(), clerk());
            Prescription rx = new Prescription("1", "Dr. A");
            s.setPrescription(rx);
            same("prescription", rx, s.getPrescription());
        });
    }

    // ------------------------------------------------------------------
    private static void employees() {
        suite("Employee, Profile e EmployeeService");

        test("Employee guarda nome e perfil e permite alterar", () -> {
            Employee e = new Employee("Ana", Profile.CLERK);
            eq("name", "Ana", e.getName());
            eq("profile", Profile.CLERK, e.getProfile());
            e.setName("Ana Maria");
            e.setProfile(Profile.PHARMACEUTICAL);
            eq("novo nome", "Ana Maria", e.getName());
            eq("novo perfil", Profile.PHARMACEUTICAL, e.getProfile());
        });

        test("Profile tem CLERK e PHARMACEUTICAL", () -> {
            eq("quantidade", 2, Profile.values().length);
            notNull("CLERK", Profile.valueOf("CLERK"));
            notNull("PHARMACEUTICAL", Profile.valueOf("PHARMACEUTICAL"));
        });

        test("começa sem funcionários", () -> eq("tamanho", 0, new EmployeeService().listEmployees().size()));

        test("registerEmployee + login devolve o funcionário com o perfil certo", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            s.registerEmployee("Carlos", Profile.PHARMACEUTICAL);
            Employee ana = s.login("Ana");
            Employee carlos = s.login("Carlos");
            notNull("Ana", ana);
            notNull("Carlos", carlos);
            eq("perfil da Ana", Profile.CLERK, ana.getProfile());
            eq("perfil do Carlos", Profile.PHARMACEUTICAL, carlos.getProfile());
        });

        test("login de nome inexistente devolve null", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            isNull("login", s.login("Fulano"));
        });

        test("login sem nenhum funcionário cadastrado devolve null", () -> isNull("login", new EmployeeService().login("Ana")));

        test("listEmployees lista todos", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            s.registerEmployee("Carlos", Profile.PHARMACEUTICAL);
            eq("tamanho", 2, s.listEmployees().size());
        });
    }
}
