package strategy;

public class CardPayment implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Analisando pagamento...");
        System.out.println("Pagamento aprovado com sucesso!");
    }

    @Override
    public String getName() {
        return "Cartão";
    }
}