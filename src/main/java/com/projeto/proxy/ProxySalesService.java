package proxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import model.*;

// Proxy de proteção: valida o acesso antes de delegar ao serviço real
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
        // Primeiro confere o perfil: controlado só pode ser vendido pelo farmacêutico
        for (ItemSale item : sale.getItems()) {
            if (item.getProduct().requiresPrescription()
                    && loggedEmployee.getProfile() != Profile.PHARMACEUTICAL) {
                String message = "Apenas o farmacêutico pode vender medicamento controlado.";

                // Registra a tentativa negada para mostrar na apresentação
                deniedAttempts.add(loggedEmployee.getName() + " tentou vender " + item.getProduct().getName() + ": " + message);

                throw new IllegalStateException(message);
            }
        }

        // Depois valida as regras de cada produto (quantidade, receita)
        Map<Product, Integer> totals = new LinkedHashMap<>();
        for (ItemSale item : sale.getItems()) {
            totals.merge(item.getProduct(), item.getAmount(), Integer::sum);
        }
        for (Map.Entry<Product, Integer> entry : totals.entrySet()) {
            entry.getKey().validateSale(entry.getValue(), sale.getPrescription());
        }

        // Tudo certo: delega ao serviço real
        realService.finalizeSale(sale);
    }

    public List<String> getDeniedAttempts() {
        return deniedAttempts;
    }
}
