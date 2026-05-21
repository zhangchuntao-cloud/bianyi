package compiler.symbol;

public class StructMember {
    public String name;
    public TypeEntry type;
    public int offset;

    public StructMember(String name, TypeEntry type, int offset) {
        this.name = name;
        this.type = type;
        this.offset = offset;
    }
}