package compiler.symbol;

import compiler.common.DataType;

public class TypeEntry {
    public DataType tval;
    public Object tpoint; // 指向 ArrayInfo 或 StructInfo

    public TypeEntry(DataType dt, Object ext) {
        this.tval = dt;
        this.tpoint = ext;
    }
}