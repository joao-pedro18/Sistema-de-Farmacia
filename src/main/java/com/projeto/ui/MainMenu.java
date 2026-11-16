package ui;

import java.util.Scanner;

import model.Employee;
import model.Profile;
import observer.RestockOrder;
import proxy.*;
import service.*;

public class MainMenu {
    private EmployeeService employeeService;
    private ProductService productService;
    private Inventory inventory;
    private CustomerService customerService;
    private SalesHistory salesHistory;
    private RestockOrder restockOrder;
    private Scanner scanner = new Scanner(System.in);

    public MainMenu(EmployeeService employeeService, ProductService productService, Inventory inventory, CustomerService customerService, SalesHistory salesHistory, RestockOrder restockOrder) {
        this.employeeService = employeeService;
        this.productService = productService;
        this.inventory = inventory;
        this.customerService = customerService;
        this.salesHistory = salesHistory;
        this.restockOrder = restockOrder;
    }

    public void show() {
        // O login só devolve quando o funcionário existe (ele repete até acertar)
        LoginMenu loginMenu = new LoginMenu(employeeService);
        Employee loggedEmployee = loginMenu.show();

        int option;

        do {
            System.out.println("\n===== MENU PRINCIPAL =====");
            System.out.println("Funcionário logado: " + loggedEmployee.getName() + " (" + profileLabel(loggedEmployee.getProfile()) + ")");
            System.out.println("1 - Produtos");
            System.out.println("2 - Estoque");
            System.out.println("3 - Clientes");
            System.out.println("4 - Realizar Venda");
            System.out.println("5 - Histórico de Vendas");
            System.out.println("6 - Trocar de usuário");
            System.out.println("0 - Sair");

            option = readInt("Escolha uma opção: ");

            switch (option) {
                case 1:
                    ProductMenu productMenu = new ProductMenu(productService);
                    productMenu.show();
                    break;

                case 2:
                    InventoryMenu inventoryMenu = new InventoryMenu(inventory, restockOrder);
                    inventoryMenu.show();
                    break;

                case 3:
                    CustomerMenu customerMenu = new CustomerMenu(customerService);
                    customerMenu.show();
                    break;

                case 4:
                    // Monta o Proxy de vendas com o funcionário que está logado agora
                    SalesService realSaleService = new RealSalesService(inventory, salesHistory);
                    SalesService saleServiceProxy = new ProxySalesService(realSaleService, loggedEmployee);

                    SalesMenu saleMenu = new SalesMenu(saleServiceProxy, inventory, customerService, loggedEmployee);
                    saleMenu.show();
                    break;

                case 5:
                    System.out.println("\n===== HISTÓRICO DE VENDAS =====");
                    salesHistory.printSales();
                    break;

                case 6:
                    System.out.println("\nTrocando de usuário...");
                    loggedEmployee = loginMenu.show();
                    break;

                case 0:
                    System.out.println("Saindo do sistema. Até logo!");
                    break;

                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        } while (option != 0);
    }

    // Traduz o perfil do funcionário para o texto mostrado no terminal
    private String profileLabel(Profile profile) {
        return switch (profile) {
            case CLERK -> "Balconista";
            case PHARMACEUTICAL -> "Farmacêutico";
        };
    }

    // Pede um número inteiro até o usuário digitar um valor válido
    private int readInt(String message) {
        while (true) {
            System.out.print(message);
            String text = scanner.nextLine().trim();

            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Digite um número inteiro.\n");
            }
        }
    }
}
