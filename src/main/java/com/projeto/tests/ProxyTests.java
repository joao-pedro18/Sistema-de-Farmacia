package tests;

import static tests.TestKit.*;

import java.util.ArrayList;
import java.util.List;

import model.Customer;
import model.CustomerType;
import model.Employee;
import model.ItemSale;
import model.PerfumeProduct;
import model.Prescription;
import model.Product;
import model.Profile;
import model.RegularProduct;
import model.Sale;
import observer.StockObserver;
import proxy.ProxySalesService;
import proxy.RealSalesService;
import proxy.SalesService;
import service.Inventory;
import service.SalesHistory;
import strategy.CardPayment;
import strategy.CashPayment;
import strategy.DiscountSeniors;
import strategy.NoDiscount;
import strategy.PaymentStrategy;
import strategy.PixPayment;

/** Pessoa 2: RealSalesService (serviço real) e ProxySalesService (proxy de proteção). */
public class ProxyTests {

    public static void main(String[] args) {
        run();
        System.exit(finish() ? 0 : 1);
    }

    public static void run() {
        realService();
        proxyAccess();
        proxyValidation();
        proxyDelegation();
        cartConsistency();
    }

    private static final Employee CLERK = new Employee("Ana", Profile.CLERK);
    private static final Employee PHARMACIST = new Employee("Carlos", Profile.PHARMACEUTICAL);
    private static final Prescription RX = new Prescription("001", "Dr. Silva");

    /** Cenário pronto: estoque com produtos, histórico, serviço real e proxies. */
    private static class Scenario {
        final Inventory inventory = new Inventory();
        final SalesHistory history = new SalesHistory();
        final SalesService real = new RealSalesService(inventory, history);
        final Product reg = regular("R1", 10.0, 20, 2);
        final Product ctl = controlled("C1", 40.0, 10, 2);
        final Product per = perfume("P1", 30.0, 20, 2);
        final ProxySalesService asClerk = new ProxySalesService(real, CLERK);
        final ProxySalesService asPharmacist = new ProxySalesService(real, PHARMACIST);

        Scenario() {
            inventory.addProduct(reg);
            inventory.addProduct(ctl);
            inventory.addProduct(per);
        }

        Sale sale(Employee who, Object... productsAndAmounts) {
            Sale s = new Sale(new Customer("Maria", "123", CustomerType.REGULAR), who);
            for (int i = 0; i < productsAndAmounts.length; i += 2) {
                s.addItem(new ItemSale((Product) productsAndAmounts[i], (Integer) productsAndAmounts[i + 1]));
            }
            s.setPrescription(RX);
            s.setDiscountStrategy(new NoDiscount());
            s.setPaymentStrategy(new CardPayment());
            return s;
        }
    }

    /** Serviço real de mentira: só conta quantas vendas chegaram até ele. */
    private static class CountingService implements SalesService {
        final List<Sale> received = new ArrayList<>();

        @Override
        public void finalizeSale(Sale sale) {
            received.add(sale);
        }
    }

    // ------------------------------------------------------------------
    private static void realService() {
        suite("RealSalesService (serviço real, sem checar perfil)");

        test("aplica o desconto na venda", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.reg, 5); // 50
            s.setDiscountStrategy(new DiscountSeniors());
            sc.real.finalizeSale(s);
            near("desconto", 5, s.getDiscount());
            near("total", 45, s.getTotal());
        });

        test("cobra o total (com desconto) na forma de pagamento escolhida", () -> {
            Scenario sc = new Scenario();
            double[] charged = {-1};
            PaymentStrategy recorder = new PaymentStrategy() {
                @Override
                public void pay(double amount) {
                    charged[0] = amount;
                }

                @Override
                public String getName() {
                    return "Teste";
                }
            };
            Sale s = sc.sale(CLERK, sc.reg, 5);
            s.setDiscountStrategy(new DiscountSeniors());
            s.setPaymentStrategy(recorder);
            sc.real.finalizeSale(s);
            near("valor cobrado", 45, charged[0]);
        });

        test("dá baixa no estoque, item por item", () -> {
            Scenario sc = new Scenario();
            sc.real.finalizeSale(sc.sale(CLERK, sc.reg, 5, sc.per, 3));
            eq("comum", 15, sc.inventory.findProduct("R1").getQuantity());
            eq("perfumaria", 17, sc.inventory.findProduct("P1").getQuantity());
            eq("controlado intacto", 10, sc.inventory.findProduct("C1").getQuantity());
        });

        test("baixa a quantidade VENDIDA, não a do estoque", () -> {
            Scenario sc = new Scenario();
            sc.real.finalizeSale(sc.sale(CLERK, sc.reg, 1));
            eq("comum", 19, sc.inventory.findProduct("R1").getQuantity());
        });

        test("envia a venda finalizada ao histórico", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.reg, 2);
            sc.real.finalizeSale(s);
            eq("vendas no histórico", 1, sc.history.getSales().size());
            same("mesma venda", s, sc.history.getSales().get(0));
        });

        test("pagamento em dinheiro insuficiente: nada muda (sem baixa, sem histórico)", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.reg, 5); // total 50
            s.setPaymentStrategy(new CashPayment(10));
            assertThrows(IllegalArgumentException.class, "insuficiente", () -> sc.real.finalizeSale(s));
            eq("estoque", 20, sc.inventory.findProduct("R1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
        });

        test("pagamento em dinheiro com troco funciona e fecha a venda", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.reg, 5); // total 50
            s.setPaymentStrategy(new CashPayment(100));
            sc.real.finalizeSale(s);
            contains("troco", out(), "Troco");
            eq("histórico", 1, sc.history.getSales().size());
        });

        test("a venda dispara os observadores do estoque (sem a venda conhecê-los)", () -> {
            Scenario sc = new Scenario();
            List<String> seen = new ArrayList<>();
            StockObserver obs = p -> seen.add(p.getCode() + ":" + p.getQuantity());
            sc.inventory.addObserver(obs);
            sc.real.finalizeSale(sc.sale(CLERK, sc.reg, 5, sc.per, 3));
            eq("avisos", "[R1:15, P1:17]", seen.toString());
        });

        test("duas vendas seguidas acumulam baixa e histórico", () -> {
            Scenario sc = new Scenario();
            sc.real.finalizeSale(sc.sale(CLERK, sc.reg, 5));
            sc.real.finalizeSale(sc.sale(CLERK, sc.reg, 7));
            eq("estoque", 8, sc.inventory.findProduct("R1").getQuantity());
            eq("histórico", 2, sc.history.getSales().size());
        });
    }

    // ------------------------------------------------------------------
    private static void proxyAccess() {
        suite("Proxy: controle de acesso (só farmacêutico vende controlado)");

        test("o proxy é um SalesService (mesma interface do serviço real)", () -> {
            SalesService s = new Scenario().asClerk;
            isTrue("instanceof", s instanceof SalesService);
        });

        test("balconista vendendo controlado: NEGADO com mensagem em português", () -> {
            Scenario sc = new Scenario();
            RuntimeException e = assertThrows(RuntimeException.class, "farmac",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            contains("fala de controlado", e.getMessage(), "controlado");
        });

        test("venda negada não mexe no estoque nem no histórico", () -> {
            Scenario sc = new Scenario();
            assertThrows(RuntimeException.class, null, () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            eq("estoque do controlado", 10, sc.inventory.findProduct("C1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
        });

        test("venda negada não cobra o cliente", () -> {
            Scenario sc = new Scenario();
            assertThrows(RuntimeException.class, null, () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            notContains("sem cobrança", out(), "aprovado");
        });

        test("a tentativa negada fica registrada com quem tentou e o produto", () -> {
            Scenario sc = new Scenario();
            eq("começa vazio", 0, sc.asClerk.getDeniedAttempts().size());
            assertThrows(RuntimeException.class, null, () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            eq("registros", 1, sc.asClerk.getDeniedAttempts().size());
            contains("quem tentou", sc.asClerk.getDeniedAttempts().get(0), "Ana");
            contains("produto", sc.asClerk.getDeniedAttempts().get(0), "Controlado C1");
        });

        test("tentativas negadas se acumulam", () -> {
            Scenario sc = new Scenario();
            assertThrows(RuntimeException.class, null, () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            assertThrows(RuntimeException.class, null, () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.ctl, 2)));
            eq("registros", 2, sc.asClerk.getDeniedAttempts().size());
        });

        test("farmacêutico vendendo controlado com receita: LIBERADO", () -> {
            Scenario sc = new Scenario();
            sc.asPharmacist.finalizeSale(sc.sale(PHARMACIST, sc.ctl, 1));
            eq("estoque", 9, sc.inventory.findProduct("C1").getQuantity());
            eq("histórico", 1, sc.history.getSales().size());
            eq("sem tentativas negadas", 0, sc.asPharmacist.getDeniedAttempts().size());
        });

        test("a receita fica guardada na venda do histórico", () -> {
            Scenario sc = new Scenario();
            sc.asPharmacist.finalizeSale(sc.sale(PHARMACIST, sc.ctl, 1));
            Prescription saved = sc.history.getSales().get(0).getPrescription();
            notNull("receita", saved);
            eq("número", "001", saved.getNumber());
            eq("médico", "Dr. Silva", saved.getDoctor());
        });

        test("balconista vendendo produto comum: LIBERADO", () -> {
            Scenario sc = new Scenario();
            sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 2));
            eq("estoque", 18, sc.inventory.findProduct("R1").getQuantity());
            eq("histórico", 1, sc.history.getSales().size());
        });

        test("balconista vendendo perfumaria: LIBERADO", () -> {
            Scenario sc = new Scenario();
            sc.asClerk.finalizeSale(sc.sale(CLERK, sc.per, 2));
            eq("histórico", 1, sc.history.getSales().size());
        });

        test("farmacêutico também pode vender produto comum", () -> {
            Scenario sc = new Scenario();
            sc.asPharmacist.finalizeSale(sc.sale(PHARMACIST, sc.reg, 2));
            eq("histórico", 1, sc.history.getSales().size());
        });

        test("carrinho misto (comum + controlado) com balconista: a venda INTEIRA é negada", () -> {
            Scenario sc = new Scenario();
            assertThrows(RuntimeException.class, "farmac",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 2, sc.ctl, 1)));
            eq("estoque do comum intacto", 20, sc.inventory.findProduct("R1").getQuantity());
            eq("estoque do controlado intacto", 10, sc.inventory.findProduct("C1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
        });

        test("a checagem de perfil vem ANTES da validação do produto", () -> {
            // balconista + controlado + sem receita: o motivo deve ser o perfil, não a receita
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.ctl, 1);
            s.setPrescription(null);
            assertThrows(RuntimeException.class, "farmac", () -> sc.asClerk.finalizeSale(s));
        });
    }

    // ------------------------------------------------------------------
    private static void proxyValidation() {
        suite("Proxy: validação das regras de cada produto antes de vender");

        test("controlado sem receita (farmacêutico): barrado e NÃO conta como acesso negado", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(PHARMACIST, sc.ctl, 1);
            s.setPrescription(null);
            assertThrows(IllegalArgumentException.class, "receita", () -> sc.asPharmacist.finalizeSale(s));
            eq("estoque", 10, sc.inventory.findProduct("C1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
            eq("acesso negado", 0, sc.asPharmacist.getDeniedAttempts().size());
        });

        test("controlado acima do limite por venda (3): barrado", () -> {
            Scenario sc = new Scenario();
            assertThrows(IllegalArgumentException.class, "limite",
                    () -> sc.asPharmacist.finalizeSale(sc.sale(PHARMACIST, sc.ctl, 3)));
            eq("estoque", 10, sc.inventory.findProduct("C1").getQuantity());
        });

        test("produto comum vencido: barrado", () -> {
            Scenario sc = new Scenario();
            Product expired = new RegularProduct("E1", "Vencido", 5, 10, inDays(-1), 1);
            sc.inventory.addProduct(expired);
            assertThrows(IllegalArgumentException.class, "vencido",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, expired, 1)));
            eq("estoque", 10, sc.inventory.findProduct("E1").getQuantity());
        });

        test("perfumaria com validade curta (20 dias): barrada", () -> {
            Scenario sc = new Scenario();
            Product soon = new PerfumeProduct("P2", "Quase vencendo", 20, 10, inDays(20), 1);
            sc.inventory.addProduct(soon);
            assertThrows(IllegalArgumentException.class, "validade",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, soon, 1)));
            eq("histórico", 0, sc.history.getSales().size());
        });

        test("quantidade acima do estoque: barrada, sem cobrança e sem alterar nada", () -> {
            Scenario sc = new Scenario();
            assertThrows(IllegalArgumentException.class, "estoque",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 21)));
            eq("estoque", 20, sc.inventory.findProduct("R1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
            notContains("sem cobrança", out(), "aprovado");
        });

        test("se UM item do carrinho é inválido, nenhum item é vendido", () -> {
            Scenario sc = new Scenario();
            assertThrows(IllegalArgumentException.class, "estoque",
                    () -> sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 2, sc.per, 99)));
            eq("comum intacto", 20, sc.inventory.findProduct("R1").getQuantity());
            eq("perfumaria intacta", 20, sc.inventory.findProduct("P1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
        });
    }

    // ------------------------------------------------------------------
    private static void proxyDelegation() {
        suite("Proxy: delegação ao serviço real");

        test("venda válida é entregue ao serviço real exatamente uma vez, com a mesma venda", () -> {
            CountingService real = new CountingService();
            ProxySalesService proxy = new ProxySalesService(real, PHARMACIST);
            Sale s = new Scenario().sale(PHARMACIST, regular("R9", 5, 10, 1), 1);
            proxy.finalizeSale(s);
            eq("chamadas", 1, real.received.size());
            same("venda", s, real.received.get(0));
        });

        test("venda negada NÃO chega ao serviço real", () -> {
            CountingService real = new CountingService();
            ProxySalesService proxy = new ProxySalesService(real, CLERK);
            Scenario sc = new Scenario();
            assertThrows(RuntimeException.class, null, () -> proxy.finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            eq("chamadas", 0, real.received.size());
        });

        test("venda inválida NÃO chega ao serviço real", () -> {
            CountingService real = new CountingService();
            ProxySalesService proxy = new ProxySalesService(real, PHARMACIST);
            Scenario sc = new Scenario();
            assertThrows(IllegalArgumentException.class, null, () -> proxy.finalizeSale(sc.sale(PHARMACIST, sc.reg, 999)));
            eq("chamadas", 0, real.received.size());
        });

        test("o perfil usado é o do funcionário logado no proxy", () -> {
            CountingService real = new CountingService();
            Scenario sc = new Scenario();
            // mesma venda de controlado: balconista nega, farmacêutico libera
            assertThrows(RuntimeException.class, null,
                    () -> new ProxySalesService(real, CLERK).finalizeSale(sc.sale(CLERK, sc.ctl, 1)));
            new ProxySalesService(real, PHARMACIST).finalizeSale(sc.sale(PHARMACIST, sc.ctl, 1));
            eq("chegou ao real", 1, real.received.size());
        });

        test("pagamento por pix também passa pelo proxy normalmente", () -> {
            Scenario sc = new Scenario();
            Sale s = sc.sale(CLERK, sc.reg, 2);
            s.setPaymentStrategy(new PixPayment());
            sc.asClerk.finalizeSale(s);
            contains("pix", out(), "Pix realizado");
            eq("histórico", 1, sc.history.getSales().size());
        });
    }

    // ------------------------------------------------------------------
    private static void cartConsistency() {
        suite("Consistência do carrinho (o mesmo produto adicionado mais de uma vez)");

        test("2 + 2 do mesmo controlado não pode furar o limite de 2 por venda", () -> {
            Scenario sc = new Scenario();
            // cada linha tem 2 (dentro do limite), mas a venda leva 4 do mesmo controlado
            assertThrows(IllegalArgumentException.class, "limite",
                    () -> sc.asPharmacist.finalizeSale(sc.sale(PHARMACIST, sc.ctl, 2, sc.ctl, 2)));
            eq("estoque do controlado", 10, sc.inventory.findProduct("C1").getQuantity());
        });

        test("12 + 12 do mesmo produto (estoque 20): a venda toda falha sem cobrar nem baixar nada", () -> {
            Scenario sc = new Scenario();
            try {
                sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 12, sc.reg, 12));
            } catch (RuntimeException expected) {
                // recusar é o comportamento correto; o que importa é o estado depois
            }
            eq("estoque deve continuar 20 (venda tudo-ou-nada)", 20, sc.inventory.findProduct("R1").getQuantity());
            eq("histórico", 0, sc.history.getSales().size());
            notContains("cliente não pode ter sido cobrado", out(), "aprovado");
        });

        test("2 linhas do mesmo produto dentro do estoque: soma as baixas", () -> {
            Scenario sc = new Scenario();
            sc.asClerk.finalizeSale(sc.sale(CLERK, sc.reg, 3, sc.reg, 4));
            eq("estoque", 13, sc.inventory.findProduct("R1").getQuantity());
        });
    }
}
