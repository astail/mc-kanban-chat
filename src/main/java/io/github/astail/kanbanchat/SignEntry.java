package io.github.astail.kanbanchat;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * ログイン看板 1 枚分の登録情報。
 * order の昇順で連結してプレイヤーへ送信する。
 */
public record SignEntry(SignLocation location, int order, Source source, List<Component> lines) {

    /** 看板がどの方法で登録されたか。 */
    public enum Source {
        /** /kanbanchat set で登録。 */
        COMMAND,
        /** 1 行目のマーカー [login] で自動登録。 */
        MARKER
    }

    /** 本文行だけを差し替えた新しいエントリを返す。 */
    public SignEntry withLines(List<Component> newLines) {
        return new SignEntry(location, order, source, newLines);
    }
}
