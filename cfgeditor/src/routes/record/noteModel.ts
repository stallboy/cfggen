export interface NoteModel {
    key: string;
    note: string;
}

export interface Notes {
    notes: NoteModel[];
}

export type NoteEditResultCode =
    'addOk'
    | 'updateOk'
    | 'deleteOk'
    | 'keyNotSet'
    | 'keyNotFoundOnDelete'
    | 'storeErr'

export interface NoteEditResult {
    resultCode: NoteEditResultCode;
    notes: Notes;
}


export function notesToMap(notes:Notes){
    const map = new Map<string, string>();
    for (const note of notes.notes) {
        map.set(note.key, note.note);
    }
    return map;
}

