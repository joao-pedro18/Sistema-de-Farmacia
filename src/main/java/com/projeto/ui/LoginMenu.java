package ui;

import java.util.Scanner;

import model.Employee;
import service.EmployeeService;

public class LoginMenu {
    private EmployeeService employeeService;
    private Scanner scanner = new Scanner(System.in);

    // Recebe o serviço de funcionários que será usado no login
    public LoginMenu(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    // Pede o nome até encontrar um funcionário válido e devolve ele
    public Employee show() {
        while (true) {
            System.out.println("===== MENU DE LOGIN =====");
            System.out.print("Nome do funcionário: ");
            String name = scanner.nextLine().trim();

            // O login devolve null se o funcionário não existir
            Employee employee = employeeService.login(name);

            if (employee != null) {
                System.out.println("Bem-vindo, " + employee.getName() + "!\n");
                return employee;
            }

            System.out.println("Erro: Funcionário não encontrado. Tente novamente.\n");
        }
    }
}
