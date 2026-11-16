package tests;

import static tests.TestKit.*;

import java.util.ArrayList;
import java.util.List;

import model.PerfumeProduct;
import model.Product;
import model.RegularProduct;
import observer.ExpirationAlert;
import observer.LowStockAlert;
import observer.RestockOrder;
import observer.StockObserver;
import service.Inventory;

/** Pessoa 1: Inventory (Subject) e os observadores LowStockAlert, ExpirationAlert e RestockOrder. */
public class InventoryObserverTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        inventoryBasics();
        inventoryStock();
        inventoryObservers();
        lowStockAlert();
        expirationAlert();
        restockOrder();
        observersWorkingTogether();
    }

    /** Observador de mentira que só anota o que recebeu. */
    private static class Recorder implements StockObserver {
        final List<Product> received = new ArrayList<>();
        final List<Integer> quantitiesSeen = new ArrayList<>();

        @Override
        public void update(Product product) {
            received.add(product);
            quantitiesSeen.add(product.getQuantity());
        }
    }

    // ------------------------------------------------------------------
    private static void inventoryBasics() {
        suite("Inventory: cadastro, busca e remoção");

        test("começa vazio", () -> eq("tamanho", 0, new Inventory().listProducts().size()));

        test("addProduct e findProduct", () -> {
            Inventory inv = new Inventory();
            Product p = regular("1", 5, 10, 2);
            inv.addProduct(p);
            same("achou o mesmo produto", p, inv.findProduct("1"));
        });

        test("findProduct devolve null se não existir", () -> isNull("busca", new Inventory().findProduct("999")));

        test("addProduct recusa código repetido e mantém só um", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, "cadastrado", () -> inv.addProduct(regular("1", 9, 9, 9)));
            eq("tamanho", 1, inv.listProducts().size());
            near("o original não foi trocado", 5, inv.findProduct("1").getPrice());
        });

        test("listProducts devolve todos, na ordem de cadastro", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.addProduct(regular("2", 5, 10, 2));
            inv.addProduct(regular("3", 5, 10, 2));
            List<Product> list = inv.listProducts();
            eq("tamanho", 3, list.size());
            eq("1º", "1", list.get(0).getCode());
            eq("3º", "3", list.get(2).getCode());
        });

        test("listProducts devolve uma cópia (mexer nela não altera o estoque)", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.listProducts().clear();
            eq("tamanho", 1, inv.listProducts().size());
        });

        test("removeProduct: true se removeu, false se não existe", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            isTrue("primeira vez", inv.removeProduct("1"));
            isFalse("segunda vez", inv.removeProduct("1"));
            isNull("não está mais", inv.findProduct("1"));
        });

        test("depois de remover, o mesmo código pode ser cadastrado de novo", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.removeProduct("1");
            inv.addProduct(regular("1", 7, 3, 1));
            near("preço do novo", 7, inv.findProduct("1").getPrice());
        });
    }

    // ------------------------------------------------------------------
    private static void inventoryStock() {
        suite("Inventory: entrada e baixa de estoque");

        test("decreaseStock diminui a quantidade", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 4);
            eq("quantidade", 6, inv.findProduct("1").getQuantity());
        });

        test("decreaseStock até zerar é permitido", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 10);
            eq("quantidade", 0, inv.findProduct("1").getQuantity());
        });

        test("decreaseStock acima do estoque é recusado e não altera nada", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, "insuficiente", () -> inv.decreaseStock("1", 11));
            eq("quantidade", 10, inv.findProduct("1").getQuantity());
        });

        test("decreaseStock de produto inexistente é recusado", () ->
                assertThrows(IllegalArgumentException.class, "não encontrado", () -> new Inventory().decreaseStock("999", 1)));

        test("decreaseStock com zero ou negativo é recusado e não altera nada", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, "inválid", () -> inv.decreaseStock("1", 0));
            assertThrows(IllegalArgumentException.class, "inválid", () -> inv.decreaseStock("1", -3));
            eq("quantidade", 10, inv.findProduct("1").getQuantity());
        });

        test("increaseStock soma a quantidade", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.increaseStock("1", 15);
            eq("quantidade", 25, inv.findProduct("1").getQuantity());
        });

        test("increaseStock com zero ou negativo é recusado e não altera nada", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, "inválid", () -> inv.increaseStock("1", 0));
            assertThrows(IllegalArgumentException.class, "inválid", () -> inv.increaseStock("1", -5));
            eq("quantidade", 10, inv.findProduct("1").getQuantity());
        });

        test("increaseStock de produto inexistente é recusado", () ->
                assertThrows(IllegalArgumentException.class, "não encontrado", () -> new Inventory().increaseStock("999", 5)));

        test("várias movimentações seguidas somam certo", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 3);
            inv.increaseStock("1", 10);
            inv.decreaseStock("1", 7);
            eq("quantidade", 10, inv.findProduct("1").getQuantity());
        });

        test("movimentar um produto não mexe nos outros", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.addProduct(regular("2", 5, 10, 2));
            inv.decreaseStock("1", 4);
            eq("produto 2", 10, inv.findProduct("2").getQuantity());
        });
    }

    // ------------------------------------------------------------------
    private static void inventoryObservers() {
        suite("Inventory: notificação dos observadores (Subject)");

        test("decreaseStock notifica o observador com o produto já atualizado", () -> {
            Inventory inv = new Inventory();
            Recorder rec = new Recorder();
            inv.addObserver(rec);
            Product p = regular("1", 5, 10, 2);
            inv.addProduct(p);
            inv.decreaseStock("1", 4);
            eq("notificações", 1, rec.received.size());
            same("produto recebido", p, rec.received.get(0));
            eq("quantidade vista pelo observador", 6, rec.quantitiesSeen.get(0));
        });

        test("increaseStock também notifica", () -> {
            Inventory inv = new Inventory();
            Recorder rec = new Recorder();
            inv.addObserver(rec);
            inv.addProduct(regular("1", 5, 10, 2));
            inv.increaseStock("1", 5);
            eq("notificações", 1, rec.received.size());
            eq("quantidade vista", 15, rec.quantitiesSeen.get(0));
        });

        test("baixa recusada NÃO notifica", () -> {
            Inventory inv = new Inventory();
            Recorder rec = new Recorder();
            inv.addObserver(rec);
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, null, () -> inv.decreaseStock("1", 99));
            assertThrows(IllegalArgumentException.class, null, () -> inv.decreaseStock("1", 0));
            assertThrows(IllegalArgumentException.class, null, () -> inv.decreaseStock("x", 1));
            eq("notificações", 0, rec.received.size());
        });

        test("entrada recusada NÃO notifica", () -> {
            Inventory inv = new Inventory();
            Recorder rec = new Recorder();
            inv.addObserver(rec);
            inv.addProduct(regular("1", 5, 10, 2));
            assertThrows(IllegalArgumentException.class, null, () -> inv.increaseStock("1", 0));
            assertThrows(IllegalArgumentException.class, null, () -> inv.increaseStock("x", 1));
            eq("notificações", 0, rec.received.size());
        });

        test("todos os observadores são avisados, na ordem em que entraram", () -> {
            Inventory inv = new Inventory();
            List<String> order = new ArrayList<>();
            inv.addObserver(p -> order.add("A"));
            inv.addObserver(p -> order.add("B"));
            inv.addObserver(p -> order.add("C"));
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 1);
            eq("ordem", "[A, B, C]", order.toString());
        });

        test("removeObserver para de avisar só aquele observador", () -> {
            Inventory inv = new Inventory();
            Recorder keep = new Recorder();
            Recorder drop = new Recorder();
            inv.addObserver(keep);
            inv.addObserver(drop);
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 1);
            inv.removeObserver(drop);
            inv.decreaseStock("1", 1);
            eq("quem ficou", 2, keep.received.size());
            eq("quem saiu", 1, drop.received.size());
        });

        test("remover observador que nunca foi inscrito não dá erro", () -> new Inventory().removeObserver(new Recorder()));

        test("sem nenhum observador o estoque funciona normalmente", () -> {
            Inventory inv = new Inventory();
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 1);
            inv.increaseStock("1", 1);
            eq("quantidade", 10, inv.findProduct("1").getQuantity());
        });

        test("observador novo (lambda) entra sem mexer no Inventory", () -> {
            Inventory inv = new Inventory();
            int[] calls = {0};
            inv.addObserver(p -> calls[0]++);
            inv.addProduct(regular("1", 5, 10, 2));
            inv.decreaseStock("1", 1);
            eq("chamadas", 1, calls[0]);
        });
    }

    // ------------------------------------------------------------------
    private static void lowStockAlert() {
        suite("LowStockAlert (estoque baixo)");

        test("abaixo do mínimo: avisa com nome, quantidade e mínimo", () -> {
            Product p = new RegularProduct("1", "Dipirona", 5, 3, inDays(365), 5);
            new LowStockAlert().update(p);
            contains("aviso", out(), "estoque baixo");
            contains("nome", out(), "Dipirona");
            contains("quantidade atual", out(), "3");
            contains("mínimo", out(), "5");
        });

        test("exatamente no mínimo: não avisa", () -> {
            new LowStockAlert().update(new RegularProduct("1", "Dipirona", 5, 5, inDays(365), 5));
            eq("saída", "", out().trim());
        });

        test("acima do mínimo: não avisa", () -> {
            new LowStockAlert().update(new RegularProduct("1", "Dipirona", 5, 50, inDays(365), 5));
            eq("saída", "", out().trim());
        });

        test("estoque zerado: avisa", () -> {
            new LowStockAlert().update(new RegularProduct("1", "Dipirona", 5, 0, inDays(365), 5));
            contains("aviso", out(), "estoque baixo");
        });
    }

    // ------------------------------------------------------------------
    private static void expirationAlert() {
        suite("ExpirationAlert (validade)");

        test("vencido: avisa que está vencido", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Ibuprofeno", 5, 5, inDays(-1), 1));
            contains("aviso", out(), "vencido");
            contains("nome", out(), "Ibuprofeno");
        });

        test("vence em 10 dias: avisa que está perto de vencer", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Xarope", 5, 5, inDays(10), 1));
            contains("aviso", out(), "perto de vencer");
            notContains("não é 'vencido'", out(), "está vencido");
        });

        test("vence em 59 dias: ainda avisa", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Xarope", 5, 5, inDays(59), 1));
            contains("aviso", out(), "perto de vencer");
        });

        test("vence em 61 dias: não avisa", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Xarope", 5, 5, inDays(61), 1));
            eq("saída", "", out().trim());
        });

        test("validade longe: não avisa", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Xarope", 5, 5, inDays(400), 1));
            eq("saída", "", out().trim());
        });

        test("vence hoje: não é 'vencido', mas avisa que está perto de vencer", () -> {
            new ExpirationAlert().update(new RegularProduct("1", "Xarope", 5, 5, inDays(0), 1));
            contains("aviso", out(), "perto de vencer");
            notContains("não é 'vencido'", out(), "está vencido");
        });

        test("funciona também para perfumaria", () -> {
            new ExpirationAlert().update(new PerfumeProduct("1", "Batom", 5, 5, inDays(-5), 1));
            contains("aviso", out(), "vencido");
        });
    }

    // ------------------------------------------------------------------
    private static void restockOrder() {
        suite("RestockOrder (pedido de reposição)");

        test("começa sem pedidos", () -> eq("pedidos", 0, new RestockOrder().getOrders().size()));

        test("abaixo do mínimo: registra o pedido e avisa", () -> {
            RestockOrder r = new RestockOrder();
            Product p = new RegularProduct("1", "Dipirona", 5, 2, inDays(365), 5);
            r.update(p);
            eq("pedidos", 1, r.getOrders().size());
            same("produto do pedido", p, r.getOrders().get(0));
            contains("aviso", out(), "Pedido de reposição");
            contains("nome", out(), "Dipirona");
        });

        test("não duplica o pedido do mesmo produto", () -> {
            RestockOrder r = new RestockOrder();
            Product p = new RegularProduct("1", "Dipirona", 5, 2, inDays(365), 5);
            r.update(p);
            r.update(p);
            r.update(p);
            eq("pedidos", 1, r.getOrders().size());
        });

        test("no mínimo ou acima: não registra", () -> {
            RestockOrder r = new RestockOrder();
            r.update(new RegularProduct("1", "A", 5, 5, inDays(365), 5));
            r.update(new RegularProduct("2", "B", 5, 50, inDays(365), 5));
            eq("pedidos", 0, r.getOrders().size());
        });

        test("produtos diferentes geram pedidos diferentes", () -> {
            RestockOrder r = new RestockOrder();
            r.update(new RegularProduct("1", "A", 5, 1, inDays(365), 5));
            r.update(new RegularProduct("2", "B", 5, 1, inDays(365), 5));
            eq("pedidos", 2, r.getOrders().size());
        });

        test("quando o estoque é reposto, o pedido sai da lista", () -> {
            RestockOrder r = new RestockOrder();
            Product p = new RegularProduct("1", "Dipirona", 5, 2, inDays(365), 5);
            r.update(p);
            p.setQuantity(20);
            r.update(p);
            eq("pedidos", 0, r.getOrders().size());
        });

        test("depois de reposto e baixo de novo, um novo pedido é aberto", () -> {
            RestockOrder r = new RestockOrder();
            Product p = new RegularProduct("1", "Dipirona", 5, 2, inDays(365), 5);
            r.update(p);
            p.setQuantity(20);
            r.update(p);
            p.setQuantity(1);
            r.update(p);
            eq("pedidos", 1, r.getOrders().size());
        });

        test("getOrders devolve uma cópia", () -> {
            RestockOrder r = new RestockOrder();
            r.update(new RegularProduct("1", "A", 5, 1, inDays(365), 5));
            r.getOrders().clear();
            eq("pedidos", 1, r.getOrders().size());
        });

        test("printOrders sem pedidos avisa que não há nenhum", () -> {
            new RestockOrder().printOrders();
            contains("mensagem", out(), "Nenhum pedido");
        });

        test("printOrders lista nome, quantidade atual e mínimo", () -> {
            RestockOrder r = new RestockOrder();
            r.update(new RegularProduct("1", "Dipirona", 5, 2, inDays(365), 5));
            clearOut();
            r.printOrders();
            contains("nome", out(), "Dipirona");
            contains("quantidade", out(), "2");
            contains("mínimo", out(), "5");
        });
    }

    // ------------------------------------------------------------------
    private static void observersWorkingTogether() {
        suite("Observer: os três alertas ligados ao Inventory");

        test("baixa que cruza o mínimo dispara estoque baixo + pedido de reposição", () -> {
            Inventory inv = new Inventory();
            RestockOrder restock = new RestockOrder();
            inv.addObserver(new LowStockAlert());
            inv.addObserver(new ExpirationAlert());
            inv.addObserver(restock);
            inv.addProduct(new RegularProduct("1", "Dipirona", 5, 10, inDays(365), 5));

            inv.decreaseStock("1", 3);
            eq("acima do mínimo: sem pedido", 0, restock.getOrders().size());
            eq("acima do mínimo: sem aviso", "", out().trim());

            inv.decreaseStock("1", 3);
            eq("abaixo do mínimo: pedido", 1, restock.getOrders().size());
            contains("alerta de estoque baixo", out(), "estoque baixo");
            contains("pedido de reposição", out(), "Pedido de reposição");
        });

        test("baixa em produto vencido dispara o alerta de validade", () -> {
            Inventory inv = new Inventory();
            inv.addObserver(new ExpirationAlert());
            inv.addProduct(new RegularProduct("1", "Ibuprofeno", 5, 10, inDays(-1), 2));
            inv.decreaseStock("1", 1);
            contains("alerta", out(), "vencido");
        });

        test("entrada de mercadoria encerra o pedido de reposição", () -> {
            Inventory inv = new Inventory();
            RestockOrder restock = new RestockOrder();
            inv.addObserver(restock);
            inv.addProduct(new RegularProduct("1", "Dipirona", 5, 10, inDays(365), 5));
            inv.decreaseStock("1", 8);
            eq("pedido aberto", 1, restock.getOrders().size());
            inv.increaseStock("1", 20);
            eq("pedido encerrado", 0, restock.getOrders().size());
        });

        test("sem o LowStockAlert inscrito, só o pedido de reposição reage", () -> {
            Inventory inv = new Inventory();
            LowStockAlert low = new LowStockAlert();
            RestockOrder restock = new RestockOrder();
            inv.addObserver(low);
            inv.addObserver(restock);
            inv.removeObserver(low);
            inv.addProduct(new RegularProduct("1", "Xarope", 5, 10, inDays(365), 8));
            inv.decreaseStock("1", 5);
            notContains("sem alerta", out(), "estoque baixo");
            eq("pedido", 1, restock.getOrders().size());
        });
    }
}
