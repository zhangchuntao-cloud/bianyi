package compiler.demo;

public class SimplePrecedenceDemo {
    public static void run() {
        SimplePrecedence sp = new SimplePrecedence();
        sp.initGrammar();
        sp.computeFirstLastOp();
        sp.buildPrecedence();
        sp.print();
    }
}