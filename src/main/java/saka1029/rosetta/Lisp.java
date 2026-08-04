package saka1029.rosetta;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Lisp {

    private Lisp() {}

    public static class Env {
        private final Map<Symbol, Expr> map = new HashMap<>();
        private final Env next;
        
        Env(Env next) {
            this.next = next;
        }

        public static Env of(Env next) {
            return new Env(next);
        }

        public static Env of() {
            return new Env(null);
        }
        
        private static Env find(Env env, Symbol key) {
            for (Env e = env; e != null; e = e.next)
                if (e.map.containsKey(key))
                    return e;
            throw new RuntimeException("Variable " + key + " not found");
        }
        
        public Expr get(Symbol key) {
            return find(this, key).map.get(key);
        }
        
        public Expr set(Symbol key, Expr value) {
            find(this, key).map.put(key, value);
            return value;
        }

        public Expr define(Symbol key, Expr value) {
            map.put(key, value);
            return value;
        }
        
        @Override
        public String toString() {
            return map.toString() + (next != null ? "->" + next : "");
        }
    }

    public interface Expr {
        default Expr eval(Env env) {
            return this;
        }
        default Expr apply(List args, Env env) {
            throw new RuntimeException("Cannot apply " + args + " to " + this);
        }
        default <T extends Expr> T as(Class<T> t) {
            if (t.isInstance(this))
                return t.cast(this);
            else
                throw new ClassCastException(
                    "Can't cast '%s' as %s".formatted(this, t.getSimpleName()));
        }

        default Cons asCons() {
            return as(Cons.class);
        }

        default Bool asBool() {
            return as(Bool.class);
        }

        default Symbol asSymbol() {
            return as(Symbol.class);
        }

        default Int asInt() {
            return as(Int.class);
        }
    }

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

    public static class Bool implements Atom {
        public static Bool TRUE = new Bool(true);
        public static Bool FALSE = new Bool(false);

        public final boolean value;

        Bool(boolean value) {
            this.value = value;
        }

        public static Bool of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override
        public final String toString() {
            return value ? "true" : "false";
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
        public Expr eval(Env env) {
            return env.get(this);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public interface List extends Expr, Iterable<Expr> {
        public static List NIL = new List() {
            @Override
            public String toString() {
                return "()";
            }
            @Override
            public Iterator<Expr> iterator() {
                return Collections.emptyIterator();
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

        default int size() {
            int size = 0;
            for (Expr e = this; e instanceof Cons c; e = c.cdr)
                ++size;
            return size;
        }

        default Expr at(int index) {
            int i = 0;
            for (Expr e = this; e instanceof Cons c; e = c.cdr, ++i)
                if (i == index)
                    return c.car;
            throw new IndexOutOfBoundsException("index");
        }

        @Override
        default Iterator<Expr> iterator() {
            return new Iterator<Lisp.Expr>() {
                Expr e = List.this;

                @Override
                public boolean hasNext() {
                    return e instanceof Cons;
                }

                @Override
                public Expr next() {
                    if (!(e instanceof Cons c))
                        throw new NoSuchElementException();
                    Expr result = c.car;
                    e = c.cdr;
                    return result;
                }
            };
        }

        default Stream<Expr> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    public record Cons(Expr car, Expr cdr) implements List {
        public static Cons of(Expr car, Expr cdr) {
            Objects.requireNonNull(car, "car");
            Objects.requireNonNull(cdr, "cdr");
            return new Cons(car, cdr);
        }

        @Override
        public Expr eval(Env env) {
            return car.eval(env).apply((List)cdr, env);
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

        public static Reader of(java.io.Reader reader) {
            return new Reader(reader);
        }

        public static Reader of(String source) {
            return Reader.of(new StringReader(source));
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
                if (e == EOF)
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

        Atom symbol() {
            while (isSymbolRest(ch))
                get();
            String value = buffer.substring(0, buffer.length() - 1);
            return switch (value) {
                case "true" -> Bool.TRUE;
                case "false" -> Bool.FALSE;
                default -> Symbol.of(value);
            };
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

    public interface Applicable extends Expr {
        Expr apply(List args, Env env);
    }

    public interface Procedure extends Atom,  Applicable {

        Expr apply(List args);
        
        static List evlis(Expr args, Env env) {
            return args instanceof Cons c
                ? new Cons(c.car().eval(env), evlis(c.cdr(), env))
                : List.NIL;
        }

        @Override
        default Expr apply(List args, Env env) {
            return apply(evlis(args, env));
        }
    }

    public static class Closure implements Procedure {
        final Expr parms, body;
        final Env env;

        Closure(Expr parms, Expr body, Env env) {
            this.parms = parms;
            this.body = body;
            this.env = env;
        }

        public static Closure of(Expr parms, Expr body, Env env) {
            return new Closure(parms, body, env);
        }

        static void pairlis(Expr parms, List args, Env env) {
            for (; parms instanceof Cons p; parms = p.cdr(), args = (List)args.asCons().cdr())
                env.define((Symbol)p.car(), args.asCons().car());
            if (parms != List.NIL)
                env.define((Symbol)parms, args);
        }

        static Expr progn(Expr body, Env env) {
            Expr result = List.NIL;
            for (Expr b = body; b instanceof Cons c; b = c.cdr)
                result = c.car().eval(env);
            return result;
            // if (!(body instanceof Cons b))
            //     return body.eval(env);
            // if (b.cdr() == List.NIL)
            //     return b.car().eval(env);
            // b.car().eval(env);
            // return progn(b.cdr(), env);
        }

        @Override
        public Expr apply(List args) {
            Env e = Env.of(env);
            pairlis(parms, args, e);
            return progn(body, e);
        }

        @Override
        public String toString() {
            return Cons.of(Symbol.of("closure"), Cons.of(parms, body)).toString();
        }
    }

    static void special(Env e, String name, Applicable value) {
        e.define(Symbol.of(name), value);
    }

    static void procedure(Env e, String name, Procedure value) {
        e.define(Symbol.of(name), value);
    }

    static Int intOperator(List args, int unit, IntBinaryOperator operator) {
        return Int.of(args.stream().mapToInt(a -> a.asInt().value()).reduce(unit, operator));
    }

    static Int intOperator2(List args, int unit, IntBinaryOperator operator) {
        IntStream is = args.stream().mapToInt(a -> a.asInt().value());
        return Int.of(args.size() <= 1 ? is.reduce(unit, operator) : is.reduce(operator).getAsInt());
    }

    public interface IntBinaryPredicate {
        boolean test(int a, int b);
    }

    static Bool intCompare(List args, IntBinaryPredicate predicate) {
        return Bool.of(predicate.test(args.at(0).asInt().value, args.at(1).asInt().value));
    }

    public static Env ENV = Env.of();
    static {
        special(ENV, "quote", (args, e) -> args.at(0));
        special(ENV, "lambda", (args, e) -> Closure.of(args.at(0), args.asCons().cdr(), e));
        special(ENV, "if", (args, e) -> args.at(0).eval(e).asBool().value
             ? args.at(1).eval(e)
             : args.size() == 3 ? args.at(2).eval(e) : List.NIL);
        special(ENV, "set", (args, e) -> e.set(args.at(0).asSymbol(), args.at(1).eval(e)));
        special(ENV, "define", (args, e) -> args.at(0) instanceof Symbol
            ? e.define(args.at(0).asSymbol(), args.at(1).eval(e))
            : e.define(args.at(0).asCons().car().asSymbol(),
                Closure.of(args.at(0).asCons().cdr(), args.asCons().cdr(), e)));
        special(ENV, "and", (args, e) -> {
            Expr result = Bool.TRUE;
            for (Expr x : args)
                if ((result = x.eval(e)) == Bool.FALSE)
                    return Bool.FALSE;
            return result;
        });
        special(ENV, "or", (args, e) -> {
            Expr result = Bool.FALSE;
            for (Expr x : args)
                if ((result = x.eval(e)) != Bool.FALSE)
                    return result;
            return Bool.FALSE;
        });
        procedure(ENV, "car", args -> args.at(0).asCons().car());
        procedure(ENV, "cdr", args -> args.at(0).asCons().cdr());
        procedure(ENV, "cons", args -> Cons.of(args.at(0), args.at(1)));
        procedure(ENV, "list", args -> args);
        procedure(ENV, "begin", args -> args instanceof Cons ? args.at(args.size() - 1) : List.NIL);
        procedure(ENV, "not", args -> Bool.of(!args.at(0).asBool().value));
        procedure(ENV, "+", args -> intOperator(args, 0, (a, b) -> a + b));
        procedure(ENV, "-", args -> intOperator2(args, 0, (a, b) -> a - b));
        procedure(ENV, "*", args -> intOperator(args, 1, (a, b) -> a * b));
        procedure(ENV, "/", args -> intOperator2(args, 1, (a, b) -> a / b));
        procedure(ENV, "=", args -> intCompare(args, (a, b) -> a == b));
        procedure(ENV, "!=", args -> intCompare(args, (a, b) -> a != b));
        procedure(ENV, "<", args -> intCompare(args, (a, b) -> a < b));
        procedure(ENV, "<=", args -> intCompare(args, (a, b) -> a <= b));
        procedure(ENV, ">", args -> intCompare(args, (a, b) -> a > b));
        procedure(ENV, ">=", args -> intCompare(args, (a, b) -> a >= b));
    }
}
