```
之后都用中文回答。我的意图是：增加app目录下java项目的测试。提高测试覆盖率。项目的文档可参考docs\docs\cfggen\目录下
的md文件。测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
- Mock External Dependencies: Isolate units by mocking databases, APIs, file systems, and other external services
- Fast Execution: Keep unit tests fast (milliseconds) so developers run them frequently during development
```


```
之后都用中文回答。我的意图是：增加app目录下java项目的configgen.gen的测试。测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
```

/feature-dev
```
之后都用中文回答。我的意图是：增加app目录下java项目的configgen.ctx的测试。测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
```

```
之后都用中文回答。我的意图是：增加app目录下configgen.ctx包下类的测试。测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
```


```
用中文，完善configgen.value package下的单元测试，挨个文件去检查是否需要补充测试，在feature-dev过程中对关键phase的结果保存到thoughts目录下方便我查看。
测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
```



minimax m2

```
用中文，完善app目录下 configgen.genjava 模块下的单元测试，主要是GenJavaData和GenJavaCode两个文件。
- GenJavaData产出的文件可以由BinaryToText来读取，可以用于验证生成数据的正确性。
- GenJavaCode主要验证生成了特定的java文件。文件名对应上就行。

测试要遵循以下准则：
- Test Behavior, Not Implementation: Focus tests on what the code does, not how it does it, to reduce brittleness
- Clear Test Names: Use descriptive names that explain what's being tested and the expected outcome
- 测试函数要加中文注释，函数体内有given/when/then中文注释
```
