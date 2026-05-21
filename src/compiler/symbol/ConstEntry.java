package compiler.symbol;

public class ConstEntry {
    public String value;
    public TypeEntry type;

    public ConstEntry(String value, TypeEntry type) {
        this.value = value;
        this.type = type;
    }
}

