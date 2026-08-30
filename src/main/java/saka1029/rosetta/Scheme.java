package saka1029.rosetta;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Scheme {

    private Scheme(){}

    public interface Expr{}

    public record Symbol(String name) implements Expr {}
    public static final Symbol QUOTE = new Symbol("quote");

    public static class Env {
        Symbol key; Expr value; Env prev;
        public Env(Symbol key, Expr value, Env prev) {
            this.key = key; this.value = value; this.prev = prev;
        }
    }

    public static Expr get(Env env, Symbol s) {
        for (Env e = env; e != null; e = e.prev)
            if (e.key.equals(s))
                return e.value;
        throw new RuntimeException("Env.get: Not found " + s);
    }

    public static void set(Env env, Symbol s, Expr v) {
        for (Env e = env; e != null; e = e.prev)
            if (e.key.equals(s))
                e.value = v;
        throw new RuntimeException("Env.set: Not found " + s);
    }

    public static Env define(Env env, Symbol s, Expr v) {
        return new Env(s, v, env);
    }

    public record Cons(Expr car, Expr cdr) implements Expr { }
    record Nil() implements Expr {}
    public static Expr NIL = new Nil();
    public static Expr car(Expr e) { return ((Cons)e).car; }
    public static Expr cdr(Expr e) { return ((Cons)e).cdr; }
    public record Int(int value) implements Expr {}
    public record Bool(boolean value) implements Expr {}
    public static final Bool TRUE = new Bool(true);
    public static final Bool FALSE = new Bool(false);

    static String printCons(Cons cons) {
        StringBuilder sb = new StringBuilder();
        if (cons.cdr instanceof Cons cdr && cons.car.equals(QUOTE) && cdr.cdr.equals(NIL))
            return sb.append("'").append(print(cdr.car)).toString();
        sb.append("(").append(print(cons.car));
        Expr e;
        for (e = cons.cdr; e instanceof Cons c; e = c.cdr)
            sb.append(" ").append(print(c.car));
        if (!e.equals(NIL))
            sb.append(" . ").append(print(e));
        return sb.append(")").toString();
    }

    public static String print(Expr e) {
        return switch (e) {
            case Symbol s -> s.name;
            case Bool b -> "" + b.value;
            case Int i -> "" + i.value;
            case Nil n -> "()";
            case Cons c -> printCons(c);
            default -> throw new RuntimeException("Unknown type " + e);
        };
    }

    public interface Apply extends Expr {
        Expr apply(Expr args, Env env);
    }


    public static Expr eval(Expr e, Env env) {
        return switch (e) {
            case Symbol s -> get(env, s);
            case Bool b -> b;
            case Int i -> i;
            case Nil n -> n;
            case Cons c -> {
                Expr head = eval(c.car, env);
                if (head instanceof Apply app)
                    yield app.apply(c.cdr, env);
                else
                    throw new RuntimeException("Cannot apply " + print(head) + " to " + print(c.cdr));
            }
            default -> throw new RuntimeException("Unknown type " + print(e));
        };
    }

    public static Expr list(Expr... list) {
        Expr r = NIL;
        for (int i = list.length - 1; i >= 0; --i)
            r = new Cons(list[i], r);
        return r;
    }

    public static Expr list(Expr dot, List<Expr> list) {
        Expr r = dot;
        for (int i = list.size() - 1; i >= 0; --i)
            r = new Cons(list.get(i), r);
        return r;
    }

    public static Expr evlis(Expr args, Env env) {
        List<Expr> list = new ArrayList<>();
        for (Expr e = args; e instanceof Cons c; e = c.cdr)
            list.add(eval(c.car, env));
        return list(NIL, list);
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

        Expr list() {
            getClear();     // skip '('
            List<Expr> list = new ArrayList<>();
            while (true) {
                spaces();
                if (ch == ')') {
                    getClear();  // skip ')'
                    return Scheme.list(NIL, list);
                } else if (ch == '.') {
                    getClear();  // skip '.'
                    Expr result = Scheme.list(read(), list);
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

        Expr quote() {
            getClear();  // skip '\''
            return Scheme.list(Scheme.QUOTE, read());
        }

        static boolean isDigit(int ch) {
            return ch >= '0' && ch <= '9';
        }

        Int integer() {
            while (isDigit(ch))
                get();
            return new Int(Integer.parseInt(buffer.substring(0, buffer.length() - 1)));
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

        Expr symbol() {
            while (isSymbolRest(ch))
                get();
            String value = buffer.substring(0, buffer.length() - 1);
            return switch (value) {
                case "true" -> Scheme.TRUE;
                case "false" -> Scheme.FALSE;
                default -> new Symbol(value);
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
                return isDigit(get()) ? integer() : new Symbol("-");
            else if (isDigit(ch))
                return integer();
            else if (isSymbolFirst(ch))
                return symbol();
            else 
                throw new RuntimeException("Unexpected character '%c'".formatted((char)ch));
        }
    }
}
