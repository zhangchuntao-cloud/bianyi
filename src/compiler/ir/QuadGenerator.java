package compiler.ir;

import java.util.ArrayList;
import java.util.List;

public class QuadGenerator {
    private final List<String> quads = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;

    public String newTemp() {
        return "t" + (tempCount++);
    }

    public String newLabel() {
        return "L" + (labelCount++);
    }

    public void emit(String s) {
        quads.add(s);
    }

    public List<String> getQuads() {
        return quads;
    }

    // 常量折叠辅助（静态工具方法）
    public static boolean tryFoldConst(String op, String left, String right, StringBuilder result) {
        try {
            double l = Double.parseDouble(left);
            double r = Double.parseDouble(right);
            double val = 0;
            switch (op) {
                case "+": val = l + r; break;
                case "-": val = l - r; break;
                case "*": val = l * r; break;
                case "/":
                    if (r == 0) return false;
                    val = l / r;
                    break;
                case "<":  val = l < r ? 1 : 0; break;
                case ">":  val = l > r ? 1 : 0; break;
                case "<=": val = l <= r ? 1 : 0; break;
                case ">=": val = l >= r ? 1 : 0; break;
                case "==": val = l == r ? 1 : 0; break;
                case "!=": val = l != r ? 1 : 0; break;
                default: return false;
            }
            String str = Double.toString(val);
            if (str.contains(".")) {
                str = str.replaceAll("0+$", "");
                if (str.endsWith(".")) str = str.substring(0, str.length()-1);
            }
            result.setLength(0);
            result.append(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}