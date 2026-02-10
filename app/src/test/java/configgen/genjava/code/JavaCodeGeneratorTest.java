package configgen.genjava.code;

import configgen.ctx.Context;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.Resources;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeGeneratorTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void generate_simpleTable() throws IOException {
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
        Context ctx = new Context(tempDir);

        // 创建GenJavaCode实例
        File outputDir = tempDir.resolve("config").toFile();
        String parameterStr = String.format("javacode,dir:%s,pkg:%s,tag:%s",
                outputDir.getAbsolutePath(), "test.config", "");
        Parameter parameter = new ParameterParser(parameterStr);

        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(parameter);

        // 执行生成
        javaCodeGenerator.generate(ctx);

        // Then: 验证生成的Java文件
        File expectedFilesDir = new File(outputDir, "test/config");
        assertTrue(expectedFilesDir.exists(), "输出目录应该被创建");

        // 验证关键文件生成
        File[] generatedFiles = expectedFilesDir.listFiles();
        assertNotNull(generatedFiles, "应该生成Java文件");
        assertTrue(generatedFiles.length > 0, "应该生成至少一个Java文件");

        // 验证特定文件
        File userEntryFile = new File(expectedFilesDir, "User.java");
        File configMgrFile = new File(expectedFilesDir, "ConfigMgr.java");
        File configLoaderFile = new File(expectedFilesDir, "ConfigLoader.java");
        File configMgrLoaderFile = new File(expectedFilesDir, "ConfigMgrLoader.java");

        assertTrue(userEntryFile.exists(), "应该生成User.java文件");
        assertTrue(configMgrFile.exists(), "应该生成ConfigMgr.java文件");
        assertTrue(configLoaderFile.exists(), "应该生成ConfigLoader.java文件");
        assertTrue(configMgrLoaderFile.exists(), "应该生成ConfigMgrLoader.java文件");

        // 验证文件内容
        String userEntryContent = Files.readString(userEntryFile.toPath());
        assertTrue(userEntryContent.contains("class User"), "User.java应该包含类定义");
        assertTrue(userEntryContent.contains("package test.config"), "应该包含正确的包声明");
        assertTrue(userEntryContent.contains("id"), "应该包含id字段");
        assertTrue(userEntryContent.contains("name"), "应该包含name字段");
        assertTrue(userEntryContent.contains("age"), "应该包含age字段");
    }

    @Test
    void generate_enumTable() throws IOException {
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
        Context ctx = new Context(tempDir);

        // 创建GenJavaCode实例
        File outputDir = tempDir.resolve("config_enum").toFile();
        String parameterStr = String.format("javacode,dir:%s,pkg:%s,tag:%s",
                outputDir.getAbsolutePath(), "test.config.enum", "");
        Parameter parameter = new ParameterParser(parameterStr);

        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(parameter);

        // 执行生成
        javaCodeGenerator.generate(ctx);

        // Then: 验证生成的Java文件
        File expectedFilesDir = new File(outputDir, "test/config/enum");
        assertTrue(expectedFilesDir.exists(), "输出目录应该被创建");

        // 验证枚举相关文件
        File abilityEntryFile = new File(expectedFilesDir, "Ability.java");

        assertTrue(abilityEntryFile.exists(), "应该生成Ability.java文件");

        // 验证枚举文件内容
        String abilityEntryContent = Files.readString(abilityEntryFile.toPath());
        assertTrue(abilityEntryContent.contains("enum Ability"), "Ability.java应该是枚举类型");
        assertTrue(abilityEntryContent.contains("Fireball"), "应该包含Fireball枚举值");
        assertTrue(abilityEntryContent.contains("IceSpike"), "应该包含IceSpike枚举值");
    }

    @Test
    void generate_structAndInterface() throws IOException {
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
        Context ctx = new Context(tempDir);

        // 创建GenJavaCode实例
        File outputDir = tempDir.resolve("config_complex").toFile();
        String parameterStr = String.format("javacode,dir:%s,pkg:%s,tag:%s",
                outputDir.getAbsolutePath(), "test.config.complex", "");
        Parameter parameter = new ParameterParser(parameterStr);

        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(parameter);

        // 执行生成
        javaCodeGenerator.generate(ctx);

        // Then: 验证生成的Java文件
        File expectedFilesDir = new File(outputDir, "test/config/complex");
        assertTrue(expectedFilesDir.exists(), "输出目录应该被创建");

        // 验证struct和interface相关文件
        File positionFile = new File(expectedFilesDir, "Position.java");
        File shapeFile = new File(expectedFilesDir, "Shape.java");
        File circleFile = new File(expectedFilesDir, "shape/Circle.java");
        File rectangleFile = new File(expectedFilesDir, "shape/Rectangle.java");

        assertTrue(positionFile.exists(), "应该生成Position.java文件");
        assertTrue(shapeFile.exists(), "应该生成Shape.java文件");
        assertTrue(circleFile.exists(), "应该生成shape/Circle.java文件");
        assertTrue(rectangleFile.exists(), "应该生成shape/Rectangle.java文件");

        // 验证文件内容
        String positionContent = Files.readString(positionFile.toPath());
        assertTrue(positionContent.contains("class Position"), "Position.java应该包含类定义");
        assertTrue(positionContent.contains("x"), "应该包含x字段");
        assertTrue(positionContent.contains("y"), "应该包含y字段");

        String shapeContent = Files.readString(shapeFile.toPath());
        assertTrue(shapeContent.contains("interface Shape"), "Shape.java应该是接口定义");

        String circleContent = Files.readString(circleFile.toPath());
        assertTrue(circleContent.contains("class Circle"), "Circle.java应该包含类定义");
        assertTrue(circleContent.contains("radius"), "应该包含radius字段");

        String rectangleContent = Files.readString(rectangleFile.toPath());
        assertTrue(rectangleContent.contains("class Rectangle"), "Rectangle.java应该包含类定义");
        assertTrue(rectangleContent.contains("width"), "应该包含width字段");
        assertTrue(rectangleContent.contains("height"), "应该包含height字段");
    }

    @Test
    void generate_withBuilders() throws IOException {
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
        Context ctx = new Context(tempDir);

        // 创建GenJavaCode实例
        File outputDir = tempDir.resolve("config_builders").toFile();
        String parameterStr = String.format("javacode,dir:%s,pkg:%s,tag:%s,builders:%s",
                outputDir.getAbsolutePath(), "test.config.builders", "", buildersFile.getAbsolutePath());
        Parameter parameter = new ParameterParser(parameterStr);

        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(parameter);

        // 执行生成
        javaCodeGenerator.generate(ctx);

        // Then: 验证生成的Builder文件
        File expectedFilesDir = new File(outputDir, "test/config/builders");
        File userBuilderFile = new File(expectedFilesDir, "UserBuilder.java");

        assertTrue(userBuilderFile.exists(), "应该生成UserBuilder.java文件");

        // 验证Builder文件内容
        String builderContent = Files.readString(userBuilderFile.toPath());
        assertTrue(builderContent.contains("class UserBuilder"), "UserBuilder.java应该包含类定义");
        assertTrue(builderContent.contains("build"), "应该包含build方法");
    }
}