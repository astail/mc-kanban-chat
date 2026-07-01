# KanbanChat

指定した**看板**に書いた内容を、**ログインした各プレイヤーのチャットに個別表示する**掲示板プラグインです（Paper 用 / サーバー側のみ）。

サーバーの「お知らせ」「ルール」「Discord 案内」などを、設定ファイルではなく**ワールド内の看板**で管理できます。看板を書き換えるだけで、次回以降ログインしたプレイヤーへ自動的に表示されます。

> **誰に表示される？** 看板を書いた人だけ…ではありません。`kanbanchat.see` 権限を持つ**ログインした全プレイヤー**が対象で、各自のログイン時に**その人のチャットへ個別表示**します（その瞬間オンラインの全員へ一斉ブロードキャストするわけではありません）。

## 解決する課題

「サーバーのお知らせを、毎回 config を編集せずに、ゲーム内で手軽に更新したい」。
KanbanChat は看板を“掲示板”として扱い、**看板に書く → ログイン時に流れる**というシンプルな運用を実現します。

## 主な機能

- **2 つの指定方法に両対応**
  - コマンド方式: 看板を見ながら `/kanbanchat set` で登録（4 行すべてを本文に使える）
  - マーカー方式: 看板の 1 行目に `[login]` と書くと自動登録（コマンド不要・置くだけ）
- **複数看板の連結**: 各看板に番号を持たせ、**番号の昇順**で 1 つのメッセージに連結。看板 1 枚（4 行）の文字数制限を超える長文に対応。
- **ログインした各プレイヤーへ個別表示**: 看板を書いた人かどうかに関係なく、**ログインしてくる全員**が対象。各自のログイン時に、その人のチャットだけへ表示します（全体への一斉ブロードキャストではありません）。参加直後に少し遅らせて表示（バニラの参加メッセージの後）。
- **装飾を保持**: 看板の色（染料）・装飾をそのままチャットへ反映。
- **編集に自動追従**: 看板を書き換えれば本文も自動更新、壊せば自動解除。
- **全体 ON/OFF**: `/kanbanchat on|off` でいつでも停止・再開。

## 動作要件

- サーバー: Paper 26.2（experimental チャンネル）
- Java: 25
- クライアント: バニラで可（MOD 不要・サーバー側のみ）

## 導入

1. `KanbanChat-1.2.0.jar` を `plugins/` に置いてサーバーを再起動します。
2. 看板を設置し、下記いずれかの方法で「ログイン看板」として登録します。
3. プレイヤーがログインすると、そのプレイヤーのチャットに内容が表示されます（ログインした全員が対象）。

## 使い方

### 方法 1: コマンドで登録

1. 看板に本文を書く（最大 4 行）。
2. その看板を**見ながら** `/kanbanchat set` を実行。
   - 番号を付けたいときは `/kanbanchat set 2` のように指定（省略時は自動採番）。
   - 裏面を使いたいときは `/kanbanchat set back`。
3. `/kanbanchat test` で自分にプレビュー表示できます。

### 方法 2: マーカー行で登録

1. 看板の **1 行目に `[login]`**（または `[ログイン]`）と書く。
2. 2〜4 行目が本文になります（1 行目の目印はメッセージに含まれません）。
3. 順番を決めたいときは `[login:2]` のように番号を付けます（省略時は自動採番）。

> 既定では誰でも看板を登録できます（コマンド方式・マーカー方式とも `kanbanchat.admin`、既定 `true`）。荒らしを防ぎたい場合は、権限プラグインで特定プレイヤー／グループの `kanbanchat.admin` を `false` にして剥奪してください。

### 複数の看板を連結する

看板を複数登録すると、**番号の昇順**で連結して 1 つのメッセージとして送信します。
長いお知らせは、看板を縦に並べて `1, 2, 3 …` と番号を振ってください。

## コマンド

| コマンド | 説明 | 権限 |
|---|---|---|
| `/kanbanchat set [番号] [front\|back]` | 見ている看板を登録（番号省略で自動採番） | `kanbanchat.admin` |
| `/kanbanchat remove [番号]` | 看板の登録を解除（番号 or 視線の看板） | `kanbanchat.admin` |
| `/kanbanchat list` | 登録一覧を表示 | `kanbanchat.admin` |
| `/kanbanchat test` | 自分にメッセージをプレビュー表示 | `kanbanchat.admin` |
| `/kanbanchat status`                | 現在の状態を表示                | `kanbanchat.admin` |
| `/kanbanchat reload`                | 設定と看板を再読み込み          | `kanbanchat.manage` |
| `/kanbanchat on \| off`             | 全体の有効 / 無効               | `kanbanchat.manage` |

エイリアス: `/kc`

## 権限

| 権限ノード | 説明 | 既定 |
|---|---|---|
| `kanbanchat.see` | ログイン時にメッセージを受け取る | `true`（全員） |
| `kanbanchat.admin` | 看板の登録・管理（set / remove / list / test・マーカー方式） | `true`（全員） |
| `kanbanchat.manage` | `on` / `off` / `reload` などサーバー全体に影響する操作 | `op` |

> 旧バージョンで `kanbanchat.admin` を付与していたモデレーターに on/off/reload も任せたい場合は、別途 `kanbanchat.manage` を付与してください（OP は既定で両方を持ちます）。

## 設定（`config.yml`）

```yaml
enabled: true          # ログイン時にメッセージを送るか（/kanbanchat on|off で切替）
join-delay-ticks: 30   # 参加後この tick だけ待ってから送信（20 tick = 1 秒）
markers:               # この語で始まる 1 行目を自動登録の目印にする
  - "[login]"
  - "[ログイン]"
skip-empty-lines: true # 空行をメッセージから取り除く
line-separator: ""     # 1 枚の看板の複数行を 1 行に連結するときの区切り（既定は区切りなし）
```

登録済みの看板は `plugins/KanbanChat/signs.yml` に保存されます（本文はキャッシュされるため、看板のチャンクが未ロードでもログイン表示は即時）。

## 仕組み / 技術メモ

- 看板の編集（`SignChangeEvent`）を監視し、マーカー方式の登録・解除、コマンド登録看板の本文更新を反映します。
- ログイン表示は `signs.yml` のキャッシュから組み立てるため高速で、看板のチャンクがロードされていなくても動作します。
- WorldEdit など**イベントを発生させない**手段で看板を書き換えた場合はキャッシュとずれることがあります。`/kanbanchat reload` でロード済みチャンクの看板を再同期できます。
- 看板の文字は Adventure の `Component` として保存（JSON シリアライズ）するため、色・装飾を保持したままチャットへ流せます。

### 制限

- Minecraft の看板は 1 面 4 行・1 行あたり約 15 文字までです。長文は複数看板の連結で対応してください。
- **1 枚の看板に書いた複数行は、チャットでは改行せず 1 行に連結して表示します**（看板どうしの間だけ改行）。看板の横幅で折り返した文章を、チャットでは続けて読めます。区切り文字は `line-separator` で変更できます（既定は区切りなし）。
- マーカー方式は 1 行目を目印に使うため、本文は 3 行になります（コマンド方式は 4 行）。

## ビルド

```bash
./deploy.sh        # Mac ネイティブ（JDK 25 + Maven）。生成物: target/KanbanChat-1.2.0.jar
# または
mvn -B clean package
```

`v*` タグを push すると GitHub Actions（`.github/workflows/build.yml`）がビルドし、リリースに jar を添付します。

## サーバーへの配置

サーバーの `plugins/` に jar を置いてサーバーを再起動します。jar の入手は次の 2 通り（A・B）です。Docker（itzg/minecraft-server）を使う場合は、後述の「Docker Compose で自動ダウンロード」も利用できます。

### A. リリース版を使う（ビルド不要・推奨）

[Releases](https://github.com/astail/mc-kanban-chat/releases) から最新の `KanbanChat-<version>.jar` をダウンロードします。JDK や Maven は不要です。

```bash
# 最新リリースの jar をダウンロード（gh CLI を使う場合）
gh release download --repo astail/mc-kanban-chat --pattern '*.jar'
```

### B. 自分でビルドする

[ビルド](#ビルド) の手順で `target/KanbanChat-1.2.0.jar` を生成します。

### 配置

入手した jar をサーバーの `plugins/` に置いてサーバーを再起動します。

```bash
# バインドマウントしている場合（ホスト側 plugins ディレクトリへコピー）
cp target/KanbanChat-1.2.0.jar /path/to/data/plugins/
docker restart <コンテナ名>

# 名前付きボリューム等の場合（コンテナへ直接コピー）
docker cp target/KanbanChat-1.2.0.jar <コンテナ名>:/data/plugins/
docker restart <コンテナ名>
```

### Docker Compose（itzg/minecraft-server）で自動ダウンロード

[`itzg/minecraft-server`](https://github.com/itzg/docker-minecraft-server) イメージを使う場合は、jar を手元に用意しなくても **`PLUGINS` 環境変数にリリースの URL を並べるだけ**で、起動時に自動ダウンロードして `plugins/` に配置できます。

```yaml
services:
  mc:
    image: itzg/minecraft-server
    tty: true
    stdin_open: true
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: "PAPER"
      VERSION: "26.2"
      PAPER_CHANNEL: "experimental"
      PLUGINS: |
        https://github.com/astail/mc-kanban-chat/releases/download/v1.2.0/KanbanChat-1.2.0.jar
    volumes:
      - ./data:/data
    restart: unless-stopped
```

`PLUGINS` は改行区切りで複数指定できます。バージョンを更新したら、URL の `v1.2.0` とファイル名を新しいリリースに合わせて変更してください（例: `.../download/v1.2.0/KanbanChat-1.2.0.jar`）。

起動ログに以下が出れば成功です。

```text
[KanbanChat] KanbanChat を有効化しました（登録看板: 0 枚 / 状態: ON）。
```

## ライセンス

MIT License — [LICENSE](LICENSE) を参照。
