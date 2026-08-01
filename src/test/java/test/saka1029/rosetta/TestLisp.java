package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import saka1029.rosetta.Lisp.Applicable;
import saka1029.rosetta.Lisp.Closure;
import saka1029.rosetta.Lisp.Cons;
import saka1029.rosetta.Lisp.Env;
import saka1029.rosetta.Lisp.Expr;
import saka1029.rosetta.Lisp.Int;
import saka1029.rosetta.Lisp.List;
import saka1029.rosetta.Lisp.Procedure;
import saka1029.rosetta.Lisp.Reader;
import saka1029.rosetta.Lisp.Symbol;

public class TestLisp {

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

    static Expr read(String source) {
        return Reader.of(source).read();
    }

    @Test
    public void testReadSymbol() {
        assertEquals(symbol("a.bc"), read("a.bc"));
        assertEquals(symbol("abc"), read("abc"));
        assertEquals(symbol("abc"), read("abc  "));
        assertEquals(symbol("abc"), read("   abc  "));
        assertEquals(symbol("abc12"), read("   abc12  "));
        assertEquals(symbol("#<"), read(" #< "));
        assertEquals(symbol("-"), read(" - "));
        assertEquals(symbol("**"), read(" ** "));
    }

    @Test
    public void testReadInt() {
        assertEquals(integer(123), read("123"));
        assertEquals(integer(123), read("123  "));
        assertEquals(integer(123), read("  123  "));
        assertEquals(integer(-123), read("-123"));
        assertEquals(integer(-123), read("-123  "));
        assertEquals(integer(-123), read("  -123  "));
        Reader reader = Reader.of("   -   123  ");
        assertEquals(symbol("-"), reader.read());
        assertEquals(integer(123), reader.read());
    }

    @Test
    public void testReadList() {
        assertEquals(List.NIL, read(" ( ) "));
        assertEquals(list(integer(123)), read("(123)"));
        assertEquals(list(integer(123)), read("(123  )"));
        assertEquals(list(integer(123)), read("(   123   )"));
        assertEquals(list(integer(123)), read("   (   123   )"));
        assertEquals(list(integer(123)), read("   (   123   )   "));
        assertEquals(list(list(symbol("a"))), read("((a))"));
    }

    @Test
    public void testReadDotPair() {
        assertEquals(cons(symbol("a"), symbol("b")), read("(a . b)"));
        assertEquals(cons(symbol("a"), symbol("b")), read("(a .b)"));
        assertEquals(list(symbol("a."), symbol("b")), read("(a. b)"));
        assertEquals(list(symbol("a.b")), read("(a.b)"));
        assertEquals(cons(symbol("a"), list(symbol("b"))), read("(a .(b))"));
    }

    @Test
    public void testToString() {
        assertEquals("12345", read("  12345").toString());
        assertEquals("abc.def", read("  abc.def").toString());
        assertEquals("()", read("()").toString());
        assertEquals("(a b)", read("(a b)").toString());
        assertEquals("(1 2 3)", read("(1 2 3)").toString());
        assertEquals("(a . b)", read("(a . b)").toString());
        assertEquals("'a", read("'a").toString());
        assertEquals("'(a b)", read("'(a b)").toString());
        assertEquals("'(a b)", read("(quote (a b))").toString());
        assertEquals("'quote", read("(quote quote)").toString());
        assertEquals("'(quote)", read("(quote (quote))").toString());
        assertEquals("(quote . cdr)", read("(quote . cdr)").toString());
        assertEquals("(quote a . b)", read("(quote a . b)").toString());
        assertEquals("(quote)", read("(quote)").toString());
    }

    @Test
    public void testListException() {
        try {
            List.list(symbol("abc"));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("exprs", e.getMessage());
        }
        try {
            read("(a b");
            fail();
        } catch (RuntimeException e) {
            assertEquals("Unexpected EOF", e.getMessage());
        }
        try {
            read(")a b");
            fail();
        } catch (RuntimeException e) {
            assertEquals("Unexpected character ')'", e.getMessage());
        }
    }

    @Test
    public void testSymbolException() {
        try {
            symbol(null);
        } catch (NullPointerException e) {
            assertEquals("value", e.getMessage());
        }
    }

    @Test
    public void testConsException() {
        try {
            Cons.of(null, symbol("b"));
        } catch (NullPointerException e) {
            assertEquals("car", e.getMessage());
        }
        try {
            Cons.of(symbol("a"), null);
        } catch (NullPointerException e) {
            assertEquals("cdr", e.getMessage());
        }
    }

    @Test
    public void testDotPairException() {
        try {
            read("(a b . a b)");
        } catch (RuntimeException e) {
            assertEquals("')' expected", e.getMessage());
        }
    }

    @Test
    public void testReader() {
        Reader reader = Reader.of("(a b)(c d)123");
        assertEquals(list(symbol("a"), symbol("b")), reader.read());
        assertEquals(list(symbol("c"), symbol("d")), reader.read());
        assertEquals(integer(123), reader.read());
        assertEquals(Reader.EOF, reader.read());
    }

    @Test
    public void testExprAs() {
        assertEquals(symbol("a"), symbol("a").as(Symbol.class));
        try {
            list(symbol("a")).as(Symbol.class);
            fail();
        } catch (ClassCastException e) {
            assertEquals("Can't cast '(a)' as Symbol", e.getMessage());
        }
        assertEquals(cons(symbol("a"), List.NIL), list(symbol("a")).asCons());
        try {
            symbol("a").asCons();
            fail();
        } catch (ClassCastException e) {
            assertEquals("Can't cast 'a' as Cons", e.getMessage());
        }
    }

    @Test
    public void testListSize() {
        assertEquals(2, list(symbol("a"), symbol("b")).size());
        assertEquals(2, list(symbol("a"), list(symbol("b"))).size());
        assertEquals(2, cons(symbol("a"), cons(symbol("b"), List.NIL)).size());
    }

    @Test
    public void testListAt() {
        assertEquals(symbol("a"), list(symbol("a"), symbol("b")).at(0));
        assertEquals(symbol("b"), list(symbol("a"), symbol("b")).at(1));
        try {
            list(symbol("a"), symbol("b")).at(2);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertEquals("index", e.getMessage());
        }
        assertEquals(symbol("a"), cons(symbol("a"), cons(symbol("b"), List.NIL)).at(0));
        assertEquals(symbol("b"), cons(symbol("a"), cons(symbol("b"), List.NIL)).at(1));
    }

    @Test
    public void testEval() {
        Env env = Env.of();
        env.define(Symbol.QUOTE, (Applicable) (args, e) -> args.at(0));
        env.define(symbol("lambda"), (Applicable) (args, e) -> Closure.of(args.at(0), args.asCons().cdr(), e));
        env.define(symbol("car"), (Procedure) args -> args.at(0).asCons().car());
        env.define(symbol("cdr"), (Procedure) args -> args.at(0).asCons().cdr());
        env.define(symbol("cons"), (Procedure) args -> Cons.of(args.at(0), args.at(1)));
        env.define(symbol("+"), (Procedure) args -> Int.of(args.stream().mapToInt(e -> e.asInt().value()).reduce(0, (a, b) -> a + b)));
        env.define(symbol("*"), (Procedure) args -> Int.of(args.stream().mapToInt(e -> e.asInt().value()).reduce(1, (a, b) -> a * b)));
        assertEquals(list(symbol("a"), symbol("b")), read("'(a b)").eval(env));
        assertEquals(cons(symbol("a"), symbol("b")), read("(cons 'a 'b)").eval(env));
        assertEquals(symbol("a"), read("(car '(a b c))").eval(env));
        assertEquals(list(symbol("b"), symbol("c")), read("(cdr '(a b c))").eval(env));
        assertEquals(symbol("a"), read("((lambda (x) (car x)) '(a b c))").eval(env));
        assertEquals(Int.of(10), read("(+ 1 2 3 4)").eval(env));
        assertEquals(Int.of(24), read("(* 1 2 3 4)").eval(env));
    }
}
