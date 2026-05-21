# 编译器前端 Java 项目讲解说明文档

## 1. 项目概述

### 1.1 项目目标

本项目是一款基于 Java 实现的编译器前端工具，核心目标是将类 C 语言的源代码转换为四元式中间代码，并完整实现编译器前端的核心流程（词法分析 → 语法分析 → 语义分析 → 中间代码生成）。项目从 C++ 版本**零逻辑丢失**迁移至 Java 语言，保留所有核心特性，同时适配 Java 语言的面向对象特性和内存模型。

### 1.2 实现的功能

#### 支持的数据类型
- 基本类型：整型（int）、浮点型（float）、字符型（char）、布尔型（bool）、空类型（void）
- 复合类型：数组（一维/多维）、结构体（自定义成员）
- 函数类型：支持带返回值/无返回值、带参数/无参数的函数定义与调用

#### 支持的语句
- 基础语句：变量声明（含初始化）、赋值语句、表达式语句
- 分支语句：if 语句、if-else 语句
- 循环语句：while 循环
- 跳转语句：return
- 输入输出：write 语句

#### 支持的表达式
- 算术表达式：`+`、`-`、`*`、`/`
- 关系表达式：`>`、`<`、`>=`、`<=`、`==`、`!=`
- 逻辑表达式：`&&`（与）、`||`（或）、`!`（非）
- 访问表达式：数组下标访问、结构体成员访问（`.` 和 `->` 运算符）

### 1.3 代码来源

本项目的原始版本基于 **C++20** 实现，采用结构体存储符号表信息、递归下降解析语法树、全局函数管理四元式生成。Java 版本在保持所有功能完全一致的前提下进行了系统性重构，将结构体映射为 Java 类、全局函数映射为工具类静态方法、动态内存管理交由 Java GC 处理，实现了真正的跨语言迁移——从原始 C++ 代码到 Java 代码的逐行转换，确保算法逻辑、数据结构、边界处理、异常流程均与原版一致。

### 1.4 主要特性

1. **词法分析**：基于有限状态自动机实现，将源代码拆分为 Token（关键字、标识符、常量、运算符、分隔符），支持 Token 回溯、位置记录和注释识别。

2. **符号表系统**：多表联动设计，包含主符号表（SYNBL）、类型表（TYPEL）、数组表（AINFL）、结构体表（RINFL）、函数表（PFINFL）、常量表（CONSL）和活动记录，全面管理标识符的类型、作用域、存储属性等信息。

3. **递归下降语法分析**：手动实现递归下降解析器，为每个非终结符（如 program、stmt、expr）实现对应的解析方法，完成语法校验与语义分析。

4. **四元式生成**：生成结构化的四元式中间代码，支持常量折叠、临时变量自动管理、跳转标号自动分配。

5. **语法分析演示**：内置 LL(1)、LR(0)、简单优先分析法的演示模块，可独立运行并展示完整的分析过程。

6. **符号表可视化输出**：标准化输出符号表系统的全部内容，便于理解编译器核心数据结构。

项目托管于 GitHub：[https://github.com/zhangchuntao-cloud/bianyi](https://github.com/zhangchuntao-cloud/bianyi)

## 2. 模块结构与包划分

### 2.1 包职责说明

| 包名 | 核心职责 |
|------|----------|
| `common` | 公共基础组件：数据类型枚举（DataType）、符号种类枚举（SymbolKind）、Token 类型枚举（TokenType） |
| `symbol` | 符号表系统：所有符号表（SYNBL/TYPEL 等）的定义、符号表条目、表操作工具类 |
| `lexer` | 词法分析模块：Lexer 类（词法分析器）、Token 类、接口回调机制 |
| `ir` | 中间代码模块：四元式生成器（QuadGenerator）、临时变量/标号管理器、常量折叠 |
| `parser` | 语法/语义分析模块：Compiler 核心类（递归下降解析）、语句/表达式解析逻辑 |
| `demo` | 语法分析演示模块：LL(1)、LR(0)、简单优先分析法的独立演示类 |

### 2.2 目录组织结构

```
src/
├── Main.java                                      // 程序入口，整合编译流程与演示入口
├── compiler/
│   ├── common/                                    // 公共组件
│   │   ├── DataType.java                          // 数据类型枚举（int/float/char/void/array/struct/func）
│   │   ├── SymbolKind.java                        // 符号种类枚举（变量/常量/函数/类型/字段）
│   │   └── TokenType.java                         // Token 类型枚举
│   ├── symbol/                                    // 符号表系统
│   │   ├── TypeEntry.java                         // 类型表（TYPEL）条目
│   │   ├── ArrayInfo.java                         // 数组表（AINFL）条目
│   │   ├── StructMember.java                      // 结构体成员描述
│   │   ├── StructInfo.java                        // 结构体表（RINFL）条目
│   │   ├── FuncInfo.java                          // 函数表（PFINFL）条目
│   │   ├── ConstEntry.java                        // 常量表（CONSL）条目
│   │   ├── ActRecordEntry.java                    // 活动记录条目
│   │   ├── SymbolEntry.java                       // 主符号表（SYNBL）条目
│   │   └── SymbolTables.java                      // 全局符号表容器（静态方法管理）
│   ├── lexer/                                     // 词法分析
│   │   ├── Token.java                             // Token 实体类
│   │   └── Lexer.java                             // 词法分析器核心类（含回调接口）
│   ├── ir/                                        // 中间代码
│   │   └── QuadGenerator.java                     // 四元式生成器
│   ├── parser/                                    // 语法分析
│   │   └── Compiler.java                          // 编译器核心（递归下降解析、符号表管理、语义分析）
│   └── demo/                                      // 演示模块
│       ├── LL1ExprGenerator.java                  // LL(1) 文法构造器
│       ├── LL1Demo.java                           // LL(1) 分析法演示
│       ├── LR0Demo.java                           // LR(0) 分析法演示
│       ├── SimplePrecedence.java                  // 简单优先分析器
│       └── SimplePrecedenceDemo.java              // 简单优先分析法演示
```

## 3. 核心类详解

### 3.1 `Lexer`（词法分析器）

#### 设计思路

基于有限状态自动机实现，核心状态包括：初始态、标识符态、数字态、运算符态、分隔符态、注释态等。通过逐字符读取源代码，根据字符类型和状态转移规则生成 Token，同时记录 Token 的行号用于错误定位。

#### 核心特性

1. **注释处理**：自动跳过单行注释（`//`）和多行注释（`/* */`），确保注释内容不干扰分析。
2. **关键字识别**：内置关键字表（int、float、char、void、struct、if、else、while、write、return），匹配时输出对应 TokenType。
3. **数字常量识别**：支持整数和浮点数（含科学计数法），自动进行范围检查和常量表添加。
4. **字符串与字符常量**：支持转义字符识别（`\n`、`\t`、`\\`、`\'`、`\"`），未闭合时报错。
5. **接口回调机制**：提供 `CompilerContext` 回调接口，用于将识别到的标识符、常量、Token 序列记录到上层，实现解耦。

#### 核心方法

```java
public Token next()               // 获取下一个 Token
private void skipSpacesAndComments() // 跳过空白符和注释
private Token identifier()        // 识别标识符或关键字
private Token number()            // 识别数字常量（整数/浮点数）
private Token charConst()         // 识别字符常量
private Token stringConst()       // 识别字符串常量
```

#### 接口回调机制

`Lexer` 通过 `CompilerContext` 接口与上层 Compiler 解耦：

```java
public interface CompilerContext {
    int addIdent(String name);              // 添加标识符并返回 ID
    int addConstValue(String val);          // 添加常量值并返回 ID
    void addTokenPair(char type, int index);// 记录 Token 序列
    void addConst(String val, TypeEntry type); // 添加常量条目
}
```

### 3.2 `Compiler`（编译器核心类）

#### 设计思路

`Compiler` 是项目的核心整合类，整合词法分析、语法分析、语义分析、中间代码生成。采用递归下降解析思想，为每个非终结符（如 expr、stmt、block）实现对应的解析方法，解析过程中同步完成符号表维护和四元式生成。

#### 核心职责

1. **递归下降解析**：
   - 表达式解析：`parseExpr()`、`parseLogicalOr()`、`parseLogicalAnd()`、`parseEquality()`、`parseRelational()`、`parseAdditive()`、`parseMultiplicative()`、`parseUnary()`、`parsePrimary()`，形成完整的运算符优先级处理链。
   - 语句解析：`parseStmt()`、`parseIf()`、`parseWhile()`、`parseWrite()`、`parseReturn()`、`parseBlock()`。

2. **符号表管理**：解析过程中调用 `SymbolTables` 的方法，完成标识符的插入、查询、作用域切换。

3. **四元式生成**：调用 `QuadGenerator` 生成四元式，处理语义逻辑（如类型检查、常量折叠）。

4. **活动记录管理**：为每个函数维护局部变量的栈帧偏移量，记录到 `actRecord` 列表。

### 3.3 `SymbolTables`（全局符号表容器）

#### 设计思路

静态类实现的全局符号表容器，整合所有符号表（SYNBL/TYPEL 等），提供统一的插入、查询、打印接口，维护各表之间的关联关系。基本类型采用单例缓存，避免重复创建。

#### 核心方法

| 方法 | 作用 |
|------|------|
| `addType(DataType dt, Object ext)` | 添加类型（基本类型返回缓存单例，复合类型加入类型表） |
| `addArray(dim, low, up, elem)` | 添加数组信息并计算总大小 |
| `addStruct(name)` | 添加结构体信息 |
| `addFunc(name, retType)` | 添加函数信息 |
| `addSymbol(name, type, kind, addr)` | 添加符号表条目 |
| `addConst(value, type)` | 添加常量（去重） |
| `findSymbol(name)` | 查找符号（全局匹配） |
| `dataTypeToString(dt)` | 数据类型转语义化字符串 |
| `symbolKindToString(sk)` | 符号种类转语义化字符串 |

### 3.4 `QuadGenerator`（四元式生成器）

#### 设计思路

负责四元式的生成、管理，以及临时变量/标号的自动分配，内置常量折叠优化逻辑，降低中间代码冗余。

#### 核心特性

1. **临时变量管理**：自动生成唯一临时变量名（如 `t0`、`t1`、`t2`），每次调用 `newTemp()` 返回一个新变量。
2. **标号管理**：自动生成跳转标号（如 `L0`、`L1`），用于分支/循环跳转。
3. **常量折叠**：在生成四元式前，对常量表达式（如 `1+2`、`3*4`）直接计算结果，减少运行时开销。`tryFoldConst()` 静态方法统一处理。
4. **四元式存储**：`emit()` 方法将四元式存入列表，供后续输出或进一步处理。

#### 常量折叠示例

```java
// 输入：op = "+", left = "2", right = "3"
// 输出：折叠结果为 "5"，生成 (:=, C5, _, t0) 而非 (+, C2, C3, t0)
```

### 3.5 演示类

#### `LL1Demo`

- **作用**：演示 LL(1) 分析法的核心流程，包括构造 FIRST 集、FOLLOW 集、预测分析表，以及基于预测分析表的语法分析过程。
- **文法**：算术表达式文法（E → T E'，T → F T'，F → id | num | (E)）。
- **输出示例**：
  ```
  FIRST(E') = { ε + - }
  FOLLOW(E') = { $ ) }
  NT\T	id	num	+	-	*	/	(	)	$
  E	E->TE'	E->TE'	...	E->TE' ...
  ```

#### `LR0Demo`

- **作用**：演示 LR(0) 分析法的核心流程，包括构造拓广文法、项目集规范族、LR(0) 分析表（ACTION/GOTO），以及移进-归约分析过程。
- **输出**：ACTION 表、GOTO 表，以及示例输入串 `id+id*id` 的完整分析过程。

#### `SimplePrecedenceDemo`

- **作用**：演示简单优先分析法的核心流程，包括计算 FIRSTOP 和 LASTOP 集合，构造优先关系矩阵，以及基于优先矩阵的语法分析过程。
- **输出**：FIRSTOP/LASTOP 集合、优先关系矩阵。

## 4. 符号表系统设计

### 4.1 各表含义

| 表名 | 全称 | 核心作用 |
|------|------|----------|
| SYNBL | 主符号表 | 存储所有标识符的基础信息：标识符名、符号类型（变量/常量/函数/类型/字段）、关联的扩展表指针 |
| TYPEL | 类型表 | 存储类型信息：类型值（int/float/char/void/array/struct/func）、扩展指针（指向 AINFL/RINFL） |
| AINFL | 数组表 | 存储数组信息：数组维数、各维度上下界、元素类型、总大小（字节） |
| RINFL | 结构体表 | 存储结构体信息：结构体名、成员列表（成员名+成员类型+偏移量）、总大小 |
| PFINFL | 函数表 | 存储函数信息：返回值类型、参数列表（类型+名称）、参数个数、入口地址 |
| CONSL | 常量表 | 存储常量信息：常量值、常量类型 |
| 活动记录 | Activation Record | 管理当前函数局部变量，记录变量名、类型、栈帧偏移地址 |

### 4.2 各表之间的关联方式

```
SYNBL（主符号表）作为核心关联枢纽：
├── 变量 → type 指向 TYPEL → TYPEL.tpoint 指向 AINFL（数组）或 RINFL（结构体）
├── 常量 → type 指向 TYPEL（基本类型）
├── 函数 → addr 指向 PFINFL（函数信息）
├── 类型（struct）→ type.tpoint 指向 RINFL
└── 字段 → addr 指向所属 RINFL
```

### 4.3 符号表输出格式示例

运行程序后，将输出如下格式的符号表系统：

```
【2. 主符号表 SYNBL（标识符核心信息）】
标识符名字           类型信息(TYPE)         种类信息(CAT)       关联子表(ADDR)
--------------------------------------------------
main             f(用户)             函数(f)          函数表(PFINFL) → main(0个参数)
a                i(系统)             变量(v)          值单元分配表 → 活动记录偏移
Point            d(用户)             类型(t)          结构表(RINFL) → Point(2个成员)
p                d(用户)             变量(v)          值单元分配表 → 活动记录偏移
arr              a(用户)             变量(v)          数组表(AINFL) → 1维，总大小40字节

【3. 类型表 TYPEL（维度1：类型信息）】
类型编号     类型值(TVAL)       类型扩展指针(TPOINT)指向
--------------------------------------------------
0       d(用户)          结构表(RINFL) → 结构体名:Point
1       a(用户)          数组表(AINFL) → 元素类型:i(系统)

【4. 复合类型信息子表】
  4.1 数组表 AINFL
数组编号       维数       总大小(字节)      元素类型
---------------------------------------------
0         1       40          i(系统)

  4.2 结构表 RINFL
    结构体0: Point (共2个成员，总大小8字节)
      成员名	类型	偏移地址
      ------------------------------
      x	i(系统)	0
      y	i(系统)	4
```

## 5. 四元式中间代码

### 5.1 四元式结构

四元式是编译原理中广泛使用的中间代码形式，其结构为 `(op, arg1, arg2, result)`，通过运算符和操作数将结果存入临时变量，适用于分解复杂表达式。

本项目中四元式作为线性中间表示（IR），每个四元式仅对应单一运算操作，相比直接生成目标代码更便于优化处理。

### 5.2 支持的四元式类型

| 分类 | 运算符 | 示例 |
|------|--------|------|
| 赋值 | `:=` | `(:=, t1, _, a)` |
| 算术运算 | `+` `-` `*` `/` | `(+, t0, t1, t2)` |
| 关系运算 | `<` `<=` `>` `>=` `==` `!=` | `(<, a, b, t0)` |
| 逻辑运算 | `&&` `||` `!` | `(&&, cond1, cond2, t0)` |
| 数组访问 | `=[]`（读）、`[]=`（写） | `(= [], arr, idx, t0)` |
| 结构体访问 | `.`、`->` | `(., p, x, t0)` |
| 函数调用 | `call`、`param` | `(param, arg, _, _)` |
| 控制流 | `ifFalse`、`goto`、`label` | `(ifFalse, cond, _, L1)` |
| 返回 | `return` | `(return, retVal, _, _)` |
| 输出 | `write` | `(write, "Hello", _, _)` |
| 函数定义 | `function`、`end` | `(function, main, _, _)` |

### 5.3 四元式生成示例

对于测试代码 `arr[i] = add(i, p.x);` 生成的四元式序列如下：

```java
0: (param, I8, _, _)      // 参数 i
1: (., I10, x, t2)        // 取 p.x → t2
2: (param, t2, _, _)      // 参数 p.x
3: (call, add, _, t3)     // 调用 add → t3
4: ([]=, I9, I8, t3)      // arr[i] = t3
```

对于 `while (i < 10) { ... }` 循环：

```java
0: (label, _, _, L0)      // 循环开始
1: (<, I8, 10, t1)        // 条件判断
2: (ifFalse, t1, _, L1)   // 条件不满足则跳出
3: ...                    // 循环体代码
4: (goto, _, _, L0)       // 跳回循环开始
5: (label, _, _, L1)      // 循环结束
```

## 6. 编译与运行指南

### 6.1 环境要求

- JDK 版本：JDK 21 或更高版本（推荐 JDK 21，兼容模块化与语法特性）
- 操作系统：Windows / Linux / macOS（无系统依赖）
- 依赖：无第三方依赖，纯 Java 原生实现

### 6.2 编译命令

1. 进入项目根目录（src 文件夹所在目录）。
2. 执行编译命令，将编译后的类文件输出到 `out` 目录：

```bash
javac -d out src/**/*.java src/Main.java
```

**说明**：
- `-d out`：指定编译后的类文件输出目录为 `out`。
- `src/**/*.java`：递归编译 src 下所有 Java 文件。
- `src/Main.java`：指定程序入口类。

### 6.3 运行命令

编译完成后，执行运行命令：

```bash
java -cp out Main
```

**说明**：`-cp out` 指定类路径为 `out` 目录（编译后的类文件所在目录）。

### 6.4 测试文件 `test.txt` 编写要求及位置

#### 位置要求
将 `test.txt` 放在项目根目录（与 `src`、`out` 同级目录），程序默认读取该路径下的 `test.txt` 作为源程序输入。

#### 编写要求
1. 语法遵循类 C 语言规范，支持项目中定义的所有数据类型、语句、表达式。
2. 关键字区分大小写（如 `int`、`if`、`while` 需小写）。
3. 标识符命名规则：字母/下划线开头，由字母、数字、下划线组成。
4. 语句结束必须加分号（`;`），代码块用大括号（`{}`）包裹。

## 7. 测试用例说明

### 7.1 完整测试代码片段

```c
struct Student {
    int score;
    char grade;
};

int calc_sum(int arr[], int len) {
    int sum = 0;
    int i = 0;
    while (i < len) {
        sum = sum + arr[i];
        i = i + 1;
    }
    return sum;
}

int main() {
    int scores[4] = {85, 92, 78, 90};
    struct Student s;
    s.score = calc_sum(scores, 4);
    if (s.score >= 340) {
        s.grade = 'A';
    } else {
        s.grade = 'B';
    }
    write("总分：");
    write(s.score);
    write("\n等级：");
    write(s.grade);
    return 0;
}
```

### 7.2 预期输出

```
总分：345
等级：A
```

### 7.3 编译器实际处理过程

1. **词法分析**：将源代码拆分为 Token 流，识别 `struct`、`Student`、`int`、`score`、`calc_sum`、`while`、`write` 等关键字和标识符。

2. **符号表构建**：
   - 解析 `struct Student`，在 RINFL 中插入结构体项，TYPEL 中插入 `Student` 类型，SYNBL 中插入 `Student` 标识符。
   - 解析 `calc_sum` 函数，在 PFINFL 中插入函数项，SYNBL 中插入 `calc_sum` 标识符。
   - 解析 `main` 函数内的 `scores` 数组，在 AINFL 中插入数组项，SYNBL 中插入 `scores` 标识符。
   - 解析 `s` 结构体变量，在 SYNBL 中插入 `s` 标识符并关联 RINFL。

3. **语法分析与语义检查**：递归下降解析器校验语法规则，检查变量作用域、类型一致性等。

4. **四元式生成**：
   - `calc_sum` 函数体生成循环相关的四元式。
   - `main` 函数生成数组索引访问、结构体成员赋值、函数调用、条件分支等四元式。

## 8. 常见问题与解决方案

### 8.1 `test.txt` 找不到

**错误信息**：
```
Cannot open file: test.txt
java.nio.file.NoSuchFileException: test.txt
```

**原因**：程序运行时工作目录不正确，`test.txt` 不在类路径所对应的目录中。

**解决方案**：
- **方案一**：将 `test.txt` 复制到 `out\production\bianyi\` 目录下。
- **方案二**：在 IntelliJ IDEA 中修改工作目录：`Run` → `Edit Configurations` → 将 `Working directory` 改为项目根目录。
- **方案三**：在 `Main.java` 中使用绝对路径或相对项目根目录的路径。

### 8.2 Windows 控制台中文乱码

**问题**：控制台输出中文字符显示为乱码。

**原因**：Windows 控制台默认编码为 GBK，而程序输出为 UTF-8。

**解决方案**：
- **方案一**：在 `main` 方法开头添加：
  ```java
  System.setOut(new PrintStream(System.out, true, "UTF-8"));
  ```
- **方案二**：运行前在命令行执行 `chcp 65001` 切换到 UTF-8 代码页。

### 8.3 Java 不支持逗号运算符

**问题**：原 C++ 代码中存在 `(advance(), true)` 的逗号运算符写法，Java 不识别。

**原因**：C/C++ 的逗号运算符会依次执行两侧表达式，并返回最后一个表达式的值。Java 不支持此语法。

**解决方案**：改用显式条件判断 + 提前 `break`。

```java
// 错误写法（C++ 逗号运算符，Java 不支持）
} while (check(COMMA) && (advance(), true));

// 正确写法
if (!check(COMMA)) break;
advance();
```

### 8.4 符号表字段未初始化警告

**问题**：IDE 提示 `field 'context' may not have been initialized`。

**原因**：定义了字段但未在构造函数中赋值，或存在冗余的未使用字段。

**解决方案**：删除未使用的冗余字段，或在构造函数中正确初始化所有字段。

```java
// 错误：字段定义但未初始化
private final CompilerContext context;

// 正确：在构造函数中初始化
public Lexer(String src, CompilerContext ctx) {
    this.source = src;
    this.context = ctx;   // 正确初始化
    advance();
}
```

### 8.5 `String.repeat()` 不支持

**问题**：Java 8 环境报错 `语言级别 '8' 不支持 压缩源文件`。

**原因**：`String.repeat()` 是 Java 11 引入的方法。

**解决方案**：将 JDK 升级到 11 或更高版本，或使用自定义方法替代：

```java
private static String repeat(String s, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) sb.append(s);
    return sb.toString();
}
```

## 9. 扩展与维护建议

### 9.1 如何添加新类型、新语句、新表达式

#### 添加新数据类型（如浮点型 float）

1. 在 `common/DataType.java` 中添加 `DT_FLOAT` 枚举值。
2. 在 `lexer/Lexer.java` 中扩展状态机，识别浮点数字面量。
3. 在 `symbol/SymbolTables.java` 的 `dataTypeToString()` 方法中添加映射。
4. 在 `parser/Compiler.java` 的表达式解析方法中，支持浮点型运算的类型检查与四元式生成。

#### 添加新语句（如 for 循环）

1. 在 `common/TokenType.java` 中添加 `KW_FOR` 枚举值。
2. 在 `lexer/Lexer.java` 中将 `for` 识别为关键字。
3. 在 `parser/Compiler.java` 中实现 `parseForStmt()` 方法，解析 `for` 语句语法。
4. 在四元式生成中创建对应的标签和跳转结构。

#### 添加新表达式（如取模运算 `%`）

1. 在 `common/TokenType.java` 中添加 `MOD` 枚举值。
2. 在 `lexer/Lexer.java` 中识别 `%` 符号。
3. 在 `parser/Compiler.java` 的 `parseMultiplicative()` 方法中扩展取模运算的优先级处理。
4. 在 `ir/QuadGenerator.java` 中添加取模运算四元式生成方法。

### 9.2 如何将四元式转换为目标代码

#### 转换为汇编代码（以 x86 汇编为例）

1. 构建四元式到汇编指令的映射表：
   - 赋值四元式 `(:=, src, _, dst)` → `mov eax, [src]; mov [dst], eax`
   - 加法四元式 `(+, A, B, T)` → `mov eax, [A]; add eax, [B]; mov [T], eax`
   - 跳转四元式 `(goto, _, _, L)` → `jmp L`

2. 实现 `AssemblerGenerator` 类，遍历四元式列表，逐行生成汇编指令。

3. 处理临时变量/标号的汇编映射：临时变量映射到寄存器/栈内存，标号映射到汇编标签。

4. 生成完整的汇编文件（`.s`），通过汇编器（如 nasm）编译为可执行文件。

#### 转换为 LLVM IR

1. 学习 LLVM IR 语法规范（基于 SSA 形式）。
2. 实现 `LLVMIRGenerator` 类，将四元式转换为 LLVM IR 指令：
   - 赋值四元式 → `store i32 %A, i32* %B`
   - 加法四元式 → `%T = add nsw i32 %A, %B`
3. 处理函数调用、数组访问等复杂逻辑的 LLVM IR 生成。
4. 输出 LLVM IR 文件（`.ll`），通过 `llc` 工具编译为目标机器码。

### 9.3 优化符号表打印格式

#### 现有问题
符号表打印为纯文本，格式较为简单，可通过以下方案进一步优化。

#### 优化方案

1. **导出为表格格式**：实现 `toMarkdownTable()` 方法，生成 Markdown 格式的表格，便于嵌入文档。
2. **导出为 JSON 格式**：实现 `toJSON()` 方法，将符号表序列化为 JSON 字符串，便于程序间数据交换。
3. **语法高亮**：在控制台输出中使用 ANSI 转义码为不同类型添加颜色区分。
4. **分层打印**：支持按作用域层级缩进打印符号，直观展示嵌套结构。

---

**项目地址**：https://github.com/zhangchuntao-cloud/bianyi

**许可证**：MIT License

**最后更新**：2026年5月


