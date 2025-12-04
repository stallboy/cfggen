package configgen.gen;

public abstract class Tool {
    protected final Parameter parameter;

    /**
     * @param parameter 此接口有2个实现类，一个用于收集usage，一个用于实际参数解析
     *                  从而实现在各Tool的参数需求，只在构造函数里写一次就ok
     */
    public Tool(Parameter parameter) {
        this.parameter = parameter;
    }

    public abstract void call();
}
