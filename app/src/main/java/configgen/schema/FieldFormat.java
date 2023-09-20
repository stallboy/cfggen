package configgen.schema;

public sealed interface FieldFormat {

    /**
     * auto: 适用type: primitive, struct, interface。
     * 占格子：primitive都是1，bean则自动计算
     * <p>
     * pack: 适用type: struct, interface, container。
     * 推荐使用，使用,(),[],{}来写任何结构
     */
    enum AutoOrPack implements FieldFormat {
        AUTO,
        PACK
    }

    /**
     * sep: 适用type: struct, container，
     * 如果field是list,struct结构，list这配置的sep和要struct配置的sep有区分才行（不支持在此field上配置struct的sep，为了方便理解，不要这种灵活性。）。
     *
     * @param sep，分隔符，可以是, : = $等等都可以
     */
    record Sep(char sep) implements FieldFormat {
    }

    /**
     * fixed: 适用type: container。
     * 横向扩展格子
     *
     * @param count 个数，占格子数 = 容器内元素占的格子 * count
     */
    record Fix(int count) implements FieldFormat {
        public Fix {
            if (count < 1) {
                throw new IllegalArgumentException("fixed count must >= 1, count=" + count);
            }
        }
    }


    /**
     * block: 适用type: container。
     * fixedCount参数负责横向扩展格子，而纵向随意扩展
     *
     * @param fix 决定占多少列
     */
    record Block(int fix) implements FieldFormat {
        public Block {
            if (fix < 1) {
                throw new IllegalArgumentException("block fixedCount must >= 1, fix=" + fix);
            }
        }
    }
}
