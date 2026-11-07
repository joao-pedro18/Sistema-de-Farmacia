package factory;

import model.Product;
import model.RegularProduct;

import java.time.LocalDate;

public class RegularProductCreator extends ProductCreator{

    @Override
    public Product createProduct(String code, String name, double price, int quantity, LocalDate expirationDate, int minimumStock) {
        return new RegularProduct(code, name, price, quantity, expirationDate, minimumStock);
    }
}
