package tests;

import static tests.TestKit.*;

import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

import model.ControlledProduct;
import model.Customer;
import model.CustomerType;
import model.Employee;
import model.PerfumeProduct;
import model.Product;
import model.Profile;
import model.RegularProduct;
import model.Sale;
import observer.RestockOrder;
import proxy.ProxySalesService;
import proxy.RealSalesService;
import proxy.SalesService;
import service.CustomerService;
import service.EmployeeService;
import service.Inventory;
import service.ProductService;
import service.SalesHistory;
import ui.CustomerMenu;
import ui.InventoryMenu;
import ui.LoginMenu;
import ui.ProductMenu;
import ui.SalesMenu;

/**
 * Testa os menus do terminal simulando o teclado (System.in).
 * Regra: input(...) sempre ANTES de criar o menu, porque o menu cria o Scanner no construtor.
 */
public class MenuTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        productMenu();
        customerMenu();
        inventoryMenu();
        loginMenu();
        salesMenu();
        salesMenuRobustness();
    }

    private static String date(int days) {
        return inDays(days).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ==================================================================
    // ProductMenu
    // ==================================================================
    private static void productMenu() {
        suite("ProductMenu (menu de produtos)");

        test("cadastra produto comum (preço com vírgula) e guarda tudo certo", () -> {
            input("1", "1", "P1", "Dipirona", "8,50", "20", date(200), "5", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "Produto cadastrado com sucesso: Dipirona");
            Product p = inv.findProduct("P1");
            notNull("produto no estoque", p);
            eq("classe", RegularProduct.class, p.getClass());
            near("preço", 8.5, p.getPrice());
            eq("quantidade", 20, p.getQuantity());
            eq("mínimo", 5, minStock(p));
            eq("validade", inDays(200), p.getExpirationDate());
        });

        test("cadastra medicamento controlado (opção 2)", () -> {
            input("1", "2", "C1", "Clonazepam", "25", "10", date(200), "3", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            eq("classe", ControlledProduct.class, inv.findProduct("C1").getClass());
        });

        test("cadastra perfumaria (opção 3)", () -> {
            input("1", "3", "F1", "Hidratante", "32.90", "15", date(200), "4", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            eq("classe", PerfumeProduct.class, inv.findProduct("F1").getClass());
        });

        test("código repetido: mostra o motivo e não duplica", () -> {
            Inventory inv = new Inventory();
            ProductService service = new ProductService(inv);
            input("1", "1", "P1", "A", "5", "10", date(200), "2",
                    "1", "1", "P1", "B", "6", "11", date(200), "3", "0");
            new ProductMenu(service).show();
            contains("motivo", out(), "Produto não cadastrado");
            contains("já cadastrado", out(), "já cadastrado");
            eq("tamanho", 1, inv.listProducts().size());
            eq("o primeiro continua", "A", inv.findProduct("P1").getName());
        });

        test("código em branco: recusa", () -> {
            input("1", "1", "", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "Código não informado");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("nome em branco: recusa", () -> {
            input("1", "1", "P1", "", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "Nome não informado");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("preço zero: recusa", () -> {
            input("1", "1", "P1", "A", "0", "10", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "preço deve ser maior que zero");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("preço negativo: recusa", () -> {
            input("1", "1", "P1", "A", "-5", "10", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "preço deve ser maior que zero");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("quantidade negativa: recusa", () -> {
            input("1", "1", "P1", "A", "5", "-1", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "não podem ser negativas");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("estoque mínimo negativo: recusa", () -> {
            input("1", "1", "P1", "A", "5", "10", date(200), "-2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("mensagem", out(), "não podem ser negativas");
            eq("tamanho", 0, inv.listProducts().size());
        });

        test("quantidade zero em estoque é aceita", () -> {
            input("1", "1", "P1", "A", "5", "0", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            eq("quantidade", 0, inv.findProduct("P1").getQuantity());
        });

        test("data inexistente (31/02) pede de novo até digitar uma válida", () -> {
            input("1", "1", "P1", "A", "5", "10", "31/02/2030", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("aviso", out(), "Data inválida");
            eq("validade final", inDays(200), inv.findProduct("P1").getExpirationDate());
        });

        test("data em formato errado pede de novo", () -> {
            input("1", "1", "P1", "A", "5", "10", "2030-03-20", "abc", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("aviso", out(), "Data inválida");
            notNull("cadastrou no fim", inv.findProduct("P1"));
        });

        test("texto no lugar do preço pede de novo", () -> {
            input("1", "1", "P1", "A", "barato", "5", "10", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("aviso", out(), "Valor inválido");
            near("preço final", 5, inv.findProduct("P1").getPrice());
        });

        test("texto no lugar da quantidade pede de novo", () -> {
            input("1", "1", "P1", "A", "5", "muitos", "10", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("aviso", out(), "Valor inválido");
            eq("quantidade final", 10, inv.findProduct("P1").getQuantity());
        });

        test("tipo de produto inválido pede de novo", () -> {
            input("1", "9", "1", "P1", "A", "5", "10", date(200), "2", "0");
            Inventory inv = new Inventory();
            new ProductMenu(new ProductService(inv)).show();
            contains("aviso", out(), "Opção inválida");
            notNull("cadastrou no fim", inv.findProduct("P1"));
        });

        test("opção inexistente do menu avisa e continua", () -> {
            input("7", "0");
            new ProductMenu(new ProductService(new Inventory())).show();
            contains("aviso", out(), "Opção inválida");
        });

        test("texto no lugar da opção do menu avisa e continua", () -> {
            input("abc", "0");
            new ProductMenu(new ProductService(new Inventory())).show();
            contains("aviso", out(), "Valor inválido");
        });

        test("buscar produto comum mostra os dados e 'Exige receita: não'", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            s.registerProduct(model.ProductType.REGULAR, "P1", "Dipirona", 8.5, 20, inDays(100), 5);
            input("2", "P1", "0");
            new ProductMenu(s).show();
            contains("nome", out(), "Dipirona");
            contains("preço", out(), String.format("%.2f", 8.5));
            contains("quantidade", out(), "20");
            contains("receita", out(), "Exige receita: não");
            contains("data em dd/mm/aaaa", out(), date(100));
        });

        test("buscar controlado mostra 'Exige receita: sim'", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            s.registerProduct(model.ProductType.CONTROLLED, "C1", "Clonazepam", 25, 10, inDays(100), 3);
            input("2", "C1", "0");
            new ProductMenu(s).show();
            contains("receita", out(), "Exige receita: sim");
        });

        test("buscar código que não existe avisa", () -> {
            input("2", "999", "0");
            new ProductMenu(new ProductService(new Inventory())).show();
            contains("aviso", out(), "Produto não encontrado");
        });

        test("listar sem produtos avisa", () -> {
            input("3", "0");
            new ProductMenu(new ProductService(new Inventory())).show();
            contains("aviso", out(), "Nenhum produto cadastrado");
        });

        test("listar mostra todos os produtos", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            s.registerProduct(model.ProductType.REGULAR, "P1", "Dipirona", 8.5, 20, inDays(100), 5);
            s.registerProduct(model.ProductType.PERFUME, "P2", "Hidratante", 30, 5, inDays(300), 2);
            input("3", "0");
            new ProductMenu(s).show();
            contains("1º", out(), "Dipirona");
            contains("2º", out(), "Hidratante");
        });

        test("remover produto existente e depois tentar de novo", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            s.registerProduct(model.ProductType.REGULAR, "P1", "Dipirona", 8.5, 20, inDays(100), 5);
            input("4", "P1", "4", "P1", "0");
            new ProductMenu(s).show();
            contains("removeu", out(), "Produto removido com sucesso");
            contains("segunda vez", out(), "Produto não encontrado");
            isNull("não está mais", inv.findProduct("P1"));
        });
    }

    // ==================================================================
    // CustomerMenu
    // ==================================================================
    private static void customerMenu() {
        suite("CustomerMenu (menu de clientes)");

        test("cadastra cliente de cada tipo (opções 1 a 4)", () -> {
            CustomerType[] expected = {CustomerType.REGULAR, CustomerType.SENIOR, CustomerType.INSURANCE, CustomerType.LOYALTY};
            for (int i = 0; i < expected.length; i++) {
                input("1", "Cliente", "123", String.valueOf(i + 1), "0");
                CustomerService s = new CustomerService();
                new CustomerMenu(s).show();
                eq("tipo da opção " + (i + 1), expected[i], s.findByCpf("123").getType());
            }
        });

        test("cadastro mostra mensagem de sucesso", () -> {
            input("1", "Maria", "123", "1", "0");
            new CustomerMenu(new CustomerService()).show();
            contains("mensagem", out(), "Cliente cadastrado com sucesso: Maria");
        });

        test("CPF repetido: mostra o motivo e não duplica", () -> {
            CustomerService s = new CustomerService();
            input("1", "Maria", "123", "1", "1", "Outra", "123", "2", "0");
            new CustomerMenu(s).show();
            contains("motivo", out(), "Cliente não cadastrado");
            contains("já cadastrado", out(), "já cadastrado");
            eq("tamanho", 1, s.listCustomers().size());
        });

        test("nome em branco: recusa com o motivo", () -> {
            CustomerService s = new CustomerService();
            input("1", "", "123", "1", "0");
            new CustomerMenu(s).show();
            contains("motivo", out(), "Cliente não cadastrado");
            eq("tamanho", 0, s.listCustomers().size());
        });

        test("CPF em branco: recusa com o motivo", () -> {
            CustomerService s = new CustomerService();
            input("1", "Maria", "", "1", "0");
            new CustomerMenu(s).show();
            contains("motivo", out(), "Cliente não cadastrado");
            eq("tamanho", 0, s.listCustomers().size());
        });

        test("tipo inválido pede de novo", () -> {
            CustomerService s = new CustomerService();
            input("1", "Maria", "123", "9", "2", "0");
            new CustomerMenu(s).show();
            contains("aviso", out(), "Opção inválida");
            eq("tipo final", CustomerType.SENIOR, s.findByCpf("123").getType());
        });

        test("buscar cliente mostra nome, CPF e tipo em português", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.SENIOR);
            input("2", "123", "0");
            new CustomerMenu(s).show();
            contains("nome", out(), "Maria");
            contains("cpf", out(), "123");
            contains("tipo", out(), "Idoso");
        });

        test("buscar cliente que não existe avisa", () -> {
            input("2", "999", "0");
            new CustomerMenu(new CustomerService()).show();
            contains("aviso", out(), "Cliente não encontrado");
        });

        test("listar sem clientes avisa", () -> {
            input("3", "0");
            new CustomerMenu(new CustomerService()).show();
            contains("aviso", out(), "Nenhum cliente cadastrado");
        });

        test("listar mostra todos com o tipo traduzido", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("A", "1", CustomerType.REGULAR);
            s.registerCustomer("B", "2", CustomerType.INSURANCE);
            s.registerCustomer("C", "3", CustomerType.LOYALTY);
            input("3", "0");
            new CustomerMenu(s).show();
            contains("comum", out(), "Comum");
            contains("convênio", out(), "Convênio");
            contains("fidelidade", out(), "Fidelidade");
        });

        test("remover cliente existente e depois tentar de novo", () -> {
            CustomerService s = new CustomerService();
            s.registerCustomer("Maria", "123", CustomerType.REGULAR);
            input("4", "123", "4", "123", "0");
            new CustomerMenu(s).show();
            contains("removeu", out(), "Cliente removido com sucesso");
            contains("segunda vez", out(), "Cliente não encontrado");
            isNull("não está mais", s.findByCpf("123"));
        });

        test("opção inexistente avisa e continua", () -> {
            input("9", "0");
            new CustomerMenu(new CustomerService()).show();
            contains("aviso", out(), "Opção inválida");
        });
    }

    // ==================================================================
    // InventoryMenu
    // ==================================================================
    private static void inventoryMenu() {
        suite("InventoryMenu (menu de estoque)");

        test("entrada de mercadoria soma ao estoque e informa a nova quantidade", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("P1", 5, 10, 2));
            input("1", "P1", "15", "0");
            new InventoryMenu(inv, new RestockOrder()).show();
            eq("quantidade", 25, inv.findProduct("P1").getQuantity());
            contains("mensagem", out(), "Entrada realizada");
            contains("nova quantidade", out(), "25");
        });

        test("entrada em código inexistente avisa", () -> {
            input("1", "999", "0");
            new InventoryMenu(new Inventory(), new RestockOrder()).show();
            contains("aviso", out(), "Produto não encontrado");
        });

        test("entrada com quantidade zero ou negativa é recusada", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("P1", 5, 10, 2));
            input("1", "P1", "0", "1", "P1", "-4", "0");
            new InventoryMenu(inv, new RestockOrder()).show();
            contains("aviso", out(), "Entrada não realizada");
            eq("quantidade intacta", 10, inv.findProduct("P1").getQuantity());
        });

        test("texto no lugar da quantidade pede de novo", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("P1", 5, 10, 2));
            input("1", "P1", "muitas", "5", "0");
            new InventoryMenu(inv, new RestockOrder()).show();
            contains("aviso", out(), "Valor inválido");
            eq("quantidade", 15, inv.findProduct("P1").getQuantity());
        });

        test("ver estoque vazio avisa", () -> {
            input("2", "0");
            new InventoryMenu(new Inventory(), new RestockOrder()).show();
            contains("aviso", out(), "Estoque vazio");
        });

        test("ver estoque mostra código, nome, quantidade e mínimo", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(new RegularProduct("P1", "Dipirona", 5, 10, inDays(100), 3));
            input("2", "0");
            new InventoryMenu(inv, new RestockOrder()).show();
            contains("código", out(), "P1");
            contains("nome", out(), "Dipirona");
            contains("quantidade", out(), "10");
            contains("mínimo", out(), "3");
        });

        test("pedidos de reposição: sem pedidos avisa", () -> {
            input("3", "0");
            new InventoryMenu(new Inventory(), new RestockOrder()).show();
            contains("aviso", out(), "Nenhum pedido de reposição");
        });

        test("pedidos de reposição: mostra o produto que ficou abaixo do mínimo", () -> {
            Inventory inv = new Inventory();
            RestockOrder restock = new RestockOrder();
            inv.addObserver(restock);
            inv.addProduct(new RegularProduct("P1", "Dipirona", 5, 10, inDays(100), 5));
            inv.decreaseStock("P1", 8);
            clearOut();
            input("3", "0");
            new InventoryMenu(inv, restock).show();
            contains("produto no pedido", out(), "Dipirona");
        });

        test("dar entrada pelo menu encerra o pedido de reposição", () -> {
            Inventory inv = new Inventory();
            RestockOrder restock = new RestockOrder();
            inv.addObserver(restock);
            inv.addProduct(new RegularProduct("P1", "Dipirona", 5, 10, inDays(100), 5));
            inv.decreaseStock("P1", 8);
            eq("pedido aberto", 1, restock.getOrders().size());
            input("1", "P1", "50", "0");
            new InventoryMenu(inv, restock).show();
            eq("pedido encerrado", 0, restock.getOrders().size());
        });

        test("opção inexistente avisa e continua", () -> {
            input("9", "0");
            new InventoryMenu(new Inventory(), new RestockOrder()).show();
            contains("aviso", out(), "Opção inválida");
        });
    }

    // ==================================================================
    // LoginMenu
    // ==================================================================
    private static void loginMenu() {
        suite("LoginMenu (login do funcionário)");

        test("login válido devolve o funcionário e dá boas-vindas", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            input("Ana");
            Employee e = new LoginMenu(s).show();
            eq("nome", "Ana", e.getName());
            contains("boas-vindas", out(), "Bem-vindo, Ana");
        });

        test("devolve o funcionário com o perfil certo (balconista e farmacêutico)", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            s.registerEmployee("Carlos", Profile.PHARMACEUTICAL);
            input("Carlos");
            eq("perfil do Carlos", Profile.PHARMACEUTICAL, new LoginMenu(s).show().getProfile());
            input("Ana");
            eq("perfil da Ana", Profile.CLERK, new LoginMenu(s).show().getProfile());
        });

        test("nome que não existe: avisa e pergunta de novo até acertar", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            input("Fulano", "Beltrano", "Ana");
            Employee e = new LoginMenu(s).show();
            contains("aviso", out(), "Funcionário não encontrado");
            eq("nome final", "Ana", e.getName());
        });

        test("espaços antes e depois do nome são ignorados", () -> {
            EmployeeService s = new EmployeeService();
            s.registerEmployee("Ana", Profile.CLERK);
            input("   Ana   ");
            eq("nome", "Ana", new LoginMenu(s).show().getName());
        });
    }

    // ==================================================================
    // SalesMenu
    // ==================================================================

    private static final Employee CLERK = new Employee("Ana", Profile.CLERK);
    private static final Employee PHARMACIST = new Employee("Carlos", Profile.PHARMACEUTICAL);

    /** Loja pronta: estoque, clientes (um de cada tipo), histórico e serviço real. */
    private static class Shop {
        final Inventory inventory = new Inventory();
        final SalesHistory history = new SalesHistory();
        final CustomerService customers = new CustomerService();
        final SalesService real = new RealSalesService(inventory, history);
        ProxySalesService lastProxy;

        Shop() {
            inventory.addProduct(regular("R1", 10.0, 20, 2));
            inventory.addProduct(controlled("C1", 40.0, 10, 2));
            inventory.addProduct(perfume("P1", 30.0, 20, 2));
            inventory.addProduct(new RegularProduct("V1", "Vencido", 5.0, 10, inDays(-3), 1));
            customers.registerCustomer("Maria", "111", CustomerType.REGULAR);
            customers.registerCustomer("Seu José", "222", CustomerType.SENIOR);
            customers.registerCustomer("Carla", "333", CustomerType.INSURANCE);
            customers.registerCustomer("Paulo", "444", CustomerType.LOYALTY);
        }

        /** Cria o menu de venda (o proxy fica na frente, como no sistema real). Chamar DEPOIS de input(...). */
        SalesMenu menu(Employee who) {
            lastProxy = new ProxySalesService(real, who);
            return new SalesMenu(lastProxy, inventory, customers, who);
        }

        int stock(String code) {
            return inventory.findProduct(code).getQuantity();
        }

        Sale onlySale() {
            eq("quantidade de vendas no histórico", 1, history.getSales().size());
            return history.getSales().get(0);
        }
    }

    private static void salesMenu() {
        suite("SalesMenu (menu de venda)");

        test("venda comum no cartão: finaliza, baixa o estoque e vai ao histórico", () -> {
            Shop shop = new Shop();
            input("111", "R1", "2", "n", "1");
            shop.menu(CLERK).show();
            contains("mensagem", out(), "Venda finalizada com sucesso");
            eq("estoque", 18, shop.stock("R1"));
            Sale sale = shop.onlySale();
            eq("pagamento", "Cartão", sale.getPaymentMethod());
            near("total", 20, sale.getTotal());
        });

        test("mostra o cliente encontrado antes de vender", () -> {
            Shop shop = new Shop();
            input("222", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("nome do cliente", out(), "Seu José");
        });

        test("registra o cliente e o funcionário certos na venda", () -> {
            Shop shop = new Shop();
            input("222", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            Sale sale = shop.onlySale();
            eq("cliente", "Seu José", sale.getCustomer().getName());
            eq("funcionário", "Ana", sale.getEmployee().getName());
        });

        test("cliente comum: sem desconto", () -> {
            Shop shop = new Shop();
            input("111", "R1", "10", "n", "1"); // subtotal 100
            shop.menu(CLERK).show();
            near("desconto", 0, shop.onlySale().getDiscount());
            near("total", 100, shop.onlySale().getTotal());
        });

        test("cliente idoso: 10% de desconto", () -> {
            Shop shop = new Shop();
            input("222", "R1", "10", "n", "1");
            shop.menu(CLERK).show();
            near("desconto", 10, shop.onlySale().getDiscount());
            near("total", 90, shop.onlySale().getTotal());
        });

        test("cliente de convênio: 15% de desconto", () -> {
            Shop shop = new Shop();
            input("333", "R1", "10", "n", "1");
            shop.menu(CLERK).show();
            near("desconto", 15, shop.onlySale().getDiscount());
            near("total", 85, shop.onlySale().getTotal());
        });

        test("cliente fidelidade: 5% de desconto", () -> {
            Shop shop = new Shop();
            input("444", "R1", "10", "n", "1");
            shop.menu(CLERK).show();
            near("desconto", 5, shop.onlySale().getDiscount());
            near("total", 95, shop.onlySale().getTotal());
        });

        test("pagamento por pix", () -> {
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "2");
            shop.menu(CLERK).show();
            contains("confirmação", out(), "Pix realizado");
            eq("pagamento", "Pix", shop.onlySale().getPaymentMethod());
        });

        test("pagamento em dinheiro com troco", () -> {
            Shop shop = new Shop();
            input("111", "R1", "2", "n", "3", "50"); // total 20
            shop.menu(CLERK).show();
            contains("troco", out(), "Troco");
            contains("valor do troco", out(), String.format("%.2f", 30.0));
            contains("sucesso", out(), "Venda finalizada com sucesso");
            eq("pagamento", "Dinheiro", shop.onlySale().getPaymentMethod());
        });

        test("pagamento em dinheiro no valor exato", () -> {
            Shop shop = new Shop();
            input("111", "R1", "2", "n", "3", "20");
            shop.menu(CLERK).show();
            contains("sem troco", out(), "Sem nenhum troco");
            contains("sucesso", out(), "Venda finalizada com sucesso");
        });

        test("pagamento em dinheiro insuficiente: venda não realizada e nada muda", () -> {
            Shop shop = new Shop();
            input("111", "R1", "2", "n", "3", "5"); // total 20
            shop.menu(CLERK).show();
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "insuficiente");
            notContains("sem sucesso", out(), "Venda finalizada com sucesso");
            eq("estoque", 20, shop.stock("R1"));
            eq("histórico", 0, shop.history.getSales().size());
        });

        test("produto que não existe: avisa e pergunta de novo", () -> {
            Shop shop = new Shop();
            input("111", "XXX", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Produto não encontrado");
            contains("sucesso", out(), "Venda finalizada com sucesso");
        });

        test("quantidade zero: avisa e não adiciona o item", () -> {
            Shop shop = new Shop();
            input("111", "R1", "0", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Quantidade inválida");
            eq("só o item válido foi vendido", 1, shop.onlySale().getItems().size());
            eq("estoque", 19, shop.stock("R1"));
        });

        test("quantidade negativa: avisa e não adiciona o item", () -> {
            Shop shop = new Shop();
            input("111", "R1", "-3", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Quantidade inválida");
            eq("estoque", 19, shop.stock("R1"));
        });

        test("vários itens no carrinho (resposta 's' continua adicionando)", () -> {
            Shop shop = new Shop();
            input("111", "R1", "2", "s", "P1", "3", "n", "1");
            shop.menu(CLERK).show();
            Sale sale = shop.onlySale();
            eq("itens", 2, sale.getItems().size());
            near("total", 2 * 10.0 + 3 * 30.0, sale.getTotal());
            eq("estoque do comum", 18, shop.stock("R1"));
            eq("estoque da perfumaria", 17, shop.stock("P1"));
        });

        test("quantidade acima do estoque: venda não realizada, nada muda", () -> {
            Shop shop = new Shop();
            input("111", "R1", "99", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "estoque");
            eq("estoque", 20, shop.stock("R1"));
            eq("histórico", 0, shop.history.getSales().size());
        });

        test("produto vencido: venda não realizada", () -> {
            Shop shop = new Shop();
            input("111", "V1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "vencido");
            eq("histórico", 0, shop.history.getSales().size());
        });

        test("forma de pagamento inválida: avisa e pergunta de novo", () -> {
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "7", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "Opção inválida");
            contains("sucesso", out(), "Venda finalizada com sucesso");
        });

        test("produto comum não pede receita", () -> {
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            notContains("sem pedido de receita", out(), "Nome do médico");
        });

        test("balconista + controlado: pede a receita, mas a venda é NEGADA pelo proxy", () -> {
            Shop shop = new Shop();
            input("111", "C1", "1", "n", "Dr. Silva", "001", "1");
            shop.menu(CLERK).show();
            contains("pediu a receita", out(), "Nome do médico");
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "farmac");
            eq("estoque", 10, shop.stock("C1"));
            eq("histórico", 0, shop.history.getSales().size());
            eq("tentativa registrada", 1, shop.lastProxy.getDeniedAttempts().size());
        });

        test("farmacêutico + controlado com receita: venda liberada e receita guardada", () -> {
            Shop shop = new Shop();
            input("111", "C1", "1", "n", "Dr. Silva", "001", "1");
            shop.menu(PHARMACIST).show();
            contains("sucesso", out(), "Venda finalizada com sucesso");
            eq("estoque", 9, shop.stock("C1"));
            Sale sale = shop.onlySale();
            notNull("receita", sale.getPrescription());
            eq("médico", "Dr. Silva", sale.getPrescription().getDoctor());
            eq("número", "001", sale.getPrescription().getNumber());
            eq("funcionário", "Carlos", sale.getEmployee().getName());
        });

        test("farmacêutico + controlado acima do limite (3): venda não realizada", () -> {
            Shop shop = new Shop();
            input("111", "C1", "3", "n", "Dr. Silva", "001", "1");
            shop.menu(PHARMACIST).show();
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "limite");
            eq("estoque", 10, shop.stock("C1"));
        });

        test("carrinho com comum + controlado: o controlado pede a receita e o desconto segue o cliente", () -> {
            Shop shop = new Shop();
            input("222", "R1", "1", "s", "C1", "1", "n", "Dra. Ana", "777", "1"); // 10 + 40 = 50, idoso -10%
            shop.menu(PHARMACIST).show();
            Sale sale = shop.onlySale();
            near("subtotal", 50, sale.calculateSubtotal());
            near("desconto", 5, sale.getDiscount());
            near("total", 45, sale.getTotal());
        });
    }

    // ==================================================================
    // Robustez do SalesMenu (entradas erradas do usuário não podem derrubar o programa)
    // ==================================================================

    /** Roda o menu; só falha se ele "quebrar" com erro de conversão/nulo. Acabar a entrada simulada é aceitável. */
    private static void mustNotCrash(Body menuRun) throws Exception {
        try {
            menuRun.run();
        } catch (NoSuchElementException endOfScriptedInput) {
            // faltou entrada simulada: o menu estava esperando o usuário digitar de novo (ok)
        } catch (NumberFormatException | NullPointerException crash) {
            fail("o menu QUEBROU em vez de avisar o usuário: " + crash.getClass().getSimpleName()
                    + " (" + crash.getMessage() + ")");
        }
    }

    private static void salesMenuRobustness() {
        suite("SalesMenu: robustez a entradas erradas do usuário");

        test("CPF não cadastrado: avisa e pergunta de novo (não pode dar NullPointerException)", () -> {
            Shop shop = new Shop();
            input("999", "111", "R1", "1", "n", "1");
            shop.menu(CLERK).show();
            contains("aviso", out(), "não registrado");
            contains("depois do CPF certo a venda segue", out(), "Venda finalizada com sucesso");
        });

        test("quantidade digitada como texto não derruba o menu", () -> {
            Shop shop = new Shop();
            input("111", "R1", "duas", "R1", "1", "n", "1");
            mustNotCrash(() -> shop.menu(CLERK).show());
        });

        test("opção de pagamento digitada como texto não derruba o menu", () -> {
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "cartao", "1");
            mustNotCrash(() -> shop.menu(CLERK).show());
        });

        test("dinheiro recebido com vírgula (50,00) não derruba o menu", () -> {
            // o ProductMenu já aceita vírgula (12,50); quem digita em reais costuma usar vírgula
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "3", "50,00", "50");
            mustNotCrash(() -> shop.menu(CLERK).show());
        });

        test("dinheiro recebido digitado como texto não derruba o menu", () -> {
            Shop shop = new Shop();
            input("111", "R1", "1", "n", "3", "cinquenta", "50");
            mustNotCrash(() -> shop.menu(CLERK).show());
        });
    }
}
