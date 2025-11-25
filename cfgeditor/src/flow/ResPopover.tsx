import {memo, useCallback, useRef} from "react";
import {Button, Flex, Space, Tabs, TabsProps} from "antd";
import {convertFileSrc} from "@tauri-apps/api/core";
import {Command} from "@tauri-apps/plugin-shell";
import {readFile} from "@tauri-apps/plugin-fs";
import {useQuery} from "@tanstack/react-query";
import {ResInfo} from "../res/resInfo.ts";

function srt2vtt(srtBody: string) {
    return 'WEBVTT\n\n' + srtBody.split(/\n/g).map(line => line.replace(/((\d+:){0,2}\d+),(\d+)/g, '$1.$3')).join('\n');
}

async function getSrt2VttUrls(resInfo: ResInfo) {
    const {subtitlesTracks} = resInfo;
    if (!subtitlesTracks || subtitlesTracks.length == 0) {
        return;
    }

    const urls = [];
    for (const st of subtitlesTracks) {
        const contentBytes = await readFile(st.path);
        const txt = new TextDecoder().decode(contentBytes);
        const vtt = srt2vtt(txt);
        // console.log(st.name, vtt);
        const blobCaption = new Blob([vtt])
        const url = URL.createObjectURL(blobCaption)
        urls.push(url);
    }
    return urls;
}

async function goExplorer(file: string) {
    file = file.replace(/\//g, '\\')
    const command = Command.create('explorer', ['/select,', file]);
    // message.info(file, 4);
    // console.log(file, command);
    await command.execute();
}

export const VideoAudioSyncer = memo(function VideoAudioSyncer({resInfo}: { resInfo: ResInfo }) {
    const ref = useRef<HTMLElement>(null);
    const {data: vttUrls} = useQuery({
        queryKey: ['vtt', resInfo.path],
        queryFn: () => getSrt2VttUrls(resInfo)
    })

    const onPlay = useCallback(() => {
        if (ref.current) {
            const video = ref.current.querySelector<HTMLVideoElement>('video');
            const allV = document.querySelectorAll("video");
            allV.forEach(function (v) {
                if (v != video) {
                    v.pause();
                }
            });
            const allA = document.querySelectorAll("audio");
            allA.forEach(function (a) {
                a.pause();
            });

            if (video) {
                const audios = ref.current.querySelectorAll<HTMLAudioElement>('audio');
                for (const audio of audios) {
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
                for (const audio of audios) {
                    audio.pause();
                    audio.currentTime = video.currentTime;
                }
            }
        }
    }, [ref]);


    const {path, audioTracks, subtitlesTracks} = resInfo;

    const tracks = [];
    if (subtitlesTracks && vttUrls) {
        let i = 0;
        for (const st of subtitlesTracks) {
            if (i < vttUrls.length) {
                const url = vttUrls[i];
                tracks.push(<track key={st.path} kind='subtitles' src={url} label={st.name} srcLang={st.lang}
                                   default={i == 0}/>);
            }
            i++;
        }
    }

    const assetUrl = convertFileSrc(path);
    return <Flex ref={ref} vertical>
        <Space key={resInfo.path}>
            <video src={assetUrl} onPlay={onPlay} onPause={onPause}
                   controls={true} width="320px">
                {tracks}
            </video>

            <Flex vertical>
                <Button key={resInfo.path} onClick={() => goExplorer(resInfo.path)}>{resInfo.name}</Button>
                {subtitlesTracks && subtitlesTracks.map(st =>
                    <Button key={st.path} onClick={() => goExplorer(st.path)}>{st.name}</Button>)}
            </Flex>
        </Space>
        {audioTracks && audioTracks.map(({name, path}) => getAudioEle(name, path))}
    </Flex>;
});


function getAudioEle(name: string, path: string) {
    const assetUrl = convertFileSrc(path);
    return <Space key={path}>
        <audio src={assetUrl} controls/>
        <Button onClick={() => goExplorer(path)}>{name}</Button>
    </Space>;
}

function getImageEle(name: string, path: string) {
    const assetUrl = convertFileSrc(path);
    return <Space key={path}>
        <img src={assetUrl} alt={path} width="320px"/>
        <Button onClick={() => goExplorer(path)}>{name}</Button>
    </Space>;
}

export const ResPopover = memo(function ({resInfos}: { resInfos: ResInfo[] }) {
    const items: TabsProps['items'] = [];
    for (const r of resInfos) {
        const {type, name, path} = r;
        let content;
        switch (type) {
            case "video":
                content = <VideoAudioSyncer resInfo={r}/>;
                break;
            case "audio":
                content = getAudioEle(name, path);
                break;
            case "image":
                content = getImageEle(name, path);
                break;
            default:
                content = <Button onClick={() => goExplorer(path)}>{path}</Button>
                break;
        }

        items.push({
            key: path,
            label: name,
            children: content
        });
    }
    if (items.length == 1) {
        return items[0].children;
    }

    return <Tabs tabPlacement={items.length < 4 ? 'top' : 'start'} items={items}/>
});

