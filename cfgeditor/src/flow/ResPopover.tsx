import {ResInfo} from "../routes/setting/store.ts";
import {memo, useCallback, useRef} from "react";
import {Button, Flex, Space, Tabs, TabsProps} from "antd";
import {convertFileSrc} from "@tauri-apps/api/tauri";
import {Command} from "@tauri-apps/api/shell";
import {readTextFile} from "@tauri-apps/api/fs";
import {useQuery} from "@tanstack/react-query";

function srt2vtt(srtBody: string) {
    return 'WEBVTT\n\n' + srtBody.split(/\n/g).map(line => line.replace(/((\d+:){0,2}\d+),(\d+)/g, '$1.$3')).join('\n');
}

async function getSrt2VttUrls(resInfo: ResInfo) {
    const {subtitlesTracks} = resInfo;
    if (!subtitlesTracks || subtitlesTracks.length == 0) {
        return;
    }

    const urls = [];
    for (let st of subtitlesTracks) {
        const contents = await readTextFile(st.path);
        const vtt = srt2vtt(contents);
        // console.log(st.name, vtt);
        const blobCaption = new Blob([vtt])
        const url = URL.createObjectURL(blobCaption)
        urls.push(url);
    }
    return urls;
}

async function goExplorer(file: string) {
    file = file.replace(/\//g, '\\')
    const command = new Command('explorer', ['/select,', file]);
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
            let allA = document.querySelectorAll("audio");
            allA.forEach(function (a) {
                a.pause();
            });

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


    const {path, audioTracks, subtitlesTracks} = resInfo;

    let tracks = [];
    if (subtitlesTracks && vttUrls) {
        let i = 0;
        for (let st of subtitlesTracks) {
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

export const ResPopover = memo(function ResPopover({resInfos}: { resInfos: ResInfo[] }) {
    const items: TabsProps['items'] = [];
    for (let r of resInfos) {
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

    return <Tabs tabPosition={items.length < 4 ? 'top' : 'left'} items={items}/>
});

export function getResBrief(res: ResInfo[]) {
    let v = 0;
    let a = 0;
    let i = 0;
    let o = 0;
    for (let {type, audioTracks, subtitlesTracks} of res) {
        switch (type) {
            case "video":
                v++;
                break;
            case "audio":
                a++;
                break;
            case "image":
                i++;
                break;
            default:
                o++;
                break;
        }
        if (audioTracks) {
            a += audioTracks.length;
        }
        if (subtitlesTracks) {
            o += subtitlesTracks.length;
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
