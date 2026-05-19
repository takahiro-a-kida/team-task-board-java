# チームタスクボード

チーム単位で共有するシンプルなカンバン型タスク管理アプリです。
バックエンドは Spring Boot (Java 21) + SQLite、フロントエンドは Vanilla JavaScript で実装しています。

## 技術スタック

- Java 21 / Spring Boot 3 / Maven
- SQLite (`org.xerial:sqlite-jdbc`)
- Spring JDBC (`JdbcClient` を主に使用)
- Spring Security (フォームログイン + BCrypt、HttpSession + CSRF Cookie)
- 静的 HTML + Vanilla JavaScript (HTML5 Drag & Drop API)
- JUnit 5 / AssertJ / spring-security-test

## ディレクトリ構成

```
src/
├── main/
│   ├── java/com/example/taskboard/
│   │   ├── TaskBoardApplication.java
│   │   ├── config/        # SecurityConfig, DataInitializer
│   │   ├── common/        # 共通例外, GlobalExceptionHandler, ApiError
│   │   ├── user/          # User, UserRepository, JdbcUserDetailsService, UserController
│   │   ├── team/          # Team, TeamMember, TeamRepository, TeamService, TeamController
│   │   └── task/          # Task, TaskRepository, TaskService, TaskController, dto/...
│   └── resources/
│       ├── application.yml
│       ├── db/{schema.sql, seed.sql}
│       └── static/        # login.html, index.html, teams.html, css/, js/
└── test/
    ├── java/com/example/taskboard/
    │   ├── TestFixtures.java
    │   ├── task/          # TaskRepositoryTest, TaskServiceTest
    │   ├── team/          # TeamServiceTest
    │   └── api/           # ApiSmokeTest (MockMvc)
    └── resources/application-test.yml
```

## 動かし方

### 必要環境

- Java 21
- Maven 3.8+

### 起動

```bash
mvn spring-boot:run
```

初回起動時に `./data/taskboard.db` が作成され、スキーマ・seed データが自動投入されます。
再起動しても seed は再適用されません（DB ファイルを消すと再投入されます）。

ブラウザで <http://localhost:8080/> を開くと自動的にログイン画面へリダイレクトされます。

### サンプルユーザー

| ユーザー名 | 表示名     | チーム                   | ロール |
| ---------- | ---------- | ------------------------ | ------ |
| alice      | アリス     | 製品開発チーム / マーケチーム | OWNER  |
| bob        | ボブ       | 製品開発チーム           | MEMBER |
| carol      | キャロル   | マーケチーム             | MEMBER |

### テスト

```bash
mvn test
```

## 機能

- ログイン / ログアウト
- 自分が所属するチームの一覧と切替
- チームの新規作成・メンバー追加 / ロール変更 / 削除（OWNER のみ）
- タスク一覧（カンバン: TODO / IN_PROGRESS / DONE、ドラッグ&ドロップで状態変更）
- タスク作成・編集・削除
- ステータス変更 / 優先度設定 (LOW/MEDIUM/HIGH) / 担当者設定 / 期限日設定
- 検索・絞り込み（キーワード、ステータス、優先度、担当者、期限範囲）

## REST API 一覧

| Method | パス | 説明 |
| --- | --- | --- |
| GET    | `/api/me`                                                | ログイン中ユーザー |
| POST   | `/api/auth/login`                                        | ログイン (`username`, `password` を `application/x-www-form-urlencoded`) |
| POST   | `/api/auth/logout`                                       | ログアウト |
| GET    | `/api/teams`                                             | 自分が所属するチーム一覧 |
| POST   | `/api/teams`                                             | チーム作成（作成者は OWNER） |
| GET    | `/api/teams/{teamId}/members`                            | メンバー一覧 |
| POST   | `/api/teams/{teamId}/members`                            | メンバー追加（OWNER のみ） |
| PATCH  | `/api/teams/{teamId}/members/{userId}`                   | ロール変更（OWNER のみ） |
| DELETE | `/api/teams/{teamId}/members/{userId}`                   | メンバー削除（最後の OWNER は不可） |
| GET    | `/api/teams/{teamId}/tasks`                              | タスク検索 (`keyword,status,priority,assignee,dueFrom,dueTo`) |
| POST   | `/api/teams/{teamId}/tasks`                              | タスク作成 |
| GET    | `/api/teams/{teamId}/tasks/{taskId}`                     | タスク詳細 |
| PUT    | `/api/teams/{teamId}/tasks/{taskId}`                     | タスク更新 |
| PATCH  | `/api/teams/{teamId}/tasks/{taskId}/status`              | ステータスのみ変更 |
| DELETE | `/api/teams/{teamId}/tasks/{taskId}`                     | タスク削除 |
| GET    | `/api/users/search?q=...`                                | ユーザー検索（メンバー追加用） |

CSRF は有効です。`/api/...` への状態変更系リクエストは `XSRF-TOKEN` Cookie を読み
`X-XSRF-TOKEN` ヘッダに付与してください（同梱 JS では自動で行っています）。
