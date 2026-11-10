package model;

public class ItemSale {
    private Product product;
    private int amount;

    public ItemSale(Product product, int amount) {
        this.product = product;
        this.amount = amount;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public double getSubtotal() {
        return product.getPrice() * amount;
    }
}