package tests;

import static tests.TestKit.*;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.List;

import factory.ControlledProductCreator;
import factory.PerfumeProductCreator;
import factory.ProductCreator;
import factory.RegularProductCreator;
import model.ControlledProduct;
import model.Customer;
import model.CustomerType;
import model.PerfumeProduct;
import model.Prescription;
import model.Product;
import model.ProductType;
import model.RegularProduct;
import service.Inventory;
import service.ProductService;

/** Pessoa 1: produtos (model), fábricas (Factory Method) e ProductService. */
public class ProductTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        regularProduct();
        controlledProduct();
        perfumeProduct();
        basicModel();
        factories();
        productService();
    }

    private static final Prescription RX = new Prescription("123", "Dr. Silva");

    // ------------------------------------------------------------------
    private static void regularProduct() {
        suite("Produto comum (RegularProduct)");

        test("não exige receita", () -> isFalse("requiresPrescription", regular("R", 5, 10, 2).requiresPrescription()));

        test("guarda os dados do construtor", () -> {
            LocalDate d = inDays(100);
            Product p = new RegularProduct("R1", "Dipirona", 8.5, 20, d, 5);
            eq("code", "R1", p.getCode());
            eq("name", "Dipirona", p.getName());
            near("price", 8.5, p.getPrice());
            eq("quantity", 20, p.getQuantity());
            eq("expirationDate", d, p.getExpirationDate());
            eq("minimumStock", 5, minStock(p));
        });

        test("setQuantity atualiza a quantidade", () -> {
            Product p = regular("R", 5, 10, 2);
            p.setQuantity(3);
            eq("quantity", 3, p.getQuantity());
        });

        test("venda normal é permitida", () -> regular("R", 5, 10, 2).validateSale(3, null));

        test("vender exatamente o que há em estoque é permitido", () -> regular("R", 5, 10, 2).validateSale(10, null));

        test("quantidade zero é barrada", () ->
                assertThrows(IllegalArgumentException.class, "inválid", () -> regular("R", 5, 10, 2).validateSale(0, null)));

        test("quantidade negativa é barrada", () ->
                assertThrows(IllegalArgumentException.class, "inválid", () -> regular("R", 5, 10, 2).validateSale(-1, null)));

        test("quantidade acima do estoque é barrada", () ->
                assertThrows(IllegalArgumentException.class, "estoque", () -> regular("R", 5, 10, 2).validateSale(11, null)));

        test("produto vencido é barrado", () -> {
            Product p = new RegularProduct("R", "Vencido", 5, 10, inDays(-1), 2);
            assertThrows(IllegalArgumentException.class, "vencido", () -> p.validateSale(1, null));
        });

        test("produto que vence hoje ainda pode ser vendido", () -> {
            Product p = new RegularProduct("R", "Vence hoje", 5, 10, inDays(0), 2);
            p.validateSale(1, null);
        });

        test("ter receita na mão não atrapalha a venda de produto comum", () -> regular("R", 5, 10, 2).validateSale(1, RX));
    }

    // ------------------------------------------------------------------
    private static void controlledProduct() {
        suite("Medicamento controlado (ControlledProduct)");

        test("exige receita", () -> isTrue("requiresPrescription", controlled("C", 20, 10, 2).requiresPrescription()));

        test("venda com receita é permitida", () -> controlled("C", 20, 10, 2).validateSale(1, RX));

        test("vender 2 unidades (limite) com receita é permitido", () -> controlled("C", 20, 10, 2).validateSale(2, RX));

        test("sem receita é barrado", () ->
                assertThrows(IllegalArgumentException.class, "receita", () -> controlled("C", 20, 10, 2).validateSale(1, null)));

        test("3 unidades passa do limite por venda", () ->
                assertThrows(IllegalArgumentException.class, "limite", () -> controlled("C", 20, 10, 2).validateSale(3, RX)));

        test("quantidade zero é barrada", () ->
                assertThrows(IllegalArgumentException.class, "inválid", () -> controlled("C", 20, 10, 2).validateSale(0, RX)));

        test("quantidade negativa é barrada", () ->
                assertThrows(IllegalArgumentException.class, "inválid", () -> controlled("C", 20, 10, 2).validateSale(-1, RX)));

        test("quantidade acima do estoque é barrada (mesmo dentro do limite)", () ->
                assertThrows(IllegalArgumentException.class, "estoque", () -> controlled("C", 20, 1, 1).validateSale(2, RX)));

        test("controlado vencido é barrado", () -> {
            Product p = new ControlledProduct("C", "Vencido", 20, 10, inDays(-1), 2);
            assertThrows(IllegalArgumentException.class, "vencido", () -> p.validateSale(1, RX));
        });
    }

    // ------------------------------------------------------------------
    private static void perfumeProduct() {
        suite("Perfumaria (PerfumeProduct)");

        test("não exige receita", () -> isFalse("requiresPrescription", perfume("P", 30, 10, 2).requiresPrescription()));

        test("venda normal é permitida (validade longa)", () -> perfume("P", 30, 10, 2).validateSale(2, null));

        test("quantidade zero é barrada", () ->
                assertThrows(IllegalArgumentException.class, "inválid", () -> perfume("P", 30, 10, 2).validateSale(0, null)));

        test("quantidade acima do estoque é barrada", () ->
                assertThrows(IllegalArgumentException.class, "estoque", () -> perfume("P", 30, 10, 2).validateSale(11, null)));

        test("produto vencido é barrado", () -> {
            Product p = new PerfumeProduct("P", "Vencido", 30, 10, inDays(-1), 2);
            assertThrows(IllegalArgumentException.class, "vencido", () -> p.validateSale(1, null));
        });

        test("validade em 59 dias é barrada (regra de 60 dias)", () -> {
            Product p = new PerfumeProduct("P", "Quase vencendo", 30, 10, inDays(59), 2);
            assertThrows(IllegalArgumentException.class, "validade", () -> p.validateSale(1, null));
        });

        test("validade em 61 dias é permitida", () -> {
            Product p = new PerfumeProduct("P", "Ok", 30, 10, inDays(61), 2);
            p.validateSale(1, null);
        });
    }

    // ------------------------------------------------------------------
    private static void basicModel() {
        suite("Modelos básicos (Product, Customer, Prescription, enums)");

        test("Product é abstrata (não pode ser instanciada direto)", () ->
                isTrue("Product abstract", Modifier.isAbstract(Product.class.getModifiers())));

        test("ProductCreator é abstrata", () ->
                isTrue("ProductCreator abstract", Modifier.isAbstract(ProductCreator.class.getModifiers())));

        test("os 3 tipos concretos são Product", () -> {
            isTrue("Regular", regular("R", 1, 1, 1) instanceof Product);
            isTrue("Controlled", controlled("C", 1, 1, 1) instanceof Product);
            isTrue("Perfume", perfume("P", 1, 1, 1) instanceof Product);
        });

        test("ProductType tem REGULAR, CONTROLLED e PERFUME", () -> {
            eq("quantidade de valores", 3, ProductType.values().length);
            notNull("REGULAR", ProductType.valueOf("REGULAR"));
            notNull("CONTROLLED", ProductType.valueOf("CONTROLLED"));
            notNull("PERFUME", ProductType.valueOf("PERFUME"));
        });

        test("CustomerType tem REGULAR, SENIOR, INSURANCE e LOYALTY", () -> {
            eq("quantidade de valores", 4, CustomerType.values().length);
            notNull("REGULAR", CustomerType.valueOf("REGULAR"));
            notNull("SENIOR", CustomerType.valueOf("SENIOR"));
            notNull("INSURANCE", CustomerType.valueOf("INSURANCE"));
            notNull("LOYALTY", CustomerType.valueOf("LOYALTY"));
        });

        test("Customer guarda nome, cpf e tipo", () -> {
            Customer c = new Customer("Maria", "123", CustomerType.SENIOR);
            eq("name", "Maria", c.getName());
            eq("cpf", "123", c.getCpf());
            eq("type", CustomerType.SENIOR, c.getType());
        });

        test("Prescription guarda número e médico", () -> {
            Prescription p = new Prescription("987", "Dra. Ana");
            eq("number", "987", p.getNumber());
            eq("doctor", "Dra. Ana", p.getDoctor());
        });
    }

    // ------------------------------------------------------------------
    private static void factories() {
        suite("Factory Method (criadores de produto)");

        test("RegularProductCreator cria RegularProduct com os dados certos", () -> {
            LocalDate d = inDays(90);
            Product p = new RegularProductCreator().createProduct("1", "Dipirona", 8.5, 20, d, 5);
            eq("classe", RegularProduct.class, p.getClass());
            eq("code", "1", p.getCode());
            eq("name", "Dipirona", p.getName());
            near("price", 8.5, p.getPrice());
            eq("quantity", 20, p.getQuantity());
            eq("expirationDate", d, p.getExpirationDate());
            eq("minimumStock", 5, minStock(p));
        });

        test("ControlledProductCreator cria ControlledProduct", () -> {
            Product p = new ControlledProductCreator().createProduct("2", "Clonazepam", 25, 10, inDays(90), 3);
            eq("classe", ControlledProduct.class, p.getClass());
            isTrue("exige receita", p.requiresPrescription());
            eq("name", "Clonazepam", p.getName());
        });

        test("PerfumeProductCreator cria PerfumeProduct", () -> {
            Product p = new PerfumeProductCreator().createProduct("3", "Hidratante", 32.9, 15, inDays(90), 4);
            eq("classe", PerfumeProduct.class, p.getClass());
            isFalse("não exige receita", p.requiresPrescription());
            eq("name", "Hidratante", p.getName());
        });

        test("cada chamada cria um objeto novo", () -> {
            ProductCreator c = new RegularProductCreator();
            Product a = c.createProduct("1", "A", 1, 1, inDays(30), 1);
            Product b = c.createProduct("1", "A", 1, 1, inDays(30), 1);
            isTrue("instâncias diferentes", a != b);
        });

        test("o produto criado respeita as regras do próprio tipo", () -> {
            ProductCreator controlled = new ControlledProductCreator();
            Product p = controlled.createProduct("2", "Clonazepam", 25, 10, inDays(90), 3);
            assertThrows(IllegalArgumentException.class, "receita", () -> p.validateSale(1, null));
        });
    }

    // ------------------------------------------------------------------
    private static void productService() {
        suite("ProductService");

        test("cadastra um produto de cada tipo com a classe certa", () -> {
            ProductService s = new ProductService(new Inventory());
            Product r = s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            Product c = s.registerProduct(ProductType.CONTROLLED, "2", "B", 1, 1, inDays(30), 1);
            Product p = s.registerProduct(ProductType.PERFUME, "3", "C", 1, 1, inDays(30), 1);
            eq("REGULAR", RegularProduct.class, r.getClass());
            eq("CONTROLLED", ControlledProduct.class, c.getClass());
            eq("PERFUME", PerfumeProduct.class, p.getClass());
        });

        test("o produto cadastrado vai para o estoque compartilhado", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            Product p = s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            same("mesma instância no estoque", p, inv.findProduct("1"));
        });

        test("os dados do cadastro chegam corretos ao produto", () -> {
            LocalDate d = inDays(77);
            Product p = new ProductService(new Inventory())
                    .registerProduct(ProductType.REGULAR, "9", "Xarope", 15.75, 12, d, 4);
            eq("code", "9", p.getCode());
            eq("name", "Xarope", p.getName());
            near("price", 15.75, p.getPrice());
            eq("quantity", 12, p.getQuantity());
            eq("expirationDate", d, p.getExpirationDate());
            eq("minimumStock", 4, minStock(p));
        });

        test("código repetido é recusado e não duplica no estoque", () -> {
            ProductService s = new ProductService(new Inventory());
            s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            assertThrows(IllegalArgumentException.class, "cadastrado",
                    () -> s.registerProduct(ProductType.REGULAR, "1", "Outro", 2, 2, inDays(30), 1));
            eq("tamanho da lista", 1, s.listProducts().size());
        });

        test("código repetido é recusado mesmo com tipo diferente", () -> {
            ProductService s = new ProductService(new Inventory());
            s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            assertThrows(IllegalArgumentException.class, "cadastrado",
                    () -> s.registerProduct(ProductType.PERFUME, "1", "B", 1, 1, inDays(90), 1));
        });

        test("tipo nulo é recusado", () -> {
            ProductService s = new ProductService(new Inventory());
            assertThrows(IllegalArgumentException.class, "tipo",
                    () -> s.registerProduct(null, "1", "A", 1, 1, inDays(30), 1));
            eq("nada foi cadastrado", 0, s.listProducts().size());
        });

        test("searchProduct encontra pelo código e devolve null se não existir", () -> {
            ProductService s = new ProductService(new Inventory());
            s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            eq("achou", "A", s.searchProduct("1").getName());
            isNull("não achou", s.searchProduct("999"));
        });

        test("removeProduct remove e informa se removeu", () -> {
            ProductService s = new ProductService(new Inventory());
            s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            isTrue("removeu", s.removeProduct("1"));
            isFalse("de novo não acha", s.removeProduct("1"));
            isNull("não está mais", s.searchProduct("1"));
        });

        test("listProducts lista todos e devolve uma cópia", () -> {
            ProductService s = new ProductService(new Inventory());
            s.registerProduct(ProductType.REGULAR, "1", "A", 1, 1, inDays(30), 1);
            s.registerProduct(ProductType.CONTROLLED, "2", "B", 1, 1, inDays(30), 1);
            List<Product> list = s.listProducts();
            eq("tamanho", 2, list.size());
            list.clear();
            eq("a lista interna não muda", 2, s.listProducts().size());
        });

        test("o serviço e o estoque enxergam os mesmos produtos nos dois sentidos", () -> {
            Inventory inv = new Inventory();
            ProductService s = new ProductService(inv);
            inv.addProduct(regular("X", 1, 1, 1));
            notNull("serviço enxerga o que entrou direto no estoque", s.searchProduct("X"));
            s.removeProduct("X");
            isNull("estoque enxerga a remoção do serviço", inv.findProduct("X"));
        });
    }
}
