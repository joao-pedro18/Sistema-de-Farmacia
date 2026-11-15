package strategy;

public class CashPayment implements PaymentStrategy {
    private double receivedAmount;

    public CashPayment(double receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    @Override
    public void pay(double amount) {
        // Cliente entregou menos que o total: a venda não pode continuar
        if (receivedAmount < amount) {
            double missing = amount - receivedAmount;
            throw new IllegalArgumentException("Valor insuficiente. Ainda faltam R$ " + String.format("%.2f", missing));
        }

        // Cliente entregou o valor exato ou mais: calcula o troco
        double change = receivedAmount - amount;

        if (change == 0) {
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