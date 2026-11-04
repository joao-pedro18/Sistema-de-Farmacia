package model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import strategy.PaymentStrategy;

public class Sale {
    private List<ItemSale> items = new ArrayList<>();
    private Customer customer;
    private Employee employee;
    private double total;
    private double discount;
    private PaymentStrategy paymentMethod;
    private LocalDate date;

    public Sale(List<ItemSale> items, Customer customer, Employee employee, double total, double discount, PaymentStrategy paymentMethod, LocalDate date) {
        this.items = items;
        this.customer = customer;
        this.employee = employee;
        this.total = total;
        this.discount = discount;
        this.paymentMethod = paymentMethod;
        this.date = date;
    }

    public List<ItemSale> getItems() {
        return items;
    }

    public void setItems(List<ItemSale> items) {
        this.items = items;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public PaymentStrategy getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentStrategy paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}