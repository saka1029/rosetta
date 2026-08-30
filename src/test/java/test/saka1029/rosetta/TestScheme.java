package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;
import static saka1029.rosetta.Scheme.*;

import org.junit.Test;

public class TestScheme {

    static Symbol sym(String name) { return new Symbol(name);}
    static Int i(int value) { return new Int(value);}
    static Int i(Expr e) { return (Int)e;}

    @Test
    public void testEval() {
        Env env = define(null, sym("a"), i(3));
        assertEquals(i(3), eval(sym("a"), env));
        assertEquals(i(3), eval(i(3), env));
        assertEquals(TRUE, eval(TRUE, env));
        assertEquals(FALSE, eval(FALSE, env));
        env = define(env, sym("+"), (Apply)(a, e) -> {
            Expr evaled = evlis(a, e);
            return i(i(car(evaled)).value() + i(car(cdr(evaled))).value());
        });
        assertEquals(i(3), eval(list(sym("+"), i(1), i(2)), env));
    }
}
