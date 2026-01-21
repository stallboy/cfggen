package configgen.schema.cfg;

import configgen.schema.CfgSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CfgWriterTest {

    @Test
    void stringifyTable() {
        eqs("""
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                }
                """);
    }

    private void eqs(String source) {
        CfgSchema cfg = CfgReader.parse(source);
        String dst = cfg.stringify();
        assertEquals(source.trim(), dst.replaceAll("\r\n", "\n").trim());
    }

    @Test
    void stringifyStruct() {
        eqs("""
                struct AttrRandom {
                	Attr:int ->common.fightattrs; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """);
    }

    @Test
    void stringifyInterface() {
        eqs("""
                interface achievement.Achievementtype (enumRef='achievement.achievementtype') {
                	struct BiographyAchievement {
                		biographyid:int ->biography.biographyinfo;
                	}
                
                	struct TitleItemAchievement {
                		commonitemid:int ->item.commonitem;
                	}
                
                	struct MainMenuUnlockAchievement {
                		moduleopenid:int ->common.moduleopen;
                	}
                
                	struct VIPAchievement {
                		vipLevel:int ->vip.vip;
                	}
                
                	struct TitleIdAchievement {
                		titleid:int ->title.title;
                	}
                
                	struct EmptyAchievement {
                		_dummy:int;
                	}
                
                	struct RushRankAchievement {
                		rushrankid:int;
                	}
                
                	struct CommonAchievement {
                		_dummy:int;
                	}
                
                	struct CatteryAchievement {
                		typeid:int;
                	}
                
                }
                """);
    }

    @Test
    void stringifyTableWithLeadingComment() {
        // 测试带有声明前注释的 table 能够正确序列化
        eqs("""
                // 这是能力表
                // 用于定义游戏中的各种能力
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                }
                """);
    }

    @Test
    void stringifyStructWithLeadingComment() {
        // 测试带有声明前注释的 struct 能够正确序列化
        eqs("""
                // 属性随机范围配置
                // 用于生成随机属性值
                struct AttrRandom {
                	Attr:int ->common.fightattrs; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """);
    }

    @Test
    void stringifyInterfaceWithLeadingComment() {
        // 测试带有声明前注释的 interface 能够正确序列化
        eqs("""
                // 成就类型接口
                // 定义所有成就的基础结构
                interface achievement.AchievementType {
                	struct BiographyAchievement {
                		biographyid:int ->biography.biographyinfo;
                	}
                
                }
                """);
    }

    @Test
    void stringifyFieldWithLeadingComment() {
        // 测试带有声明前注释的字段能够正确序列化
        eqs("""
                struct AttrRandom {
                	// 属性ID
                	// 关联到fightprops表
                	Attr:int ->common.fightattrs;
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """);
    }

    @Test
    void stringifyMultiLineLeadingComment() {
        // 测试多行声明前注释能够正确序列化
        eqs("""
                // 第一行注释
                // 第二行注释
                // 第三行注释
                table ability[id] {
                	id:int;
                }
                """);
    }

    @Test
    void stringifyOnlyLeadingComment() {
        // 测试只有声明前注释（没有行尾注释）的情况
        eqs("""
                // 只有声明前注释
                table ability[id] {
                	id:int;
                }
                """);
    }

    @Test
    void roundTripWithMixedComments() {
        // 测试混合注释的往返一致性
        String source = """
                // 这是能力表
                // 用于定义游戏中的各种能力
                table ability[id] (enum='name') {
                	// 主键ID
                	// 唯一标识
                	id:int; // 属性类型
                	// 名称字段
                	name:str; // 程序用名字
                }
                """;

        CfgSchema cfg = CfgReader.parse(source);
        String result = cfg.stringify();

        // 验证声明前注释被正确输出
        assertTrue(result.contains("// 这是能力表"));
        assertTrue(result.contains("// 用于定义游戏中的各种能力"));
        assertTrue(result.contains("// 主键ID"));
        assertTrue(result.contains("// 唯一标识"));
        assertTrue(result.contains("// 名称字段"));

        // 验证行尾注释被正确保留
        assertTrue(result.contains("// 属性类型"));
        assertTrue(result.contains("// 程序用名字"));
    }
}
