import {useLocationData} from "@/store/store";
import {Alert, Flex} from "antd";
import {Link} from "react-router";

export function PathNotFound() {
    const {pathname} = useLocationData();

    return <Flex vertical style={{height: '100%'}} justify={'center'} align={'center'} gap={'large'}>
        <Alert title={`${pathname} Not Found`}/>
        <Link to="/">
            Return to home page
        </Link>
    </Flex>
}