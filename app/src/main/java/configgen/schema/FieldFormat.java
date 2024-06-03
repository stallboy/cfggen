package configgen.schema;

public sealed interface FieldFormat {


    enum AutoOrPack implements FieldFormat {
        /**
         * auto: 适用type: primitive, struct, interface。
         * <p>占格子：primitive都是1，struct，interface则自动计算
         */
        AUTO,
        /**
         * pack: 适用type: struct, interface, container。
         * <p>推荐使用，使用,()来写任何结构
         */
        PACK
    }

    /**
     * sep: 适用type: struct, list
     * <li>struct里field只有都是primitive，才能设置sep
     *      <ol>不支持type为struct的field上设置sep（为了简单一致性）</ol>
     *      <ol>不支持interface里的struct设置sep</ol>
     * </li>
     *
     * <li>list,primitive，可以设置sep</li>
     * <li>list,struct，可以设置sep，struct分两种情况
     *      <ol> struct本身设置sep</ol>
     *      <ol> struct里有一个field类型是list，设置了sep（这里可能递归，请检测）</ol>
     * </li>
     *
     * @param sep，分隔符，可以是, : = $等等都可以
     */
    record Sep(char sep) implements FieldFormat {
    }

    /**
     * fix: 适用type: container。
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
     * <p>fixedCount参数负责横向扩展格子，而纵向随意扩展
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
