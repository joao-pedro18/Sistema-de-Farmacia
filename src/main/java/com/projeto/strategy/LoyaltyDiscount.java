package strategy;

import model.Customer;

public class LoyaltyDiscount implements DiscountStrategy {
    @Override
    public double calculate(double subtotal, Customer customer) {
        return subtotal * 0.05;
    }
}
