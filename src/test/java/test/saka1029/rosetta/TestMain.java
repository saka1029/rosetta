package test.saka1029.rosetta;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import saka1029.rosetta.Main;

public class TestMain {

    @Test
    public void testEvalInteger() {
        assertEquals(Integer.valueOf(6), Main.eval("(+ 1 2 3)"));
    }

    // @Test
    // public void testEvalSymbol() {
    //     assertEquals(Integer.valueOf(6), Main.eval("(def s 'z) s"));
    // }

    @Test
    public void testEvalTestLisp() {
        assertEquals(Integer.valueOf(144),
            Main.eval("""
                (def sum (lambda (a b) (+ a b)))

                (def ft 14)

                (def fib (lambda (n)
                (if (<= n 1)
                    n
                    (+ (fib (- n 1)) (fib (- n 2))))))

                (sum 12 (- ft 2))

                (fib 12)
            """));
    }

    // 関数は一度評価されるのでScheme的なLisp
    @Test
    public void testLambda() {
        assertEquals(Integer.valueOf(4), Main.eval("((lambda (a) (+ a a)) 2)"));
    }

}
