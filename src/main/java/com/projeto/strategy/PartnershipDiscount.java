package strategy;

import model.Customer;

public class PartnershipDiscount implements DiscountStrategy {
    @Override
    public double calculate(double subtotal, Customer customer) {
        return subtotal * 0.15;
    }
}
