export type ResType = 'video' | 'audio' | 'image' | 'subtitles' | 'other';

export interface ResAudioTrack {
    name: string;
    path: string;
}

export interface ResSubtitlesTrack {
    name: string;
    path: string;
    lang: string;
}

export interface ResInfo {
    type: ResType;
    name: string;
    path: string;
    lang?: string;
    audioTracks?: ResAudioTrack[];
    subtitlesTracks?: ResSubtitlesTrack[];
}