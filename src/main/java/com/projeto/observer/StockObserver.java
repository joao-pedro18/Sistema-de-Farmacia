package observer;

import model.Product;

public interface StockObserver {
    public void update(Product product);
}
