import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.Product;
import service.Inventory;

import java.time.LocalDate;

public class Part1Tests {

    public static void main(String[] args) {

        testFactories();
        testInventory();
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

        // addProduct e listProducts
        inventory.addProduct(regular);
        inventory.addProduct(controlled);
        inventory.addProduct(perfume);
        System.out.println("Produtos cadastrados: " + inventory.listProducts().size());

        System.out.println("Adicionar código repetido:");
        expectError(() -> inventory.addProduct(regular));

        // findProduct
        System.out.println("Busca do código 001: " + inventory.findProduct("001").getName());
        System.out.println("Busca do código 999: " + inventory.findProduct("999"));

        // decreaseStock
        inventory.decreaseStock("001", 5);
        System.out.println("Dipirona após baixa de 5: " + inventory.findProduct("001").getQuantity());
        System.out.println("Baixa maior que o estoque:");
        expectError(() -> inventory.decreaseStock("001", 100));
        System.out.println("Baixa de produto inexistente:");
        expectError(() -> inventory.decreaseStock("999", 1));
        System.out.println("Baixa com quantidade zero:");
        expectError(() -> inventory.decreaseStock("001", 0));

        // increaseStock
        inventory.increaseStock("001", 10);
        System.out.println("Dipirona após entrada de 10: " + inventory.findProduct("001").getQuantity());
        System.out.println("Entrada com quantidade zero:");
        expectError(() -> inventory.increaseStock("001", 0));
        System.out.println("Entrada em produto inexistente:");
        expectError(() -> inventory.increaseStock("999", 5));

        // removeProduct
        System.out.println("Remover 003: " + inventory.removeProduct("003"));
        System.out.println("Busca do 003 depois de remover: " + inventory.findProduct("003"));
        System.out.println("Remover 003 de novo: " + inventory.removeProduct("003"));
        System.out.println("Produtos restantes: " + inventory.listProducts().size());
    }
}