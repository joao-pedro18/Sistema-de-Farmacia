package ui;

import java.util.Scanner;

import model.*;
import proxy.SalesService;
import service.CustomerService;
import service.Inventory;
import strategy.*;

public class SalesMenu {
    private SalesService salesService;
    private Inventory inventory;
    private CustomerService customerService;
    private Employee loggedEmployee;
    private Scanner scanner = new Scanner(System.in);

    // Recebe o serviço de venda (o proxy) e as demais dependências
    public SalesMenu(SalesService salesService,Inventory inventory, CustomerService customerService, Employee loggedEmployee) {
        this.salesService = salesService;
        this.inventory = inventory;
        this.customerService = customerService;
        this.loggedEmployee = loggedEmployee;
    }

    public void show() {
        Customer customer;

        // Pede o CPF até encontrar um cliente cadastrado
        while (true) {
            System.out.println("===== MENU DE VENDA =====");
            System.out.print("Cpf do cliente: ");
            String cpf = scanner.nextLine().trim();

            customer = customerService.findByCpf(cpf);

            if (cpf != null) {
                System.out.println("Cliente: " + customer.getName() + " | CPF: " + customer.getCpf() + "\n");
                break;
            } 

            System.out.println("Cliente não registrado. Vá até o menu de clientes para registrá-lo.\n");
        }

        Sale sale = new Sale(customer, loggedEmployee);

        // Adiciona os itens no carrinho
        while (true) {
            System.out.print("Código do produto: ");
            String code = scanner.nextLine().trim();

            Product product = inventory.findProduct(code);

            if (product == null) {
                System.out.println("Produto não encontrado!");
                continue;
            }

            System.out.println("Quantidade do produto: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            if (quantity <= 0) { 
                System.out.println("Quantidade inválida.");
                continue;
            }

            sale.addItem(new ItemSale(product, quantity));

            System.out.print("Adicionar outro? (s/n): ");
            String response = scanner.nextLine().trim();

            if (response.equalsIgnoreCase("n")) {
                break; // sai do loop
            }
        }

        // Verifica se algum produto exige receita
        boolean needsPrescription = false;

        for (ItemSale i : sale.getItems()) {
            if (i.getProduct().requiresPrescription()) {
                needsPrescription = true;
            }
        }

        if (needsPrescription) {
            System.out.println("Há produto(s) que exige(m) de receita médica.\n");

            System.out.print("Nome do médico: ");
            String doctor = scanner.nextLine().trim();

            System.out.print("Número da receita: ");
            String number = scanner.nextLine().trim();

            sale.setPrescription(new Prescription(number, doctor));
        }

        // O desconto é escolhido pelo tipo do cliente
        DiscountStrategy discountType;

        switch (customer.getType()) {
            case REGULAR:
                discountType = new NoDiscount();
                break;
        
            case SENIOR:
                discountType = new DiscountSeniors();
                break;

            case INSURANCE:
                discountType = new PartnershipDiscount();
                break;

            case LOYALTY:
                discountType = new LoyaltyDiscount();
                break;
        }

        sale.setDiscountStrategy(discountType);

        // Escolha da forma de pagamento (repete até a opção ser válida)
        PaymentStrategy paymentMethod = null;

        while (paymentMethod == null) {
            System.out.println("Forma de pagamento:");
            System.out.println("1 - Cartão");
            System.out.println("2 - Pix");
            System.out.println("3 - Dinheiro");
            System.out.print("Opção: ");
            int option = Integer.parseInt(scanner.nextLine().trim());

            switch (option) {
                case 1:
                    paymentMethod = new CardPayment();
                    break;
                case 2:
                    paymentMethod = new PixPayment();
                    break;
                case 3:
                    System.out.print("Dinheiro recebido: ");
                    double receivedAmount = Double.parseDouble(scanner.nextLine().trim());
                    paymentMethod = new CashPayment(receivedAmount);
                    break;
                default:
                    System.out.println("Opção inválida.\n");
            }
        }

        sale.setPaymentStrategy(paymentMethod);

        // O proxy pode negar a venda (ex.: balconista vendendo controlado)
        try {
            salesService.finalizeSale(sale);
            
            System.out.println("Venda finalizada com sucesso!");
        } catch (Exception e) {
            System.out.println("Venda não realizada: " + e.getMessage());
        }
    }
}