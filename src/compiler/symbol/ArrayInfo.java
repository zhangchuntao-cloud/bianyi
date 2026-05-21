package compiler.symbol;

import java.util.List;

public class ArrayInfo {
    public int dim;
    public List<Integer> lower;
    public List<Integer> upper;
    public TypeEntry elemType;
    public int totalSize;

    public ArrayInfo(int dim, List<Integer> lower, List<Integer> upper, TypeEntry elemType) {
        this.dim = dim;
        this.lower = lower;
        this.upper = upper;
        this.elemType = elemType;
        this.totalSize = 0;
    }
}