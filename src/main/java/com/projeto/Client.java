import java.time.LocalDate;

import model.CustomerType;
import model.ProductType;
import model.Profile;
import observer.*;
import service.*;
import ui.MainMenu;

public class Client {
    public static void main(String[] args) {
        // Cria os serviços compartilhados e as estruturas principais
        Inventory inventory = new Inventory();
        SalesHistory salesHistory = new SalesHistory();
        CustomerService customerService = new CustomerService();
        EmployeeService employeeService = new EmployeeService();
        ProductService productService = new ProductService(inventory);

        // Registra os observadores no Inventory (Subject)
        inventory.addObserver(new LowStockAlert());
        inventory.addObserver(new ExpirationAlert()); // Alerta produtos vencidos ou a vencer em 60 dias
        RestockOrder restockOrder = new RestockOrder();
        inventory.addObserver(restockOrder);

        // Funcionários (OBRIGATÓRIO: não existe tela de cadastro, então sem isso ninguém consegue fazer login)
        registerEmployees(employeeService);

        // Clientes e produtos de exemplo para a demonstração (opcional: pode comentar a linha abaixo)
        loadDemoData(productService, customerService);

        // Inicia o menu principal
        MainMenu mainMenu = new MainMenu(employeeService, productService, inventory, customerService, salesHistory, restockOrder);
        mainMenu.show();
    }

    // Cadastra os funcionários que podem entrar no sistema
    private static void registerEmployees(EmployeeService employeeService) {
        employeeService.registerEmployee("Ana", Profile.CLERK);
        employeeService.registerEmployee("Carlos", Profile.PHARMACEUTICAL);
    }

    // Cadastra clientes (um de cada tipo) e produtos (um de cada tipo) para facilitar a demonstração
    private static void loadDemoData(ProductService productService, CustomerService customerService) {
        customerService.registerCustomer("João Silva", "111.111.111-11", CustomerType.REGULAR);
        customerService.registerCustomer("Maria Souza", "222.222.222-22", CustomerType.SENIOR);
        customerService.registerCustomer("Carla Lima", "333.333.333-33", CustomerType.INSURANCE);
        customerService.registerCustomer("Paulo Reis", "444.444.444-44", CustomerType.LOYALTY);

        productService.registerProduct(ProductType.REGULAR, "P001", "Paracetamol", 10.0, 50, LocalDate.now().plusMonths(6), 10);
        productService.registerProduct(ProductType.CONTROLLED, "P002", "Rivotril", 45.0, 5, LocalDate.now().plusMonths(12), 2);
        productService.registerProduct(ProductType.PERFUME, "P003", "Desodorante", 20.0, 30, LocalDate.now().plusMonths(24), 5);
    }
}
