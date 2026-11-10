import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.Prescription;
import model.Product;
import model.ProductType;
import observer.ExpirationAlert;
import observer.LowStockAlert;
import observer.RestockOrder;
import service.Inventory;
import service.ProductService;

import java.time.LocalDate;

public class Part1Tests {

    public static void main(String[] args) {

        testFactories();
        testInventory();
        testProductService();
        testValidateSale();
        testObservers();
    }

    // Testa o Factory Method: cada criador devolve o tipo certo de produto
    private static void testFactories() {
        System.out.println("=== Teste das fábricas de produto ===");

        ProductCreator regularCreator = new RegularProductCreator();
        ProductCreator controlledCreator = new ControlledProductCreator();
        ProductCreator perfumeCreator = new PerfumeProductCreator();

        LocalDate expiration = LocalDate.now().plusDays(120);

        Product regular = regularCreator.createProduct("001", "Dipirona", 8.50, 20, expiration, 5);
        Product controlled = controlledCreator.createProduct("002", "Clonazepam", 25.00, 10, expiration, 3);
        Product perfume = perfumeCreator.createProduct("003", "Hidratante", 32.90, 15, expiration, 4);

        printProduct(regular);
        printProduct(controlled);
        printProduct(perfume);
    }

    // Mostra o tipo real do produto e se ele exige receita
    private static void printProduct(Product product) {
        System.out.println("Produto: " + product.getName());
        System.out.println("  Classe criada: " + product.getClass().getSimpleName());
        System.out.println("  Exige receita? " + product.requiresPrescription());
    }

    // Roda uma ação que deve dar erro e mostra a mensagem da exceção
    private static void expectError(Runnable action) {
        try {
            action.run();
            System.out.println("  ERRO: deveria ter lançado exceção!");
        } catch (IllegalArgumentException e) {
            System.out.println("  Erro esperado: " + e.getMessage());
        }
    }

    // Testa todos os métodos do estoque
    private static void testInventory() {
        System.out.println("\n=== Teste do estoque (Inventory) ===");

        Inventory inventory = new Inventory();
        LocalDate expiration = LocalDate.now().plusDays(120);

        Product regular = new RegularProductCreator().createProduct("001", "Dipirona", 8.50, 20, expiration, 5);
        Product controlled = new ControlledProductCreator().createProduct("002", "Clonazepam", 25.00, 10, expiration, 3);
        Product perfume = new PerfumeProductCreator().createProduct("003", "Hidratante", 32.90, 15, expiration, 4);

        inventory.addProduct(regular);
        inventory.addProduct(controlled);
        inventory.addProduct(perfume);
        System.out.println("Produtos cadastrados: " + inventory.listProducts().size());

        System.out.println("Adicionar código repetido:");
        expectError(() -> inventory.addProduct(regular));

        System.out.println("Busca do código 001: " + inventory.findProduct("001").getName());
        System.out.println("Busca do código 999: " + inventory.findProduct("999"));

        inventory.decreaseStock("001", 5);
        System.out.println("Dipirona após baixa de 5: " + inventory.findProduct("001").getQuantity());
        System.out.println("Baixa maior que o estoque:");
        expectError(() -> inventory.decreaseStock("001", 100));
        System.out.println("Baixa de produto inexistente:");
        expectError(() -> inventory.decreaseStock("999", 1));
        System.out.println("Baixa com quantidade zero:");
        expectError(() -> inventory.decreaseStock("001", 0));

        inventory.increaseStock("001", 10);
        System.out.println("Dipirona após entrada de 10: " + inventory.findProduct("001").getQuantity());
        System.out.println("Entrada com quantidade zero:");
        expectError(() -> inventory.increaseStock("001", 0));
        System.out.println("Entrada em produto inexistente:");
        expectError(() -> inventory.increaseStock("999", 5));

        System.out.println("Remover 003: " + inventory.removeProduct("003"));
        System.out.println("Busca do 003 depois de remover: " + inventory.findProduct("003"));
        System.out.println("Remover 003 de novo: " + inventory.removeProduct("003"));
        System.out.println("Produtos restantes: " + inventory.listProducts().size());
    }

    // Testa o ProductService: o cadastro usa o mapa de criadores, sem if por tipo
    private static void testProductService() {
        System.out.println("\n=== Teste do ProductService ===");

        Inventory inventory = new Inventory();
        ProductService service = new ProductService(inventory);
        LocalDate expiration = LocalDate.now().plusDays(120);

        Product regular = service.registerProduct(ProductType.REGULAR, "101", "Paracetamol", 6.90, 30, expiration, 5);
        Product controlled = service.registerProduct(ProductType.CONTROLLED, "102", "Diazepam", 22.00, 8, expiration, 2);
        Product perfume = service.registerProduct(ProductType.PERFUME, "103", "Protetor solar", 45.90, 12, expiration, 3);

        printProduct(regular);
        printProduct(controlled);
        printProduct(perfume);

        System.out.println("Produtos no serviço: " + service.listProducts().size());
        System.out.println("O estoque compartilhado tem o 102? " + (inventory.findProduct("102") != null));

        System.out.println("Cadastrar código repetido:");
        expectError(() -> service.registerProduct(ProductType.REGULAR, "101", "Outro", 1.0, 1, expiration, 1));

        System.out.println("Cadastrar tipo inválido (null):");
        expectError(() -> service.registerProduct(null, "104", "Teste", 1.0, 1, expiration, 1));

        System.out.println("Busca do 101: " + service.searchProduct("101").getName());
        System.out.println("Busca do 999: " + service.searchProduct("999"));
        System.out.println("Remover 101: " + service.removeProduct("101"));
        System.out.println("Remover 101 de novo: " + service.removeProduct("101"));
        System.out.println("Produtos restantes: " + service.listProducts().size());
    }

    // Mostra se a venda foi permitida ou barrada (e o motivo)
    private static void checkSale(String label, Product product, int quantity, Prescription prescription) {
        try {
            product.validateSale(quantity, prescription);
            System.out.println("  " + label + " -> permitida");
        } catch (IllegalArgumentException e) {
            System.out.println("  " + label + " -> barrada: " + e.getMessage());
        }
    }

    // Testa as regras de venda (validateSale) de cada tipo de produto
    private static void testValidateSale() {
        System.out.println("\n=== Teste das regras de venda (validateSale) ===");

        LocalDate future = LocalDate.now().plusDays(120);
        LocalDate soon = LocalDate.now().plusDays(20);
        LocalDate past = LocalDate.now().minusDays(1);
        Prescription prescription = new Prescription("123", "Dr. Silva");

        Product regular = new RegularProductCreator().createProduct("201", "Dipirona", 8.50, 10, future, 2);
        Product regularExpired = new RegularProductCreator().createProduct("202", "Ibuprofeno", 9.00, 10, past, 2);
        Product controlled = new ControlledProductCreator().createProduct("203", "Clonazepam", 25.00, 10, future, 2);
        Product perfume = new PerfumeProductCreator().createProduct("204", "Perfume", 89.90, 10, future, 2);
        Product perfumeSoon = new PerfumeProductCreator().createProduct("205", "Hidratante", 32.90, 10, soon, 2);
        Product perfumeExpired = new PerfumeProductCreator().createProduct("206", "Batom", 19.90, 10, past, 2);

        System.out.println("RegularProduct:");
        checkSale("2 unidades", regular, 2, null);
        checkSale("0 unidades", regular, 0, null);
        checkSale("50 unidades (estoque 10)", regular, 50, null);
        checkSale("produto vencido", regularExpired, 1, null);

        System.out.println("ControlledProduct:");
        checkSale("1 unidade com receita", controlled, 1, prescription);
        checkSale("1 unidade sem receita", controlled, 1, null);
        checkSale("0 unidades com receita", controlled, 0, prescription);
        checkSale("5 unidades com receita (limite 2)", controlled, 5, prescription);

        System.out.println("PerfumeProduct:");
        checkSale("1 unidade, validade longa", perfume, 1, null);
        checkSale("0 unidades", perfume, 0, null);
        checkSale("validade em 20 dias", perfumeSoon, 1, null);
        checkSale("produto vencido", perfumeExpired, 1, null);
    }

    // Testa o Observer: o Inventory avisa os observadores quando o estoque muda
    private static void testObservers() {
        System.out.println("\n=== Teste do Observer (alertas do estoque) ===");

        Inventory inventory = new Inventory();

        LowStockAlert lowStockAlert = new LowStockAlert();
        ExpirationAlert expirationAlert = new ExpirationAlert();
        RestockOrder restockOrder = new RestockOrder();

        // Inscreve os três observadores no estoque
        inventory.addObserver(lowStockAlert);
        inventory.addObserver(expirationAlert);
        inventory.addObserver(restockOrder);

        LocalDate future = LocalDate.now().plusDays(120);
        LocalDate soon = LocalDate.now().plusDays(10);
        LocalDate past = LocalDate.now().minusDays(1);

        inventory.addProduct(new RegularProductCreator().createProduct("301", "Dipirona", 8.50, 10, future, 5));
        inventory.addProduct(new RegularProductCreator().createProduct("302", "Ibuprofeno", 9.00, 10, past, 2));
        inventory.addProduct(new PerfumeProductCreator().createProduct("303", "Hidratante", 32.90, 10, soon, 2));

        // Estoque ainda acima do mínimo: nenhum aviso
        System.out.println("Baixa de 3 na Dipirona (10 -> 7, mínimo 5): nenhum aviso esperado");
        inventory.decreaseStock("301", 3);

        // Passou do mínimo: aviso de estoque baixo e pedido de reposição
        System.out.println("Baixa de 3 na Dipirona (7 -> 4): aviso de estoque baixo e pedido de reposição");
        inventory.decreaseStock("301", 3);

        // Continua baixo: novo aviso, mas o pedido não se repete
        System.out.println("Baixa de 1 na Dipirona (4 -> 3): novo aviso, mas sem pedido repetido");
        inventory.decreaseStock("301", 1);

        // Produto vencido: aviso de vencido
        System.out.println("Baixa de 1 no Ibuprofeno (vencido): aviso de vencido");
        inventory.decreaseStock("302", 1);

        // Produto perto de vencer: aviso de perto de vencer
        System.out.println("Baixa de 1 no Hidratante (validade em 10 dias): aviso de perto de vencer");
        inventory.decreaseStock("303", 1);

        // Mostra os pedidos de reposição registrados até agora
        restockOrder.printOrders();

        // Entrada de mercadoria também avisa, mas o estoque volta ao normal
        System.out.println("Entrada de 20 na Dipirona (3 -> 23): nenhum aviso esperado");
        inventory.increaseStock("301", 20);

        // Remove um observador: ele deixa de receber avisos
        inventory.removeObserver(lowStockAlert);
        inventory.addProduct(new RegularProductCreator().createProduct("304", "Xarope", 15.00, 10, future, 8));
        System.out.println("Sem o LowStockAlert, baixa de 5 no Xarope (10 -> 5, mínimo 8): só o pedido de reposição");
        inventory.decreaseStock("304", 5);

        System.out.println("Total de pedidos de reposição: " + restockOrder.getOrders().size());

        // Um observador novo entra sem mexer na classe Inventory
        inventory.addObserver(p -> System.out.println("  [Novo observador] Estoque do produto "
                + p.getName() + " mudou para " + p.getQuantity()));
        System.out.println("Novo observador registrado sem mexer no Inventory. Baixa de 1 na Dipirona (23 -> 22):");
        inventory.decreaseStock("301", 1);
    }
}