// A primeira linha (package) deve ficar como o IntelliJ gerou na sua pasta de testes
import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.Product;

import java.time.LocalDate;

public class Part1Tests {

    public static void main(String[] args) {

        testFactories();
    }

    // Testa o Factory Method: cada criador devolve o tipo certo de produto
    private static void testFactories() {
        System.out.println("=== Teste das fábricas de produto ===");

        // Variáveis do tipo ProductCreator: quem usa não precisa saber qual é o criador concreto
        ProductCreator regularCreator = new RegularProductCreator();
        ProductCreator controlledCreator = new ControlledProductCreator();
        ProductCreator perfumeCreator = new PerfumeProductCreator();

        LocalDate expiration = LocalDate.now().plusDays(120);

        // Variáveis do tipo Product: quem recebe também não sabe qual classe foi criada
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
}