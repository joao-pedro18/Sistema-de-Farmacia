package factory;

import java.time.LocalDate;
import model.Product;

public abstract class ProductCreator {

    public abstract Product createProduct(String code, String name, double price, int quantity,
                                          LocalDate expirationDate, int minimumStock);
}
