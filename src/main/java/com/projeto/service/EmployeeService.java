package service;

import java.util.ArrayList;
import java.util.List;

import model.Employee;
import model.Profile;

public class EmployeeService {
    private List<Employee> employees = new ArrayList<>();

    public void registerEmployee(String name, Profile profile) {
        employees.add(new Employee(name, profile));
    }

    public Employee login(String name) {
        Employee employee = null;

        for (Employee e : employees) {
            if (name.equals(e.getName())) {
                employee = e;
            }
        }

        return employee;
    }

    public List<Employee> listEmployees() {
        return employees;
    }
}