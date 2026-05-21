编译器前端 Java 项目说明文档

## 1. 项目概述

### 1.1 项目目标

本项目是一款基于 Java 实现的编译器前端工具，核心目标是将类 C 语言的源代码转换为四元式中间代码，并完整实现编译器前端的核心流程（词法分析→语法分析→语义分析→中间代码生成）。项目从 C++ 版本零逻辑丢失迁移至 Java 语言，保留所有核心特性，同时适配 Java 语言的面向对象特性和内存模型。

### 1.2 实现的功能

#### 支持的数据类型

- 基本类型：整型（int）、字符型（char）、布尔型（bool）
- 复合类型：数组（一维/多维）、结构体（自定义成员）
- 函数类型：支持带返回值/无返回值、带参数/无参数的函数定义与调用

#### 支持的语句

- 基础语句：赋值语句、表达式语句、空语句
- 分支语句：if 语句、if-else 语句、switch 语句
- 循环语句：for 循环、while 循环、do-while 循环
- 跳转语句：break、continue、return
- 输入输出：read（读入）、write（输出）语句

#### 支持的表达式

- 算术表达式：+、-、*、/、%（取模）、++（自增）、--（自减）
- 关系表达式：>、<、>=、<=、==、!=
- 逻辑表达式：&&（与）、||（或）、!（非）
- 复合表达式：括号优先级、逗号表达式（模拟实现）
- 访问表达式：数组下标访问、结构体成员访问（. 运算符）

### 1.3 主要特性

1. **词法分析**：基于状态机实现，将源代码拆分为 Token（关键字、标识符、常量、运算符、分隔符），支持 Token 回溯、位置记录。
2. **符号表系统**：多表联动设计，管理标识符的类型、作用域、存储属性等信息。
3. **递归下降语法分析**：手动实现 LL(1) 风格的递归下降解析器，完成语法校验与语义分析。
4. **四元式生成**：生成结构化的四元式中间代码，支持常量折叠、临时变量自动管理。
5. **语法分析演示**：内置 LL(1)、LR(0)、简单优先分析法的演示模块，可独立运行并展示分析过程。

## 2. 模块结构与包划分

### 2.1 包职责说明

| 包名     | 核心职责                                                     |
| -------- | ------------------------------------------------------------ |
| `common` | 公共基础组件：全局常量（关键字、运算符、Token 类型）、工具类（字符串处理、异常定义）、通用枚举（TokenType、QuadType 等） |
| `symbol` | 符号表系统：所有符号表（SYNBL/TYPEL 等）的定义、符号表项、表操作工具类 |
| `lexer`  | 词法分析模块：Lexer 类（词法分析器）、Token 类、词法错误定义 |
| `ir`     | 中间代码模块：四元式（Quad）定义、四元式生成器（QuadGenerator）、临时变量/标号管理器 |
| `parser` | 语法/语义分析模块：Compiler 核心类（递归下降解析）、语义错误处理、语句/表达式解析逻辑 |
| `demo`   | 语法分析演示模块：LL(1)、LR(0)、简单优先分析法的独立演示类   |

### 2.2 目录组织结构

```Plaintext
src/
├── Main.java                // 程序入口，整合编译流程与演示入口
├── common/
│   ├── Constants.java       // 全局常量（关键字、运算符、正则表达式）
│   ├── TokenType.java       // Token 类型枚举
│   ├── QuadType.java        // 四元式类型枚举
│   ├── CompilerException.java // 编译异常基类
│   └── Utils.java           // 通用工具类（字符串、类型转换）
├── symbol/
│   ├── SymbolTables.java    // 全局符号表容器
│   ├── SynblItem.java       // 主符号表（SYNBL）项
│   ├── TypelItem.java       // 类型表（TYPEL）项
│   ├── AinflItem.java       // 数组表（AINFL）项
│   ├── RinflItem.java       // 结构体表（RINFL）项
│   ├── PfinflItem.java      // 函数表（PFINFL）项
│   ├── ConslItem.java       // 常量表（CONSL）项
│   └── ActivationRecord.java // 活动记录（作用域管理）
├── lexer/
│   ├── Token.java           // Token 实体类（类型、值、位置）
│   ├── Lexer.java           // 词法分析器核心类
│   └── LexerException.java  // 词法错误异常
├── ir/
│   ├── Quad.java            // 四元式实体类（操作码、操作数1、操作数2、结果）
│   ├── QuadGenerator.java   // 四元式生成器
│   └── TempManager.java     // 临时变量/标号管理器
├── parser/
│   ├── Compiler.java        // 编译器核心（递归下降解析、语义分析）
│   ├── ParserException.java // 语法/语义错误异常
│   └── SemanticAnalyzer.java // 语义分析辅助类
└── demo/
    ├── LL1Demo.java         // LL(1) 分析法演示
    ├── LR0Demo.java         // LR(0) 分析法演示
    └── SimplePrecedenceDemo.java // 简单优先分析法演示
```

## 3. 核心类详解

### 3.1 `Lexer`（词法分析器）

#### 设计思路

基于**有限状态自动机（FSA）** 实现，核心状态包括：初始态、标识符态、数字态、运算符态、分隔符态、注释态等。通过逐字符读取源代码，根据字符类型和状态转移规则生成 Token，同时记录 Token 的行号、列号（用于错误定位）。

#### 核心特性

1. **Token 缓存与回溯**：内置 Token 缓冲区，支持 `peek()`（预览下一个 Token）、`unget()`（回溯已读取的 Token），满足递归下降解析的回溯需求。
2. **接口回调机制**：提供 `TokenHandler` 回调接口，可自定义 Token 处理逻辑（如打印 Token 流、过滤无效 Token）。
3. **错误处理**：识别非法字符、未闭合注释等词法错误，抛出 `LexerException` 并附带位置信息。

#### 核心方法

```Java
// 获取下一个 Token
public Token nextToken() throws LexerException;
// 预览下一个 Token（不消费）
public Token peekToken() throws LexerException;
// 回溯 Token
public void ungetToken(Token token);
// 设置 Token 处理回调
public void setTokenHandler(TokenHandler handler);
```

### 3.2 `Compiler`（编译器核心类）

#### 设计思路

整合词法分析、语法分析、语义分析、中间代码生成的核心类，采用**递归下降解析**思想，为每个非终结符（如 program、stmt、expr）实现对应的解析方法，解析过程中同步完成符号表维护和四元式生成。

#### 核心职责

1. **递归下降解析**：
	1. 程序入口：`parseProgram()` 解析整个源程序；
	2. 语句解析：`parseIfStmt()`、`parseForStmt()` 等解析对应语句；
	3. 表达式解析：`parseExpr()`、`parseArithExpr()` 等解析表达式，处理运算符优先级。
2. **符号表管理**：解析过程中调用 `SymbolTables` 的方法，完成标识符的插入、查询、作用域切换。
3. **四元式生成**：调用 `QuadGenerator` 生成四元式，处理语义逻辑（如类型检查、常量折叠）。
4. **错误处理**：捕获语法/语义错误，抛出 `ParserException` 并定位错误位置。

### 3.3 `SymbolTables`（全局符号表容器）

#### 设计思路

单例模式实现的全局符号表容器，整合所有符号表（SYNBL/TYPEL 等），提供统一的插入、查询、打印接口，维护各表之间的关联关系。

#### 核心方法

1. **表操作**：`insertSynbl()`（插入主符号表）、`queryTypel()`（查询类型表）等；
2. **作用域管理**：`pushScope()`（进入作用域）、`popScope()`（退出作用域）；
3. **辅助工具**：`getSymbolByID()`（通过 ID 关联各表）、`printAllTables()`（打印所有符号表）。

### 3.4 `QuadGenerator`（四元式生成器）

#### 设计思路

负责四元式的生成、管理，以及临时变量/标号的自动分配，内置常量折叠优化逻辑，降低中间代码冗余。

#### 核心特性

1. **临时变量管理**：自动生成唯一临时变量名（如 `t1`、`t2`），支持回收复用；
2. **标号管理**：自动生成跳转标号（如 `L1`、`L2`），用于分支/循环跳转；
3. **常量折叠**：在生成四元式前，对常量表达式（如 `1+2`、`3*4`）直接计算结果，减少运行时开销；
4. **四元式生成**：提供 `genAssign()`、`genArith()`、`genJump()` 等方法，生成不同类型的四元式。

### 3.5 演示类

#### `LL1Demo`

- 作用：演示 LL(1) 分析法的核心流程，包括构造 FIRST 集、FOLLOW 集、预测分析表，以及基于预测分析表的语法分析过程。
- 使用：独立运行，输入文法规则和待分析串，输出分析栈、剩余输入串、动作步骤。

#### `LR0Demo`

- 作用：演示 LR(0) 分析法的核心流程，包括构造拓广文法、项目集规范族、LR(0) 分析表，以及移进-归约分析过程。
- 使用：独立运行，输入文法规则和待分析串，输出分析栈、符号栈、剩余输入串、动作步骤。

#### `SimplePrecedenceDemo`

- 作用：演示简单优先分析法的核心流程，包括构造优先关系矩阵，以及基于优先矩阵的语法分析过程。
- 使用：独立运行，输入文法规则和待分析串，输出分析栈、剩余输入串、优先关系判断、动作步骤。

## 4. 符号表系统设计

### 4.1 各表含义

| 表名     | 全称              | 核心作用                                                     |
| -------- | ----------------- | ------------------------------------------------------------ |
| SYNBL    | 主符号表          | 存储所有标识符的基础信息：标识符名、符号类型（变量/数组/结构体/函数）、作用域、关联的 TYPEL/AINFL 等表项 ID |
| TYPEL    | 类型表            | 存储类型信息：类型名（int/char/结构体名）、类型长度、类型类别（基本/复合） |
| AINFL    | 数组表            | 存储数组信息：数组基类型、维度、各维度长度、数组首地址       |
| RINFL    | 结构体表          | 存储结构体信息：结构体名、成员列表（成员名+成员类型+偏移量）、结构体总长度 |
| PFINFL   | 函数表            | 存储函数信息：函数返回值类型、参数列表（参数名+参数类型+参数个数）、函数入口地址、局部变量个数 |
| CONSL    | 常量表            | 存储常量信息：常量值、常量类型、常量存储地址                 |
| 活动记录 | Activation Record | 管理作用域（全局/局部）、当前作用域的符号表项、作用域嵌套层级 |

### 4.2 各表之间的关联方式

1. **SYNBL 作为核心关联枢纽**：
	1. SYNBL 项的 `typeId` 关联 TYPEL 项的唯一 ID，标识标识符的类型；
	2. SYNBL 项的 `extId` 关联扩展表 ID：数组类型关联 AINFL ID、结构体类型关联 RINFL ID、函数类型关联 PFINFL ID、常量关联 CONSL ID。
2. **TYPEL 关联复合类型**：
	1. 若 TYPEL 项为结构体类型，其 `extId` 关联 RINFL 项 ID；
	2. 若 TYPEL 项为数组类型，其 `extId` 关联 AINFL 项 ID。
3. **活动记录关联 SYNBL**：
	1. 每个活动记录对应一个作用域，存储该作用域下的所有 SYNBL 项 ID，支持作用域的入栈/出栈，实现局部变量的生命周期管理。

### 4.3 符号表输出格式示例

#### 主符号表（SYNBL）

| 符号名 | 符号类型 | 作用域 | TypeID | ExtID | 行号 |
| ------ | -------- | ------ | ------ | ----- | ---- |
| a      | 变量     | 全局   | 1      | -     | 2    |
| arr    | 数组     | 局部   | 2      | 1     | 5    |
| user   | 结构体   | 全局   | 3      | 1     | 8    |
| add    | 函数     | 全局   | 1      | 1     | 12   |

#### 类型表（TYPEL）

| TypeID | 类型名 | 类型类别 | 长度 | ExtID |
| ------ | ------ | -------- | ---- | ----- |
| 1      | int    | 基本类型 | 4    | -     |
| 2      | int[]  | 数组     | 20   | 1     |
| 3      | user   | 结构体   | 8    | 1     |

#### 数组表（AINFL）

| AINFLID | 基类型ID | 维度 | 各维度长度 | 首地址 |
| ------- | -------- | ---- | ---------- | ------ |
| 1       | 1        | 1    | [5]        | 0x1000 |

## 5. 四元式中间代码

### 5.1 支持的四元式类型

| 四元式类型 | 格式                        | 说明                                  |
| ---------- | --------------------------- | ------------------------------------- |
| 赋值       | (assign, A, -, B)           | B = A                                 |
| 算术运算   | (op, A, B, T)               | T = A op B（op：+、-、*、/ 等）       |
| 关系运算   | (relop, A, B, T)            | T = A relop B（relop：>、<、== 等）   |
| 逻辑运算   | (logop, A, B, T)            | T = A logop B（logop：&&、            |
| 数组访问   | (arr_get, A, B, T)          | T = A[B]（取数组元素）                |
| 数组赋值   | (arr_set, A, B, C)          | A[B] = C（设置数组元素）              |
| 结构体访问 | (struct_get, A, B, T)       | T = A.B（取结构体成员）               |
| 结构体赋值 | (struct_set, A, B, C)       | A.B = C（设置结构体成员）             |
| 函数调用   | (call, F, ParamList, T)     | T = F(ParamList)（无返回值则 T 为 -） |
| 无条件跳转 | (jmp, -, -, L)              | 跳转到标号 L                          |
| 条件跳转   | (jrelop, A, B, L)           | 若 A relop B 为真，跳转到 L           |
| 标签       | (label, -, -, L)            | 定义标号 L                            |
| 返回       | (ret, A, -, -)              | 返回值 A（无返回值则 A 为 -）         |
| 写操作     | (write, A, -, -)            | 输出 A                                |
| 读操作     | (read, -, -, A)             | 读入值到 A                            |
| 函数定义   | (func_def, F, ParamList, -) | 定义函数 F，参数列表为 ParamList      |

### 5.2 典型代码生成的四元式示例

#### 源程序片段

```C
int main() {
    int a = 10, b = 20;
    int c = a + b * 2;
    if (c > 30) {
        write(c);
    }
    return 0;
}
```

#### 生成的四元式

| 序号 | 四元式                 | 说明                  |
| ---- | ---------------------- | --------------------- |
| 1    | (label, -, -, L_main)  | 主函数入口标号        |
| 2    | (assign, 10, -, a)     | a = 10                |
| 3    | (assign, 20, -, b)     | b = 20                |
| 4    | (mul, b, 2, t1)        | t1 = b * 2            |
| 5    | (add, a, t1, c)        | c = a + t1            |
| 6    | (relop, c, 30, t2)     | t2 = c > 30           |
| 7    | (jrelop, t2, true, L1) | 若 t2 为真，跳转到 L1 |
| 8    | (jmp, -, -, L2)        | 无条件跳转到 L2       |
| 9    | (label, -, -, L1)      | 分支入口标号          |
| 10   | (write, c, -, -)       | 输出 c                |
| 11   | (label, -, -, L2)      | 分支结束标号          |
| 12   | (ret, 0, -, -)         | 返回 0                |

## 6. 编译与运行指南

### 6.1 环境要求

- JDK 版本：JDK 21 或更高版本（推荐 JDK 21，兼容模块化与语法特性）；
- 操作系统：Windows/Linux/macOS（无系统依赖）；
- 依赖：无第三方依赖，纯 Java 原生实现。

### 6.2 编译命令

1. 进入项目根目录（src 文件夹所在目录）；
2. 执行编译命令，将编译后的类文件输出到 `out` 目录：

```Bash
javac -d out src/**/*.java src/Main.java
```

- 说明：
	- `-d out`：指定编译后的类文件输出目录为 `out`；
	- `src/**/*.java`：递归编译 src 下所有 Java 文件；
	- `src/Main.java`：指定程序入口类。

### 6.3 运行命令

1. 编译完成后，执行运行命令：

```Bash
java -cp out Main
```

- 说明：`-cp out` 指定类路径为 `out` 目录（编译后的类文件所在目录）。

### 6.4 测试文件 `test.txt` 编写要求及位置

#### 位置要求

- 将 `test.txt` 放在项目根目录（与 `src`、`out` 同级目录），程序默认读取该路径下的 `test.txt` 作为源程序输入。

#### 编写要求

1. 语法遵循类 C 语言规范，支持项目中定义的所有数据类型、语句、表达式；
2. 关键字区分大小写（如 `int`、`if`、`while` 需小写）；
3. 标识符命名规则：字母/下划线开头，由字母、数字、下划线组成；
4. 语句结束必须加分号（;），代码块用大括号（{}）包裹；
5. 示例模板：

```C
// 注释支持（单行//）
struct User {
    int id;
    char name;
};

int add(int x, int y) {
    return x + y;
}

int main() {
    int arr[5] = {1,2,3,4,5};
    User u;
    u.id = 100;
    u.name = 'a';
    int res = add(arr[2], u.id);
    write(res);
    return 0;
}
```

## 7. 测试用例说明

### 7.1 完整测试代码片段

```C
// 测试用例：包含结构体、数组、函数调用、循环、输出
struct Student {
    int score;
    char grade;
};

int calc_sum(int arr[], int len) {
    int sum = 0;
    int i = 0;
    while (i < len) {
        sum += arr[i];
        i++;
    }
    return sum;
}

int main() {
    // 定义数组
    int scores[4] = {85, 92, 78, 90};
    // 定义结构体
    Student s;
    s.score = calc_sum(scores, 4);
    // 计算等级
    if (s.score >= 340) {
        s.grade = 'A';
    } else {
        s.grade = 'B';
    }
    // 输出结果
    write("总分：");
    write(s.score);
    write("\n等级：");
    write(s.grade);
    return 0;
}
```

### 7.2 预期输出

```Plaintext
总分：345
等级：A
```

### 7.3 编译器实际处理过程

1. **词法分析**：将源代码拆分为 Token 流，如 `struct`、`Student`、`{`、`int`、`score`、`;` 等，识别关键字、标识符、常量。
2. **符号表构建**：
	1. 解析 `struct Student`，在 RINFL 中插入结构体项，TYPEL 中插入 `Student` 类型，SYNBL 中插入 `Student` 标识符；
	2. 解析 `calc_sum` 函数，在 PFINFL 中插入函数项，SYNBL 中插入 `calc_sum` 标识符；
	3. 解析 `main` 函数内的 `scores` 数组，在 AINFL 中插入数组项，SYNBL 中插入 `scores` 标识符；
	4. 解析 `s` 结构体变量，在 SYNBL 中插入 `s` 标识符，关联 TYPEL 中的 `Student` 类型。
3. **语法分析**：递归下降解析各语句/表达式，校验语法正确性（如函数参数类型匹配、数组下标类型、结构体成员访问合法性）。
4. **四元式生成**：
	1. 生成 `calc_sum` 函数的四元式（循环累加数组元素）；
	2. 生成 `s.score = calc_sum(...)` 的赋值四元式；
	3. 生成 if-else 分支的条件跳转四元式；
	4. 生成 write 语句的输出四元式。
5. **输出结果**：运行编译器后，输出符号表内容、四元式列表，最终模拟执行四元式输出预期结果。

## 8. 常见问题与解决方案

### 8.1 `test.txt` 找不到的解决方法

#### 问题现象

运行程序时抛出 `FileNotFoundException: test.txt (系统找不到指定的文件)`。

#### 解决方案

1. 确认 `test.txt` 放在项目根目录（与 `src`、`out` 同级）；
2. 若自定义文件路径，修改 `Main.java` 中读取文件的代码：

```Java
// 原代码
File file = new File("test.txt");
// 修改为自定义路径
File file = new File("D:/project/compiler/test.txt"); // Windows
// 或
File file = new File("/home/user/compiler/test.txt"); // Linux/macOS
```

1. 运行时指定文件路径参数，在 `Main` 类中接收命令行参数：

```Java
public static void main(String[] args) {
    String filePath = args.length > 0 ? args[0] : "test.txt";
    File file = new File(filePath);
    // 后续逻辑
}
```

运行命令：`java -cp out Main /path/to/test.txt`。

### 8.2 Windows 控制台中文乱码处理

#### 问题现象

编译器输出的中文（如符号表注释、错误提示）在 Windows 控制台显示为乱码。

#### 解决方案

1. 运行程序时指定编码为 GBK（Windows 控制台默认编码）：

```Bash
java -cp out -Dfile.encoding=GBK Main
```

1. 编译时指定编码（可选）：

```Bash
javac -encoding UTF-8 -d out src/**/*.java src/Main.java
```

1. 若使用 IDE（如 IDEA），设置控制台编码为 GBK：Run/Debug Configurations → VM options → 添加 `-Dfile.encoding=GBK`。

### 8.3 Java 不支持逗号运算符的改写方式

#### 问题现象

C++ 支持逗号运算符（如 `a = (b=1, c=2, b+c)`），但 Java 无原生逗号运算符，直接迁移会语法错误。

#### 解决方案

1. 语法分析层模拟逗号运算符：将逗号表达式拆分为多个赋值语句，按顺序执行；
2. 四元式生成层处理：

```Java
// 原 C++ 逻辑（逗号表达式）
expr = parseExpr(',');
// Java 改写
List<ExprNode> exprList = parseCommaExpr();
for (int i = 0; i < exprList.size() - 1; i++) {
    genQuad(exprList.get(i)); // 生成中间表达式的四元式（仅执行，不保存结果）
}
ExprNode lastExpr = exprList.get(exprList.size() - 1);
genQuad(lastExpr); // 生成最后一个表达式的四元式（作为逗号表达式结果）
```

### 8.4 符号表字段未初始化警告的修复

#### 问题现象

编译时出现 `Variable 'xxx' might not have been initialized` 警告（如符号表项的 `extId` 字段）。

#### 解决方案

1. 为符号表项类的字段设置默认值：

```Java
// 原代码
public class SynblItem {
    private int extId; // 未初始化
    // ...
}
// 修改后
public class SynblItem {
    private int extId = -1; // 默认值（表示无关联扩展表）
    // ...
}
```

1. 在构造函数中强制初始化所有字段：

```Java
public SynblItem(String name, SymbolType type, int scope) {
    this.name = name;
    this.type = type;
    this.scope = scope;
    this.typeId = -1; // 初始化
    this.extId = -1;  // 初始化
    this.lineNum = 0; // 初始化
}
```

## 9. 扩展与维护建议

### 9.1 如何添加新类型、新语句、新表达式

#### 添加新数据类型（如浮点型 float）

1. 在 `common/TokenType.java` 中添加 `FLOAT` 枚举值；
2. 在 `common/Constants.java` 中添加浮点型关键字 `float`；
3. 在 `lexer/Lexer.java` 中扩展状态机，识别浮点数字面量（如 `3.14`）；
4. 在 `symbol/TypelItem.java` 中添加浮点型类型项（类型名、长度等）；
5. 在 `parser/Compiler.java` 的表达式解析方法中，支持浮点型运算的类型检查与四元式生成。

#### 添加新语句（如 goto 语句）

1. 在 `common/TokenType.java` 中添加 `GOTO` 枚举值；
2. 在 `parser/Compiler.java` 中实现 `parseGotoStmt()` 方法，解析 `goto 标号;` 语法；
3. 在 `ir/QuadGenerator.java` 中添加生成 goto 语句四元式的方法（无条件跳转）；
4. 扩展语义分析逻辑，校验标号的合法性（已定义、作用域合法）。

#### 添加新表达式（如位运算 &、|、^）

1. 在 `common/TokenType.java` 中添加位运算枚举值（`BIT_AND`、`BIT_OR`、`BIT_XOR`）；
2. 在 `lexer/Lexer.java` 中识别位运算符号；
3. 在 `parser/Compiler.java` 的 `parseArithExpr()` 方法中扩展位运算的优先级处理；
4. 在 `ir/QuadGenerator.java` 中添加位运算四元式生成方法。

### 9.2 如何将四元式转换为目标代码

#### 转换为汇编代码（以 x86 汇编为例）

1. 构建四元式到汇编指令的映射表：
	1. 赋值四元式 `(assign, A, -, B)` → `mov eax, [A]; mov [B], eax`；
	2. 加法四元式 `(add, A, B, T)` → `mov eax, [A]; add eax, [B]; mov [T], eax`；
	3. 跳转四元式 `(jmp, -, -, L)` → `jmp L`。
2. 实现 `AssemblerGenerator` 类，遍历四元式列表，逐行生成汇编指令；
3. 处理临时变量/标号的汇编映射：临时变量映射到寄存器/栈内存，标号映射到汇编标签；
4. 生成完整的汇编文件（.s），通过汇编器（如 nasm）编译为可执行文件。

#### 转换为 LLVM IR

1. 学习 LLVM IR 语法规范（基于SSA形式）；
2. 实现 `LLVMIRGenerator` 类，将四元式转换为 LLVM IR 指令：
	1. 赋值四元式 → `store i32 %A, i32* %B`；
	2. 加法四元式 → `%T = add nsw i32 %A, %B`；
3. 处理函数调用、数组访问等复杂逻辑的 LLVM IR 生成；
4. 输出 LLVM IR 文件（.ll），通过 `llc` 工具编译为目标机器码。

### 9.3 优化符号表打印格式

#### 现有问题

符号表打印为纯文本，格式混乱，不易阅读。

#### 优化方案

1. 输出为表格格式（使用 `System.out.printf` 格式化）：

```Java
// 打印主符号表
System.out.println("=== 主符号表（SYNBL）===");
System.out.printf("%-10s %-8s %-6s %-8s %-6s %-6s\n", "符号名", "符号类型", "作用域", "TypeID", "ExtID", "行号");
System.out.println("---------------------------------------------");
for (SynblItem item : synblList) {
    System.out.printf("%-10s %-8s %-6d %-8d %-6d %-6d\n",
        item.getName(), item.getType(), item.getScope(),
        item.getTypeId(), item.getExtId(), item.getLineNum());
}
```

1. 支持导出为 CSV/Excel 文件：
	1. 使用 Apache POI 库（Excel）或原生文件写入（CSV）；
	2. 遍历符号表项，按列写入文件，方便后续分析。
2. 按作用域分组打印：
	1. 先打印全局作用域符号表，再按函数/代码块打印局部作用域符号表，清晰区分作用域层级。


