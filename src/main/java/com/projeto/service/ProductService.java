package service;

import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.Product;
import model.ProductType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductService {

    //Atributos da classe

    private Inventory inventory;
    private Map<ProductType, ProductCreator> creators;

    //Construtor da classe

    public ProductService(Inventory inventory){
        this.inventory = inventory;
        this.creators = new HashMap<>();

        creators.put(ProductType.REGULAR, new RegularProductCreator());
        creators.put(ProductType.CONTROLLED, new ControlledProductCreator());
        creators.put(ProductType.PERFUME, new PerfumeProductCreator());
    }

    // Cadastra um produto do tipo informado e adiciona ao estoque

    public Product registerProduct(ProductType type, String code, String name, double price, int quantity, LocalDate expirationDate, int minimumStock){

        ProductCreator creator = creators.get(type);

        if (creator == null){
            throw new IllegalArgumentException("Tipo de produto inválido!");
        }

        Product product = creator.createProduct(code, name, price, quantity, expirationDate, minimumStock);

        inventory.addProduct(product);

        return product;
    }

    // Procura um produto pelo código

    public Product searchProduct(String code){
        return inventory.findProduct(code);
    }

    // Remove um produto pelo código

    public boolean removeProduct(String code){
        return inventory.removeProduct(code);
    }

    // Devolve a lista de produtos cadastrados

    public List<Product> listProducts(){
        return inventory.listProducts();
    }

}
