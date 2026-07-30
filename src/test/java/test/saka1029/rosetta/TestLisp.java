package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;

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
        assertEquals(jlist(list()), parse(" ( ) "));
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

    @Test
    public void testToString() {
        assertEquals("12345", parse("  12345").get(0).toString());
        assertEquals("abc.def", parse("  abc.def").get(0).toString());
        assertEquals("()", parse("()").get(0).toString());
        assertEquals("(a b)", parse("(a b)").get(0).toString());
        assertEquals("(1 2 3)", parse("(1 2 3)").get(0).toString());
        assertEquals("(a . b)", parse("(a . b)").get(0).toString());
        assertEquals("'a", parse("'a").get(0).toString());
        assertEquals("'(a b)", parse("'(a b)").get(0).toString());
        assertEquals("'(a b)", parse("(quote (a b))").get(0).toString());
        assertEquals("'quote", parse("(quote quote)").get(0).toString());
        assertEquals("'(quote)", parse("(quote (quote))").get(0).toString());
        assertEquals("(quote . cdr)", parse("(quote . cdr)").get(0).toString());
        assertEquals("(quote a . b)", parse("(quote a . b)").get(0).toString());
        assertEquals("(quote)", parse("(quote)").get(0).toString());
    }
}
