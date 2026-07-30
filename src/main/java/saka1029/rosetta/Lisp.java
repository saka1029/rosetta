package saka1029.rosetta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lisp {

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
        static final Map<String, Symbol> all = new HashMap<>();
        public static final Symbol QUOTE = Symbol.of("quote");

        public final String value;

        private Symbol(String value) {
            Objects.requireNonNull(value, "Symbol name must not be null");
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
            if (exprs.length <= 0 && end != Nil.NIL)
                throw new IllegalArgumentException("exprs");
            Expr result = end;
            for (int i = exprs.length - 1; i >= 0; --i)
                result = new Cons(exprs[i], result);
            return (List)result;
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
            Objects.requireNonNull(car, "car must not be null");
            Objects.requireNonNull(cdr, "cdr must not be null");
            return new Cons(car, cdr);
        }

        @Override
        public final String toString() {
            if (car.equals(Symbol.QUOTE) && cdr instanceof Cons ccdr && ccdr.cdr == Nil.NIL)
                return "'" + ccdr.car;
            StringBuilder sb = new StringBuilder("(");
            sb.append(car);
            Expr e;
            for (e = cdr; e instanceof Cons cons; e = cons.cdr)
                sb.append(" ").append(cons.car);
            if (e != Nil.NIL)
                sb.append(" . ").append(e);
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
            while (true) {
                spaces();
                if (ch == ')') {
                    get();  // skip ')'
                    return List.list(Nil.NIL, result);
                } else if (ch == '.') {
                    get();  // skip '.'
                    List list = List.list(read(), result);
                    spaces();
                    if (ch != ')')
                        throw new RuntimeException("')' expected");
                    get();  // skip ')'
                    return list;
                }
                Expr e = read();
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

        List quote() {
            get();  // skip '\''
            return List.of(Symbol.QUOTE, read());
        }

        Expr read() {
            spaces();
            int start = current;
            if (ch == -1)
                return null;
            else if (ch == '(')
                return list ();
            else if (ch == '\'')
                return quote();
            else if (ch == '-')
                return isDigit(get()) ? integer(start) : Symbol.of("-");
            else if (isDigit(ch))
                return integer(start);
            else if (isSymbolFirst(ch))
                return symbol(start);
            else 
                throw new RuntimeException("Unexpected character '%c'".formatted((char)ch));
        }
    }

    public static java.util.List<Expr> parse(String source) {
        Parser parser = new Parser(source);
        java.util.List<Expr> result = new ArrayList<>();
        for (Expr e = parser.read(); e != null; e = parser.read())
            result.add(e);
        return result;
    }
}
