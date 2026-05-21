package compiler.symbol;

import compiler.common.SymbolKind;

public class SymbolEntry {
    public String name;
    public TypeEntry type;
    public SymbolKind cat;
    public Object addr; // 指向具体子表

    public SymbolEntry(String name, TypeEntry type, SymbolKind cat, Object addr) {
        this.name = name;
        this.type = type;
        this.cat = cat;
        this.addr = addr;
    }
}