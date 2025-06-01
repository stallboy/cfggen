package config

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

//getters
func (t *TaskCompleteconditionCollectItem) GetItemid() int32 {
    return t.itemid
}

func (t *TaskCompleteconditionCollectItem) GetCount() int32 {
    return t.count
}


