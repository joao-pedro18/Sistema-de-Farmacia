package observer;

import model.Product;

public class LowStockAlert implements StockObserver{

    @Override
    public void update(Product product) {

        if (product.getQuantity() < product.getMininumStock()){
            System.out.println("Aviso: estoque baixo do produto " + product.getName()
                    + "! Quantidade atual: " + product.getQuantity()
                    + " (mínimo: " + product.getMininumStock() + ")");
        }
    }
}
