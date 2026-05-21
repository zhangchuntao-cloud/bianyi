package compiler.lexer;

import compiler.common.TokenType;

public class Token {
    public TokenType type;
    public String value;
    public int line;

    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    // 用于 END_OF_FILE 的便捷构造
    public Token() {
        this(TokenType.END_OF_FILE, "", 0);
    }
}