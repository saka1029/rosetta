package test.saka1029.rosetta;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TestLisp {

    public interface Expr {}

    public interface Atom extends Expr {}

    public record Int(int value) implements Atom {}

    public static class Symbol implements Atom {
        public final String value;
        static final Map<String, Symbol> all = new HashMap<>();
        private Symbol(String value) {
            this.value = value;
        }
        public static Symbol of(String value) {
            return all.computeIfAbsent(value, k -> new Symbol(value));
        }
    }

    public interface List extends Expr {
        public static List of(LinkedList<Expr> list) {
            List result = Nil.NIL;
            ListIterator<Expr> it = list.listIterator(list.size());
            while (it.hasPrevious())
                result = new Cons(it.previous(), result);
            return result;
        }
    }

    public static class Nil implements List {
        public static Nil NIL = new Nil();
        private Nil() {}
    }

    public record Cons(Expr car, List cdr) implements List {}

    static final class Parser {
        int[] in;
        int start, ch;

        static boolean isDigit(int ch) {
            return ch >= '0' && ch <= '9';
        }

        static boolean isSymbolFirst(int ch) {
            return switch (ch) {
                case '(', ')', '.' -> false;
                default -> !Character.isWhitespace(ch) && !isDigit(ch);
            };
        }

        static boolean isSymbolRest(int ch) {
            return isSymbolFirst(ch) || isDigit(ch);
        }

        int get() {
            return ch = start < in.length ? in[start++] : -1;
        }

        void spaces() {
            while (Character.isWhitespace(ch))
                get();
        }

        List list() {
            LinkedList<Expr> result = new LinkedList<>();
            for (;;) {
                Expr e = parse();
                if (e == null)
                    return;
            }

        }

        Symbol symbol() {

        }

        Expr parse() {
            spaces();
            int begin = start;
            return switch (ch) {
                case -1 -> null;
                case '(' -> list ();
                case ')' -> throw new RuntimeException();
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    while (isDigit(ch))
                        get();
                    yield new Int(Integer.parseInt(new String(in, begin, start - begin)));
                }
                default -> symbol();
            };
        }

        java.util.List<Expr> parse(String source) {
            this.in = source.codePoints().toArray();
            this.start = 0;
            this.ch = get();
            java.util.List<Expr> result = new ArrayList<>();
            for (Expr e = parse(); e != null; e = parse())
                result.add(parse());
            return result;
        }
    }

    static final Parser parser = new Parser();

}
