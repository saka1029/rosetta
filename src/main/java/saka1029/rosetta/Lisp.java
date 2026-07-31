package saka1029.rosetta;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lisp {

    private Lisp() {}

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
            Objects.requireNonNull(value, "value");
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
        public static List NIL = new List() {
            @Override
            public String toString() {
                return "()";
            }
        };

        public static List of(Expr... exprs) {
            return list(List.NIL, exprs);
        }

        public static List list(Expr end, Expr... exprs) {
            if (exprs.length <= 0 && end != List.NIL)
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

    public record Cons(Expr car, Expr cdr) implements List {
        public static Cons of(Expr car, Expr cdr) {
            Objects.requireNonNull(car, "car");
            Objects.requireNonNull(cdr, "cdr");
            return new Cons(car, cdr);
        }

        @Override
        public final String toString() {
            if (car.equals(Symbol.QUOTE) && cdr instanceof Cons ccdr && ccdr.cdr == List.NIL)
                return "'" + ccdr.car;
            StringBuilder sb = new StringBuilder("(");
            sb.append(car);
            Expr e;
            for (e = cdr; e instanceof Cons cons; e = cons.cdr)
                sb.append(" ").append(cons.car);
            if (e != List.NIL)
                sb.append(" . ").append(e);
            return sb.append(")").toString();
        }
    }

    public static class Reader {
        public static final Expr EOF = new Expr() {};

        final java.io.Reader reader;
        final StringBuilder buffer = new StringBuilder();
        int ch;

        Reader(java.io.Reader reader) {
            this.reader = reader;
            this.ch = get();
        }

        public static Reader of(String source) {
            return new Reader(new StringReader(source));
        }

        int get() {
            try {
                ch = reader.read();
                buffer.append((char)ch);    // ch == EOFの時もappendする
                return ch;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        int getClear() {
            buffer.setLength(0);
            return get();
        }

        void spaces() {
            while (Character.isWhitespace(ch))
                get();
            buffer.delete(0, buffer.length() - 1);
        }

        List list() {
            getClear();     // skip '('
            java.util.List<Expr> list = new ArrayList<>();
            while (true) {
                spaces();
                if (ch == ')') {
                    getClear();  // skip ')'
                    return List.list(List.NIL, list);
                } else if (ch == '.') {
                    getClear();  // skip '.'
                    List result = List.list(read(), list);
                    spaces();
                    if (ch != ')')
                        throw new RuntimeException("')' expected");
                    getClear();  // skip ')'
                    return result;
                }
                Expr e = read();
                if (e == null)
                    throw new RuntimeException("Unexpected EOF");
                list.addLast(e);
            }
        }

        List quote() {
            getClear();  // skip '\''
            return List.of(Symbol.QUOTE, read());
        }

        static boolean isDigit(int ch) {
            return ch >= '0' && ch <= '9';
        }

        Int integer() {
            while (isDigit(ch))
                get();
            return Int.of(Integer.parseInt(buffer.substring(0, buffer.length() - 1)));
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

        Symbol symbol() {
            while (isSymbolRest(ch))
                get();
            return Symbol.of(buffer.substring(0, buffer.length() - 1));
        }

        public Expr read() {
            spaces();
            if (ch == -1)
                return EOF;
            else if (ch == '(')
                return list();
            else if (ch == '\'')
                return quote();
            else if (ch == '-')
                return isDigit(get()) ? integer() : Symbol.of("-");
            else if (isDigit(ch))
                return integer();
            else if (isSymbolFirst(ch))
                return symbol();
            else 
                throw new RuntimeException("Unexpected character '%c'".formatted((char)ch));
        }
    }

    public static class Parser {
        final int[] in;
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
                    return List.list(List.NIL, result);
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
            return Int.of(Integer.parseInt(new String(in, start, current - start)));
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

    public static List parse(String source) {
        Parser parser = new Parser(source);
        java.util.List<Expr> result = new ArrayList<>();
        for (Expr e = parser.read(); e != null; e = parser.read())
            result.add(e);
        return List.list(List.NIL, result);
    }
}
