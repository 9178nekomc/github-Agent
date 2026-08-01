package com.ai.github.agent;

/**
 * JSON文本清洗工具。
 * AI模型即使被要求"只返回JSON"，也经常会习惯性地包一层Markdown代码块围栏（```json ... ```），
 * 这个工具负责把这层围栏干净地剥掉，避免JSON解析失败。
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    public static String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();

        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline != -1) {
                t = t.substring(firstNewline + 1);
            } else {
                t = t.substring(3);
            }
            int lastFence = t.lastIndexOf("```");
            if (lastFence != -1) {
                t = t.substring(0, lastFence);
            }
        }

        return t.trim();
    }
}
