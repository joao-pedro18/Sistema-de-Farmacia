package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Scanner;

import model.Product;
import model.ProductType;
import service.ProductService;

public class ProductMenu {
    private ProductService productService;
    private Scanner scanner = new Scanner(System.in);

    // Formato de data usado na digitação e na exibição

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Recebe o serviço de produtos que será usado no menu

    public ProductMenu(ProductService productService) {
        this.productService = productService;
    }

    // Mostra o menu de produtos até o usuário escolher voltar

    public void show() {
        while (true) {
            System.out.println("===== MENU DE PRODUTOS =====");
            System.out.println("1 - Cadastrar produto");
            System.out.println("2 - Buscar produto");
            System.out.println("3 - Listar produtos");
            System.out.println("4 - Remover produto");
            System.out.println("0 - Voltar");

            int option = readInt("Opção: ");

            switch (option) {
                case 1:
                    registerProduct();
                    break;
                case 2:
                    searchProduct();
                    break;
                case 3:
                    listProducts();
                    break;
                case 4:
                    removeProduct();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }
    }

    // Pede os dados e cadastra o produto

    private void registerProduct() {
        ProductType type = readProductType();

        System.out.print("Código do produto: ");
        String code = scanner.nextLine().trim();

        if (code.isBlank()) {
            System.out.println("Código não informado.\n");
            return;
        }

        System.out.print("Nome do produto: ");
        String name = scanner.nextLine().trim();

        if (name.isBlank()) {
            System.out.println("Nome não informado.\n");
            return;
        }

        double price = readDouble("Preço: ");
        int quantity = readInt("Quantidade em estoque: ");
        LocalDate expirationDate = readDate("Validade (dd/mm/aaaa): ");
        int minimumStock = readInt("Estoque mínimo: ");

        if (price <= 0) {
            System.out.println("O preço deve ser maior que zero.\n");
            return;
        }

        if (quantity < 0 || minimumStock < 0) {
            System.out.println("As quantidades não podem ser negativas.\n");
            return;
        }

        // O serviço pode negar o cadastro

        try {
            Product product = productService.registerProduct(type, code, name, price, quantity, expirationDate, minimumStock);

            System.out.println("Produto cadastrado com sucesso: " + product.getName() + "\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Produto não cadastrado: " + e.getMessage() + "\n");
        }
    }

    // Busca um produto pelo código e mostra os dados dele

    private void searchProduct() {
        System.out.print("Código do produto: ");
        String code = scanner.nextLine().trim();

        // A busca devolve null se o produto não existir

        Product product = productService.searchProduct(code);

        if (product == null) {
            System.out.println("Produto não encontrado!\n");
            return;
        }

        printProduct(product);
        System.out.println();
    }

    // Lista todos os produtos cadastrados

    private void listProducts() {
        List<Product> products = productService.listProducts();

        if (products.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.\n");
            return;
        }

        System.out.println("Produtos cadastrados:");
        for (Product product : products) {
            printProduct(product);
        }
        System.out.println();
    }

    // Remove um produto pelo código

    private void removeProduct() {
        System.out.print("Código do produto: ");
        String code = scanner.nextLine().trim();

        // A remoção devolve true se removeu e false se não encontrou o produto
        boolean removed = productService.removeProduct(code);

        if (removed) {
            System.out.println("Produto removido com sucesso!\n");
        } else {
            System.out.println("Produto não encontrado!\n");
        }
    }

    // Mostra os dados de um produto em uma linha

    private void printProduct(Product product) {
        String prescription = product.requiresPrescription() ? "sim" : "não";

        System.out.println("- Código: " + product.getCode()
                + " | Nome: " + product.getName()
                + " | Preço: R$ " + String.format("%.2f", product.getPrice())
                + " | Quantidade: " + product.getQuantity()
                + " | Mínimo: " + product.getMininumStock()
                + " | Validade: " + product.getExpirationDate().format(DATE_FORMAT)
                + " | Exige receita: " + prescription);
    }

    // Pede o tipo do produto até a opção ser válida

    private ProductType readProductType() {
        while (true) {
            System.out.println("Tipo do produto:");
            System.out.println("1 - Comum");
            System.out.println("2 - Controlado");
            System.out.println("3 - Perfumaria");

            int option = readInt("Opção: ");

            switch (option) {
                case 1:
                    return ProductType.REGULAR;
                case 2:
                    return ProductType.CONTROLLED;
                case 3:
                    return ProductType.PERFUME;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }
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

    // Pede um número decimal até o usuário digitar um valor válido (aceita vírgula)

    private double readDouble(String message) {
        while (true) {
            System.out.print(message);
            String text = scanner.nextLine().trim().replace(",", ".");

            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Digite um número (ex.: 12,50).\n");
            }
        }
    }

    // Pede uma data no formato dd/mm/aaaa até o usuário digitar uma data válida

    private LocalDate readDate(String message) {
        while (true) {
            System.out.print(message);
            String text = scanner.nextLine().trim();

            try {
                return LocalDate.parse(text, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use o formato dd/mm/aaaa (ex.: 20/03/2027).\n");
            }
        }
    }
}