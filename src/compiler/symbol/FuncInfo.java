package compiler.symbol;

import java.util.ArrayList;
import java.util.List;

public class FuncInfo {
    public String name;
    public TypeEntry retType;
    public int paramCount;
    public List<TypeEntry> paramTypes = new ArrayList<>();
    public List<String> paramNames = new ArrayList<>();
    public int entryAddr;
    public Object localVarTable; // 指向活动记录表

    public FuncInfo(String name, TypeEntry retType) {
        this.name = name;
        this.retType = retType;
    }
}