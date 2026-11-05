package model;

public class Customer {
    //Atributos da classe

    private String name;
    private String cpf;
    private CustomerType type;

    //Construtor da classe

    public Customer(String name, String cpf, CustomerType type){
        this.name = name;
        this.cpf = cpf;
        this.type = type;
    }

    //Metodos getters da classe

    public String getName(){
        return name;
    }
    public String getCpf(){
        return cpf;
    }
    public CustomerType getType(){
        return type;
    }

}
