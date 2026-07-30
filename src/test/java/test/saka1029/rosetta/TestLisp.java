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

        public static Int of(int value) {
            return new Int(value);
        }

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
            return list(Nil.NIL, exprs);
        }

        public static List list(Expr end, Expr... exprs) {
            if (exprs.length <= 0)
                throw new IllegalArgumentException("exprs");
            Expr result = end;
            for (int i = exprs.length - 1; i >= 0; --i)
                result = new Cons(exprs[i], result);
            return (Cons)result;
        }

        public static List list(Expr end, java.util.List<Expr> list) {
            return list(end, list.toArray(Expr[]::new));
        }
    }

    public static class Nil implements List {
        public static Nil NIL = new Nil();
        private Nil() {}

        @Override
        public String toString() {
            return "()";
        }
    }

    public record Cons(Expr car, Expr cdr) implements List {
        public static Cons of(Expr car, Expr cdr) {
            return new Cons(car, cdr);
        }

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

        Parser(String source) {
            this.in = source.codePoints().toArray();
            this.next = this.current = 0;
            this.ch = get();
        }

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
            return isSymbolFirst(ch) || isDigit(ch) || ch == '.';
        }

        void spaces() {
            while (Character.isWhitespace(ch))
                get();
        }

        List list() {
            get();  // skip '('
            java.util.List<Expr> result = new ArrayList<>();
            for (;;) {
                spaces();
                if (ch == ')') {
                    get();  // skip ')'
                    return List.list(Nil.NIL, result);
                } else if (ch == '.') {
                    get();  // skip '.'
                    List list = List.list(parse(), result);
                    spaces();
                    if (ch != ')')
                        throw new RuntimeException("')' expected");
                    get();  // skip ')'
                    return list;
                }
                Expr e = parse();
                if (e == null)
                    throw new RuntimeException("Unexpected EOF");
                result.addLast(e);
            }
        }

        Int integer(int start) {
            while (isDigit(ch))
                get();
            return new Int(Integer.parseInt(new String(in, start, current - start)));
        }

        Symbol symbol(int start) {
            while (isSymbolRest(ch))
                get();
            return Symbol.of(new String(in, start, current - start));
        }

        Expr parse() {
            spaces();
            int start = current;
            return switch (ch) {
                case -1 -> null;
                case '(' -> list ();
                case '-' -> isDigit(get()) ? integer(start) : Symbol.of("-");
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> integer(start);
                default -> {
                    if (isSymbolFirst(ch))
                        yield symbol(start);
                    else 
                        throw new RuntimeException("Unexpected character '%c'".formatted((char)ch));
                }
            };
        }
    }

    public static java.util.List<Expr> parse(String source) {
        Parser parser = new Parser(source);
        java.util.List<Expr> result = new ArrayList<>();
        for (Expr e = parser.parse(); e != null; e = parser.parse())
            result.add(e);
        return result;
    }

    static Symbol symbol(String value) {
        return Symbol.of(value);
    }

    static Int integer(int value) {
        return Int.of(value);
    }

    static List list(Expr... exprs) {
        return List.of(exprs);
    }

    static Cons cons(Expr car, Expr cdr) {
        return Cons.of(car, cdr);
    }

    static java.util.List<Expr> jlist(Expr... exprs) {
        return java.util.List.of(exprs);
    }

    @Test
    public void testParseSymbol() {
        assertEquals(jlist(symbol("a.bc")), parse("a.bc"));
        assertEquals(jlist(symbol("abc")), parse("abc"));
        assertEquals(jlist(symbol("abc")), parse("abc  "));
        assertEquals(jlist(symbol("abc")), parse("   abc  "));
        assertEquals(jlist(symbol("#<")), parse(" #< "));
        assertEquals(jlist(symbol("-")), parse(" - "));
        assertEquals(jlist(symbol("**")), parse(" ** "));
    }

    @Test
    public void testParseInt() {
        assertEquals(jlist(integer(123)), parse("123"));
        assertEquals(jlist(integer(123)), parse("123  "));
        assertEquals(jlist(integer(123)), parse("  123  "));
        assertEquals(jlist(integer(-123)), parse("-123"));
        assertEquals(jlist(integer(-123)), parse("-123  "));
        assertEquals(jlist(integer(-123)), parse("  -123  "));
        assertEquals(jlist(symbol("-"), integer(123)), parse("  -  123  "));
    }

    @Test
    public void testParseList() {
        assertEquals(jlist(list(integer(123))), parse("(123)"));
        assertEquals(jlist(list(integer(123))), parse("(123  )"));
        assertEquals(jlist(list(integer(123))), parse("(   123   )"));
        assertEquals(jlist(list(integer(123))), parse("   (   123   )"));
        assertEquals(jlist(list(integer(123))), parse("   (   123   )   "));
        assertEquals(jlist(list(list(symbol("a")))), parse("((a))"));
    }

    @Test
    public void testParseDotPair() {
        assertEquals(jlist(cons(symbol("a"), symbol("b"))), parse("(a . b)"));
        assertEquals(jlist(cons(symbol("a"), symbol("b"))), parse("(a .b)"));
        assertEquals(jlist(list(symbol("a."), symbol("b"))), parse("(a. b)"));
        assertEquals(jlist(list(symbol("a.b"))), parse("(a.b)"));
        assertEquals(jlist(cons(symbol("a"), list(symbol("b")))), parse("(a .(b))"));
    }
}
