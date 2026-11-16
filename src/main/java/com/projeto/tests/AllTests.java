package tests;

/**
 * Roda TODOS os testes do sistema da farmácia.
 *
 * Como rodar (na pasta que contém model, service, ui, tests...):
 *   javac -encoding UTF-8 -d out (todos os .java)
 *   java -cp out tests.AllTests
 * Ou no VS Code: abrir este arquivo e clicar em "Run" acima do main.
 */
public class AllTests {

    public static void main(String[] args) {
        System.out.println("TESTES DO SISTEMA DE FARMÁCIA");

        // Pessoa 1
        ProductTests.run();
        InventoryObserverTests.run();
        CustomerHistoryTests.run();

        // Pessoa 2
        StrategyTests.run();
        SaleTests.run();
        ProxyTests.run();

        // Menus (os dois) e integração
        MenuTests.run();
        MainMenuTests.run();
        IntegrationTests.run();

        boolean allOk = TestKit.finish();
        System.exit(allOk ? 0 : 1);
    }
}
