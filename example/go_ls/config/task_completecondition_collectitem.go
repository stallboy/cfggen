package config

import "fmt"

type TaskCompleteconditionCollectItem struct {
    itemid int32
    count int32
}

func createTaskCompleteconditionCollectItem(stream *Stream) *TaskCompleteconditionCollectItem {
    v := &TaskCompleteconditionCollectItem{}
    v.itemid = stream.ReadInt32()
    v.count = stream.ReadInt32()
    return v
}

func (t *TaskCompleteconditionCollectItem) String() string {
    return fmt.Sprintf("TaskCompleteconditionCollectItem{itemid=%v, count=%v}", t.itemid, t.count)
}

//getters
func (t *TaskCompleteconditionCollectItem) Itemid() int32 {
    return t.itemid
}

func (t *TaskCompleteconditionCollectItem) Count() int32 {
    return t.count
}

