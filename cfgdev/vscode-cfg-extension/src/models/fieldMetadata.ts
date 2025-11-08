import { TextRange } from './configFile';
import { Literal } from './metadataDefinition';

export interface FieldMetadata {
    name: string;               // 元数据名
    value?: Literal;            // 可选值
    isNegative: boolean;        // 是否为负标识（-前缀）
    position: TextRange;        // 位置
}
