package ui;

import java.util.List;
import java.util.Scanner;

import model.Product;
import observer.RestockOrder;
import service.Inventory;

public class InventoryMenu {
    private Inventory inventory;
    private RestockOrder restockOrder;
    private Scanner scanner = new Scanner(System.in);

    // Recebe o estoque e o observador que guarda os pedidos de reposição

    public InventoryMenu(Inventory inventory, RestockOrder restockOrder) {
        this.inventory = inventory;
        this.restockOrder = restockOrder;
    }

    // Mostra o menu de estoque até o usuário escolher voltar

    public void show() {
        while (true) {
            System.out.println("===== MENU DE ESTOQUE =====");
            System.out.println("1 - Dar entrada de mercadoria");
            System.out.println("2 - Ver estoque");
            System.out.println("3 - Ver pedidos de reposição");
            System.out.println("0 - Voltar");

            int option = readInt("Opção: ");

            switch (option) {
                case 1:
                    increaseStock();
                    break;
                case 2:
                    listStock();
                    break;
                case 3:
                    restockOrder.printOrders();
                    System.out.println();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }
    }

    // Soma a quantidade recebida ao estoque de um produto já cadastrado

    private void increaseStock() {
        System.out.print("Código do produto: ");
        String code = scanner.nextLine().trim();

        // A busca devolve null se o produto não existir

        Product product = inventory.findProduct(code);

        if (product == null) {
            System.out.println("Produto não encontrado!\n");
            return;
        }

        int quantity = readInt("Quantidade recebida: ");

        // O estoque pode negar a entrada

        try {
            inventory.increaseStock(code, quantity);

            System.out.println("Entrada realizada! " + product.getName()
                    + " agora tem " + product.getQuantity() + " unidade(s).\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Entrada não realizada: " + e.getMessage() + "\n");
        }
    }

    // Mostra a quantidade em estoque de cada produto

    private void listStock() {
        List<Product> products = inventory.listProducts();

        if (products.isEmpty()) {
            System.out.println("Estoque vazio.\n");
            return;
        }

        System.out.println("Estoque:");
        for (Product product : products) {
            System.out.println("- Código: " + product.getCode()
                    + " | Nome: " + product.getName()
                    + " | Quantidade: " + product.getQuantity()
                    + " | Mínimo: " + product.getMininumStock());
        }
        System.out.println();
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