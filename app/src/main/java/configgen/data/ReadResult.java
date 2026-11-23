package configgen.data;

import java.util.List;

public record ReadResult(List<OneSheet> sheets,
                         CfgDataStat stat) {

    public record OneSheet(String tableName,
                           CfgData.DRawSheet sheet) {
    }
}
