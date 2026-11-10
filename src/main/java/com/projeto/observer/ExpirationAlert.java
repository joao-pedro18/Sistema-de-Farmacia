package observer;

import model.Product;
import java.time.LocalDate;

public class ExpirationAlert implements StockObserver{

    //Atributos da classe

    private static final int DAYS_TO_WARN = 60;


    // Avisa se o produto está vencido ou perto de vencer

    @Override
    public void update(Product product) {

        if (product.getExpirationDate().isBefore(LocalDate.now())){
            System.out.println("Aviso: o produto " + product.getName()
                    + " está vencido! Validade: " + product.getExpirationDate());
        }
        else if (product.getExpirationDate().isBefore(LocalDate.now().plusDays(DAYS_TO_WARN))){
            System.out.println("Aviso: o produto " + product.getName()
                    + " está perto de vencer! Validade: " + product.getExpirationDate());
        }
    }
}
