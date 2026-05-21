package compiler.parser;

import compiler.common.*;
import compiler.ir.QuadGenerator;
import compiler.lexer.*;
import compiler.symbol.*;

import java.util.*;

public class Compiler implements Lexer.CompilerContext {
    // 静态词法表（与C++全局一致）
    public static final List<String> keywordTable = List.of("", "int", "float", "char", "void", "struct", "if", "else", "while", "write", "return");
    public static final List<String> delimiterTable = List.of("", "+", "-", "*", "/", "<", ">", "<=", ">=", "==", "!=", "&&", "||", "!", "=", "(", ")", "[", "]", "{", "}", ";", ",", ".", "->");

    // 编译器实例域
    private Lexer lexer;
    private Token curToken;
    private int scopeLevel = 0;
    private int globalSize = 0;
    private List<Map<String, SymbolEntry>> localScopes = new ArrayList<>();
    private SymbolEntry currentFunc = null;

    // 全局标识符/常量/Token序列（原C++全局）
    private List<String> identTable = new ArrayList<>();
    private List<String> constValueTable = new ArrayList<>();
    private List<Map.Entry<Character,Integer>> tokenSequence = new ArrayList<>();

    private QuadGenerator quadGen = new QuadGenerator();

    public Compiler(String source) {
        lexer = new Lexer(source, this);
        advance();
    }

    private void advance() {
        curToken = lexer.next();
    }

    private boolean check(TokenType t) {
        return curToken.type == t;
    }

    private void match(TokenType t, String err) {
        if (check(t)) advance();
        else {
            System.err.println("Syntax error line " + curToken.line + ": " + err);
            System.exit(1);
        }
    }

    // 实现 Lexer.CompilerContext 接口
    public int addIdent(String name) {
        for (int i = 0; i < identTable.size(); i++) {
            if (identTable.get(i).equals(name)) return i+1;
        }
        identTable.add(name);
        return identTable.size();
    }

    public int addConstValue(String val) {
        for (int i = 0; i < constValueTable.size(); i++) {
            if (constValueTable.get(i).equals(val)) return i+1;
        }
        constValueTable.add(val);
        return constValueTable.size();
    }

    public void addTokenPair(char type, int index) {
        tokenSequence.add(new AbstractMap.SimpleEntry<>(type, index));
    }

    public void addConst(String val, TypeEntry constType) {
        SymbolTables.addConst(val, constType);
    }

    // 查找符号（从内向外）
    private SymbolEntry findSymbolInScopes(String name) {
        for (int i = localScopes.size()-1; i >= 0; i--) {
            SymbolEntry se = localScopes.get(i).get(name);
            if (se != null) return se;
        }
        return SymbolTables.findSymbol(name);
    }

    private void addSymbolToCurrentScope(SymbolEntry sym) {
        if (localScopes.isEmpty()) {
            SymbolTables.symbolTable.add(sym);
        } else {
            localScopes.get(localScopes.size()-1).put(sym.name, sym);
        }
    }

    private TypeEntry parseTypeSpecifier() {
        if (check(TokenType.KW_INT)) {
            advance();
            return SymbolTables.addType(DataType.DT_INT, null);
        }
        if (check(TokenType.KW_FLOAT)) {
            advance();
            return SymbolTables.addType(DataType.DT_FLOAT, null);
        }
        if (check(TokenType.KW_CHAR)) {
            advance();
            return SymbolTables.addType(DataType.DT_CHAR, null);
        }
        if (check(TokenType.KW_STRUCT)) {
            advance();
            if (!check(TokenType.ID)) {
                System.err.println("Expected struct name");
                System.exit(1);
            }
            String sname = curToken.value;
            advance();
            SymbolEntry sym = SymbolTables.findSymbol(sname);
            if (sym == null || sym.cat != SymbolKind.SK_TYPE || sym.type.tval != DataType.DT_STRUCT) {
                System.err.println("Unknown struct " + sname);
                System.exit(1);
            }
            return sym.type;
        }
        if (check(TokenType.ID)) {
            String t = curToken.value;
            advance();
            SymbolEntry sym = SymbolTables.findSymbol(t);
            if (sym != null && sym.cat == SymbolKind.SK_TYPE && sym.type.tval == DataType.DT_STRUCT)
                return sym.type;
            System.err.println("Unknown type " + t);
            System.exit(1);
        }
        System.err.println("Expected type");
        System.exit(1);
        return null;
    }

    // ---------- 表达式解析 ----------
    private String parseExpr() { return parseLogicalOr(); }

    private String parseLogicalOr() {
        String left = parseLogicalAnd();
        while (check(TokenType.OR)) {
            advance();
            String right = parseLogicalAnd();
            String t = quadGen.newTemp();
            String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
            String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
            quadGen.emit("(||, " + leftId + ", " + rightId + ", " + t + ")");
            left = t;
        }
        return left;
    }

    private String parseLogicalAnd() {
        String left = parseEquality();
        while (check(TokenType.AND)) {
            advance();
            String right = parseEquality();
            String t = quadGen.newTemp();
            String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
            String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
            quadGen.emit("(&&, " + leftId + ", " + rightId + ", " + t + ")");
            left = t;
        }
        return left;
    }

    private String parseEquality() {
        String left = parseRelational();
        while (check(TokenType.EQ) || check(TokenType.NE)) {
            String op = curToken.value;
            advance();
            String right = parseRelational();
            String t = quadGen.newTemp();
            StringBuilder folded = new StringBuilder();
            if (QuadGenerator.tryFoldConst(op, left, right, folded)) {
                String foldedId = getConstId(folded.toString());
                quadGen.emit("(:=, " + foldedId + ", _, " + t + ")");
            } else {
                String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
                String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
                quadGen.emit("(" + op + ", " + leftId + ", " + rightId + ", " + t + ")");
            }
            left = t;
        }
        return left;
    }

    private String parseRelational() {
        String left = parseAdditive();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {
            String op = curToken.value;
            advance();
            String right = parseAdditive();
            String t = quadGen.newTemp();
            StringBuilder folded = new StringBuilder();
            if (QuadGenerator.tryFoldConst(op, left, right, folded)) {
                String foldedId = getConstId(folded.toString());
                quadGen.emit("(:=, " + foldedId + ", _, " + t + ")");
            } else {
                String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
                String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
                quadGen.emit("(" + op + ", " + leftId + ", " + rightId + ", " + t + ")");
            }
            left = t;
        }
        return left;
    }

    private String parseAdditive() {
        String left = parseMultiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = (curToken.type == TokenType.PLUS) ? "+" : "-";
            advance();
            String right = parseMultiplicative();
            String t = quadGen.newTemp();
            StringBuilder folded = new StringBuilder();
            if (QuadGenerator.tryFoldConst(op, left, right, folded)) {
                String foldedId = getConstId(folded.toString());
                quadGen.emit("(:=, " + foldedId + ", _, " + t + ")");
            } else {
                String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
                String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
                quadGen.emit("(" + op + ", " + leftId + ", " + rightId + ", " + t + ")");
            }
            left = t;
        }
        return left;
    }

    private String parseMultiplicative() {
        String left = parseUnary();
        while (check(TokenType.MUL) || check(TokenType.DIV)) {
            String op = (curToken.type == TokenType.MUL) ? "*" : "/";
            advance();
            String right = parseUnary();
            String t = quadGen.newTemp();
            StringBuilder folded = new StringBuilder();
            if (QuadGenerator.tryFoldConst(op, left, right, folded)) {
                String foldedId = getConstId(folded.toString());
                quadGen.emit("(:=, " + foldedId + ", _, " + t + ")");
            } else {
                String leftId = (left.startsWith("t") || left.startsWith("T")) ? left : getIdentId(left);
                String rightId = (right.startsWith("t") || right.startsWith("T")) ? right : getIdentId(right);
                quadGen.emit("(" + op + ", " + leftId + ", " + rightId + ", " + t + ")");
            }
            left = t;
        }
        return left;
    }

    private String parseUnary() {
        if (check(TokenType.NOT) || check(TokenType.MINUS)) {
            String op = (curToken.type == TokenType.NOT) ? "!" : "-";
            advance();
            String operand = parseUnary();
            String t = quadGen.newTemp();
            if (op.equals("-")) {
                StringBuilder folded = new StringBuilder();
                if (QuadGenerator.tryFoldConst("-", operand, "0", folded)) {
                    quadGen.emit("(:=, " + getConstId(folded.toString()) + ", _, " + t + ")");
                } else {
                    String opdId = (operand.startsWith("t") || operand.startsWith("T")) ? operand : getIdentId(operand);
                    quadGen.emit("(neg, " + opdId + ", _, " + t + ")");
                }
            } else { // "!"
                String opdId = (operand.startsWith("t") || operand.startsWith("T")) ? operand : getIdentId(operand);
                quadGen.emit("(!, " + opdId + ", _, " + t + ")");
            }
            return t;
        }
        return parsePrimary();
    }

    private String parsePrimary() {
        if (check(TokenType.INTEGER) || check(TokenType.REAL) || check(TokenType.CHAR_CONST)) {
            String v = curToken.value;
            advance();
            return v;
        }
        if (check(TokenType.STRING_CONST)) {
            String v = curToken.value;
            advance();
            return "\"" + v + "\"";
        }
        if (check(TokenType.ID)) {
            String name = curToken.value;
            advance();
            if (check(TokenType.LPAREN)) {
                SymbolEntry sym = findSymbolInScopes(name);
                if (sym == null || sym.cat != SymbolKind.SK_FUNC) {
                    System.err.println("Undefined function " + name);
                    System.exit(1);
                }
                FuncInfo fi = (FuncInfo) sym.addr;
                match(TokenType.LPAREN, "(");
                List<String> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    // 先解析第一个参数
                    args.add(parseExpr());
// 循环处理后续的逗号和参数
                    while (check(TokenType.COMMA)) {
                        advance();           // 消耗掉逗号
                        args.add(parseExpr());
                    }
                }
                match(TokenType.RPAREN, ")");
                System.out.println("[DEBUG] Call to " + name + " with " + args.size() + " arguments");
                if (args.size() != fi.paramCount) {
                    System.err.println("Argument count mismatch");
                    System.exit(1);
                }
                for (String a : args) {
                    String aid = (a.startsWith("t") || a.startsWith("T")) ? a : getIdentId(a);
                    quadGen.emit("(param, " + aid + ", _, _)");
                }
                String t = quadGen.newTemp();
                quadGen.emit("(call, " + name + ", _, " + t + ")");
                return t;
            }
            SymbolEntry sym = findSymbolInScopes(name);
            if (sym == null || (sym.cat != SymbolKind.SK_VAR && sym.cat != SymbolKind.SK_FIELD)) {
                System.err.println("Undefined variable " + name);
                System.exit(1);
            }
            String base = name;
            if (check(TokenType.LBRACKET)) {
                if (sym.type.tval != DataType.DT_ARRAY) {
                    System.err.println(name + " is not array");
                    System.exit(1);
                }
                advance();
                String idx = parseExpr();
                match(TokenType.RBRACKET, "]");
                String t = quadGen.newTemp();
                String baseId = getIdentId(base);
                String idxId = (idx.startsWith("t") || idx.startsWith("T")) ? idx : getIdentId(idx);
                quadGen.emit("(=[], " + baseId + ", " + idxId + ", " + t + ")");
                return t;
            }
            if (check(TokenType.DOT) || check(TokenType.ARROW)) {
                if (sym.type.tval != DataType.DT_STRUCT) {
                    System.err.println(name + " is not struct");
                    System.exit(1);
                }
                boolean arrow = (curToken.type == TokenType.ARROW);
                advance();
                if (!check(TokenType.ID)) {
                    System.err.println("Expected field name");
                    System.exit(1);
                }
                String field = curToken.value;
                advance();
                StructInfo st = (StructInfo) sym.type.tpoint;
                boolean found = false;
                int offset = 0;
                for (StructMember mem : st.members) {
                    if (mem.name.equals(field)) {
                        found = true;
                        break;
                    }
                    offset += (mem.type.tval == DataType.DT_INT ? 4 : 8);
                }
                if (!found) {
                    System.err.println("No field " + field + " in struct");
                    System.exit(1);
                }
                String t = quadGen.newTemp();
                String baseId = getIdentId(base);
                if (arrow)
                    quadGen.emit("(->, " + baseId + ", " + field + ", " + t + ")");
                else
                    quadGen.emit("(., " + baseId + ", " + field + ", " + t + ")");
                return t;
            }
            return base;
        }
        if (check(TokenType.LPAREN)) {
            advance();
            String e = parseExpr();
            match(TokenType.RPAREN, ")");
            return e;
        }
        System.err.println("Expected expression");
        System.exit(1);
        return "";
    }

    // ---------- 语句解析 ----------
    private void parseStmt() {
        if (check(TokenType.KW_IF)) parseIf();
        else if (check(TokenType.KW_WHILE)) parseWhile();
        else if (check(TokenType.KW_WRITE)) parseWrite();
        else if (check(TokenType.KW_RETURN)) parseReturn();
        else if (check(TokenType.LBRACE)) parseBlock();
        else if (check(TokenType.KW_INT) || check(TokenType.KW_FLOAT) || check(TokenType.KW_CHAR) ||
                check(TokenType.KW_VOID) || check(TokenType.KW_STRUCT) ||
                (check(TokenType.ID) && findSymbolInScopes(curToken.value) != null && findSymbolInScopes(curToken.value).cat == SymbolKind.SK_TYPE)) {
            if (check(TokenType.KW_STRUCT)) {
                advance();
                String structName = curToken.value;
                match(TokenType.ID, "struct name");
                if (check(TokenType.LBRACE)) {
                    parseStructDef(structName);
                } else {
                    String varName = curToken.value;
                    match(TokenType.ID, "var name");
                    TypeEntry structType = findSymbolInScopes(structName).type;
                    parseLocalVarDecl(structType, varName);
                }
            } else {
                TypeEntry type = parseTypeSpecifier();
                String name = curToken.value;
                match(TokenType.ID, "identifier");
                if (scopeLevel == 0 && check(TokenType.LPAREN)) {
                    parseFuncDef(type, name);
                } else {
                    parseLocalVarDecl(type, name);
                }
            }
        } else if (check(TokenType.ID)) {
            String name = curToken.value;
            advance();
            if (check(TokenType.LPAREN)) {
                SymbolEntry sym = findSymbolInScopes(name);
                if (sym == null || sym.cat != SymbolKind.SK_FUNC) {
                    System.err.println("Undefined function " + name);
                    System.exit(1);
                }
                FuncInfo fi = (FuncInfo) sym.addr;
                match(TokenType.LPAREN, "(");
                List<String> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    // 先解析第一个参数
                    args.add(parseExpr());
// 循环处理后续的逗号和参数
                    while (check(TokenType.COMMA)) {
                        advance();           // 消耗掉逗号
                        args.add(parseExpr());
                    }
                }
                match(TokenType.RPAREN, ")");
                if (args.size() != fi.paramCount) {
                    System.err.println("Argument count mismatch");
                    System.exit(1);
                }
                for (String a : args) {
                    String aid = (a.startsWith("t") || a.startsWith("T")) ? a : getIdentId(a);
                    quadGen.emit("(param, " + aid + ", _, _)");
                }
                quadGen.emit("(call, " + name + ", _, _)");
                match(TokenType.SEMI, ";");
            } else {
                SymbolEntry sym = findSymbolInScopes(name);
                if (sym == null || (sym.cat != SymbolKind.SK_VAR && sym.cat != SymbolKind.SK_FIELD)) {
                    System.err.println("Undefined variable " + name);
                    System.exit(1);
                }
                String lhsBase = name;
                boolean isArrayLHS = false;
                boolean isStructLHS = false;
                String arrayIndex = "";
                String structField = "";
                boolean arrowAccess = false;

                if (check(TokenType.LBRACKET)) {
                    if (sym.type.tval != DataType.DT_ARRAY) {
                        System.err.println(name + " is not array");
                        System.exit(1);
                    }
                    advance();
                    arrayIndex = parseExpr();
                    match(TokenType.RBRACKET, "]");
                    isArrayLHS = true;
                } else if (check(TokenType.DOT) || check(TokenType.ARROW)) {
                    if (sym.type.tval != DataType.DT_STRUCT) {
                        System.err.println(name + " is not struct");
                        System.exit(1);
                    }
                    arrowAccess = (curToken.type == TokenType.ARROW);
                    advance();
                    if (!check(TokenType.ID)) {
                        System.err.println("Expected field name");
                        System.exit(1);
                    }
                    structField = curToken.value;
                    advance();
                    StructInfo st = (StructInfo) sym.type.tpoint;
                    boolean found = false;
                    for (StructMember mem : st.members) {
                        if (mem.name.equals(structField)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.err.println("No field " + structField + " in struct");
                        System.exit(1);
                    }
                    isStructLHS = true;
                }

                match(TokenType.ASSIGN, "=");
                String rhs = parseExpr();
                match(TokenType.SEMI, ";");

                String rhsId = (rhs.startsWith("t") || rhs.startsWith("T")) ? rhs : getIdentId(rhs);

                if (isArrayLHS) {
                    String baseId = getIdentId(lhsBase);
                    String idxId = (arrayIndex.startsWith("t") || arrayIndex.startsWith("T")) ? arrayIndex : getIdentId(arrayIndex);
                    quadGen.emit("([]=, " + baseId + ", " + idxId + ", " + rhsId + ")");
                } else if (isStructLHS) {
                    String baseId = getIdentId(lhsBase);
                    if (arrowAccess)
                        quadGen.emit("(->=, " + baseId + ", " + structField + ", " + rhsId + ")");
                    else
                        quadGen.emit("(.=, " + baseId + ", " + structField + ", " + rhsId + ")");
                } else {
                    String lhsId = getIdentId(lhsBase);
                    quadGen.emit("(:=, " + rhsId + ", _, " + lhsId + ")");
                }
            }
        } else {
            System.err.println("Unexpected statement at line " + curToken.line);
            System.exit(1);
        }
    }

    private void parseLocalVarDecl(TypeEntry type, String firstVarName) {
        java.util.function.Consumer<String> processOne = (name) -> {
            boolean isArr = false;
            int arrSize = 0;
            if (check(TokenType.LBRACKET)) {
                advance();
                if (!check(TokenType.INTEGER)) {
                    System.err.println("Array size must be integer constant");
                    System.exit(1);
                }
                arrSize = Integer.parseInt(curToken.value);
                advance();
                match(TokenType.RBRACKET, "]");
                isArr = true;
            }
            TypeEntry varType = type;
            if (isArr) {
                List<Integer> low = List.of(0);
                List<Integer> up = List.of(arrSize - 1);
                varType = SymbolTables.addType(DataType.DT_ARRAY, SymbolTables.addArray(1, low, up, type));
            }
            if (localScopes.isEmpty()) {
                SymbolTables.addSymbol(name, varType, SymbolKind.SK_VAR, null);
            } else {
                SymbolEntry sym = SymbolTables.addSymbol(name, varType, SymbolKind.SK_VAR, null);
                localScopes.get(localScopes.size()-1).put(name, sym);
                int varSize = 0;
                switch (varType.tval) {
                    case DT_INT: case DT_CHAR: case DT_VOID: varSize = 4; break;
                    case DT_FLOAT: varSize = 8; break;
                    case DT_ARRAY: {
                        ArrayInfo ai = (ArrayInfo) varType.tpoint;
                        varSize = ai.totalSize;
                        break;
                    }
                    case DT_STRUCT: {
                        StructInfo st = (StructInfo) varType.tpoint;
                        varSize = st.totalSize;
                        break;
                    }
                    default: varSize = 4;
                }
                SymbolTables.actRecord.add(new ActRecordEntry(name, SymbolTables.currentFuncOffset, varType));
                SymbolTables.currentFuncOffset += varSize;
            }
            if (check(TokenType.ASSIGN)) {
                advance();
                String init = parseExpr();
                String initId = (init.startsWith("t") || init.startsWith("T")) ? init : getIdentId(init);
                String lhsId = getIdentId(name);
                quadGen.emit("(:=, " + initId + ", _, " + lhsId + ")");
            }
        };
        processOne.accept(firstVarName);
        while (check(TokenType.COMMA)) {
            advance();
            String name = curToken.value;
            match(TokenType.ID, "var name");
            processOne.accept(name);
        }
        match(TokenType.SEMI, ";");
    }

    private void parseFuncDef(TypeEntry retType, String fname) {
        match(TokenType.LPAREN, "(");
        FuncInfo fi = SymbolTables.addFunc(fname, retType);
        SymbolEntry existing = SymbolTables.findSymbol(fname);
        if (existing != null && existing.cat == SymbolKind.SK_FUNC) {
            System.err.println("Warning: function " + fname + " already defined!");
            System.exit(1);
        }
        SymbolEntry funcSym = SymbolTables.addSymbol(fname, retType, SymbolKind.SK_FUNC, fi);
        localScopes.add(new HashMap<>());
        scopeLevel++;
        currentFunc = funcSym;
        SymbolTables.actRecord.clear();
        SymbolTables.currentFuncOffset = 0;
        if (!check(TokenType.RPAREN)) {
            boolean first = true;
            while (true) {
                if (!first) {
                    // 如果不是第一个参数，必须遇到逗号才能继续
                    if (!check(TokenType.COMMA)) break;
                    advance(); // 消耗逗号
                }
                first = false;

                // 解析一个参数
                TypeEntry ptype = parseTypeSpecifier();
                String pname = curToken.value;
                match(TokenType.ID, "param name");
                fi.paramTypes.add(ptype);
                fi.paramNames.add(pname);
                fi.paramCount++;
                SymbolEntry paramSym = SymbolTables.addSymbol(pname, ptype, SymbolKind.SK_VAR, null);
                localScopes.get(localScopes.size() - 1).put(pname, paramSym);
                int paramSize = (ptype.tval == DataType.DT_INT || ptype.tval == DataType.DT_CHAR) ? 4 : 8;
                SymbolTables.actRecord.add(new ActRecordEntry(pname, SymbolTables.currentFuncOffset, ptype));
                SymbolTables.currentFuncOffset += paramSize;
            }
        }
        System.out.println("[DEBUG] Function " + fname + " defined with " + fi.paramCount + " parameters");
        match(TokenType.RPAREN, ")");
        quadGen.emit("(function, " + fname + ", _, _)");
        parseBlock();
        quadGen.emit("(end, " + fname + ", _, _)");
        localScopes.remove(localScopes.size()-1);
        scopeLevel--;
        currentFunc = null;
    }

    private void parseStructDef(String name) {
        match(TokenType.LBRACE, "{");
        StructInfo st = SymbolTables.addStruct(name);
        int offset = 0;
        while (!check(TokenType.RBRACE)) {
            TypeEntry ft = parseTypeSpecifier();
            String fn = curToken.value;
            match(TokenType.ID, "field name");
            match(TokenType.SEMI, ";");
            st.members.add(new StructMember(fn, ft, offset));
            offset += (ft.tval == DataType.DT_INT || ft.tval == DataType.DT_CHAR ? 4 : 8);
            SymbolTables.addSymbol(fn, ft, SymbolKind.SK_FIELD, st);
        }
        st.totalSize = offset;
        match(TokenType.RBRACE, "}");
        match(TokenType.SEMI, ";");
        TypeEntry structType = SymbolTables.addType(DataType.DT_STRUCT, st);
        SymbolTables.addSymbol(name, structType, SymbolKind.SK_TYPE, st);
    }

    private void parseIf() {
        match(TokenType.KW_IF, "if");
        match(TokenType.LPAREN, "(");
        String cond = parseExpr();
        match(TokenType.RPAREN, ")");
        String elseLabel = quadGen.newLabel();
        String endLabel = quadGen.newLabel();
        quadGen.emit("(ifFalse, " + cond + ", _, " + elseLabel + ")");
        parseStmt();
        quadGen.emit("(goto, _, _, " + endLabel + ")");
        quadGen.emit("(label, _, _, " + elseLabel + ")");
        if (check(TokenType.KW_ELSE)) {
            advance();
            parseStmt();
        }
        quadGen.emit("(label, _, _, " + endLabel + ")");
    }

    private void parseWhile() {
        match(TokenType.KW_WHILE, "while");
        String startLabel = quadGen.newLabel();
        String endLabel = quadGen.newLabel();
        quadGen.emit("(label, _, _, " + startLabel + ")");
        match(TokenType.LPAREN, "(");
        String cond = parseExpr();
        match(TokenType.RPAREN, ")");
        quadGen.emit("(ifFalse, " + cond + ", _, " + endLabel + ")");
        parseStmt();
        quadGen.emit("(goto, _, _, " + startLabel + ")");
        quadGen.emit("(label, _, _, " + endLabel + ")");
    }

    private void parseWrite() {
        match(TokenType.KW_WRITE, "write");
        match(TokenType.LPAREN, "(");
        if (check(TokenType.STRING_CONST)) {
            String s = curToken.value;
            advance();
            quadGen.emit("(write, \"" + s + "\", _, _)");
        } else {
            String src = parseExpr();
            String srcId = (src.startsWith("t") || src.startsWith("T")) ? src : getIdentId(src);
            quadGen.emit("(write, " + srcId + ", _, _)");
        }
        match(TokenType.RPAREN, ")");
        match(TokenType.SEMI, ";");
    }

    private void parseReturn() {
        match(TokenType.KW_RETURN, "return");
        if (!check(TokenType.SEMI)) {
            String e = parseExpr();
            String eId = (e.startsWith("t") || e.startsWith("T")) ? e : getIdentId(e);
            quadGen.emit("(return, " + eId + ", _, _)");
        } else {
            quadGen.emit("(return, _, _, _)");
        }
        match(TokenType.SEMI, ";");
    }

    private void parseBlock() {
        match(TokenType.LBRACE, "{");
        localScopes.add(new HashMap<>());
        scopeLevel++;
        while (!check(TokenType.RBRACE) && !check(TokenType.END_OF_FILE))
            parseStmt();
        match(TokenType.RBRACE, "}");
        localScopes.remove(localScopes.size()-1);
        scopeLevel--;
    }

    // ---------- 辅助 ----------
    private String getIdentId(String name) {
        for (int i = 0; i < identTable.size(); i++) {
            if (identTable.get(i).equals(name)) return "I" + (i+1);
        }
        return name;
    }

    private String getConstId(String val) {
        for (int i = 0; i < constValueTable.size(); i++) {
            if (constValueTable.get(i).equals(val)) return "C" + (i+1);
        }
        return val;
    }

    // ---------- 输出 ----------
    private void printTokenSequence() {
        System.out.println("\n========== Token 序列 ==========");
        for (int i = 0; i < tokenSequence.size(); i++) {
            if (i > 0) System.out.print(",");
            var e = tokenSequence.get(i);
            System.out.print("(" + e.getKey() + "," + e.getValue() + ")");
        }
        System.out.println();
    }

    private void printKeywordAndDelimiter() {
        System.out.println("\n========== 关键字表 ==========");
        for (int i = 1; i < keywordTable.size(); i++)
            System.out.println(i + ": " + keywordTable.get(i));
        System.out.println("\n========== 界符表 ==========");
        for (int i = 1; i < delimiterTable.size(); i++)
            System.out.println(i + ": " + delimiterTable.get(i));
    }

    private void printSymbolAndConst() {
        System.out.println("\n========== 标识符表 ==========");
        for (int i = 0; i < identTable.size(); i++)
            System.out.println((i+1) + ": " + identTable.get(i));
        System.out.println("\n========== 常数表 ==========");
        for (int i = 0; i < constValueTable.size(); i++)
            System.out.println((i+1) + ": " + constValueTable.get(i));
    }

    public void printSymbolTables() {
        SymbolTables.printNewSymbolTables(keywordTable, delimiterTable);
    }

    public void printQuads() {
        System.out.println("\n========== 四元式 ==========");
        var quads = quadGen.getQuads();
        for (int i = 0; i < quads.size(); i++)
            System.out.println(i + ": " + quads.get(i));
    }

    public void compile() {
        while (!check(TokenType.END_OF_FILE))
            parseStmt();
        printTokenSequence();
        printKeywordAndDelimiter();
        printSymbolAndConst();
        printSymbolTables();
        printQuads();
    }
}