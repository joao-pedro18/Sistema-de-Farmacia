package strategy;

import model.Customer;

public class NoDiscount implements DiscountStrategy {
    @Override
    public double calculate(double subtotal, Customer customer) {
        return 0;
    }
}
