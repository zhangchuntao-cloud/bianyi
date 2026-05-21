package compiler.demo;

import java.util.*;

public class LR0Demo {
    static class Prod {
        char lhs;
        String rhs;
        Prod(char l, String r) { lhs = l; rhs = r; }
    }

    static class Item {
        int prod;
        int dot;
        Item(int p, int d) { prod = p; dot = d; }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Item)) return false;
            Item i = (Item) o;
            return prod == i.prod && dot == i.dot;
        }
        @Override
        public int hashCode() { return Objects.hash(prod, dot); }
    }

    public static void run() {
        List<String> grammarStrs = List.of(
                "S->E", "E->E+T", "E->T", "T->T*F", "T->F", "F->(E)", "F->id", "F->num"
        );
        List<Prod> prods = new ArrayList<>();
        Map<Character, List<Integer>> prodByLHS = new HashMap<>();
        for (String g : grammarStrs) {
            char lhs = g.charAt(0);
            String rhs = g.substring(g.indexOf("->")+2);
            prods.add(new Prod(lhs, rhs));
            prodByLHS.computeIfAbsent(lhs, k -> new ArrayList<>()).add(prods.size()-1);
        }
        Set<Character> VN = new HashSet<>(), VT = new HashSet<>();
        for (Prod p : prods) {
            VN.add(p.lhs);
            for (char c : p.rhs.toCharArray()) {
                if (Character.isUpperCase(c)) VN.add(c);
                else if (c != '|') VT.add(c);
            }
        }
        VT.add('#');
        VN.add('S');
        List<Character> symbols = new ArrayList<>(VT);
        symbols.addAll(VN);
        Map<Character, Integer> symId = new HashMap<>();
        for (int i = 0; i < symbols.size(); i++) symId.put(symbols.get(i), i);

        java.util.function.Function<Set<Item>, Set<Item>> closure = (items) -> {
            Set<Item> result = new HashSet<>(items);
            boolean changed;
            do {
                changed = false;
                Set<Item> newItems = new HashSet<>(result);
                for (Item it : result) {
                    if (it.dot < prods.get(it.prod).rhs.length()) {
                        char next = prods.get(it.prod).rhs.charAt(it.dot);
                        if (VN.contains(next)) {
                            for (int pid : prodByLHS.getOrDefault(next, List.of())) {
                                Item ni = new Item(pid, 0);
                                if (!newItems.contains(ni)) {
                                    newItems.add(ni);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
                result = newItems;
            } while (changed);
            return result;
        };
        java.util.function.BiFunction<Set<Item>, Character, Set<Item>> go = (items, sym) -> {
            Set<Item> kernel = new HashSet<>();
            for (Item it : items) {
                if (it.dot < prods.get(it.prod).rhs.length() && prods.get(it.prod).rhs.charAt(it.dot) == sym) {
                    kernel.add(new Item(it.prod, it.dot+1));
                }
            }
            return closure.apply(kernel);
        };
        Set<Item> startItems = new HashSet<>();
        for (int pid : prodByLHS.get('S')) startItems.add(new Item(pid, 0));
        startItems = closure.apply(startItems);
        List<Set<Item>> states = new ArrayList<>();
        Map<Set<Item>, Integer> stateMap = new HashMap<>();
        states.add(startItems);
        stateMap.put(startItems, 0);
        for (int i = 0; i < states.size(); i++) {
            Set<Character> allSyms = new HashSet<>();
            for (Item it : states.get(i)) {
                if (it.dot < prods.get(it.prod).rhs.length())
                    allSyms.add(prods.get(it.prod).rhs.charAt(it.dot));
            }
            for (char sym : allSyms) {
                Set<Item> nxt = go.apply(states.get(i), sym);
                if (nxt.isEmpty()) continue;
                if (!stateMap.containsKey(nxt)) {
                    stateMap.put(nxt, states.size());
                    states.add(nxt);
                }
            }
        }
        int stateNum = states.size();
        int symNum = symbols.size();
        String[][] action = new String[stateNum][symNum];
        int[][] gototable = new int[stateNum][symNum];
        for (int i = 0; i < stateNum; i++) Arrays.fill(gototable[i], -1);
        for (int i = 0; i < stateNum; i++) {
            for (Item it : states.get(i)) {
                if (it.dot == prods.get(it.prod).rhs.length()) {
                    if (prods.get(it.prod).lhs == 'S') {
                        action[i][symId.get('#')] = "acc";
                    } else {
                        for (char t : VT) {
                            if (t != '#')
                                action[i][symId.get(t)] = "r" + it.prod;
                        }
                        action[i][symId.get('#')] = "r" + it.prod;
                    }
                } else {
                    char sym = prods.get(it.prod).rhs.charAt(it.dot);
                    Set<Item> nxt = go.apply(states.get(i), sym);
                    if (nxt.isEmpty()) continue;
                    int ns = stateMap.get(nxt);
                    if (VT.contains(sym)) {
                        action[i][symId.get(sym)] = "s" + ns;
                    } else {
                        gototable[i][symId.get(sym)] = ns;
                    }
                }
            }
        }
        System.out.println("\n========== LR(0) 分析 ==========");
        System.out.println("Grammar:");
        for (int i = 0; i < prods.size(); i++) {
            System.out.println(i + ": " + prods.get(i).lhs + " -> " + prods.get(i).rhs);
        }
        System.out.println("\nACTION table:");
        System.out.print("State\t");
        for (char t : VT) System.out.print(t + "\t");
        System.out.println();
        for (int i = 0; i < stateNum; i++) {
            System.out.print(i + "\t");
            for (char t : VT) {
                String act = action[i][symId.get(t)];
                if (act != null) System.out.print(act + "\t");
                else System.out.print("  \t");
            }
            System.out.println();
        }
        System.out.println("\nGOTO table:");
        System.out.print("State\t");
        for (char nt : VN) if (nt != 'S') System.out.print(nt + "\t");
        System.out.println();
        for (int i = 0; i < stateNum; i++) {
            System.out.print(i + "\t");
            for (char nt : VN) {
                if (nt == 'S') continue;
                int g = gototable[i][symId.get(nt)];
                if (g != -1) System.out.print(g + "\t");
                else System.out.print("  \t");
            }
            System.out.println();
        }
        String inputStr = "id+id*id#";
        System.out.println("\nParsing example: " + inputStr);
        Stack<Integer> stk = new Stack<>();
        stk.push(0);
        int ip = 0;
        while (true) {
            int s = stk.peek();
            char a = inputStr.charAt(ip);
            System.out.print("State " + s + " Input: " + a + " Action: ");
            String act = action[s][symId.get(a)];
            if (act == null) {
                System.out.println("Error");
                break;
            }
            System.out.println(act);
            if (act.equals("acc")) {
                System.out.println("Accept");
                break;
            } else if (act.charAt(0) == 's') {
                int ns = Integer.parseInt(act.substring(1));
                stk.push(ns);
                ip++;
            } else if (act.charAt(0) == 'r') {
                int pid = Integer.parseInt(act.substring(1));
                int len = prods.get(pid).rhs.length();
                for (int i = 0; i < len; i++) stk.pop();
                int top = stk.peek();
                char nonterm = prods.get(pid).lhs;
                int ns = gototable[top][symId.get(nonterm)];
                if (ns == -1) {
                    System.out.println("Error: no goto for " + nonterm);
                    break;
                }
                stk.push(ns);
            }
        }
    }
}