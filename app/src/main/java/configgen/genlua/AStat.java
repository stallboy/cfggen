package configgen.genlua;

import configgen.util.Logger;

class AStat {
    private int emptyTableCount = 0;
    private int listTableCount = 0;
    private int mapTableCount = 0;
    private int interfaceTableCount = 0;
    private int structTableCount = 0;
    private int recordTableCount = 0;
    private int sharedTableReduceCount = 0;
    private int packBoolReduceCount = 0;

    void useEmptyTable() {
        emptyTableCount++;
    }

    void useListTable() {
        listTableCount++;
    }

    void useMapTable() {
        mapTableCount++;
    }

    void useInterfaceTable() {
        interfaceTableCount++;
    }

    void useStructTable() {
        structTableCount++;
    }

    void useRecordTable() {
        recordTableCount++;
    }

    void useSharedTable(int c) {
        sharedTableReduceCount += c;
    }

    void usePackBool(int c) {
        packBoolReduceCount += c;
    }


    void print() {
        Logger.verbose(
                "可共享空table个数:%d, 共享table节省:%d，压缩bool节省:%d，总共有list:%d，map:%d，interface:%d，struct:%d，record:%d",
                emptyTableCount,
                sharedTableReduceCount,
                packBoolReduceCount,
                listTableCount,
                mapTableCount,
                interfaceTableCount,
                structTableCount,
                recordTableCount);
    }
}
