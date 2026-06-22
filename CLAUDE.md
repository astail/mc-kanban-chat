# CLAUDE.md

Claude がこのリポジトリで作業する際の開発メモ（Paper プラグイン）。

## プラグインの目的

KanbanChat は、指定した看板の内容を、ログインした各プレイヤーに個別表示する（その人のログイン時に、その人のチャットだけへ。全体ブロードキャストではない。看板の作成者とは無関係に、ログインした全員が対象）。
サーバーのお知らせを config ではなくワールド内の看板で運用するための、サーバー側のみで動くプラグイン。

## ビルド要件

- Java 25 + Maven。生成物は `KanbanChat-1.1.0.jar`。
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
- **権限は 2 段階**: 看板の登録・管理（マーカー方式／`set`・`remove`・`list`・`test`）は `kanbanchat.admin`（`default: true`＝全員可）。`on`・`off`・`reload` などサーバー全体に影響する操作のみ `kanbanchat.manage`（`default: op`）を別途要求（`KanbanChatCommand#requireManage`）。荒らしを絞りたい場合は権限プラグインで `kanbanchat.admin` を個別に剥奪する運用。
- **表示はログインした本人のチャットのみ**（受信者は「ログインしたそのプレイヤー」＝看板作成者とは無関係。`kanbanchat.see` を持つ全員が各自のログイン時に受け取る。全体ブロードキャストではない）。`join-delay-ticks`（既定 30）だけ遅らせてバニラ参加メッセージの後に出す。
- 装飾保持のため、行の素テキスト化は `PlainTextComponentSerializer`、保存は `GsonComponentSerializer` を使用。

## 既知の制限

- 看板は 1 面 4 行・1 行約 15 文字。長文は複数看板の連結で対応。
- マーカー方式は 1 行目が目印のため本文 3 行（コマンド方式は 4 行）。
- WorldEdit 等イベントを伴わない変更はキャッシュとずれる。`/kanbanchat reload` でロード済みチャンクのみ再同期。
- 表示内容は全プレイヤー共通（誰がログインしても同じメッセージ）。受信可否は `kanbanchat.see`（既定 true＝全員）で個別に制御できる。

## リリース手順

- セマンティックバージョニング。`v*` タグの push で GitHub Actions（`.github/workflows/build.yml`）がビルドし、`gh release create --generate-notes` で jar を添付する。
- サーバーへの配置（Releases から DL、または Docker `itzg/minecraft-server` の `PLUGINS` 環境変数で自動 DL）は README の「サーバーへの配置」を参照。
