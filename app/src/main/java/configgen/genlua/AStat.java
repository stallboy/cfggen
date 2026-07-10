package configgen.genlua;

import configgen.util.Logger;

import java.util.concurrent.atomic.LongAdder;

class AStat {
    // 表生成并发：计数器被多个工作线程同时累加，用 LongAdder 降低竞争
    private final LongAdder emptyTableCount = new LongAdder();
    private final LongAdder listTableCount = new LongAdder();
    private final LongAdder mapTableCount = new LongAdder();
    private final LongAdder interfaceTableCount = new LongAdder();
    private final LongAdder structTableCount = new LongAdder();
    private final LongAdder recordTableCount = new LongAdder();
    private final LongAdder sharedTableReduceCount = new LongAdder();
    private final LongAdder packBoolReduceCount = new LongAdder();

    void useEmptyTable() {
        emptyTableCount.increment();
    }

    void useListTable() {
        listTableCount.increment();
    }

    void useMapTable() {
        mapTableCount.increment();
    }

    void useInterfaceTable() {
        interfaceTableCount.increment();
    }

    void useStructTable() {
        structTableCount.increment();
    }

    void useRecordTable() {
        recordTableCount.increment();
    }

    void useSharedTable(int c) {
        sharedTableReduceCount.add(c);
    }

    void usePackBool(int c) {
        packBoolReduceCount.add(c);
    }


    void print() {
        Logger.verbose(
                "可共享空table个数:%d, 共享table节省:%d，压缩bool节省:%d，总共有list:%d，map:%d，interface:%d，struct:%d，record:%d",
                emptyTableCount.sum(),
                sharedTableReduceCount.sum(),
                packBoolReduceCount.sum(),
                listTableCount.sum(),
                mapTableCount.sum(),
                interfaceTableCount.sum(),
                structTableCount.sum(),
                recordTableCount.sum());
    }
}
