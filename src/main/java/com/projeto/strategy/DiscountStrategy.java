package strategy;

import model.Customer;

public interface DiscountStrategy {
    double calculate(double subtotal, Customer customer);
}