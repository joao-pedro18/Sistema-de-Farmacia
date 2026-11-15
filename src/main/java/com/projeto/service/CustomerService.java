package service;

import model.Customer;
import model.CustomerType;
import java.util.List;
import java.util.ArrayList;

public class CustomerService {

    //Atributos da classe

    private List<Customer> customers = new ArrayList<>();

    //Procura um cliente pelo cpf. Devolve null se não existir

    public Customer findByCpf(String cpf) {

        for (Customer customer : customers){
            if (customer.getCpf().equals(cpf)){
                return customer;
            }
        }
        return null;
    }

    //Cadastra um cliente novo. Não permite CPF repetido

    public Customer registerCustomer(String name, String cpf, CustomerType type){

        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Nome do cliente não informado!");
        }
        if (cpf == null || cpf.isBlank()){
            throw new IllegalArgumentException("Cpf do cliente não informado!");
        }
        if (type == null){
            throw new IllegalArgumentException("Tipo do cliente não informado!");
        }

        Customer existing = findByCpf(cpf);

        if (existing != null){
            throw new IllegalArgumentException("Cliente já cadastrado no sistema!");
        }

        Customer newCustomer = new Customer(name, cpf, type);

        customers.add(newCustomer);

        return newCustomer;
    }

    //Remove um cliente do sistema, só se o cliente existir

    public boolean removeCustomer(String cpf){
        Customer existing = findByCpf(cpf);

        if (existing == null){
            return false;
        }
        customers.remove(existing);
        return true;
    }

    //Devolve uma cópia da lista de clientes do sistema

    public List<Customer> listCustomers(){
        return new ArrayList<>(customers);
    }

}
