package tests;

import static tests.TestKit.*;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;

import model.CustomerType;
import model.Employee;
import model.Profile;
import observer.ExpirationAlert;
import observer.LowStockAlert;
import observer.RestockOrder;
import service.CustomerService;
import service.EmployeeService;
import service.Inventory;
import service.ProductService;
import service.SalesHistory;
import ui.MainMenu;

/**
 * Juntos: MainMenu (menu principal) e Client (a classe principal do projeto).
 * Usa typed(...) em vez de input(...) porque o MainMenu e os submenus têm Scanners separados.
 */
public class MainMenuTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        mainMenu();
        client();
    }

    /** Sistema montado só para o teste (independe dos dados de demonstração do Client). */
    private static class App {
        final Inventory inventory = new Inventory();
        final RestockOrder restock = new RestockOrder();
        final ProductService products = new ProductService(inventory);
        final CustomerService customers = new CustomerService();
        final EmployeeService employees = new EmployeeService();
        final SalesHistory history = new SalesHistory();

        App() {
            inventory.addObserver(new LowStockAlert());
            inventory.addObserver(new ExpirationAlert());
            inventory.addObserver(restock);
            employees.registerEmployee("Ana", Profile.CLERK);
            employees.registerEmployee("Carlos", Profile.PHARMACEUTICAL);
            customers.registerCustomer("Maria", "111", CustomerType.REGULAR);
            customers.registerCustomer("Seu José", "222", CustomerType.SENIOR);
            inventory.addProduct(regular("R1", 10.0, 20, 2));
            inventory.addProduct(controlled("C1", 40.0, 10, 2));
            inventory.addProduct(controlled("C2", 40.0, 3, 2));
        }

        /** Chamar DEPOIS de typed(...), porque o menu cria o Scanner no construtor. */
        MainMenu menu() {
            return new MainMenu(employees, products, inventory, customers, history, restock);
        }
    }

    private static void runClient() throws Exception {
        try {
            Class.forName("Client").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw (Error) cause;
        }
    }

    // ------------------------------------------------------------------
    private static void mainMenu() {
        suite("MainMenu (menu principal)");

        test("login e sair: boas-vindas e despedida", () -> {
            typed("Ana", "0");
            new App().menu().show();
            contains("boas-vindas", out(), "Bem-vindo, Ana");
            contains("despedida", out(), "Saindo do sistema");
        });

        test("login com nome errado repete até acertar", () -> {
            typed("Fulano", "Ana", "0");
            new App().menu().show();
            contains("aviso", out(), "Funcionário não encontrado");
            contains("entrou", out(), "Bem-vindo, Ana");
        });

        test("mostra todas as opções do menu em português", () -> {
            typed("Ana", "0");
            new App().menu().show();
            contains("1", out(), "Produtos");
            contains("2", out(), "Estoque");
            contains("3", out(), "Clientes");
            contains("4", out(), "Realizar Venda");
            contains("5", out(), "Histórico");
            contains("6 troca de usuário", out(), "troc");
            contains("0", out(), "Sair");
        });

        test("mostra o perfil do balconista em português (não 'CLERK')", () -> {
            typed("Ana", "0");
            new App().menu().show();
            contains("perfil", out(), "Balconista");
            notContains("sem inglês", out(), "CLERK");
        });

        test("mostra o perfil do farmacêutico em português (não 'PHARMACEUTICAL')", () -> {
            typed("Carlos", "0");
            new App().menu().show();
            contains("perfil", out(), "Farmacêutico");
            notContains("sem inglês", out(), "PHARMACEUTICAL");
        });

        test("opção inexistente avisa e continua", () -> {
            typed("Ana", "7", "0");
            new App().menu().show();
            contains("aviso", out(), "Opção inválida");
            contains("continuou até sair", out(), "Saindo do sistema");
        });

        test("texto no lugar da opção avisa e continua", () -> {
            typed("Ana", "abc", "0");
            new App().menu().show();
            contains("aviso de valor inválido", out(), "válid");
            contains("continuou até sair", out(), "Saindo do sistema");
        });

        test("linha em branco no lugar da opção não trava o menu", () -> {
            typed("Ana", "", "0");
            new App().menu().show();
            contains("continuou até sair", out(), "Saindo do sistema");
        });

        test("opção 1 abre o menu de produtos e volta ao principal", () -> {
            typed("Ana", "1", "0", "0");
            new App().menu().show();
            contains("menu de produtos", out(), "MENU DE PRODUTOS");
            contains("saiu", out(), "Saindo do sistema");
        });

        test("opção 2 abre o menu de estoque", () -> {
            typed("Ana", "2", "0", "0");
            new App().menu().show();
            contains("menu de estoque", out(), "MENU DE ESTOQUE");
        });

        test("opção 3 abre o menu de clientes", () -> {
            typed("Ana", "3", "0", "0");
            new App().menu().show();
            contains("menu de clientes", out(), "MENU DE CLIENTES");
        });

        test("cadastrar cliente pelo menu principal chega ao CustomerService", () -> {
            App app = new App();
            typed("Ana", "3", "1", "Novo Cliente", "999", "2", "0", "0");
            app.menu().show();
            notNull("cliente cadastrado", app.customers.findByCpf("999"));
            eq("tipo", CustomerType.SENIOR, app.customers.findByCpf("999").getType());
        });

        test("cadastrar produto pelo menu principal chega ao estoque", () -> {
            App app = new App();
            String date = inDays(200).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            typed("Ana", "1", "1", "1", "N1", "Novo", "5", "10", date, "2", "0", "0");
            app.menu().show();
            notNull("produto no estoque", app.inventory.findProduct("N1"));
        });

        test("balconista vende produto comum pelo menu principal", () -> {
            App app = new App();
            typed("Ana", "4", "111", "R1", "2", "n", "1", "0");
            app.menu().show();
            contains("sucesso", out(), "Venda finalizada com sucesso");
            eq("estoque", 18, app.inventory.findProduct("R1").getQuantity());
            eq("histórico", 1, app.history.getSales().size());
        });

        test("balconista NÃO vende controlado (o proxy usa o perfil de quem está logado)", () -> {
            App app = new App();
            typed("Ana", "4", "111", "C1", "1", "n", "Dr. Silva", "001", "1", "0");
            app.menu().show();
            contains("aviso", out(), "Venda não realizada");
            contains("motivo", out(), "farmac");
            eq("estoque", 10, app.inventory.findProduct("C1").getQuantity());
            eq("histórico", 0, app.history.getSales().size());
        });

        test("farmacêutico vende controlado pelo menu principal", () -> {
            App app = new App();
            typed("Carlos", "4", "111", "C1", "1", "n", "Dr. Silva", "001", "1", "0");
            app.menu().show();
            contains("sucesso", out(), "Venda finalizada com sucesso");
            eq("estoque", 9, app.inventory.findProduct("C1").getQuantity());
        });

        test("trocar de usuário: depois de trocar para o farmacêutico, a venda de controlado é liberada", () -> {
            App app = new App();
            typed("Ana", "6", "Carlos", "4", "111", "C1", "1", "n", "Dr. Silva", "001", "1", "0");
            app.menu().show();
            contains("sucesso", out(), "Venda finalizada com sucesso");
            eq("quem vendeu", "Carlos", app.history.getSales().get(0).getEmployee().getName());
        });

        test("trocar de usuário: depois de trocar para o balconista, a venda de controlado é negada", () -> {
            App app = new App();
            typed("Carlos", "6", "Ana", "4", "111", "C1", "1", "n", "Dr. Silva", "001", "1", "0");
            app.menu().show();
            contains("motivo", out(), "farmac");
            eq("histórico", 0, app.history.getSales().size());
        });

        test("trocar de usuário com nome errado repete até acertar", () -> {
            typed("Ana", "6", "Fulano", "Carlos", "0");
            new App().menu().show();
            contains("aviso", out(), "Funcionário não encontrado");
            contains("novo usuário", out(), "Bem-vindo, Carlos");
        });

        test("histórico vazio avisa que não há vendas", () -> {
            typed("Ana", "5", "0");
            new App().menu().show();
            contains("aviso", out(), "Nenhuma venda registrada");
        });

        test("histórico mostra a venda feita (cliente, funcionário e pagamento)", () -> {
            typed("Ana", "4", "222", "R1", "1", "n", "2", "5", "0");
            new App().menu().show();
            contains("cliente", out(), "Seu José");
            contains("funcionário", out(), "Ana");
            contains("pagamento", out(), "Pix");
        });

        test("os observadores funcionam pelo menu: venda deixa estoque baixo e gera pedido de reposição", () -> {
            App app = new App();
            // C2 tem 3 unidades e mínimo 2: vender 2 deixa 1
            typed("Carlos", "4", "111", "C2", "2", "n", "Dr. Silva", "001", "1", "2", "3", "0", "0");
            app.menu().show();
            contains("alerta", out(), "estoque baixo");
            contains("pedido registrado pelo observador", out(), "Pedido de reposição registrado para o produto Controlado C2");
            notContains("o menu de estoque lista o pedido (não 'nenhum pedido')", out(), "Nenhum pedido de reposição");
            eq("pedido guardado", 1, app.restock.getOrders().size());
        });

        test("o menu de produtos lista os produtos do sistema", () -> {
            typed("Ana", "1", "3", "0", "0");
            new App().menu().show();
            contains("produto do sistema", out(), "Nome: Comum R1");
        });

        test("o menu de estoque mostra o estoque do sistema", () -> {
            typed("Ana", "2", "2", "0", "0");
            new App().menu().show();
            contains("produto do estoque", out(), "Nome: Controlado C1");
        });

        test("o menu de clientes lista os clientes do sistema", () -> {
            typed("Ana", "3", "3", "0", "0");
            new App().menu().show();
            contains("cliente do sistema", out(), "Seu José");
        });

        test("vender produto perto de vencer pelo menu dispara o alerta de validade", () -> {
            App app = new App();
            app.inventory.addProduct(new model.RegularProduct("X1", "Xarope", 5.0, 10, inDays(10), 1));
            typed("Ana", "4", "111", "X1", "1", "n", "1", "0");
            app.menu().show();
            contains("alerta", out(), "perto de vencer");
        });
    }

    // ------------------------------------------------------------------
    private static void client() {
        suite("Client (classe principal do projeto)");

        test("o sistema permite fazer login (existe balconista cadastrado) e sair", () -> {
            typed("Ana", "0");
            runClient();
            contains("boas-vindas", out(), "Bem-vindo, Ana");
            contains("despedida", out(), "Saindo do sistema");
        });

        test("existe farmacêutico cadastrado", () -> {
            typed("Carlos", "0");
            runClient();
            contains("perfil", out(), "Farmacêutico");
        });

        test("balconista consegue vender produto de exemplo (Client carrega os dados de demonstração)", () -> {
            typed("Ana", "4", "111.111.111-11", "P001", "2", "n", "1", "5", "0");
            runClient();
            contains("sucesso", out(), "Venda finalizada com sucesso");
            contains("histórico", out(), "Ana");
        });

        test("há clientes de exemplo dos 4 tipos (para demonstrar os descontos)", () -> {
            typed("Ana", "3", "3", "0", "0");
            runClient();
            contains("comum", out(), "Comum");
            contains("idoso", out(), "Idoso");
            contains("convênio", out(), "Convênio");
            contains("fidelidade", out(), "Fidelidade");
        });

        test("há um produto de exemplo de cada tipo", () -> {
            typed("Ana", "1", "3", "0", "0");
            runClient();
            contains("comum", out(), "Exige receita: não");
            contains("controlado", out(), "Exige receita: sim");
            contains("perfumaria", out(), "Desodorante");
        });

        test("Client liga os 3 observadores: duas vendas do controlado de exemplo geram alerta e pedido", () -> {
            // Rivotril de exemplo: 5 unidades, mínimo 2. Vender 2 + 2 deixa 1 (abaixo do mínimo)
            typed("Carlos",
                    "4", "111.111.111-11", "P002", "2", "n", "Dr. Silva", "1", "1",
                    "4", "111.111.111-11", "P002", "2", "n", "Dr. Silva", "1", "1",
                    "2", "3", "0", "0");
            runClient();
            contains("alerta de estoque baixo", out(), "estoque baixo");
            contains("pedido registrado pelo observador", out(), "Pedido de reposição registrado para o produto Rivotril");
            notContains("menu de estoque lista o pedido (não 'nenhum pedido')", out(), "Nenhum pedido de reposição");
        });

        test("Client liga o alerta de validade: produto cadastrado perto de vencer avisa ao vender", () -> {
            String soon = inDays(10).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            typed("Ana",
                    "1", "1", "1", "X1", "Xarope", "5", "10", soon, "2", "0",
                    "4", "111.111.111-11", "X1", "1", "n", "1",
                    "0");
            runClient();
            contains("alerta de validade", out(), "perto de vencer");
        });
    }
}
