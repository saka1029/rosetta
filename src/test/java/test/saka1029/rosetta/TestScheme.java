package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static saka1029.rosetta.Scheme.*;

import org.junit.Test;

public class TestScheme {

    @Test
    public void testEval() {
        Env env = define(null, sym("a"), i(3));
        assertEquals(i(3), eval(sym("a"), env));
        assertEquals(i(3), eval(i(3), env));
        assertEquals(TRUE, eval(TRUE, env));
        assertEquals(FALSE, eval(FALSE, env));
        env = define(env, sym("+"), (Apply)(a, e) -> {
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
    }

}
