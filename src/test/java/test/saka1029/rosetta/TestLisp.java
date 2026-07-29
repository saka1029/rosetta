package test.saka1029.rosetta;

import java.util.HashMap;
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

    public interface List extends Expr {}

    public static class Nil implements List {
        Nil NIL = new Nil();
        private Nil() {}
    }

    public record Cons(Expr car, List cdr) implements List {}

    static final class Parser {
        String in;
        int start, end;

        List list() {

        }

        Symbol symbol() {

        }

        Expr parse() {
            while (Character.isWhitespace(in.charAt(start)))
                ++start;
            int begin = start;
            return switch (in.charAt(start)) {
                case '(' -> list();
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    while (Character.isDigit(in.charAt(start)))
                        ++start;
                    yield new Int(Integer.parseInt(in.substring(begin, start)));
                }
                default -> symbol();
            };
        }

        Expr parse(String source) {
            this.in = source;
            this.start = 0;
            this.end = source.length();
            return parse();
        }
    }

    static final Parser parser = new Parser();

}
