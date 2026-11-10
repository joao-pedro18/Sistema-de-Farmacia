package proxy;

import java.util.ArrayList;
import java.util.List;

import model.Employee;
import model.Sale;

public class ProxySalesService implements SalesService {
    private SalesService realService;
    private Employee loggedEmployee;
    private List<String> deniedAttempts = new ArrayList<>();

    public ProxySalesService(SalesService realService, Employee loggedEmployee) {
        this.realService = realService;
        this.loggedEmployee = loggedEmployee;
    }

    @Override
    public void finalizeSale(Sale sale) {
        // AINDA FAZENDO
    }

    public List<String> getDeniedAttempts() {
        return deniedAttempts;
    }
}
