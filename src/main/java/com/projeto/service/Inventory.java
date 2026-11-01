package service;

import model.Product;

public class Inventory {

    // Procura um produto pelo código. Devolve null se não existir

    public Product findProduct(String code) {
        return null;
    }

    // Dá baixa no estoque do produto (usado na finalização da venda)

    public void decreaseStock(String code, int quantity) {
        throw new UnsupportedOperationException("Ainda não implementado");
    }
}

