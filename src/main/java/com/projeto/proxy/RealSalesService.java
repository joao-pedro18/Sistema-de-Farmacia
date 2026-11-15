package proxy;

import model.ItemSale;
import model.Sale;
import service.Inventory;
import service.SalesHistory;

public class RealSalesService implements SalesService {
    private Inventory inventory;
    private SalesHistory history;
    
    public RealSalesService(Inventory inventory, SalesHistory history) {
        this.inventory = inventory;
        this.history = history;
    }
    
    @Override
    public void finalizeSale(Sale sale) {
        // Calcula e guarda o desconto da venda
        sale.applyDiscount();

        // Cobra usando a forma de pagamento escolhida
        sale.pay();

        // Dá baixa no estoque item por item (quantidade vendida, não a do estoque)
        for (ItemSale item : sale.getItems()) {
            inventory.decreaseStock(item.getProduct().getCode(), item.getAmount());
        }

        // Envia a venda finalizada ao histórico
        history.addSale(sale);
    }
}