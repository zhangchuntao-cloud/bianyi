package compiler.lexer;

import compiler.common.TokenType;
import compiler.symbol.SymbolTables;
import compiler.symbol.TypeEntry;
import compiler.common.DataType;

import java.util.*;

public class Lexer {
    private final String source;
    private int pos = 0;
    private int line = 1;
    private final CompilerContext contextObj;

    // 为了解耦，定义一个内部接口，让 Lexer 能够回调添加标识符/常量/Token序列
    public interface CompilerContext {
        int addIdent(String name);
        int addConstValue(String val);
        void addTokenPair(char type, int index);
        void addConst(String val, TypeEntry constType);
    }

    //private CompilerContext contextObj;

    public Lexer(String src, CompilerContext ctx) {
        this.source = src;
        this.contextObj = ctx;
    }

    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private void advance() {
        if (pos < source.length()) pos++;
    }

    private void skipSpacesAndComments() {
        while (pos < source.length()) {
            char c = peek();
            if (Character.isWhitespace(c)) {
                if (c == '\n') line++;
                advance();
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos+1) == '/') {
                pos += 2;
                while (pos < source.length() && peek() != '\n') advance();
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos+1) == '*') {
                pos += 2;
                while (pos < source.length()) {
                    if (peek() == '\n') line++;
                    if (peek() == '*' && pos+1 < source.length() && source.charAt(pos+1) == '/') {
                        pos += 2;
                        break;
                    }
                    advance();
                }
            } else {
                break;
            }
        }
    }

    private Token number() {
        int start = pos;
        boolean isReal = false;
        while (pos < source.length() && Character.isDigit(peek())) advance();
        if (peek() == '.') {
            isReal = true;
            advance();
            while (pos < source.length() && Character.isDigit(peek())) advance();
        }
        if (peek() == 'e' || peek() == 'E') {
            isReal = true;
            advance();
            if (peek() == '+' || peek() == '-') advance();
            while (pos < source.length() && Character.isDigit(peek())) advance();
        }
        String val = source.substring(start, pos);
        if (isReal) {
            try {
                double d = Double.parseDouble(val);
                if (Double.isInfinite(d))
                    System.err.println("Warning (line " + line + "): Float overflow");
            } catch (NumberFormatException e) {
                System.err.println("Error (line " + line + "): Invalid float");
                System.exit(1);
            }
        } else {
            try {
                long ll = Long.parseLong(val);
                if (ll > 2147483647L || ll < -2147483648L)
                    System.err.println("Warning: int out of range");
            } catch (NumberFormatException e) {
                System.err.println("Error: invalid integer");
                System.exit(1);
            }
        }
        int constIdx = contextObj.addConstValue(val);
        contextObj.addTokenPair('c', constIdx);
        TypeEntry constType = isReal ? SymbolTables.addType(DataType.DT_FLOAT, null) : SymbolTables.addType(DataType.DT_INT, null);
        contextObj.addConst(val, constType);
        return new Token(isReal ? TokenType.REAL : TokenType.INTEGER, val, line);
    }

    private Token identifier() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) advance();
        String word = source.substring(start, pos);
        // 关键字表
        Map<String, TokenType> kwMap = Map.of(
                "int", TokenType.KW_INT, "float", TokenType.KW_FLOAT, "char", TokenType.KW_CHAR,
                "void", TokenType.KW_VOID, "struct", TokenType.KW_STRUCT, "if", TokenType.KW_IF,
                "else", TokenType.KW_ELSE, "while", TokenType.KW_WHILE, "write", TokenType.KW_WRITE,
                "return", TokenType.KW_RETURN
        );
        if (kwMap.containsKey(word)) {
            TokenType tt = kwMap.get(word);
            int kwIdx = 0;
            // 从预定义的关键字表中获取索引（为了方便，使用switch或事先建立静态映射）
            kwIdx = getKeywordIndex(word);
            contextObj.addTokenPair('k', kwIdx);
            return new Token(tt, word, line);
        } else {
            int idIdx = contextObj.addIdent(word);
            contextObj.addTokenPair('i', idIdx);
            return new Token(TokenType.ID, word, line);
        }
    }

    private int getKeywordIndex(String word) {
        // 与C++中 keywordTable 索引一致: 1..10
        switch (word) {
            case "int": return 1;
            case "float": return 2;
            case "char": return 3;
            case "void": return 4;
            case "struct": return 5;
            case "if": return 6;
            case "else": return 7;
            case "while": return 8;
            case "write": return 9;
            case "return": return 10;
            default: return 0;
        }
    }

    private Token charConst() {
        advance(); // '
        int val = 0;
        if (peek() == '\\') {
            advance();
            switch (peek()) {
                case 'n': val = '\n'; break;
                case 't': val = '\t'; break;
                case '\\': val = '\\'; break;
                case '\'': val = '\''; break;
                default: val = peek(); break;
            }
        } else {
            val = (int) peek();
        }
        advance();
        if (peek() != '\'') {
            System.err.println("Unclosed char constant");
            System.exit(1);
        }
        advance();
        String valStr = Integer.toString(val);
        int constIdx = contextObj.addConstValue(valStr);
        contextObj.addTokenPair('c', constIdx);
        contextObj.addConst(valStr, SymbolTables.addType(DataType.DT_CHAR, null));
        return new Token(TokenType.CHAR_CONST, valStr, line);
    }

    private Token stringConst() {
        advance(); // "
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && peek() != '"') {
            if (peek() == '\n') {
                System.err.println("Unclosed string");
                System.exit(1);
            }
            if (peek() == '\\') {
                advance();
                switch (peek()) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(peek()); break;
                }
            } else {
                sb.append(peek());
            }
            advance();
        }
        if (peek() != '"') {
            System.err.println("Unclosed string");
            System.exit(1);
        }
        advance();
        String val = sb.toString();
        int constIdx = contextObj.addConstValue(val);
        contextObj.addTokenPair('c', constIdx);
        contextObj.addConst(val, SymbolTables.addType(DataType.DT_CHAR, null));
        return new Token(TokenType.STRING_CONST, val, line);
    }

    public Token next() {
        while (pos < source.length()) {
            skipSpacesAndComments();
            if (pos >= source.length()) break;
            char c = peek();
            if (Character.isLetter(c) || c == '_') return identifier();
            if (Character.isDigit(c)) return number();
            if (c == '\'') return charConst();
            if (c == '"') return stringConst();
            if (c == '-') {
                advance();
                if (peek() == '>') {
                    advance();
                    contextObj.addTokenPair('p', 24); // 24 对应 "->"
                    return new Token(TokenType.ARROW, "->", line);
                }
                contextObj.addTokenPair('p', 2); // 2 对应 "-"
                return new Token(TokenType.MINUS, "-", line);
            }
            advance();
            switch (c) {
                case '+':
                    contextObj.addTokenPair('p', 1);
                    return new Token(TokenType.PLUS, "+", line);
                case '*':
                    contextObj.addTokenPair('p', 3);
                    return new Token(TokenType.MUL, "*", line);
                case '/':
                    contextObj.addTokenPair('p', 4);
                    return new Token(TokenType.DIV, "/", line);
                case '=':
                    if (peek() == '=') {
                        advance();
                        contextObj.addTokenPair('p', 9);
                        return new Token(TokenType.EQ, "==", line);
                    }
                    contextObj.addTokenPair('p', 14);
                    return new Token(TokenType.ASSIGN, "=", line);
                case '<':
                    if (peek() == '=') {
                        advance();
                        contextObj.addTokenPair('p', 7);
                        return new Token(TokenType.LE, "<=", line);
                    }
                    contextObj.addTokenPair('p', 5);
                    return new Token(TokenType.LT, "<", line);
                case '>':
                    if (peek() == '=') {
                        advance();
                        contextObj.addTokenPair('p', 8);
                        return new Token(TokenType.GE, ">=", line);
                    }
                    contextObj.addTokenPair('p', 6);
                    return new Token(TokenType.GT, ">", line);
                case '!':
                    if (peek() == '=') {
                        advance();
                        contextObj.addTokenPair('p', 10);
                        return new Token(TokenType.NE, "!=", line);
                    }
                    contextObj.addTokenPair('p', 13);
                    return new Token(TokenType.NOT, "!", line);
                case '&':
                    if (peek() == '&') {
                        advance();
                        contextObj.addTokenPair('p', 11);
                        return new Token(TokenType.AND, "&&", line);
                    }
                    System.err.println("& error");
                    System.exit(1);
                case '|':
                    if (peek() == '|') {
                        advance();
                        contextObj.addTokenPair('p', 12);
                        return new Token(TokenType.OR, "||", line);
                    }
                    System.err.println("| error");
                    System.exit(1);
                case '(':
                    contextObj.addTokenPair('p', 15);
                    return new Token(TokenType.LPAREN, "(", line);
                case ')':
                    contextObj.addTokenPair('p', 16);
                    return new Token(TokenType.RPAREN, ")", line);
                case '[':
                    contextObj.addTokenPair('p', 17);
                    return new Token(TokenType.LBRACKET, "[", line);
                case ']':
                    contextObj.addTokenPair('p', 18);
                    return new Token(TokenType.RBRACKET, "]", line);
                case '{':
                    contextObj.addTokenPair('p', 19);
                    return new Token(TokenType.LBRACE, "{", line);
                case '}':
                    contextObj.addTokenPair('p', 20);
                    return new Token(TokenType.RBRACE, "}", line);
                case ';':
                    contextObj.addTokenPair('p', 21);
                    return new Token(TokenType.SEMI, ";", line);
                case ',':
                    contextObj.addTokenPair('p', 22);
                    return new Token(TokenType.COMMA, ",", line);
                case '.':
                    contextObj.addTokenPair('p', 23);
                    return new Token(TokenType.DOT, ".", line);
                default:
                    System.err.println("Unknown char '" + c + "' at line " + line);
                    System.exit(1);
            }
        }
        return new Token(TokenType.END_OF_FILE, "", line);
    }
}