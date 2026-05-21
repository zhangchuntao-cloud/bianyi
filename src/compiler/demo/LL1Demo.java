package compiler.demo;

import java.util.*;

public class LL1Demo {
    public static void run() {
        LL1ExprGenerator gen = new LL1ExprGenerator();
        gen.print();
        List<String> tokens = List.of("id", "+", "num", "*", "id");
        System.out.println("\nParsing example: id + num * id");
        Stack<String> stk = new Stack<>();
        stk.push("$");
        stk.push("E");
        int ip = 0;
        List<String> input = new ArrayList<>(tokens);
        input.add("$");
        while (!stk.isEmpty()) {
            String top = stk.peek();
            String cur = input.get(ip);
            System.out.print("Stack top: " + top + "   Input: " + cur + "   ");
            if (top.equals("$") && cur.equals("$")) {
                System.out.println("Accept");
                break;
            }
            if (gen.terminals.contains(top) || top.equals("$")) {
                if (top.equals(cur)) {
                    System.out.println("Match " + top);
                    stk.pop();
                    ip++;
                } else {
                    System.out.println("Error: expected " + top);
                    break;
                }
            } else {
                var entry = new AbstractMap.SimpleEntry<>(top, cur);
                var prod = gen.table.get(entry);
                if (prod != null) {
                    System.out.print("Expand " + prod.lhs + " ->");
                    for (String s : prod.rhs) System.out.print(" " + s);
                    System.out.println();
                    stk.pop();
                    if (!prod.rhs.get(0).equals("ε")) {
                        for (int i = prod.rhs.size()-1; i >= 0; i--)
                            stk.push(prod.rhs.get(i));
                    }
                } else {
                    System.out.println("Error: no rule");
                    break;
                }
            }
        }
    }
}