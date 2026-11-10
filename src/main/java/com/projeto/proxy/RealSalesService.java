package proxy;

import model.Sale;
import service.Inventory;

public class RealSalesService implements SalesService {
    private Inventory inventory;
    private SalesHistory history;
    
    public RealSalesService(Inventory inventory, SalesHistory history) {
        this.inventory = inventory;
        this.history = history;
    }
    
    @Override
    public void finalizeSale(Sale sale) {
        // AINDA FAZENDO
    }
}