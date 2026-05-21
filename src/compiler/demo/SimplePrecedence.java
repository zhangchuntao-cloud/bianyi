package compiler.demo;

import java.util.*;

public class SimplePrecedence {
    public List<String> symbols = new ArrayList<>();
    public Set<String> nonterminals = new HashSet<>(Set.of("E", "T", "F"));
    public Set<String> terminals = new HashSet<>(Set.of("+", "*", "id", "(", ")", "$"));
    public List<Map.Entry<String, List<String>>> grammar = new ArrayList<>();
    public Map<String, Set<String>> firstop = new HashMap<>();
    public Map<String, Set<String>> lastop = new HashMap<>();
    public Map<Map.Entry<String, String>, Character> precedence = new HashMap<>();

    public void initGrammar() {
        grammar.add(new AbstractMap.SimpleEntry<>("E", List.of("E", "+", "T")));
        grammar.add(new AbstractMap.SimpleEntry<>("E", List.of("T")));
        grammar.add(new AbstractMap.SimpleEntry<>("T", List.of("T", "*", "F")));
        grammar.add(new AbstractMap.SimpleEntry<>("T", List.of("F")));
        grammar.add(new AbstractMap.SimpleEntry<>("F", List.of("(", "E", ")")));
        grammar.add(new AbstractMap.SimpleEntry<>("F", List.of("id")));
        symbols = List.of("E", "T", "F", "+", "*", "id", "(", ")", "$");
    }

    public void computeFirstLastOp() {
        for (String nt : nonterminals) {
            firstop.put(nt, new HashSet<>());
            lastop.put(nt, new HashSet<>());
        }
        for (String nt : nonterminals) {
            for (var p : grammar) {
                if (p.getKey().equals(nt)) {
                    String firstSym = p.getValue().get(0);
                    if (terminals.contains(firstSym)) firstop.get(nt).add(firstSym);
                }
            }
        }
        boolean changed;
        do {
            changed = false;
            for (String nt : nonterminals) {
                for (var p : grammar) {
                    if (p.getKey().equals(nt)) {
                        for (String sym : p.getValue()) {
                            if (terminals.contains(sym)) {
                                if (!firstop.get(nt).contains(sym)) {
                                    firstop.get(nt).add(sym);
                                    changed = true;
                                }
                                break;
                            } else if (nonterminals.contains(sym)) {
                                for (String f : firstop.get(sym)) {
                                    if (!firstop.get(nt).contains(f)) {
                                        firstop.get(nt).add(f);
                                        changed = true;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } while (changed);

        for (String nt : nonterminals) {
            for (var p : grammar) {
                if (p.getKey().equals(nt)) {
                    String lastSym = p.getValue().get(p.getValue().size()-1);
                    if (terminals.contains(lastSym)) lastop.get(nt).add(lastSym);
                }
            }
        }
        do {
            changed = false;
            for (String nt : nonterminals) {
                for (var p : grammar) {
                    if (p.getKey().equals(nt)) {
                        for (int i = p.getValue().size()-1; i >= 0; i--) {
                            String sym = p.getValue().get(i);
                            if (terminals.contains(sym)) {
                                if (!lastop.get(nt).contains(sym)) {
                                    lastop.get(nt).add(sym);
                                    changed = true;
                                }
                                break;
                            } else if (nonterminals.contains(sym)) {
                                for (String l : lastop.get(sym)) {
                                    if (!lastop.get(nt).contains(l)) {
                                        lastop.get(nt).add(l);
                                        changed = true;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    public void buildPrecedence() {
        for (String a : symbols) {
            for (String b : symbols) {
                precedence.put(new AbstractMap.SimpleEntry<>(a, b), ' ');
            }
        }
        for (var p : grammar) {
            List<String> rhs = p.getValue();
            for (int i = 0; i < rhs.size()-1; i++) {
                String left = rhs.get(i), right = rhs.get(i+1);
                precedence.put(new AbstractMap.SimpleEntry<>(left, right), '=');
            }
        }
        for (var p : grammar) {
            List<String> rhs = p.getValue();
            for (int i = 0; i < rhs.size()-1; i++) {
                String left = rhs.get(i), right = rhs.get(i+1);
                if (terminals.contains(left) && nonterminals.contains(right)) {
                    for (String f : firstop.get(right)) {
                        precedence.put(new AbstractMap.SimpleEntry<>(left, f), '<');
                    }
                }
            }
        }
        for (var p : grammar) {
            List<String> rhs = p.getValue();
            for (int i = 0; i < rhs.size()-1; i++) {
                String left = rhs.get(i), right = rhs.get(i+1);
                if (nonterminals.contains(left) && terminals.contains(right)) {
                    for (String l : lastop.get(left)) {
                        precedence.put(new AbstractMap.SimpleEntry<>(l, right), '>');
                    }
                }
            }
        }
        for (String a : symbols) {
            if (!a.equals("$")) {
                precedence.put(new AbstractMap.SimpleEntry<>("$", a), '<');
                precedence.put(new AbstractMap.SimpleEntry<>(a, "$"), '>');
            }
        }
        precedence.put(new AbstractMap.SimpleEntry<>("$", "E"), '=');
        precedence.put(new AbstractMap.SimpleEntry<>("E", "$"), '>');
        precedence.put(new AbstractMap.SimpleEntry<>("$", "id"), '<');
    }

    public void print() {
        System.out.println("\n========== 简单优先分析 ==========");
        System.out.println("Grammar:");
        for (var p : grammar) {
            System.out.print("  " + p.getKey() + " ->");
            for (String s : p.getValue()) System.out.print(" " + s);
            System.out.println();
        }
        System.out.println("\nFIRSTOP:");
        for (String nt : nonterminals) {
            System.out.print("  FIRSTOP(" + nt + ") = {");
            for (String f : firstop.get(nt)) System.out.print(" " + f);
            System.out.println(" }");
        }
        System.out.println("\nLASTOP:");
        for (String nt : nonterminals) {
            System.out.print("  LASTOP(" + nt + ") = {");
            for (String l : lastop.get(nt)) System.out.print(" " + l);
            System.out.println(" }");
        }
        System.out.println("\nPrecedence matrix:");
        System.out.print("    ");
        for (String s : symbols) System.out.print(String.format("%3s", s));
        System.out.println();
        for (String a : symbols) {
            System.out.print(String.format("%3s", a) + " ");
            for (String b : symbols) {
                char ch = precedence.get(new AbstractMap.SimpleEntry<>(a, b));
                if (ch == ' ') System.out.print("   ");
                else System.out.print(String.format("%3s", ch));
            }
            System.out.println();
        }
    }
}