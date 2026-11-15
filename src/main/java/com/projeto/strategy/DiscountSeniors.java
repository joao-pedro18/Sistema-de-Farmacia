package strategy;

import model.Customer;

public class DiscountSeniors implements DiscountStrategy {
    @Override 
    public double calculate(double subtotal, Customer customer) {
        return subtotal * 0.1;
    }
}
