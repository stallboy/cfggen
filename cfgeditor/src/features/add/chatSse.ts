/** SSE 流式帧累积（OpenAI 兼容 chat 流）：从 useXChat 的 onSuccess 收到的 chunks 数组里
 *  提取完整回复。每帧形如 {data: '<json 字符串 或 [DONE]>'}，JSON 解析后含
 *  choices[0].delta.content（增量片段）与 choices[0].finish_reason（完成标记）。
 *
 *  - 单帧异常一律 continue、不中断整条流：chunk/data 形态脏、JSON 非法、解析得 null、
 *    choices 缺失或为空、choices[0] 为 null（onSuccess 收到的流可能夹带 keepalive/脏帧）；
 *  - 累积 choices[0].delta.content；
 *  - 遇 finish_reason 视为完成，返回 trim 后内容（可能为空串）；
 *  - [DONE] 哨兵帧也视为流结束：某些 OpenAI 兼容网关只发内容帧 + [DONE]、不发 finish_reason，
 *    不认 [DONE] 会把整段回复误判为"流未完成"。置 finished 但不 break，兼容 [DONE] 夹在中间的脏流；
 *  - 全程未遇 finish_reason / [DONE] 返回 null（流未完成）。
 *
 *  返回 '' 与 null 都不触发后续 checkJson——调用方仅对非空返回值 mutate。
 *  抽成纯函数以便单测覆盖（原逻辑内联在 Chat.tsx 的 onSuccess 回调里无法测）。 */
interface SseFrame {
    choices?: Array<{ delta?: { content?: string }; finish_reason?: string | null } | null>;
}

export function accumulateSseContent(chunks: readonly unknown[]): string | null {
    let fullContent = '';
    let finished = false;
    for (const chunk of chunks) {
        if (chunk === null || typeof chunk !== 'object') continue;
        const raw = 'data' in chunk ? (chunk as { data?: unknown }).data : undefined;
        if (raw === '[DONE]') {
            // [DONE] 也视为流结束（部分网关不发 finish_reason）；不 break，兼容其夹在内容帧之前的脏流
            finished = true;
            continue;
        }
        if (raw == null) continue;
        if (typeof raw !== 'string') continue;

        let parsed: unknown;
        try {
            parsed = JSON.parse(raw);
        } catch {
            continue;
        }
        if (parsed === null || typeof parsed !== 'object') continue;
        const frame = parsed as SseFrame;

        const choices = frame.choices;
        if (!Array.isArray(choices) || choices.length === 0) continue;
        const choice = choices[0];
        if (!choice) continue;
        const content = choice.delta?.content;
        if (content) {
            fullContent += content;
        }
        if (choice.finish_reason) {
            finished = true;
            break;
        }
    }
    return finished ? fullContent.trim() : null;
}
