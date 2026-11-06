package model;
import java.time.LocalDate;

public class PerfumeProduct extends Product{

    //Atributo da classe
    private static final int MIN_DAYS_TO_EXPIRE = 60;

    // Construtor: repassa os dados para a classe Product

    public PerfumeProduct(String code, String name, double price, int quantity,
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
        if (getExpirationDate().isBefore(LocalDate.now().plusDays(MIN_DAYS_TO_EXPIRE))) {
            throw new IllegalArgumentException("Produto com validade muito próxima!");
        }
    }
}
