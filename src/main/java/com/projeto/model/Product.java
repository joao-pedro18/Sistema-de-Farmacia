package model;

import java.time.LocalDate;

public abstract class Product {
    //Atributos da classe

    private String code;
    private String name;
    private double price;
    private int quantity;
    private LocalDate expirationDate;
    private int mininumStock;

    //Construtor da classe

    public Product(String code, String name, double price, int quantity, LocalDate expirationDate, int mininumStock){
        this.code = code;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.mininumStock = mininumStock;
    }

    //Metodos getters e setters da classe

    public String getCode(){
        return code;
    }
    public String getName(){
        return name;
    }
    public double getPrice(){
        return price;
    }
    public int getQuantity(){
        return quantity;
    }
    public LocalDate getExpirationDate(){
        return expirationDate;
    }
    public int getMininumStock(){
        return mininumStock;
    }

    public void setQuantity(int quantity){
        this.quantity = quantity;
    }


    //Informa se o produto só pode ser vendido com receita

    public abstract boolean requiresPrescription();


    //Confere se a venda é permitida (quantidade e receita etc); se não for, lança uma exceção com o motivo.
    public abstract void validateSale(int quantity, Prescription prescription);

}
