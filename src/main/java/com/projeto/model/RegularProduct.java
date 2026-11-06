package model;
import java.time.LocalDate;

public class RegularProduct extends Product{


    // Construtor: repassa os dados para a classe Product

    public RegularProduct(String code, String name, double price, int quantity,
                          LocalDate expirationDate, int minimumStock) {
        super(code, name, price, quantity, expirationDate, minimumStock);
    }

    @Override
    public boolean requiresPrescription() {
        return false;
    }

    @Override
    public void validateSale(int quantity, Prescription prescription) {

        if (quantity <= 0){
            throw new IllegalArgumentException("Quantidade inválida!");
        }
        if (quantity > getQuantity()){
            throw new IllegalArgumentException("Quantidade indisponível no estoque!");
        }
        if (getExpirationDate().isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Produto Vencido!");
        }
    }
}
