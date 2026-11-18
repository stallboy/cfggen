package configgen.genjava.code;

import configgen.ctx.Context;
import configgen.gen.CliException;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.Resources;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeGeneratorTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(ints = {-1, 2})
    void generatedSourcesCompileWithCollidingNamesAndBothSchemaLayouts(int schemaNumPerFile) throws Exception {
        var context = configgen.TestCtx.newContext(Files.createDirectories(tempDir.resolve("data")), """
                struct Plain { value:int; }
                struct WithMap { values:map<int,int> (pack); }
                struct Required { id:int ->target; mgr:int ->target; errors:int ->target; }
                struct Optional { id:int ->target (nullable); }
                struct Nested { value:Required; }
                interface Choice { struct Empty {} }
                table target[id] { id:int; }
                table indexed[id] { id:int (seq); }
                table state[id] (enum='name') { id:int; name:str; power:int; }
                table flag[id] (enum='name') { id:int; name:str; }
                table empty[id] (entry='name') { id:int; name:str; }
                """, java.util.Map.of("target", "id\nid\n1\n", "indexed", "id\nid\n0\n",
                "state", "id,name,power\nid,name,power\n1,LoadValueErrs,2\n", "flag", "id,name\nid,name\n1,On\n",
                "empty", "id,name\nid,name\n1,\n"));
        Path generated = tempDir.resolve("generated");
        new JavaCodeGenerator(new ParameterParser("java,dir:" + generated + ",pkg:config,schemaNumPerFile:" + schemaNumPerFile)).generate(context);
        try (var loader = compileGenerated(generated)) {
            var schemaClass = loader.loadClass("config.ConfigCodeSchema");
            var codeSchema = (configgen.genjava.SchemaInterface) schemaClass.getMethod("getCodeSchema").invoke(null);
            var packageSchema = (configgen.genjava.SchemaInterface) schemaClass.getMethod("getPackageSchema").invoke(null);
            var packageOnly = new java.util.HashSet<>(packageSchema.implementations.keySet());
            assertTrue(packageOnly.containsAll(codeSchema.implementations.keySet()));
            packageOnly.removeAll(codeSchema.implementations.keySet());
            assertEquals(java.util.Set.of("flag_Detail"), packageOnly);
        }
    }

    private URLClassLoader compileGenerated(Path generated) throws Exception {
        var classes = Files.createDirectory(tempDir.resolve("classes"));
        var args = new ArrayList<>(List.of("--release", "25", "-d", classes.toString(), "-classpath",
                Path.of(configgen.genjava.ConfigInput.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString()));
        try (var files = Files.walk(generated)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> args.add(path.toString()));
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)),
                "Generated sources must compile");
        return new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void generate_simpleTable() throws Exception {
        // Given: 有效的配置目录，包含schema和数据文件
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 创建Context并生成配置值
        File outputDir = generateJavaCode("config", "test.config", "");

        try (var loader = compileGenerated(outputDir.toPath())) {
            var user = loader.loadClass("test.config.User");
            assertEquals(int.class, user.getMethod("getId").getReturnType());
            assertEquals(String.class, user.getMethod("getName").getReturnType());
            assertEquals(int.class, user.getMethod("getAge").getReturnType());
            var manager = loader.loadClass("test.config.ConfigMgr");
            assertEquals(user, manager.getMethod("getUser", int.class).getReturnType());
            loader.loadClass("test.config.ConfigMgrLoader");
        }
    }

    @Test
    void generate_enumTable() throws Exception {
        // Given: 有效的配置目录，包含枚举表schema和数据文件
        String cfgStr = """
                table ability[id] (enum='name') {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                技能ID,技能名称
                id,name
                1,Fireball
                2,IceSpike
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("ability.csv", tempDir, csvData);

        // When: 创建Context并生成配置值
        File outputDir = generateJavaCode("config_enum", "test.config.abilities", "");

        try (var loader = compileGenerated(outputDir.toPath())) {
            Class<?> ability = loader.loadClass("test.config.abilities.Ability");
            assertTrue(ability.isEnum());
            assertEquals(List.of("FIREBALL", "ICESPIKE"),
                    Arrays.stream(ability.getEnumConstants()).map(value -> ((Enum<?>) value).name()).toList());
        }
    }

    @Test
    void generate_enumTable_beautifulName() throws Exception {
        // Given: 枚举表，枚举值用 camelCase 命名
        String cfgStr = """
                table ability[id] (enum='name') {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                技能ID,技能名称
                id,name
                1,ResetDuration
                2,MaxHp
                3,Fireball
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("ability.csv", tempDir, csvData);

        // When: 开启 beautifulName 生成代码
        File outputDir = generateJavaCode("config_snake", "test.config.snake", ",beautifulName");

        try (var loader = compileGenerated(outputDir.toPath())) {
            Class<?> ability = loader.loadClass("test.config.snake.Ability");
            Object[] constants = ability.getEnumConstants();
            assertEquals(List.of("RESET_DURATION", "MAX_HP", "FIREBALL"),
                    Arrays.stream(constants).map(value -> ((Enum<?>) value).name()).toList());
            var getName = ability.getMethod("getName");
            assertEquals(List.of("ResetDuration", "MaxHp", "Fireball"),
                    List.of(getName.invoke(constants[0]), getName.invoke(constants[1]), getName.invoke(constants[2])));
        }
    }

    @Test
    void generate_beautifulName_snakeTableName() throws IOException {
        // Given: snake_case 表名，验证 beautifulName 对类名及 ConfigMgr 派生标识符的美化
        String cfgStr = """
                table user_login[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user_login.csv", tempDir, csvData);

        // When: 开启 beautifulName 生成代码
        File outputDir = generateJavaCode("config_beautiful", "test.config.beautiful", ",beautifulName");

        // Then: 类名 user_login -> UserLogin，ConfigMgr 的 getter/all 函数名同步美化
        File expectedFilesDir = new File(outputDir, "test/config/beautiful");
        File userLoginFile = new File(expectedFilesDir, "UserLogin.java");
        assertTrue(userLoginFile.exists(), "snake_case 表名应美化出 UserLogin.java");

        String classContent = Files.readString(userLoginFile.toPath());
        assertTrue(classContent.contains("class UserLogin"), "类名应为 UserLogin");

        File configMgrFile = new File(expectedFilesDir, "ConfigMgr.java");
        assertTrue(configMgrFile.exists(), "应该生成ConfigMgr.java文件");
        String mgrContent = Files.readString(configMgrFile.toPath());
        assertTrue(mgrContent.contains("getUserLogin"), "主键 getter 应美化为 getUserLogin");
        assertTrue(mgrContent.contains("allUserLogin"), "all 函数应美化为 allUserLogin");
        assertFalse(mgrContent.contains("getUser_login"), "不应残留未美化的 getUser_login");
        assertFalse(mgrContent.contains("allUser_login"), "不应残留未美化的 allUser_login");
    }

    @Test
    void generate_beautifulName_keepsDetailPostfix() throws IOException {
        // Given: snake_case 表名的 enum 表，带额外字段 -> 生成 enum 类 + _Detail 数据类
        String cfgStr = """
                table user_login[id] (enum='name') {
                    id:int;
                    name:str;
                    value:int;
                }
                """;

        String csvData = """
                用户ID,名称,值
                id,name,value
                1,Foo,10
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user_login.csv", tempDir, csvData);

        // When: 开启 beautifulName 生成代码
        File outputDir = generateJavaCode("config_detail", "test.config.detail", ",beautifulName");

        // Then: 基础名美化（user_login -> UserLogin），但生成器后缀 _Detail 必须原样保留
        File expectedFilesDir = new File(outputDir, "test/config/detail");
        File enumFile = new File(expectedFilesDir, "UserLogin.java");
        File detailFile = new File(expectedFilesDir, "UserLogin_Detail.java");
        assertTrue(enumFile.exists(), "enum 类应美化出 UserLogin.java");
        assertTrue(detailFile.exists(), "_Detail 后缀必须保留，应为 UserLogin_Detail.java");
        assertFalse(new File(expectedFilesDir, "UserLoginDetail.java").exists(),
                "不应把 _Detail 吸收成 UserLoginDetail");

        String detailContent = Files.readString(detailFile.toPath());
        assertTrue(detailContent.contains("class UserLogin_Detail"), "详情类名应为 UserLogin_Detail");
    }

    @Test
    void generate_structAndInterface() throws Exception {
        // Given: 包含struct和interface的schema
        String cfgStr = """
                struct Position {
                    x:int;
                    y:int;
                }
                
                interface Shape {
                    struct Circle {
                        radius:float;
                        center:Position;
                    }
                
                    struct Rectangle {
                        width:float;
                        height:float;
                        pos:Position;
                    }
                }
                
                table shapes[id] {
                    id:int;
                    shape:Shape;
                }
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // When: 创建Context并生成配置值
        File outputDir = generateJavaCode("config_complex", "test.config.complex", "");

        try (var loader = compileGenerated(outputDir.toPath())) {
            var position = loader.loadClass("test.config.complex.Position");
            assertEquals(int.class, position.getMethod("getX").getReturnType());
            assertEquals(int.class, position.getMethod("getY").getReturnType());
            var shape = loader.loadClass("test.config.complex.shape.Shape");
            var circle = loader.loadClass("test.config.complex.shape.Circle");
            var rectangle = loader.loadClass("test.config.complex.shape.Rectangle");
            assertTrue(shape.isInterface());
            assertTrue(shape.isAssignableFrom(circle));
            assertTrue(shape.isAssignableFrom(rectangle));
            assertEquals(float.class, circle.getMethod("getRadius").getReturnType());
            assertEquals(position, circle.getMethod("getCenter").getReturnType());
            assertEquals(float.class, rectangle.getMethod("getWidth").getReturnType());
            assertEquals(float.class, rectangle.getMethod("getHeight").getReturnType());
            assertEquals(position, rectangle.getMethod("getPos").getReturnType());
        }
    }

    @Test
    void generate_withBuilders() throws Exception {
        // Given: 有效的配置目录，包含schema
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // 添加一个空的CSV文件来避免schema对齐失败
        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                """;
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // 创建builders文件
        File buildersFile = tempDir.resolve("builders.txt").toFile();
        Files.write(buildersFile.toPath(), "user".getBytes());

        // When: 创建Context并生成配置值
        File outputDir = generateJavaCode("config_builders", "test.config.builders",
                ",builders:" + buildersFile.getAbsolutePath());

        try (var loader = compileGenerated(outputDir.toPath())) {
            var builderClass = loader.loadClass("test.config.builders.UserBuilder");
            Object builder = builderClass.getConstructor().newInstance();
            builderClass.getField("id").setInt(builder, 7);
            builderClass.getField("name").set(builder, "Alice");
            builderClass.getField("age").setInt(builder, 25);

            Object user = builderClass.getMethod("build").invoke(builder);
            assertEquals(7, user.getClass().getMethod("getId").invoke(user));
            assertEquals("Alice", user.getClass().getMethod("getName").invoke(user));
            assertEquals(25, user.getClass().getMethod("getAge").invoke(user));
        }
    }

    @Test
    void generate_copiesInspectorSupportFiles() throws IOException {
        // Given: 简单表 schema + 数据
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;
        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 带 configgenDir 生成
        String configgenDir = tempDir.resolve("cgsrc").toString();
        generateJavaCode("config_cg", "test.config.cg", ",configgenDir:" + configgenDir);

        // Then: CodeDataInspector/CodeDataPrinter/JsonValue/Repl 应被拷到 configgenDir
        File supportDir = new File(configgenDir, "configgen/genjava");
        assertTrue(new File(supportDir, "CodeDataInspector.java").exists(), "应拷出 CodeDataInspector.java");
        assertTrue(new File(supportDir, "CodeDataPrinter.java").exists(), "应拷出 CodeDataPrinter.java");
        assertTrue(new File(supportDir, "JsonValue.java").exists(), "应拷出 JsonValue.java");
        assertTrue(new File(supportDir, "Repl.java").exists(), "应拷出 Repl.java");
    }

    @Test
    void constructor_invalidPrefix_throws() {
        // 非法前缀应 fail-fast，而不是静默产出编不过的代码
        assertThrows(CliException.class, () -> new JavaCodeGenerator(new ParameterParser("javacode,prefix:1a")));
        assertThrows(CliException.class, () -> new JavaCodeGenerator(new ParameterParser("javacode,prefix:my-c")));
        // 合法值：空（默认）、字母、下划线、$（java 标识符允许）
        assertDoesNotThrow(() -> new JavaCodeGenerator(new ParameterParser("javacode,prefix:")));
        assertDoesNotThrow(() -> new JavaCodeGenerator(new ParameterParser("javacode,prefix:_x")));
        assertDoesNotThrow(() -> new JavaCodeGenerator(new ParameterParser("javacode,prefix:$x")));
    }

    @Test
    void generate_withPrefix() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;
        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        File outputDir = generateJavaCode("config_prefix", "test.config.prefix", ",prefix:My");

        File expectedFilesDir = new File(outputDir, "test/config/prefix");
        File userFile = new File(expectedFilesDir, "MyUser.java");
        assertTrue(userFile.exists(), "应生成带前缀的MyUser.java");
        String content = Files.readString(userFile.toPath());
        assertTrue(content.contains("class MyUser"), "类名应带前缀My");

        // 固定名类不带前缀，但其引用指向带前缀的表类
        String mgrContent = Files.readString(new File(expectedFilesDir, "ConfigMgr.java").toPath());
        assertTrue(mgrContent.contains("test.config.prefix.MyUser"), "ConfigMgr 应引用带前缀的类全名");
        assertFalse(new File(expectedFilesDir, "User.java").exists(), "不应再生成无前缀的User.java");
    }

    /**
     * 公共的代码生成逻辑：创建 Context、构建参数、执行生成，返回输出目录。
     *
     * @param outputDirName 输出子目录名（相对于 tempDir）
     * @param pkg           Java 包名
     * @param extraParams   附加参数片段（如 ",builders:xxx" 或 ",tag:"），无则传空串
     * @return 输出目录
     */
    private File generateJavaCode(String outputDirName, String pkg, String extraParams) throws IOException {
        Context ctx = new Context(tempDir);
        File outputDir = tempDir.resolve(outputDirName).toFile();
        String parameterStr = String.format("javacode,dir:%s,pkg:%s%s",
                outputDir.getAbsolutePath(), pkg, extraParams);
        Parameter parameter = new ParameterParser(parameterStr);
        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(parameter);
        javaCodeGenerator.generate(ctx);
        return outputDir;
    }
}
