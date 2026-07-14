import {CSSProperties, memo, useMemo} from 'react';
import {AutoComplete, Input} from 'antd';
import {EntityEditFieldOption} from "@/domain/entityModel";
import type {FilterOption} from "./edit/shared/types.ts";

const suffixStyle: CSSProperties = {
    color: '#597ef7', fontSize: '0.8em',
    textOverflow: "clip", whiteSpace: 'nowrap', overflow: 'hidden', maxWidth: 85
}

export interface CustomAutoCompleteProps {
    id?: string;
    value?: string | number;
    onChange?: (value: string | number) => void;

    options: EntityEditFieldOption[];
    filters: FilterOption;
}

// https://ant-design.antgroup.com/components/form-cn#form-demo-customized-form-controls
export const CustomAutoComplete = memo(function CustomAutoComplete(
            {id, value, onChange, options, filters}: CustomAutoCompleteProps
        ) {
            const input = useMemo(() => {
                const matchedOptionTitle = options.find(option => option.value == value)?.title ?? '';
                return <Input suffix={<span style={suffixStyle}>{matchedOptionTitle}</span>}/>
            }, [value, options]);

            // AutoComplete 的 onChange 在「输入」与「选中选项」时都会触发（受控值回调契约），
            // 已覆盖 onSelect / onSearch 的情形。原先三处都别名到 onChange，导致每次输入/选中
            // editOnUpdateValues → session.updateFormValues 被调 2 次（白跑 schema/转换器查找）。
            // 这里只留 onChange + {...filters}（filters 内的 showSearch:boolean 负责下拉过滤）。
            return <AutoComplete id={id} className='nodrag' {...filters}
                                 options={options}
                                 value={value}
                                 onChange={onChange}>
                {input}
            </AutoComplete>
        }
    )
;

