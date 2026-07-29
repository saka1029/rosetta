package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

public class TestLisp {

    public interface Expr {}

    public interface Atom extends Expr {}

    public record Int(int value) implements Atom {
        @Override
        public final String toString() {
            return "" + value;
        }
    }

    public static class Symbol implements Atom {
        public final String value;
        static final Map<String, Symbol> all = new HashMap<>();
        private Symbol(String value) {
            this.value = value;
        }
        public static Symbol of(String value) {
            return all.computeIfAbsent(value, k -> new Symbol(value));
        }
        @Override
        public String toString() {
            return value;
        }
    }

    public interface List extends Expr {
        public static List of(Expr... exprs) {
            List result = Nil.NIL;
            for (int i = exprs.length - 1; i >= 0; --i)
                result = new Cons(exprs[i], result);
            return result;
        }
        public static List of(java.util.List<Expr> list) {
            return of(list.toArray(Expr[]::new));
        }
    }

    public static class Nil implements List {
        public static Nil NIL = new Nil();
        private Nil() {}
        @Override public String toString() { return "()"; }
    }

    public record Cons(Expr car, Expr cdr) implements List {
        @Override
        public final String toString() {
            StringBuilder sb = new StringBuilder("(");
            sb.append(car);
            Expr e = cdr;
            while (true) {
                if (e == Nil.NIL) {
                    break;
                } else if (e instanceof Cons cons) {
                    sb.append(" ").append(cons.car);
                    e = cons.cdr;
                } else { // dot pair
                    sb.append(" . ").append(e);
                    break;
                }
            }
            return sb.append(")").toString();
        }
    }

    static final class Parser {
        int[] in;
        int next, current, ch;

        int get() {
            current = next;
            return ch = next < in.length ? in[next++] : -1;
        }

        static boolean isDigit(int ch) {
            return ch >= '0' && ch <= '9';
        }

        static boolean isSymbolFirst(int ch) {
            return switch (ch) {
                case -1, '(', ')', '.' -> false;
                default -> !Character.isWhitespace(ch) && !isDigit(ch);
            };
        }

        static boolean isSymbolRest(int ch) {
            return isSymbolFirst(ch) || isDigit(ch);
        }

        void spaces() {
            while (Character.isWhitespace(ch))
                get();
        }

        List list() {
            get();  // skip '('
            LinkedList<Expr> result = new LinkedList<>();
            for (;;) {
                spaces();
                if (ch == ')') {
                    get();  // skip ')'
                    return List.of(result);
                }
                Expr e = parse();
                if (e == null)
                    throw new RuntimeException("Unexpected EOF");
                result.addFirst(e);
            }
        }

        Int integer(int begin, int sign) {
            while (isDigit(ch))
                get();
            return new Int(sign * Integer.parseInt(new String(in, begin, current - begin)));
        }

        Symbol symbol(int begin) {
            get();
            while (isSymbolRest(ch))
                get();
            return Symbol.of(new String(in, begin, current - begin));
        }

        Expr parse() {
            spaces();
            int begin = next - 1;
            return switch (ch) {
                case -1 -> null;
                case '(' -> list ();
                case ')' -> throw new RuntimeException("Unexpected ')'");
                case '-' -> isDigit(get()) ? integer(begin, -1) : Symbol.of("-");
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> integer(begin, 1);
                default -> {
                    if (isSymbolFirst(get()))
                        yield symbol(begin);
                    else 
                        throw new RuntimeException("Unknown character '%c'".formatted((char)ch));
                }
            };
        }

        java.util.List<Expr> parse(String source) {
            in = source.codePoints().toArray();
            next = current = 0;
            ch = get();
            java.util.List<Expr> result = new ArrayList<>();
            for (Expr e = parse(); e != null; e = parse())
                result.add(e);
            return result;
        }
    }

    static final Parser parser = new Parser();

    @Test
    public void testParseAtom() {
        assertEquals(java.util.List.of(Symbol.of("abc")), parser.parse("abc"));
        assertEquals(java.util.List.of(Symbol.of("abc")), parser.parse("abc  "));
        assertEquals(java.util.List.of(Symbol.of("abc")), parser.parse("   abc  "));
    }

    @Test
    public void testParseInt() {
        assertEquals(java.util.List.of(new Int(123)), parser.parse("123"));
        assertEquals(java.util.List.of(new Int(123)), parser.parse("123  "));
        assertEquals(java.util.List.of(new Int(123)), parser.parse("  123  "));
    }

    @Test
    public void testParseList() {
        assertEquals(java.util.List.of(List.of(new Int(123))), parser.parse("(123)"));
        assertEquals(java.util.List.of(List.of(new Int(123))), parser.parse("(123  )"));
        assertEquals(java.util.List.of(List.of(new Int(123))), parser.parse("(   123   )"));
        assertEquals(java.util.List.of(List.of(new Int(123))), parser.parse("   (   123   )"));
        assertEquals(java.util.List.of(List.of(new Int(123))), parser.parse("   (   123   )   "));
    }
}
