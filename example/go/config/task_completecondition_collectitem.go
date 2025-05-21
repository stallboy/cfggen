package config

type TaskCompleteconditionCollectItem struct {
    itemid int
    count int
}

//getters
func (t *TaskCompleteconditionCollectItem) GetItemid() int {
    return t.itemid
}

func (t *TaskCompleteconditionCollectItem) GetCount() int {
    return t.count
}

