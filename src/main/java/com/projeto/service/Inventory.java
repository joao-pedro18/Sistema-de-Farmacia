package service;

import model.Product;
import observer.StockObserver;

import java.util.List;
import java.util.ArrayList;

public class Inventory {

    //Atributos da classe

    private List<Product> products = new ArrayList<>();
    private List<StockObserver> observers = new ArrayList<>();

    // Procura um produto pelo código. Devolve null se não existir

    public Product findProduct(String code) {

        for (Product product : products){
            if (product.getCode().equals(code)){
                return product;
            }
        }
        return null;
    }

    // Dá baixa no estoque do produto (usado na finalização da venda)

    public void decreaseStock(String code, int quantity) {

        Product product = findProduct(code);

        if (product == null){
            throw new IllegalArgumentException("Produto não encontrado!");
        }
        if (quantity <= 0){
            throw new IllegalArgumentException("Quantidade inválida!");
        }
        if (quantity > product.getQuantity()){
            throw new IllegalArgumentException("Estoque insuficiente!");
        }
        else {
            product.setQuantity(product.getQuantity() - quantity);
            notifyObservers(product);
        }
    }

    //Adiciona um produto ao estoque. Não permite código repetido

    public void addProduct(Product product){
        Product existing = findProduct(product.getCode());

        if (existing != null ){
            throw new IllegalArgumentException("Produto já cadastrado!");
        }
        else {
            products.add(product);
        }
    }

    //Devolve uma cópia da lista de produtos do estoque

    public List<Product> listProducts(){
        return new ArrayList<>(products);
    }


    //Remove um produto do estoque, só se o produto existir
    public boolean removeProduct(String code){
        Product existing = findProduct(code);

        if (existing == null){
            return false;
        }
        products.remove(existing);
        return true;
    }

    //Adiciona quantidade a produtos já cadastrados do estoque

    public void increaseStock(String code, int quantity){

        Product product = findProduct(code);

            if (product == null){
                throw new IllegalArgumentException("Produto não encontrado!");
        }
            if (quantity <= 0){
                throw new IllegalArgumentException("Quantidade inválida!");
            }
            else {
                product.setQuantity(product.getQuantity() + quantity);
                notifyObservers(product);
            }
    }

    // Inscreve um observador para receber avisos quando o estoque mudar

    public void addObserver(StockObserver observer){
        observers.add(observer);
    }

    // Cancela a inscrição de um observador

    public void removeObserver(StockObserver observer){
        observers.remove(observer);
    }

    // Avisa todos os observadores inscritos que o produto mudou

    private void notifyObservers(Product product){
        for (StockObserver observer : observers){
            observer.update(product);
        }
    }
}

