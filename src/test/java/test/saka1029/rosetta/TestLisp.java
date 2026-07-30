package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import static saka1029.rosetta.Lisp.*;

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

    static java.util.List<Expr> jlist(Expr... exprs) {
        return java.util.List.of(exprs);
    }

    static Expr parseFirst(String s) {
        return ((Cons)parse(s)).car();
    }

    @Test
    public void testReadSymbol() {
        assertEquals(symbol("a.bc"), parseFirst("a.bc"));
        assertEquals(symbol("abc"), parseFirst("abc"));
        assertEquals(symbol("abc"), parseFirst("abc  "));
        assertEquals(symbol("abc"), parseFirst("   abc  "));
        assertEquals(symbol("abc12"), parseFirst("   abc12  "));
        assertEquals(symbol("#<"), parseFirst(" #< "));
        assertEquals(symbol("-"), parseFirst(" - "));
        assertEquals(symbol("**"), parseFirst(" ** "));
    }

    @Test
    public void testReadInt() {
        assertEquals(integer(123), parseFirst("123"));
        assertEquals(integer(123), parseFirst("123  "));
        assertEquals(integer(123), parseFirst("  123  "));
        assertEquals(integer(-123), parseFirst("-123"));
        assertEquals(integer(-123), parseFirst("-123  "));
        assertEquals(integer(-123), parseFirst("  -123  "));
        assertEquals(list(symbol("-"), integer(123)), parse("  -  123  "));
    }

    @Test
    public void testReadList() {
        assertEquals(List.NIL, parseFirst(" ( ) "));
        assertEquals(list(integer(123)), parseFirst("(123)"));
        assertEquals(list(integer(123)), parseFirst("(123  )"));
        assertEquals(list(integer(123)), parseFirst("(   123   )"));
        assertEquals(list(integer(123)), parseFirst("   (   123   )"));
        assertEquals(list(integer(123)), parseFirst("   (   123   )   "));
        assertEquals(list(list(symbol("a"))), parseFirst("((a))"));
    }

    @Test
    public void testReadDotPair() {
        assertEquals(cons(symbol("a"), symbol("b")), parseFirst("(a . b)"));
        assertEquals(cons(symbol("a"), symbol("b")), parseFirst("(a .b)"));
        assertEquals(list(symbol("a."), symbol("b")), parseFirst("(a. b)"));
        assertEquals(list(symbol("a.b")), parseFirst("(a.b)"));
        assertEquals(cons(symbol("a"), list(symbol("b"))), parseFirst("(a .(b))"));
    }

    @Test
    public void testToString() {
        assertEquals("12345", parseFirst("  12345").toString());
        assertEquals("abc.def", parseFirst("  abc.def").toString());
        assertEquals("()", parseFirst("()").toString());
        assertEquals("(a b)", parseFirst("(a b)").toString());
        assertEquals("(1 2 3)", parseFirst("(1 2 3)").toString());
        assertEquals("(a . b)", parseFirst("(a . b)").toString());
        assertEquals("'a", parseFirst("'a").toString());
        assertEquals("'(a b)", parseFirst("'(a b)").toString());
        assertEquals("'(a b)", parseFirst("(quote (a b))").toString());
        assertEquals("'quote", parseFirst("(quote quote)").toString());
        assertEquals("'(quote)", parseFirst("(quote (quote))").toString());
        assertEquals("(quote . cdr)", parseFirst("(quote . cdr)").toString());
        assertEquals("(quote a . b)", parseFirst("(quote a . b)").toString());
        assertEquals("(quote)", parseFirst("(quote)").toString());
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
            parseFirst("(a b");
            fail();
        } catch (RuntimeException e) {
            assertEquals("Unexpected EOF", e.getMessage());
        }
        try {
            parseFirst(")a b");
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
            parseFirst("(a b . a b)");
        } catch (RuntimeException e) {
            assertEquals("')' expected", e.getMessage());
        }
    }
}
