# CLAUDE.md

Claude がこのリポジトリで作業する際の開発メモ（Paper プラグイン）。

## プラグインの目的

KanbanChat は、指定した看板の内容をプレイヤーのログイン時に**本人のチャットだけ**へ流す。
サーバーのお知らせを config ではなくワールド内の看板で運用するための、サーバー側のみで動くプラグイン。

## ビルド要件

- Java 25 + Maven。生成物は `KanbanChat-1.0.0.jar`。
- 唯一の依存は `io.papermc.paper:paper-api:26.1.2.build.69-stable`（provided）。
- ローカルビルドは `./deploy.sh`（Homebrew `openjdk@25` を想定）。

## アーキテクチャ構成

- **KanbanChatPlugin**: 本体。コマンド/リスナー登録、設定読込、`PlayerJoinEvent`・`SignChangeEvent`・`BlockBreakEvent` の処理、メッセージ組み立て。
- **KanbanChatCommand**: `/kanbanchat <set|remove|list|test|reload|on|off|status>` の実処理とタブ補完。
- **SignRegistry**: 登録看板の保持と `signs.yml` への永続化。位置をキーに重複排除、order 昇順で連結用リストを返す。
- **SignEntry / SignLocation**: 登録 1 件分のデータ（位置・面・番号・登録元・本文）と位置キー（record）。
- **MarkerMatcher**: 1 行目が `[login]` / `[login:番号]` 等のマーカーかを正規表現で判定。

## 設計上の要点

- **看板の指定は 2 方式に両対応**: コマンド（`/kanbanchat set`）とマーカー行（`[login]`）。どちらも同じ `SignRegistry` に入る。
- **複数看板は order 昇順で連結**（同値は y, x, z 順で安定ソート）。番号はコマンド/マーカーで明示、省略時は `最大+1` で自動採番。
- **本文はキャッシュが真実源**: 登録/編集時に `signs.yml` へ Component を JSON 保存し、ログイン表示はキャッシュから組み立てる（チャンク非依存・高速）。
- **マーカー方式は `kanbanchat.admin` を要求**（`SignChangeEvent` 内で権限チェック）。荒らし対策。
- **表示は本人のみ**、`join-delay-ticks`（既定 30）だけ遅らせてバニラ参加メッセージの後に出す。
- 装飾保持のため、行の素テキスト化は `PlainTextComponentSerializer`、保存は `GsonComponentSerializer` を使用。

## 既知の制限

- 看板は 1 面 4 行・1 行約 15 文字。長文は複数看板の連結で対応。
- マーカー方式は 1 行目が目印のため本文 3 行（コマンド方式は 4 行）。
- WorldEdit 等イベントを伴わない変更はキャッシュとずれる。`/kanbanchat reload` でロード済みチャンクのみ再同期。
- `kanbanchat.see` のような per-player トグルはあるが、表示内容は全プレイヤー共通。

## リリース手順

- セマンティックバージョニング。`v*` タグの push で GitHub Actions（`.github/workflows/build.yml`）がビルドし、`gh release create --generate-notes` で jar を添付する。
