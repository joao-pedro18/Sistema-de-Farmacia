package factory;

import model.Product;
import model.ControlledProduct;

import java.time.LocalDate;

public class ControlledProductCreator extends ProductCreator{

    @Override
    public Product createProduct(String code, String name, double price, int quantity, LocalDate expirationDate, int minimumStock) {
        return new ControlledProduct(code, name, price, quantity, expirationDate, minimumStock);
    }
}
