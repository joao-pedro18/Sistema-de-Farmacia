package service;

import java.util.ArrayList;
import java.util.List;

import model.Sale;

// Versão provisória: a Pessoa 1 vai substituir por a classe completa
public class SalesHistory {
    private List<Sale> sales = new ArrayList<>();

    public void addSale(Sale sale) {
        sales.add(sale);
    }

    public List<Sale> getSales() {
        return sales;
    }
}