# 前提条件

このドキュメントは、チームタスクボードを GitHub Copilot のハンズオン教材として利用する前に、参加者・講師が準備しておく環境と確認事項をまとめたものです。

## 対象アプリケーション

- アプリケーション名: チームタスクボード
- 種別: Spring Boot による Web アプリケーション
- 主な構成:
  - バックエンド: Java 21 / Spring Boot 3.3.5
  - データベース: SQLite
  - フロントエンド: 静的 HTML / CSS / Vanilla JavaScript
  - ビルド・テスト: Maven / JUnit 5

## 必須ソフトウェア

| 項目 | 必須・推奨バージョン | 用途 | 備考 |
| --- | --- | --- | --- |
| Java Development Kit | **Java 21** | アプリケーションのビルド・実行 | `pom.xml` の `java.version` が `21` に設定されています。 |
| Maven | **3.8 以上** | 依存関係の取得、ビルド、テスト、起動 | このリポジトリには Maven Wrapper が含まれていないため、ローカルに Maven をインストールしてください。 |
| Git | 最新安定版 | リポジトリの取得、変更差分の確認 | ハンズオンでブランチ作成や差分確認を行う場合に使用します。 |
| Web ブラウザー | 最新版の Chrome / Edge / Firefox など | アプリケーションの操作確認 | HTML5 Drag and Drop API を使用します。 |
| Visual Studio Code | 最新安定版推奨 | ハンズオン作業用エディター | GitHub Copilot と Java 開発に使用します。 |

## VS Code 拡張機能

ハンズオンを円滑に進めるため、以下の拡張機能を事前にインストールしておくことを推奨します。

| 拡張機能 | 用途 |
| --- | --- |
| GitHub Copilot | コード補完、実装支援 |
| GitHub Copilot Chat | チャットによるコード理解、修正案作成、テスト作成支援 |
| Extension Pack for Java | Java プロジェクトのビルド、テスト、デバッグ支援 |
| Maven for Java | Maven プロジェクトの操作支援 |

GitHub Copilot を利用するには、GitHub アカウントで VS Code にサインインし、Copilot を利用できるライセンスまたはトライアルが有効になっている必要があります。

## プロジェクトで使用している主なライブラリ

`pom.xml` で定義されている主要なライブラリは次のとおりです。

| ライブラリ | バージョン | 用途 |
| --- | --- | --- |
| Spring Boot | 3.3.5 | アプリケーション基盤、依存関係管理 |
| spring-boot-starter-web | Spring Boot 管理 | REST API、組み込み Web サーバー |
| spring-boot-starter-jdbc | Spring Boot 管理 | JDBC によるデータアクセス |
| spring-boot-starter-security | Spring Boot 管理 | フォームログイン、認可、CSRF 対策 |
| spring-boot-starter-validation | Spring Boot 管理 | リクエスト DTO の入力検証 |
| org.xerial:sqlite-jdbc | 3.46.1.3 | SQLite 接続用 JDBC ドライバー |
| spring-boot-starter-test | Spring Boot 管理 | JUnit 5、AssertJ、MockMvc などのテスト基盤 |
| spring-security-test | Spring Boot 管理 | Spring Security を含むテスト支援 |

Spring Boot のスターター配下の詳細な依存バージョンは、Spring Boot 3.3.5 の依存関係管理に従います。

## 事前確認コマンド

ハンズオン開始前に、プロジェクトルートで以下を確認してください。

```bash
java -version
mvn -version
mvn test
```

確認ポイント:

- `java -version` で Java 21 が使用されていること
- `mvn -version` で Maven 3.8 以上が使用されていること
- `mvn test` が成功すること

## 起動前に確認すること

### 作業ディレクトリ

コマンドは、`pom.xml` があるプロジェクトルートで実行します。

```bash
cd team-task-board
```

### ネットワーク接続

初回ビルド時に Maven Central から依存ライブラリを取得します。企業ネットワークやプロキシ環境では、Maven が外部リポジトリへアクセスできることを確認してください。

### ポート

アプリケーションは既定で `8080` ポートを使用します。

既に別のアプリケーションが `8080` を使用している場合は、該当プロセスを停止するか、起動時に別ポートを指定してください。

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### データベース

外部のデータベースサーバーは不要です。アプリケーションは SQLite を使用し、初回起動時に `./data/taskboard.db` を作成します。

- スキーマ: `src/main/resources/db/schema.sql`
- 初期データ: `src/main/resources/db/seed.sql`
- DB ファイル: `data/taskboard.db`

初期状態に戻したい場合は、アプリケーションを停止してから `data/taskboard.db` を削除してください。次回起動時にスキーマと seed データが再投入されます。

## アプリケーションの起動確認

次のコマンドでアプリケーションを起動します。

```bash
mvn spring-boot:run
```

起動後、ブラウザーで次の URL を開きます。

```text
http://localhost:8080/
```

ログイン画面にリダイレクトされれば起動確認は完了です。

## サンプルログイン情報

初期データには、次のユーザーが登録されています。

| ユーザー名 | パスワード | 表示名 | 所属チーム | ロール |
| --- | --- | --- | --- | --- |
| alice | password | アリス | 製品開発チーム / マーケチーム | OWNER |
| bob | password | ボブ | 製品開発チーム | MEMBER |
| carol | password | キャロル | マーケチーム | MEMBER |

## このアプリで不要な準備

この教材では、次の準備は不要です。

- Node.js / npm のインストール
- フロントエンドのビルド
- 外部 SQLite サーバーの構築
- Docker / Docker Compose
- クラウド環境や外部 API キー
- `.env` ファイルやシークレット設定

フロントエンドは `src/main/resources/static` 配下の静的ファイルとして Spring Boot から配信されます。

## ハンズオン講師向けチェックリスト

事前に以下を確認しておくと、当日のトラブルを減らせます。

- [ ] 参加者が GitHub Copilot を利用できる GitHub アカウントで VS Code にサインインしている
- [ ] Java 21 が VS Code とターミナルの両方で選択されている
- [ ] Maven 3.8 以上がインストールされている
- [ ] `mvn test` が成功する
- [ ] `mvn spring-boot:run` で起動できる
- [ ] `http://localhost:8080/` にアクセスできる
- [ ] `alice` / `password` でログインできる
- [ ] 必要に応じて `data/taskboard.db` を削除し、初期データから開始できる状態にしている

## 補足: API を直接操作する場合

このアプリケーションでは Spring Security の CSRF 対策が有効です。画面から操作する場合は同梱の JavaScript が CSRF トークンを自動で扱います。

REST クライアントなどから `/api/...` の状態変更系 API を直接呼び出す場合は、`XSRF-TOKEN` Cookie の値を `X-XSRF-TOKEN` ヘッダーに付与してください。