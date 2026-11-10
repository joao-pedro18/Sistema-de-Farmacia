package model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import strategy.*;

public class Sale {
    // ATRIBUTOS DA CLASSE
    private List<ItemSale> items = new ArrayList<>();
    private Customer customer;
    private Employee employee;
    private Prescription prescription;
    private DiscountStrategy discountStrategy;
    private PaymentStrategy paymentStrategy;
    private double discount;    
    private LocalDateTime date;

    // CONSTRUTOR
    public Sale(Customer customer, Employee employee) {
        this.customer = customer;
        this.employee = employee;
        this.date = LocalDateTime.now();
    }

    // GETTERS E SETTERS 
    public List<ItemSale> getItems() {
        return items;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Employee getEmployee() {
        return employee;
    }

    public double getDiscount() {
        return discount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getPaymentMethod() {
        return paymentStrategy.getName();
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    // OUTROS MÉTODOS
    public void addItem(ItemSale item) {
        items.add(item);
    }

    public boolean removeItem(String productCode) {
        boolean remove = false;
        // Percorre o carrinho e remove o item cujo produto tem o código informado
        for (ItemSale item : items) {
            if (item.getProduct().getCode().equals(productCode)) {
                // Removeu, então já pode sair (evita erro de modificar a lista no for)
                items.remove(item);
                remove = true; 
                break;
            }
        }

        return remove; 
    }

    public double calculateSubtotal() {
        double sumSubtotals = 0;

        for (ItemSale item : items) {
            sumSubtotals += item.getSubtotal();
        }

        return sumSubtotals; 
    }

    public void applyDiscount() {
        discount = discountStrategy.calculate(calculateSubtotal(), customer);
    }

    public void pay() {
        paymentStrategy.pay(getTotal());
    }

    public double getTotal() {
        return calculateSubtotal() - discount;
    }
}