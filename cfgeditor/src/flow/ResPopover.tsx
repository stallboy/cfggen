import {memo, useCallback, useEffect, useRef, useState} from "react";
import {Button, Flex, Space, Tabs, TabsProps, Tooltip} from "antd";
import {convertFileSrc} from "@tauri-apps/api/core";
import {Command} from "@tauri-apps/plugin-shell";
import {readFile} from "@tauri-apps/plugin-fs";
import {useQuery} from "@tanstack/react-query";
import {ResInfo} from "@/domain/resInfo";

// 取路径末段作短名（兼容 / 与 \）；原 default 分支把完整 path 当按钮文字，长路径撑爆按钮。
function basename(p: string): string {
    return p.split(/[/\\]/).pop() || p;
}

function srt2vtt(srtBody: string) {
    return 'WEBVTT\n\n' + srtBody.split(/\n/g).map(line => line.replace(/((\d+:){0,2}\d+),(\d+)/g, '$1.$3')).join('\n');
}

// 读取字幕并转成 VTT 文本（纯字符串，可正常被 React Query 缓存/GC）。
// 不再在此 createObjectURL——blob URL 的生命周期下沉到 VideoAudioSyncer 的 effect 管理，
// 避免返回的 url 被缓存后永不 revoke（全仓曾 0 处 revokeObjectURL → blob store 泄漏到 unload）。
async function getSrt2Vtts(resInfo: ResInfo) {
    const {subtitlesTracks} = resInfo;
    if (!subtitlesTracks || subtitlesTracks.length == 0) {
        return;
    }

    const vtts: string[] = [];
    for (const st of subtitlesTracks) {
        const contentBytes = await readFile(st.path);
        const txt = new TextDecoder().decode(contentBytes);
        const vtt = srt2vtt(txt);
        vtts.push(vtt);
    }
    return vtts;
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
    const {data: vttTexts} = useQuery({
        // queryKey bump 到 'vtt2'：旧 'vtt' 缓存存的是 blob URL 字符串，新存 VTT 文本，避免错配
        queryKey: ['vtt2', resInfo.path],
        queryFn: () => getSrt2Vtts(resInfo)
    })

    // 由 VTT 文本创建 blob URL 并在卸载/文本变化时 revoke，修复"blob URL 永不释放"泄漏。
    // 在 effect（非 useMemo）里 createObjectURL：本应用开了 React.StrictMode，useMemo 会被
    // dev 双调用产生孤儿 blob；effect + cleanup 保证创建即登记、卸载即释放。
    const [vttUrls, setVttUrls] = useState<string[] | undefined>();
    useEffect(() => {
        if (!vttTexts) return;
        const urls = vttTexts.map(t => URL.createObjectURL(new Blob([t], {type: 'text/vtt'})));
        setVttUrls(urls);
        return () => urls.forEach(u => URL.revokeObjectURL(u));
    }, [vttTexts]);

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
                content = <Tooltip title={path}><Button onClick={() => goExplorer(path)}>{basename(path)}</Button></Tooltip>
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

