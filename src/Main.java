import compiler.parser.Compiler;
import compiler.demo.LL1Demo;
import compiler.demo.LR0Demo;
import compiler.demo.SimplePrecedenceDemo;
import java.io.*;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) {
        // 设置控制台编码（Windows）
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "chcp 65001").inheritIO().start().waitFor();
            }
        } catch (Exception e) { /* ignore */ }

        String filename = "test.txt";
        try {
            String source = Files.readString(Path.of(filename));
            Compiler comp = new Compiler(source);
            comp.compile();
        } catch (IOException e) {
            System.err.println("Cannot open file: " + filename);
            e.printStackTrace();
            System.exit(1);
        }

        LL1Demo.run();
        LR0Demo.run();
        SimplePrecedenceDemo.run();
    }
}