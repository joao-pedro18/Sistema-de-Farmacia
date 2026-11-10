package observer;

import model.Product;

import java.util.ArrayList;
import java.util.List;

public class RestockOrder implements StockObserver {

    // Atributos da classe

    private List<Product> productsToRestock = new ArrayList<>();

    // Registra um pedido de reposição quando o estoque fica abaixo do mínimo

    @Override
    public void update(Product product) {

        if (product.getQuantity() < product.getMininumStock()){
            if (!productsToRestock.contains(product)){
                productsToRestock.add(product);
                System.out.println("Pedido de reposição registrado para o produto " + product.getName());
            }
        }
    }

    // Devolve uma cópia da lista de produtos com pedido de reposição

    public List<Product> getOrders() {
        return new ArrayList<>(productsToRestock);
    }

    // Mostra os pedidos de reposição no terminal

    public void printOrders() {

        if (productsToRestock.isEmpty()){
            System.out.println("Nenhum pedido de reposição registrado.");
            return;
        }

        System.out.println("Pedidos de reposição:");
        for (Product product : productsToRestock){
            System.out.println("- " + product.getName()
                    + " | Quantidade atual: " + product.getQuantity()
                    + " | Mínimo: " + product.getMininumStock());
        }
    }
}