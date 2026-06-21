package io.github.astail.kanbanchat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 看板の 1 行目がログインマーカー（[login] / [login:番号] など）かどうかを判定する。
 * 設定の markers から正規表現を組み立てる。
 */
public final class MarkerMatcher {

    private final Pattern pattern;

    private MarkerMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * "[login]" のようなトークン一覧から判定器を作る。
     * 角括弧の内側（login / ログイン など）を取り出し、{@code ^[(...)(:数字)?]$} の形で照合する。
     */
    public static MarkerMatcher fromTokens(List<String> tokens) {
        StringBuilder alternation = new StringBuilder();
        for (String token : tokens) {
            String inner = token.trim();
            if (inner.startsWith("[")) {
                inner = inner.substring(1);
            }
            if (inner.endsWith("]")) {
                inner = inner.substring(0, inner.length() - 1);
            }
            inner = inner.trim();
            if (inner.isEmpty()) {
                continue;
            }
            if (alternation.length() > 0) {
                alternation.append('|');
            }
            alternation.append(Pattern.quote(inner));
        }
        if (alternation.length() == 0) {
            alternation.append(Pattern.quote("login"));
        }
        Pattern pattern = Pattern.compile(
                "^\\[\\s*(" + alternation + ")\\s*(?::\\s*(\\d+)\\s*)?\\]$",
                Pattern.CASE_INSENSITIVE);
        return new MarkerMatcher(pattern);
    }

    /** 1 行目の素テキストを照合する。 */
    public Match match(String plainLine) {
        Matcher matcher = pattern.matcher(plainLine);
        if (!matcher.matches()) {
            return new Match(false, null);
        }
        String number = matcher.group(2);
        Integer order = number != null ? Integer.parseInt(number) : null;
        return new Match(true, order);
    }

    /** 照合結果。matched=false ならマーカーではない。order=null は番号未指定（自動採番）。 */
    public record Match(boolean matched, Integer order) {
    }
}
