package strategy;

public class PixPayment implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Realizando transferência...");
        System.out.println("Pix realizado com sucesso!");
    }

    @Override
    public String getName() {
        return "Pix";
    }
}
