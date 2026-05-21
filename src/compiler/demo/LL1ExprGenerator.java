package compiler.demo;

import java.util.*;

public class LL1ExprGenerator {
    public static class Production {
        public String lhs;
        public List<String> rhs;
        public Production(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public List<Production> grammar = new ArrayList<>();
    public Set<String> nonterminals = new HashSet<>();
    public Set<String> terminals = new HashSet<>();
    public Map<String, Set<String>> first = new HashMap<>();
    public Map<String, Set<String>> follow = new HashMap<>();
    public Map<String, Set<String>> select = new HashMap<>();
    public Map<Map.Entry<String, String>, Production> table = new HashMap<>();

    public LL1ExprGenerator() {
        nonterminals.addAll(Set.of("E", "E'", "T", "T'", "F"));
        terminals.addAll(Set.of("+", "-", "*", "/", "(", ")", "id", "num"));
        grammar.add(new Production("E", List.of("T", "E'")));
        grammar.add(new Production("E'", List.of("+", "T", "E'")));
        grammar.add(new Production("E'", List.of("-", "T", "E'")));
        grammar.add(new Production("E'", List.of("ε")));
        grammar.add(new Production("T", List.of("F", "T'")));
        grammar.add(new Production("T'", List.of("*", "F", "T'")));
        grammar.add(new Production("T'", List.of("/", "F", "T'")));
        grammar.add(new Production("T'", List.of("ε")));
        grammar.add(new Production("F", List.of("(", "E", ")")));
        grammar.add(new Production("F", List.of("id")));
        grammar.add(new Production("F", List.of("num")));
        computeFirst();
        computeFollow();
        computeSelect();
        buildTable();
    }

    private void computeFirst() {
        boolean changed;
        do {
            changed = false;
            for (Production p : grammar) {
                String nt = p.lhs;
                int i = 0;
                boolean allNullable = true;
                for (; i < p.rhs.size(); i++) {
                    String sym = p.rhs.get(i);
                    if (sym.equals("ε")) break;
                    if (terminals.contains(sym)) {
                        if (!first.computeIfAbsent(nt, k -> new HashSet<>()).contains(sym)) {
                            first.get(nt).add(sym);
                            changed = true;
                        }
                        allNullable = false;
                        break;
                    } else {
                        boolean nullable = false;
                        // 先安全获取 nt 对应的 Set，不存在就创建
                        Set<String> ntFirstSet = first.computeIfAbsent(nt, k -> new HashSet<>());

                        for (String s : first.computeIfAbsent(sym, k -> new HashSet<>())) {
                            if (!s.equals("ε") && !ntFirstSet.contains(s)) {
                                ntFirstSet.add(s);
                                changed = true;
                            }
                            if (s.equals("ε")) {
                                nullable = true;
                            }
                        }
                        if (!nullable) {
                            allNullable = false;
                            break;
                        }
                    }
                }
                if (allNullable && !first.get(nt).contains("ε")) {
                    first.get(nt).add("ε");
                    changed = true;
                }
            }
        } while (changed);
    }

    private void computeFollow() {
        follow.computeIfAbsent("E", k -> new HashSet<>()).add("$");
        boolean changed;
        do {
            changed = false;
            for (Production p : grammar) {
                String nt = p.lhs;
                for (int i = 0; i < p.rhs.size(); i++) {
                    String sym = p.rhs.get(i);
                    if (nonterminals.contains(sym)) {
                        int j = i+1;
                        boolean allNullable = true;
                        for (; j < p.rhs.size(); j++) {
                            String next = p.rhs.get(j);
                            if (terminals.contains(next)) {
                                if (!follow.get(sym).contains(next)) {
                                    follow.get(sym).add(next);
                                    changed = true;
                                }
                                allNullable = false;
                                break;
                            } else {
                                // 安全获取 first 集合
                                Set<String> firstSet = first.computeIfAbsent(next, k -> new HashSet<>());
                                // 安全获取 follow 集合
                                Set<String> followSet = follow.computeIfAbsent(sym, k -> new HashSet<>());

                                for (String f : firstSet) {
                                    if (!f.equals("ε") && !followSet.contains(f)) {
                                        followSet.add(f);
                                        changed = true;
                                    }
                                }
                                if (!first.get(next).contains("ε")) {
                                    allNullable = false;
                                    break;
                                }
                            }
                        }
                        if (j == p.rhs.size() && allNullable) {
                            // 安全获取两个集合，不存在就创建空Set
                            Set<String> followNt = follow.computeIfAbsent(nt, k -> new HashSet<>());
                            Set<String> followSym = follow.computeIfAbsent(sym, k -> new HashSet<>());

                            for (String f : followNt) {
                                if (!followSym.contains(f)) {
                                    followSym.add(f);
                                    changed = true;
                                }
                            }
                        }
                        //空指针异常
                        /*if (j == p.rhs.size() && allNullable) {
                            for (String f : follow.get(nt)) {
                                if (!follow.get(sym).contains(f)) {
                                    follow.get(sym).add(f);
                                    changed = true;
                                }
                            }
                        }*/
                    }
                }
            }
        } while (changed);
    }

    private void computeSelect() {
        for (Production p : grammar) {
            String key = p.lhs + " ->" + String.join(" ", p.rhs);
            if (p.rhs.get(0).equals("ε")) {
                select.put(key, new HashSet<>(follow.get(p.lhs)));
            } else {
                Set<String> result = new HashSet<>();
                int i = 0;
                boolean allNullable = true;
                for (; i < p.rhs.size(); i++) {
                    String sym = p.rhs.get(i);
                    if (terminals.contains(sym)) {
                        result.add(sym);
                        allNullable = false;
                        break;
                    } else {
                        for (String f : first.get(sym)) {
                            if (!f.equals("ε")) result.add(f);
                        }
                        if (!first.get(sym).contains("ε")) {
                            allNullable = false;
                            break;
                        }
                    }
                }
                if (allNullable) result.addAll(follow.get(p.lhs));
                select.put(key, result);
            }
        }
    }

    private void buildTable() {
        for (Production p : grammar) {
            String key = p.lhs + " ->" + String.join(" ", p.rhs);
            for (String t : select.get(key)) {
                table.put(new AbstractMap.SimpleEntry<>(p.lhs, t), p);
            }
        }
    }

    public void print() {
        System.out.println("\n========== LL(1) 分析 ==========");
        System.out.println("Grammar:");
        for (Production p : grammar) {
            System.out.print("  " + p.lhs + " ->");
            for (String s : p.rhs) System.out.print(" " + s);
            System.out.println();
        }
        System.out.println("\nFIRST sets:");
        for (String nt : nonterminals) {
            System.out.print("  FIRST(" + nt + ") = {");
            for (String s : first.get(nt)) System.out.print(" " + s);
            System.out.println(" }");
        }
        System.out.println("\nFOLLOW sets:");
        for (String nt : nonterminals) {
            System.out.print("  FOLLOW(" + nt + ") = {");
            for (String s : follow.get(nt)) System.out.print(" " + s);
            System.out.println(" }");
        }
        List<String> termList = List.of("id", "num", "+", "-", "*", "/", "(", ")", "$");
        System.out.println("\nLL(1) Parsing Table:");
        System.out.print("NT\\T\t");
        for (String t : termList) System.out.print(t + "\t");
        System.out.println();
        for (String nt : nonterminals) {
            System.out.print(nt + "\t");
            for (String t : termList) {
                Production prod = table.get(new AbstractMap.SimpleEntry<>(nt, t));
                if (prod != null) {
                    System.out.print(nt + "->");
                    for (String s : prod.rhs) System.out.print(s);
                    System.out.print("\t");
                } else {
                    System.out.print("  \t");
                }
            }
            System.out.println();
        }
    }
}