package tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.ControlledProduct;
import model.PerfumeProduct;
import model.Product;
import model.RegularProduct;

/**
 * Mini-framework de testes (sem JUnit, sem dependências externas).
 * Cada teste roda isolado: a saída do terminal (System.out) é capturada,
 * a entrada (System.in) pode ser simulada, e uma falha em um teste não derruba os outros.
 */
public final class TestKit {

    @FunctionalInterface
    public interface Body {
        void run() throws Exception;
    }

    private static final PrintStream REAL_OUT = System.out;
    private static final InputStream REAL_IN = System.in;
    private static final long TIMEOUT_MS = 10_000;

    private static final List<String> failures = new ArrayList<>();
    private static int passed = 0;
    private static int failed = 0;
    private static String currentSuite = "";

    private static ByteArrayOutputStream captured = new ByteArrayOutputStream();

    private TestKit() {
    }

    // ===================== Execução =====================

    public static void suite(String name) {
        currentSuite = name;
        REAL_OUT.println();
        REAL_OUT.println("=== " + name + " ===");
    }

    public static void test(String name, Body body) {
        captured = new ByteArrayOutputStream();
        Throwable[] problem = new Throwable[1];

        Thread worker = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                problem[0] = t;
            }
        });
        worker.setDaemon(true);

        try {
            System.setOut(new PrintStream(captured, true, "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        worker.start();
        try {
            worker.join(TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean hung = worker.isAlive();

        System.setOut(REAL_OUT);
        System.setIn(REAL_IN);

        if (hung) {
            report(name, "TRAVOU: passou de " + (TIMEOUT_MS / 1000) + "s (possível loop infinito)");
        } else if (problem[0] == null) {
            passed++;
            REAL_OUT.println("[OK] " + name);
        } else {
            report(name, describe(problem[0]));
        }
    }

    private static void report(String name, String detail) {
        failed++;
        REAL_OUT.println("[FALHOU] " + name);
        REAL_OUT.println("         " + detail);
        failures.add("[" + currentSuite + "] " + name + "\n         " + detail);
    }

    private static String describe(Throwable t) {
        if (t instanceof AssertionError) {
            return t.getMessage();
        }
        String where = "";
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (!c.startsWith("java.") && !c.startsWith("jdk.") && !c.startsWith("tests.")) {
                where = " (em " + c + "." + e.getMethodName() + ", linha " + e.getLineNumber() + ")";
                break;
            }
        }
        return "ERRO inesperado: " + t.getClass().getSimpleName()
                + (t.getMessage() != null ? ": " + t.getMessage() : "") + where;
    }

    /** Imprime o resumo final e devolve true se tudo passou. */
    public static boolean finish() {
        REAL_OUT.println();
        REAL_OUT.println("==================================================");
        REAL_OUT.println("RESULTADO: " + passed + " passaram, " + failed + " falharam, "
                + (passed + failed) + " no total");
        if (!failures.isEmpty()) {
            REAL_OUT.println();
            REAL_OUT.println("TESTES QUE FALHARAM:");
            for (String f : failures) {
                REAL_OUT.println(" - " + f);
            }
        } else {
            REAL_OUT.println("Tudo certo!");
        }
        REAL_OUT.println("==================================================");
        return failed == 0;
    }

    // ===================== Entrada e saída simuladas =====================

    /** Simula o que o usuário digita (uma linha por argumento). Chamar ANTES de criar o menu. */
    public static void input(String... lines) {
        String text = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Simula um terminal de verdade: entrega UMA linha por leitura.
     * Use quando vários Scanners leem o mesmo System.in (ex.: MainMenu + os submenus),
     * porque com input(...) o primeiro Scanner "engoliria" todas as linhas de uma vez.
     */
    public static void typed(String... lines) {
        byte[] data = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);
        System.setIn(new InputStream() {
            private int pos = 0;

            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xff : -1;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (pos >= data.length) {
                    return -1;
                }
                int n = 0;
                while (pos < data.length && n < length) {
                    byte c = data[pos++];
                    buffer[offset + n++] = c;
                    if (c == '\n') {
                        break;
                    }
                }
                return n;
            }
        });
    }

    /** Tudo o que foi impresso no terminal durante o teste atual. */
    public static String out() {
        try {
            return captured.toString("UTF-8");
        } catch (Exception e) {
            return captured.toString();
        }
    }

    public static void clearOut() {
        captured.reset();
    }

    // ===================== Asserções =====================

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    public static void isTrue(String what, boolean condition) {
        if (!condition) {
            fail(what + " (esperado: verdadeiro)");
        }
    }

    public static void isFalse(String what, boolean condition) {
        if (condition) {
            fail(what + " (esperado: falso)");
        }
    }

    public static void eq(String what, Object expected, Object actual) {
        boolean same = (expected == null) ? actual == null : expected.equals(actual);
        if (!same) {
            fail(what + " | esperado: <" + expected + "> mas veio: <" + actual + ">");
        }
    }

    public static void near(String what, double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-6) {
            fail(what + " | esperado: <" + expected + "> mas veio: <" + actual + ">");
        }
    }

    public static void isNull(String what, Object value) {
        if (value != null) {
            fail(what + " | esperado: null mas veio: <" + value + ">");
        }
    }

    public static void notNull(String what, Object value) {
        if (value == null) {
            fail(what + " | esperado: algum valor mas veio null");
        }
    }

    public static void same(String what, Object expected, Object actual) {
        if (expected != actual) {
            fail(what + " | esperado a MESMA instância, mas são objetos diferentes");
        }
    }

    /** Confere que o texto contém o trecho (sem diferenciar maiúsculas de minúsculas). */
    public static void contains(String what, String text, String fragment) {
        if (text == null || !text.toLowerCase().contains(fragment.toLowerCase())) {
            fail(what + " | esperado conter: \"" + fragment + "\" mas o texto foi: \"" + shorten(text) + "\"");
        }
    }

    public static void notContains(String what, String text, String fragment) {
        if (text != null && text.toLowerCase().contains(fragment.toLowerCase())) {
            fail(what + " | NÃO deveria conter: \"" + fragment + "\" mas o texto foi: \"" + shorten(text) + "\"");
        }
    }

    /**
     * Confere que a ação lança a exceção esperada (ou uma subclasse) e,
     * se informado, que a mensagem contém o trecho. Devolve a exceção.
     */
    public static <T extends Throwable> T assertThrows(Class<T> type, String messageFragment, Body action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (!type.isInstance(t)) {
                fail("esperava " + type.getSimpleName() + " mas lançou " + t.getClass().getSimpleName()
                        + (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""));
            }
            if (messageFragment != null) {
                contains("mensagem da exceção", t.getMessage(), messageFragment);
            }
            return type.cast(t);
        }
        fail("esperava " + type.getSimpleName() + " mas nenhuma exceção foi lançada");
        return null;
    }

    private static String shorten(String s) {
        if (s == null) {
            return "null";
        }
        String flat = s.replace("\n", " | ").trim();
        return flat.length() > 300 ? flat.substring(0, 300) + "..." : flat;
    }

    // ===================== Fábricas de dados de teste =====================

    public static LocalDate inDays(int days) {
        return LocalDate.now().plusDays(days);
    }

    public static Product regular(String code, double price, int quantity, int minimum) {
        return new RegularProduct(code, "Comum " + code, price, quantity, inDays(365), minimum);
    }

    public static Product controlled(String code, double price, int quantity, int minimum) {
        return new ControlledProduct(code, "Controlado " + code, price, quantity, inDays(365), minimum);
    }

    public static Product perfume(String code, double price, int quantity, int minimum) {
        return new PerfumeProduct(code, "Perfume " + code, price, quantity, inDays(365), minimum);
    }

    /**
     * Único lugar dos testes que chama o getter do estoque mínimo.
     * No código ele se chama getMininumStock (sem o "m"); se vocês renomearem
     * para getMinimumStock, basta ajustar esta linha.
     */
    public static int minStock(Product product) {
        return product.getMininumStock();
    }
}
