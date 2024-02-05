import {ResInfo} from "../routes/setting/store.ts";
import {memo, useCallback, useRef} from "react";
import {Flex, Tabs, TabsProps} from "antd";
import {convertFileSrc} from "@tauri-apps/api/tauri";

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
        <video src={assetUrl} onPlay={onPlay} onPause={onPause} controls={true} width="320px"/>
        {audioTracks.map(a => {
            const url = convertFileSrc(a.path);
            return <audio key={a.path} src={url} controls={true}/>;
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
                    content = <video src={assetUrl} controls={true} width="320px"/>;
                }
                break;
            case "audio":
                assetUrl = convertFileSrc(path);
                content = <audio src={assetUrl} controls={true}/>;
                break;
            case "image":
                assetUrl = convertFileSrc(path);
                content = <img src={assetUrl} alt={path}/>;
                break;
            case "other":
                content = <p>{path}</p>
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
