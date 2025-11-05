# 意图历史

```
1.CfgEditorApp里的两个Drawer不再使用。Query直接删除，因为功能已经在finder，adder中存在了；Setting挪到HeaderBar.tsx的dragOptions里。
2.HeaderBar.tsx的options功能UI挪出去，放到Setting的Operations UI里
3.检查Operations UI里的设置固定页面和固定当前页面，与HeaderBar.tsx的dragOptions的pageConf.pages的关系。似乎没有及时更新。有bug。fix
```
