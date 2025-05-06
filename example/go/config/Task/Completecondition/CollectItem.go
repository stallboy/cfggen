package completecondition

type CollectItem struct {
    itemid int
    count int
}

//is config.Task.Completecondition
func getType() string {
    return "CollectItem"
}

//getters
func (t *CollectItem) GetItemid() int {
    return t.itemid
}

func (t *CollectItem) GetCount() int {
    return t.count
}

