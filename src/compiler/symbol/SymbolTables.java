package compiler.symbol;

import compiler.common.DataType;
import compiler.common.SymbolKind;

import java.util.*;

public final class SymbolTables {
    // 全局容器
    public static final List<SymbolEntry> symbolTable = new ArrayList<>();   // SYNBL
    public static final List<TypeEntry> typeTable = new ArrayList<>();       // TYPEL
    public static final List<ArrayInfo> arrayTable = new ArrayList<>();      // AINFL
    public static final List<StructInfo> structTable = new ArrayList<>();    // RINFL
    public static final List<FuncInfo> funcTable = new ArrayList<>();        // PFINFL
    public static final List<ConstEntry> constTable = new ArrayList<>();     // CONSL
    public static final List<ActRecordEntry> actRecord = new ArrayList<>();  // 当前活动记录
    public static int currentFuncOffset = 0;
    public static String currentFuncName = "";

    // 基本类型静态缓存
    private static final TypeEntry[] basicTypeCache = new TypeEntry[DataType.values().length];

    // 辅助函数
    public static TypeEntry addType(DataType dt, Object ext) {
        // 基本类型：返回缓存的唯一实例
        if (dt == DataType.DT_INT || dt == DataType.DT_FLOAT || dt == DataType.DT_CHAR ||
                dt == DataType.DT_BOOL || dt == DataType.DT_VOID) {
            int idx = dt.ordinal();
            if (basicTypeCache[idx] == null) {
                basicTypeCache[idx] = new TypeEntry(dt, null);
            }
            return basicTypeCache[idx];
        }
        // 复合类型
        TypeEntry te = new TypeEntry(dt, ext);
        typeTable.add(te);
        return te;
    }

    public static ArrayInfo addArray(int dim, List<Integer> low, List<Integer> up, TypeEntry elem) {
        ArrayInfo ai = new ArrayInfo(dim, low, up, elem);
        // 计算总大小（假设基本类型占4字节）
        int size = 4;
        for (int i = 0; i < dim; ++i) {
            size *= (up.get(i) - low.get(i) + 1);
        }
        ai.totalSize = size;
        arrayTable.add(ai);
        return ai;
    }

    public static StructInfo addStruct(String name) {
        StructInfo si = new StructInfo(name);
        structTable.add(si);
        return si;
    }

    public static FuncInfo addFunc(String name, TypeEntry ret) {
        FuncInfo fi = new FuncInfo(name, ret);
        funcTable.add(fi);
        return fi;
    }

    public static SymbolEntry addSymbol(String name, TypeEntry type, SymbolKind kind, Object addr) {
        SymbolEntry se = new SymbolEntry(name, type, kind, addr);
        symbolTable.add(se);
        return se;
    }

    public static ConstEntry addConst(String value, TypeEntry type) {
        for (ConstEntry ce : constTable) {
            if (ce.value.equals(value) && ce.type.tval == type.tval) {
                return ce;
            }
        }
        ConstEntry ce = new ConstEntry(value, type);
        constTable.add(ce);
        return ce;
    }

    public static SymbolEntry findSymbol(String name) {
        for (SymbolEntry se : symbolTable) {
            if (se.name.equals(name)) return se;
        }
        return null;
    }

    public static String dataTypeToString(DataType dt) {
        switch (dt) {
            case DT_INT:    return "i(系统)";
            case DT_FLOAT:  return "r(系统)";
            case DT_CHAR:   return "c(系统)";
            case DT_BOOL:   return "b(系统)";
            case DT_VOID:   return "v(系统)";
            case DT_ARRAY:  return "a(用户)";
            case DT_STRUCT: return "d(用户)";
            case DT_FUNC:   return "f(用户)";
            default:        return "unknown";
        }
    }

    public static String symbolKindToString(SymbolKind sk) {
        switch (sk) {
            case SK_VAR:   return "变量(v)";
            case SK_CONST: return "常量(c)";
            case SK_FUNC:  return "函数(f)";
            case SK_TYPE:  return "类型(t)";
            case SK_FIELD: return "域名(d)";
            default:       return "unknown";
        }
    }
    /**
     * 标准化输出符号表系统
     * @param keywordTable   关键字表（从 Compiler 传入）
     * @param delimiterTable 界符表（从 Compiler 传入）
     */
    public static void printNewSymbolTables(List<String> keywordTable, List<String> delimiterTable) {
        // 辅助函数：重复字符，替代 String.repeat()
        java.util.function.BiFunction<String, Integer, String> repeat = (s, count) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) sb.append(s);
            return sb.toString();
        };

        System.out.println("\n" + repeat.apply("=", 80));
        System.out.println("                    编译原理符号表系统（标准化输出）");
        System.out.println(repeat.apply("=", 80));

        // ====================== 一、词法分类总览 ======================
        System.out.println("\n【1. 词法分析后单词整体分类】");
        System.out.println("  关键字(K) → 关键字表 (共" + (keywordTable.size() - 1) + "个)");
        System.out.println("  界符(P)   → 界符表 (共" + (delimiterTable.size() - 1) + "个)");
        System.out.println("  标识符(I) → 符号表主表 (共" + symbolTable.size() + "个，核心语义载体)");
        System.out.println("  常量(C)   → 常量表 (共" + constTable.size() + "个)");

        // ====================== 二、主符号表 SYNBL ======================
        System.out.println("\n\n【2. 主符号表 SYNBL（标识符核心信息）】");
        System.out.printf("%-15s %-18s %-15s %s\n", "标识符名字", "类型信息(TYPE)", "种类信息(CAT)", "关联子表(ADDR)");
        System.out.println(repeat.apply("-", 50));

        for (SymbolEntry s : symbolTable) {
            System.out.printf("%-15s", s.name);
            System.out.printf("%-18s", dataTypeToString(s.type.tval));
            System.out.printf("%-15s", symbolKindToString(s.cat));

            if (s.cat == SymbolKind.SK_FUNC) {
                FuncInfo fi = (FuncInfo) s.addr;
                System.out.print("函数表(PFINFL) → " + fi.name + "(" + fi.paramCount + "个参数)");
            } else if (s.cat == SymbolKind.SK_TYPE && s.type.tval == DataType.DT_STRUCT) {
                StructInfo st = (StructInfo) s.type.tpoint;
                System.out.print("结构表(RINFL) → " + st.name + "(" + st.members.size() + "个成员)");
            } else if (s.cat == SymbolKind.SK_FIELD) {
                StructInfo st = (StructInfo) s.addr;
                System.out.print("所属结构体 → " + st.name + "，偏移地址:" + st.members.get(0).offset);
            } else if (s.type.tval == DataType.DT_ARRAY) {
                ArrayInfo ai = (ArrayInfo) s.type.tpoint;
                System.out.print("数组表(AINFL) → " + ai.dim + "维，总大小" + ai.totalSize + "字节");
            } else if (s.cat == SymbolKind.SK_VAR) {
                System.out.print("值单元分配表 → 活动记录偏移");
            } else {
                System.out.print("无");
            }
            System.out.println();
        }

        // ====================== 三、类型表 TYPEL ======================
        System.out.println("\n\n【3. 类型表 TYPEL（维度1：类型信息）】");
        System.out.println("  分类：系统定义类型(基本类型) | 用户定义类型(复合类型)");
        System.out.printf("%-8s %-15s %s\n", "类型编号", "类型值(TVAL)", "类型扩展指针(TPOINT)指向");
        System.out.println(repeat.apply("-", 50));

        for (int i = 0; i < typeTable.size(); i++) {
            TypeEntry te = typeTable.get(i);
            System.out.printf("%-8d", i);
            System.out.printf("%-15s", dataTypeToString(te.tval));

            if (te.tpoint != null) {
                if (te.tval == DataType.DT_ARRAY) {
                    ArrayInfo ai = (ArrayInfo) te.tpoint;
                    System.out.print("数组表(AINFL) → 元素类型:" + dataTypeToString(ai.elemType.tval));
                } else if (te.tval == DataType.DT_STRUCT) {
                    StructInfo st = (StructInfo) te.tpoint;
                    System.out.print("结构表(RINFL) → 结构体名:" + st.name);
                } else {
                    System.out.print(te.tpoint);
                }
            } else {
                System.out.print("无(基本类型)");
            }
            System.out.println();
        }

        // ====================== 四、复合类型信息子表 ======================
        System.out.println("\n\n【4. 复合类型信息子表】");

        // 4.1 数组表 AINFL
        System.out.println("\n  4.1 数组表 AINFL");
        if (arrayTable.isEmpty()) {
            System.out.println("    无数组类型定义");
        } else {
            System.out.printf("%-10s %-8s %-12s %s\n", "数组编号", "维数", "总大小(字节)", "元素类型");
            System.out.println(repeat.apply("-", 45));
            for (int i = 0; i < arrayTable.size(); i++) {
                ArrayInfo ai = arrayTable.get(i);
                System.out.printf("%-10d", i);
                System.out.printf("%-8d", ai.dim);
                System.out.printf("%-12d", ai.totalSize);
                System.out.println(dataTypeToString(ai.elemType.tval));
            }
        }

        // 4.2 结构表 RINFL
        System.out.println("\n  4.2 结构表 RINFL");
        if (structTable.isEmpty()) {
            System.out.println("    无结构体类型定义");
        } else {
            for (int i = 0; i < structTable.size(); i++) {
                StructInfo st = structTable.get(i);
                System.out.printf("    结构体%d: %s (共%d个成员，总大小%d字节)\n", i, st.name, st.members.size(), st.totalSize);
                System.out.println("      成员名\t类型\t偏移地址");
                System.out.println("      " + repeat.apply("-", 30));
                for (StructMember mem : st.members) {
                    System.out.printf("      %s\t%s\t%d\n", mem.name, dataTypeToString(mem.type.tval), mem.offset);
                }
            }
        }

        // ====================== 五、标识符类别信息子表 ======================
        System.out.println("\n\n【5. 标识符类别信息子表】");

        // 5.1 函数表 PFINFL
        System.out.println("\n  5.1 函数表 PFINFL");
        if (funcTable.isEmpty()) {
            System.out.println("    无函数定义");
        } else {
            System.out.printf("%-12s %-15s %-10s %s\n", "函数名", "返回值类型", "参数个数", "参数列表");
            System.out.println(repeat.apply("-", 60));
            for (FuncInfo f : funcTable) {
                System.out.printf("%-12s", f.name);
                System.out.printf("%-15s", dataTypeToString(f.retType.tval));
                System.out.printf("%-10d", f.paramCount);
                for (int i = 0; i < f.paramNames.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    System.out.print(dataTypeToString(f.paramTypes.get(i).tval) + " " + f.paramNames.get(i));
                }
                System.out.println();
            }
        }

        // 5.2 常量表 CONSL
        System.out.println("\n  5.2 常量表 CONSL");
        if (constTable.isEmpty()) {
            System.out.println("    无字面量常量");
        } else {
            System.out.printf("%-10s %-20s %s\n", "常量编号", "常量值", "类型");
            System.out.println(repeat.apply("-", 40));
            for (int i = 0; i < constTable.size(); i++) {
                ConstEntry ce = constTable.get(i);
                System.out.printf("%-10d", i);
                System.out.printf("%-20s", ce.value);
                System.out.println(dataTypeToString(ce.type.tval));
            }
        }

        // 5.3 值单元分配表（活动记录）
        System.out.println("\n  5.3 值单元分配表（活动记录）");
        if (actRecord.isEmpty()) {
            System.out.println("    无局部变量分配");
        } else {
            System.out.printf("%-15s %-15s %s\n", "变量名", "类型", "栈帧偏移地址");
            System.out.println(repeat.apply("-", 45));
            for (ActRecordEntry a : actRecord) {
                System.out.printf("%-15s", a.name);
                System.out.printf("%-15s", dataTypeToString(a.type.tval));
                System.out.println(a.offset);
            }
        }

        System.out.println("\n" + repeat.apply("=", 80));
    }
}