package compiler.symbol;

import java.util.ArrayList;
import java.util.List;

public class StructInfo {
    public String name;
    public List<StructMember> members = new ArrayList<>();
    public int totalSize;

    public StructInfo(String name) {
        this.name = name;
    }
}