package model;
import java.time.LocalDate;

public class ControlledProduct extends Product{

    private static final int MAX_QUANTITY = 2;

    // Construtor: repassa os dados para a classe Product

    public ControlledProduct(String code, String name, double price, int quantity,
                          LocalDate expirationDate, int minimumStock) {
        super(code, name, price, quantity, expirationDate, minimumStock);
    }
    @Override
    public boolean requiresPrescription() {
        return true;
    }

    @Override
    public void validateSale(int quantity, Prescription prescription) {

        if (prescription == null){
            throw new IllegalArgumentException("Produto sem receita!");
        }
        if (quantity <= 0){
            throw new IllegalArgumentException("Quantidade inválida!");
        }
        if (quantity > getQuantity()){
            throw new IllegalArgumentException("Quantidade indisponível no estoque!");
        }
        if (getExpirationDate().isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Produto Vencido!");
        }
        if (quantity > MAX_QUANTITY){
            throw new IllegalArgumentException("Venda excede o limite permitido!");
        }
    }
}
