package compiler.symbol;

public class ActRecordEntry {
    public String name;
    public int offset;
    public TypeEntry type;

    public ActRecordEntry(String name, int offset, TypeEntry type) {
        this.name = name;
        this.offset = offset;
        this.type = type;
    }
}