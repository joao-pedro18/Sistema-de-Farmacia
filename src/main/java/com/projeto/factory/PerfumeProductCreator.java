package factory;

import model.PerfumeProduct;
import model.Product;

import java.time.LocalDate;

public class PerfumeProductCreator extends ProductCreator{

    @Override
    public Product createProduct(String code, String name, double price, int quantity, LocalDate expirationDate, int minimumStock) {
        return new PerfumeProduct(code, name, price, quantity, expirationDate, minimumStock);
    }
}
