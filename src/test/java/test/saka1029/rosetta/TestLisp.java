package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


import org.junit.Test;

import static saka1029.rosetta.Lisp.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
    public void testReadException() {
        class IllegalReader extends java.io.Reader {
            @Override
            public void close() throws IOException { }
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("can't read");
            }
        }
        try {
            Reader.of(new IllegalReader());
            fail();
        } catch (RuntimeException e) {
            assertEquals("can't read", e.getCause().getMessage());
        }
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
        assertEquals("true", Bool.TRUE.toString());
        assertEquals("false", Bool.FALSE.toString());
        assertEquals("(closure (a) b)", Closure.of(list(symbol("a")), list(symbol("b")), Env.of()).toString());
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
    public void testListIterator() {
        try {
            List list = list(symbol("a"));
            Iterator<Expr> it = list.iterator();
            assertEquals(symbol("a"), it.next());
            it.next();
            fail();
        } catch (NoSuchElementException e) {
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
    public void testEnvToString() {
        Env env = Env.of();
        env.define(symbol("a"), integer(1));
        assertEquals("{a=1}", env.toString());
        Env env2 = Env.of(env);
        env2.define(symbol("b"), integer(2));
        assertEquals("{b=2}->{a=1}", env2.toString());
    }

    @Test
    public void testEnvFindException() {
        Env env = Env.of();
        env.define(symbol("a"), integer(1));
        Env env2 = Env.of(env);
        env2.define(symbol("b"), integer(2));
        try {
            env.get(symbol("c"));
            fail();
        } catch (RuntimeException e) {
            assertEquals("Variable c not found", e.getMessage());
        }
    }

    @Test
    public void testExprApply() {
        Env env = Env.of();
        try {
            symbol("a").apply(List.NIL, env);
        } catch (RuntimeException e) {
            assertEquals("Cannot apply () to a", e.getMessage());
        }
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
    public void testEvalQuote() {
        assertEquals(list(symbol("a"), symbol("b")), read("'(a b)").eval(ENV));
        assertEquals(symbol("a"), read("'a").eval(ENV));
    }

    @Test
    public void testEvalIf() {
        assertEquals(integer(1), read("(if true 1 2)").eval(ENV));
        assertEquals(integer(2), read("(if false 1 2)").eval(ENV));
        assertEquals(List.NIL, read("(if false 1)").eval(ENV));
    }

    @Test
    public void testEvalSet() {
        assertEquals(integer(1), read("(define v 1)").eval(ENV));
        assertEquals(integer(2), read("(set v 2)").eval(ENV));
        assertEquals(integer(2), read("v").eval(ENV));
    }

    @Test
    public void testEvalBegin() {
        assertEquals(integer(3), read("(begin 1 2 3)").eval(ENV));
        assertEquals(integer(3), read("(begin (define begin-var (+ 1 3)) 2 3)").eval(ENV));
        assertEquals(integer(4), ENV.get(symbol("begin-var")));
        assertEquals(List.NIL, read("(begin)").eval(ENV));
    }

    @Test
    public void testEvalNot() {
        assertEquals(Bool.FALSE, read("(not true)").eval(ENV));
        assertEquals(Bool.TRUE, read("(not false)").eval(ENV));
        assertEquals(Bool.FALSE, read("(not (< 1 2))").eval(ENV));
        assertEquals(Bool.TRUE, read("(not (< 2 1))").eval(ENV));
    }

    @Test
    public void testEvalDefine() {
        assertEquals(integer(3), read("(define three (+ 1 2))").eval(ENV));
        assertEquals(integer(3), ENV.get(symbol("three")));
        read("(define func (lambda (x) 1 (car x)))").eval(ENV);
        assertNotNull(ENV.get(symbol("func")));
        assertEquals(symbol("a"), read("(func '(a b))").eval(ENV));
        read("(define (func2 x) 1 (car x)))").eval(ENV);
        assertNotNull(ENV.get(symbol("func2")));
        assertEquals(symbol("a"), read("(func2 '(a b))").eval(ENV));
        read("(define (func3 . x) x)").eval(ENV);
        assertEquals(list(integer(1), integer(2), integer(3)), read("(func3 1 2 3)").eval(ENV));
        read("(define (func4 x . y) x)").eval(ENV);
        assertEquals(integer(1), read("(func4 1 2 3)").eval(ENV));
        read("(define (func5 x . y) y)").eval(ENV);
        assertEquals(list(integer(2), integer(3)), read("(func5 1 2 3)").eval(ENV));
        read("(define (func6 x y . z) (cons x z))").eval(ENV);
        assertEquals(list(integer(1), integer(3)), read("(func6 1 2 3)").eval(ENV));
    }

    @Test
    public void testEvalCarCdrCons() {
        assertEquals(symbol("a"), read("(car '(a b c))").eval(ENV));
        assertEquals(list(symbol("b"), symbol("c")), read("(cdr '(a b c))").eval(ENV));
        assertEquals(cons(symbol("a"), symbol("b")), read("(cons 'a 'b)").eval(ENV));
        assertEquals(list(symbol("a")), read("(cons 'a '())").eval(ENV));
    }

    @Test
    public void testEvalLambda() {
        assertEquals(symbol("a"), read("((lambda (x) (car x)) '(a b c))").eval(ENV));
        assertEquals(integer(3), read("((lambda (x) 1 2 3) '(a))").eval(ENV));
        assertEquals(list(symbol("b")), read("((lambda (x) (car x) (cdr x)) '(a b))").eval(ENV));
        assertEquals(list(integer(1), integer(2)), read("((lambda x x) 1 2)").eval(ENV));
        assertEquals(integer(1), read("((lambda (x . y) x) 1 2)").eval(ENV));
        assertEquals(list(integer(2)), read("((lambda (x . y) y) 1 2)").eval(ENV));
    }

    @Test
    public void testEvalList() {
        assertEquals(list(symbol("a"), symbol("b")), read("(list 'a 'b)").eval(ENV));
        assertEquals(list(cons(symbol("a"), symbol("b"))), read("(list '(a . b))").eval(ENV));
    }

    @Test
    public void testEvalArithmetic() {
        assertEquals(integer(0), read("(+)").eval(ENV));
        assertEquals(integer(1), read("(+ 1)").eval(ENV));
        assertEquals(integer(10), read("(+ 1 2 3 4)").eval(ENV));
        assertEquals(integer(0), read("(-)").eval(ENV));
        assertEquals(integer(-1), read("(- 1)").eval(ENV));
        assertEquals(integer(-8), read("(- 1 2 3 4)").eval(ENV));
        assertEquals(integer(1), read("(*)").eval(ENV));
        assertEquals(integer(2), read("(* 2)").eval(ENV));
        assertEquals(integer(24), read("(* 1 2 3 4)").eval(ENV));
        assertEquals(integer(1), read("(/)").eval(ENV));
        assertEquals(integer(1), read("(/ 1)").eval(ENV));
        assertEquals(integer(0), read("(/ 2)").eval(ENV));
        assertEquals(integer(5), read("(/ 40 2 4)").eval(ENV));
    }

    @Test
    public void testEvalCompare() {
        assertEquals(Bool.TRUE, read("(= 0 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(= 1 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(= 0 1)").eval(ENV));
        assertEquals(Bool.FALSE, read("(!= 0 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(!= 1 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(!= 0 1)").eval(ENV));
        assertEquals(Bool.FALSE, read("(< 0 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(< 1 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(< 0 1)").eval(ENV));
        assertEquals(Bool.TRUE, read("(<= 0 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(<= 1 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(<= 0 1)").eval(ENV));
        assertEquals(Bool.FALSE, read("(> 0 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(> 1 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(> 0 1)").eval(ENV));
        assertEquals(Bool.TRUE, read("(>= 0 0)").eval(ENV));
        assertEquals(Bool.TRUE, read("(>= 1 0)").eval(ENV));
        assertEquals(Bool.FALSE, read("(>= 0 1)").eval(ENV));
    }

    @Test
    public void testEvalAnd() {
        assertEquals(Bool.TRUE, read("(and)").eval(ENV));
        assertEquals(Bool.FALSE, read("(and false 1)").eval(ENV));
        assertEquals(integer(3), read("(and 1 2 3)").eval(ENV));
        assertEquals(Bool.FALSE, read("(and 1 2 3 false)").eval(ENV));
    }

    @Test
    public void testEvalOr() {
        assertEquals(Bool.FALSE, read("(or)").eval(ENV));
        assertEquals(integer(1), read("(or false 1)").eval(ENV));
        assertEquals(integer(1), read("(or 1 2 3)").eval(ENV));
        assertEquals(Bool.FALSE, read("(or false false false)").eval(ENV));
    }

    @Test
    public void testEvalRecursion() {
        read("(define (fact n) (if (<= n 0) 1 (* n (fact (- n 1)))))").eval(ENV);
        assertEquals(integer(1), read("(fact 0)").eval(ENV));
        assertEquals(integer(1), read("(fact 1)").eval(ENV));
        assertEquals(integer(2), read("(fact 2)").eval(ENV));
        assertEquals(integer(6), read("(fact 3)").eval(ENV));
        assertEquals(integer(24), read("(fact 4)").eval(ENV));
    }
}
