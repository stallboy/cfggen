import {ResInfo} from "../routes/setting/store.ts";
import {memo, useCallback, useRef} from "react";
import {Button, Flex, Space, Tabs, TabsProps} from "antd";
import {convertFileSrc} from "@tauri-apps/api/tauri";
import {Command} from "@tauri-apps/api/shell";


function goExplorer(file: string) {
    const command = new Command('explorer', ['/select,', file]);
    // console.log(file, command);
    command.execute();
}

export const VideoAudioSyncer = memo(function VideoAudioSyncer({resInfo}: { resInfo: ResInfo }) {
    const ref = useRef<HTMLElement>(null);
    const onPlay = useCallback(() => {
        if (ref.current) {
            const video = ref.current.querySelector<HTMLVideoElement>('video');
            if (video) {
                const audios = ref.current.querySelectorAll<HTMLAudioElement>('audio');
                for (let audio of audios) {
                    audio.play();
                    audio.currentTime = video.currentTime;
                }
            }
        }
    }, [ref]);

    const onPause = useCallback(() => {
        if (ref.current) {
            const video = ref.current.querySelector<HTMLVideoElement>('video');
            if (video) {
                const audios = ref.current.querySelectorAll<HTMLAudioElement>('audio');
                for (let audio of audios) {
                    audio.pause();
                    audio.currentTime = video.currentTime;
                }
            }
        }
    }, [ref]);


    const assetUrl = convertFileSrc(resInfo.path);
    const audioTracks = resInfo.audioTracks!;
    return <Flex ref={ref} vertical>
        <Space key={resInfo.path}>
            <video src={assetUrl} onPlay={onPlay} onPause={onPause}
                   controls={true} width="320px"/>
            <Button onClick={() => goExplorer(resInfo.path)}>{resInfo.name}</Button>
        </Space>
        {audioTracks.map(a => {
            const url = convertFileSrc(a.path);
            return <Space key={a.path} >
                <audio src={url} controls={true}/>
                <Button onClick={() => goExplorer(a.path)}>{a.name}</Button>
            </Space>;
        })}
    </Flex>;
});

export const ResPopover = memo(function ResPopover({resInfos}: { resInfos: ResInfo[] }) {
    const items: TabsProps['items'] = [];
    for (let r of resInfos) {
        const {type, name, path, audioTracks} = r;
        let content;
        let assetUrl;
        switch (type) {
            case "video":
                if (audioTracks) {
                    content = <VideoAudioSyncer resInfo={r}/>;
                } else {
                    assetUrl = convertFileSrc(path);
                    content = <Space>
                        <video src={assetUrl} controls={true} width="320px"/>
                        <Button onClick={() => goExplorer(path)}>{name}</Button>
                    </Space>;
                }
                break;
            case "audio":
                assetUrl = convertFileSrc(path);
                content = <Space>
                    <audio src={assetUrl} controls={true}/>
                    <Button onClick={() => goExplorer(path)}>{name}</Button>
                </Space>
                break;
            case "image":
                assetUrl = convertFileSrc(path);
                content = <Space>
                    <img src={assetUrl} alt={path} width="320px"/>
                    <Button onClick={() => goExplorer(path)}>{name}</Button>
                </Space>;
                break;
            case "other":
                content = <Button onClick={() => goExplorer(path)}>{path}</Button>
                break;
        }

        items.push({
            key: path,
            label: name,
            children: content
        });
    }

    return <Tabs tabPosition='left' items={items}/>
});

export function getResBrief(res: ResInfo[]) {
    let v = 0;
    let a = 0;
    let i = 0;
    let o = 0;
    for (let r of res) {
        switch (r.type) {
            case "video":
                v++;
                if (r.audioTracks) {
                    a += r.audioTracks.length;
                }
                break;
            case "audio":
                a++;
                break;
            case "image":
                i++;
                break;
            case "other":
                o++;
                break;

        }

    }
    let info = '';
    if (v > 0) {
        info += v + 'v';
    }
    if (a > 0) {
        info += a + 'a';
    }
    if (i > 0) {
        info += i + 'i';
    }
    if (o > 0) {
        info += o + 'o';
    }
    return info;
}
