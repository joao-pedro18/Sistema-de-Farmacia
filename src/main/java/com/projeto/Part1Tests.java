import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.Prescription;
import model.Product;
import model.ProductType;
import service.Inventory;
import service.ProductService;

import java.time.LocalDate;

public class Part1Tests {

    public static void main(String[] args) {

        testFactories();
        testInventory();
        testProductService();
        testValidateSale();
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
}