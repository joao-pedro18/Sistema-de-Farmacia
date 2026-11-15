package service;

import model.Sale;
import java.util.List;
import java.util.ArrayList;

public class SalesHistory {

    //Atributos da classe

    private List<Sale> sales = new ArrayList<>();


    //Metodo get

    public List<Sale> getSales(){
        return new ArrayList<>(sales);
    }

    //Adiciona uma venda no historico de vendas

    public void addSale(Sale sale){

        if (sale == null){
            throw new IllegalArgumentException("Venda inexistente!");
        }

        sales.add(sale);
    }

    // Mostra as vendas registradas no terminal

    public void printSales(){

        if (sales.isEmpty()){
            System.out.println("Nenhuma venda registrada.");
            return;
        }

        System.out.println("Histórico de vendas:");
        for (Sale sale : sales){
            System.out.println("- Data: " + sale.getDate()
                    + " | Cliente: " + sale.getCustomer().getName()
                    + " | Funcionário: " + sale.getEmployee().getName()
                    + " | Desconto: " + sale.getDiscount()
                    + " | Pagamento: " + sale.getPaymentMethod()
                    + " | Total: " + sale.getTotal());
        }
    }


}
