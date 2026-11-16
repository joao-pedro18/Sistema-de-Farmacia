package strategy;

public class CashPayment implements PaymentStrategy {
    private double receivedAmount;

    public CashPayment(double receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    @Override
    public void pay(double amount) {
        // Cliente entregou menos que o total: a venda não pode continuar
        long receivedCents = Math.round(receivedAmount * 100);
        long amountCents = Math.round(amount * 100);
        if (receivedCents < amountCents) {
            double missing = (amountCents - receivedCents) / 100.0;
            throw new IllegalArgumentException("Valor insuficiente. Ainda faltam R$ " + String.format("%.2f", missing));
        }

        // Cliente entregou o valor exato ou mais: calcula o troco
        double change = (receivedCents - amountCents) / 100.0;

        if (receivedCents == amountCents) {
            System.out.println("Valor total pago. Sem nenhum troco.");
        } else {
            System.out.println("Valor total pago. Troco: R$ " + String.format("%.2f", change));
        }
    }

    @Override
    public String getName() {
        return "Dinheiro";
    }
}