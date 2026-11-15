import observer.ExpirationAlert;
import observer.LowStockAlert;
import observer.RestockOrder;
import service.CustomerService;
import service.Inventory;
import service.ProductService;
import ui.CustomerMenu;
import ui.InventoryMenu;
import ui.ProductMenu;

public class Part1MenuTests {

    public static void main(String[] args) {

        // Objetos compartilhados: os três menus usam o mesmo estoque e os mesmos serviços
        Inventory inventory = new Inventory();
        RestockOrder restockOrder = new RestockOrder();

        // Registra os observadores no estoque
        inventory.addObserver(new LowStockAlert());
        inventory.addObserver(new ExpirationAlert());
        inventory.addObserver(restockOrder);

        ProductService productService = new ProductService(inventory);
        CustomerService customerService = new CustomerService();

        // Cada menu recebe o que precisa pelo construtor
        ProductMenu productMenu = new ProductMenu(productService);
        CustomerMenu customerMenu = new CustomerMenu(customerService);
        InventoryMenu inventoryMenu = new InventoryMenu(inventory, restockOrder);

        // Abre os menus em sequência: ao escolher "0 - Voltar", passa para o próximo
        System.out.println(">>> TESTE 1 de 3: menu de produtos");
        productMenu.show();

        System.out.println(">>> TESTE 2 de 3: menu de clientes");
        customerMenu.show();

        System.out.println(">>> TESTE 3 de 3: menu de estoque");
        inventoryMenu.show();

        System.out.println(">>> Fim do teste dos menus.");
    }
}