package ui;

import java.util.List;
import java.util.Scanner;

import model.Customer;
import model.CustomerType;
import service.CustomerService;

public class CustomerMenu {
    private CustomerService customerService;
    private Scanner scanner = new Scanner(System.in);

    // Recebe o serviço de clientes que será usado no menu

    public CustomerMenu(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Mostra o menu de clientes até o usuário escolher voltar

    public void show() {
        while (true) {
            System.out.println("===== MENU DE CLIENTES =====");
            System.out.println("1 - Cadastrar cliente");
            System.out.println("2 - Buscar cliente por CPF");
            System.out.println("3 - Listar clientes");
            System.out.println("4 - Remover cliente");
            System.out.println("0 - Voltar");

            int option = readInt("Opção: ");

            switch (option) {
                case 1:
                    registerCustomer();
                    break;
                case 2:
                    searchCustomer();
                    break;
                case 3:
                    listCustomers();
                    break;
                case 4:
                    removeCustomer();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }
    }

    // Pede os dados e cadastra o cliente

    private void registerCustomer() {
        System.out.print("Nome do cliente: ");
        String name = scanner.nextLine().trim();

        System.out.print("CPF do cliente: ");
        String cpf = scanner.nextLine().trim();

        CustomerType type = readCustomerType();

        // O serviço pode negar o cadastro

        try {
            Customer customer = customerService.registerCustomer(name, cpf, type);

            System.out.println("Cliente cadastrado com sucesso: " + customer.getName() + "\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Cliente não cadastrado: " + e.getMessage() + "\n");
        }
    }

    // Busca um cliente pelo CPF e mostra os dados dele

    private void searchCustomer() {
        System.out.print("CPF do cliente: ");
        String cpf = scanner.nextLine().trim();

        // A busca devolve null se o cliente não existir

        Customer customer = customerService.findByCpf(cpf);

        if (customer == null) {
            System.out.println("Cliente não encontrado!\n");
            return;
        }

        printCustomer(customer);
        System.out.println();
    }

    // Lista todos os clientes cadastrados

    private void listCustomers() {
        List<Customer> customers = customerService.listCustomers();

        if (customers.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.\n");
            return;
        }

        System.out.println("Clientes cadastrados:");
        for (Customer customer : customers) {
            printCustomer(customer);
        }
        System.out.println();
    }

    // Remove um cliente pelo CPF

    private void removeCustomer() {
        System.out.print("CPF do cliente: ");
        String cpf = scanner.nextLine().trim();

        // A remoção devolve true se removeu e false se não encontrou o cliente

        boolean removed = customerService.removeCustomer(cpf);

        if (removed) {
            System.out.println("Cliente removido com sucesso!\n");
        } else {
            System.out.println("Cliente não encontrado!\n");
        }
    }

    // Mostra os dados de um cliente em uma linha

    private void printCustomer(Customer customer) {
        System.out.println("- Nome: " + customer.getName()
                + " | CPF: " + customer.getCpf()
                + " | Tipo: " + typeLabel(customer.getType()));
    }

    // Traduz o tipo do cliente para o texto mostrado no terminal

    private String typeLabel(CustomerType type) {
        return switch (type) {
            case REGULAR -> "Comum";
            case SENIOR -> "Idoso";
            case INSURANCE -> "Convênio";
            case LOYALTY -> "Fidelidade";
        };
    }

    // Pede o tipo do cliente até a opção ser válida

    private CustomerType readCustomerType() {
        while (true) {
            System.out.println("Tipo do cliente:");
            System.out.println("1 - Comum");
            System.out.println("2 - Idoso");
            System.out.println("3 - Convênio");
            System.out.println("4 - Fidelidade");

            int option = readInt("Opção: ");

            switch (option) {
                case 1:
                    return CustomerType.REGULAR;
                case 2:
                    return CustomerType.SENIOR;
                case 3:
                    return CustomerType.INSURANCE;
                case 4:
                    return CustomerType.LOYALTY;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }
    }

    // Pede um número inteiro até o usuário digitar um valor válido

    private int readInt(String message) {
        while (true) {
            System.out.print(message);
            String text = scanner.nextLine().trim();

            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Digite um número inteiro.\n");
            }
        }
    }
}