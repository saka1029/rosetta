package saka1029.rosetta;

public class Scheme {

    private Scheme(){}

    interface Expr{}

    record Symbol(String name) implements Expr {}
    static Symbol QUOTE = new Symbol("quote");

    static class Env {
        Symbol key; Expr value; Env prev;
        public Env(Symbol key, Expr value, Env prev) {
            this.key = key; this.value = value; this.prev = prev;
        }
    }

    static Expr get(Env env, Symbol s) {
        for (Env e = env; e != null; e = e.prev)
            if (e.key.equals(s))
                return e.value;
        throw new RuntimeException("Env.get: Not found " + s);
    }

    static void set(Env env, Symbol s, Expr v) {
        for (Env e = env; e != null; e = e.prev)
            if (e.key.equals(s))
                e.value = v;
        throw new RuntimeException("Env.set: Not found " + s);
    }

    static Env define(Env env, Symbol s, Expr v) {
        return new Env(s, v, env);
    }

    record Cons(Expr car, Expr cdr) implements Expr { }
    record Nil() implements Expr {}
    static Expr NIL = new Nil();
    static Expr car(Expr e) { return ((Cons)e).car; }
    static Expr cdr(Expr e) { return ((Cons)e).cdr; }
    record Int(int value) implements Expr {}
    record Bool(boolean value) implements Expr {}
    static final Bool TRUE = new Bool(true);
    static final Bool FALSE = new Bool(false);

    String printCons(Cons cons) {
        StringBuilder sb = new StringBuilder();
        if (cons.cdr instanceof Cons cdr && cons.car.equals(QUOTE) && cdr.cdr.equals(NIL))
            return sb.append("'").append(print(cdr.car)).toString();
        sb.append("(").append(print(cons.car));
        Expr e;
        for (e = cons.cdr; e instanceof Cons c; e = c.cdr)
            sb.append(" ").append(print(c.car));
        if (!e.equals(NIL))
            sb.append(" . ").append(print(e));
        return sb.append(")").toString();
    }

    String print(Expr e) {
        return switch (e) {
            case Symbol s -> s.name;
            case Bool b -> "" + b.value;
            case Int i -> "" + i.value;
            case Nil n -> "()";
            case Cons c -> printCons(c);
            default -> throw new RuntimeException("Unknown type " + e)
        }
    }

}
