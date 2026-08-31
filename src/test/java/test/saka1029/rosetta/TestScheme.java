package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static saka1029.rosetta.Scheme.*;

import org.junit.Test;

public class TestScheme {

    @Test
    public void testEval() {
        Env env = new Env();
        define(env, sym("a"), i(3));
        assertEquals(i(3), eval(sym("a"), env));
        assertEquals(i(3), eval(i(3), env));
        assertEquals(TRUE, eval(TRUE, env));
        assertEquals(FALSE, eval(FALSE, env));
        define(env, sym("+"), (Apply)(a, e) -> {
            Expr evaled = evlis(a, e);
            return i(i(car(evaled)) + i(car(cdr(evaled))));
        });
        assertEquals(i(3), eval(list(sym("+"), i(1), i(2)), env));
    }

    static Expr read(String s) { return new Reader(s).read(); }

    @Test
    public void testRead() {
        assertEquals(list(i(1), sym("a")), read("(1 a)"));
        assertEquals(cons(i(1), sym("a")), read("(1 . a)"));
    }

    static Expr evalRead(String s, Env e) { return eval(read(s), e); }

    @Test
    public void evalRead() {
        Env env = defaultEnv();
        assertEquals(i(1), evalRead("(car '(1 a))", env));
        assertEquals(sym("a"), evalRead("(cdr '(1 . a))", env));
        assertEquals(FALSE, evalRead("(not true)", env));
        assertEquals(TRUE, evalRead("(not false)", env));
        assertEquals(FALSE, evalRead("(not (== 0 0))", env));
        assertEquals(sym("a"), evalRead("((lambda (a) (car a)) '(a b))", env));
        assertEquals(i(6), evalRead("(+ 1 2 3)", env));
        assertEquals(i(6), evalRead("(+ 1 2 (+ 1 2))", env));
        assertEquals(i(0), evalRead("(-)", env));
        assertEquals(i(-1), evalRead("(- 1)", env));
        assertEquals(i(-4), evalRead("(- 1 2 3)", env));
        assertEquals(TRUE, evalRead("(== 2 2)", env));
        assertEquals(FALSE, evalRead("(== 0 2)", env));
        define(env, sym("fact"), evalRead("(lambda (n) (if (<= n 0) 1 (* n (fact (- n 1)))))", env));
        assertEquals(i(1), evalRead("(fact 0)", env));
        assertEquals(i(1), evalRead("(fact 1)", env));
        assertEquals(i(2), evalRead("(fact 2)", env));
        assertEquals(i(6), evalRead("(fact 3)", env));
        assertEquals(sym("fact2"), evalRead("(define fact2 (lambda (n) (if (<= n 0) 1 (* n (fact (- n 1))))))", env));
        assertEquals(i(1), evalRead("(fact2 0)", env));
        assertEquals(i(1), evalRead("(fact2 1)", env));
        assertEquals(i(2), evalRead("(fact2 2)", env));
        assertEquals(i(6), evalRead("(fact2 3)", env));
        assertEquals(TRUE, evalRead("(and)", env));
        assertEquals(i(3), evalRead("(and 2 3)", env));
        assertEquals(FALSE, evalRead("(and false 3)", env));
        assertEquals(FALSE, evalRead("(or)", env));
        assertEquals(i(2), evalRead("(or 2 3)", env));
        assertEquals(i(3), evalRead("(or false 3)", env));
    }

}
