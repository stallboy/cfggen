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
}
