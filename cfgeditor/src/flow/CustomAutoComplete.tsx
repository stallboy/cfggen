import {CSSProperties, memo} from 'react';
import {AutoComplete, Input} from 'antd';
import {EntityEditFieldOption} from "./entityModel.ts";

const suffixStyle: CSSProperties = {
    color: '#597ef7', fontSize: '0.85em',
    textOverflow: "clip", whiteSpace: 'nowrap', overflow: 'hidden', maxWidth: 80
}

export interface CustomAutoCompleteProps {
    id?: string;
    value?: string | number;
    onChange?: (value: string | number) => void;

    options: EntityEditFieldOption[];
    filters: any;
}

// https://ant-design.antgroup.com/components/form-cn#form-demo-customized-form-controls
export const CustomAutoComplete = memo(function CustomAutoComplete(
            {id, value, onChange, options, filters}: CustomAutoCompleteProps
        ) {
            const matchedOptionTitle = options.find(option => option.value == value)?.title ?? '';
            return <AutoComplete id={id} className='nodrag' {...filters}
                                 options={options}
                                 value={value}
                                 onSelect={onChange}
                                 onSearch={onChange}>
                <Input suffix={<span style={suffixStyle}>{matchedOptionTitle}</span>}/>
            </AutoComplete>
        }
    )
;

