import {useLocationData} from "../store/store.ts";
import {Alert, Flex} from "antd";
import {Link} from "react-router-dom";

export function PathNotFound() {
    const {pathname} = useLocationData();

    return <Flex vertical style={{height: '100%'}} justify={'center'} align={'center'} gap={'large'}>
        <Alert title={`${pathname} Not Found`}/>
        <Link to="/">
            Return to home page
        </Link>
    </Flex>
}