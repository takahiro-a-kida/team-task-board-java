-- 全ユーザーのログインパスワードは "password" (BCryptハッシュ済み)
INSERT INTO users (id, username, password_hash, display_name) VALUES
  (1, 'alice', '$2a$10$zPQXYgz2lbHHMJ49Het12.EYV49HD3XTA6Vg7S1HLJW3GOIUZoCT.', 'アリス'),
  (2, 'bob',   '$2a$10$zPQXYgz2lbHHMJ49Het12.EYV49HD3XTA6Vg7S1HLJW3GOIUZoCT.', 'ボブ'),
  (3, 'carol', '$2a$10$zPQXYgz2lbHHMJ49Het12.EYV49HD3XTA6Vg7S1HLJW3GOIUZoCT.', 'キャロル');

INSERT INTO teams (id, name) VALUES
  (1, '製品開発チーム'),
  (2, 'マーケチーム');

INSERT INTO team_members (team_id, user_id, role) VALUES
  (1, 1, 'OWNER'),
  (1, 2, 'MEMBER'),
  (2, 1, 'OWNER'),
  (2, 3, 'MEMBER');

INSERT INTO tasks (team_id, title, description, status, priority, assignee_user_id, due_date, created_by_user_id) VALUES
  (1, 'ログイン画面の実装',         'OAuth ではなくフォームログインで実装する。BCrypt を利用すること。', 'DONE',        'HIGH',   1, '2026-04-30', 1),
  (1, 'タスク一覧のカンバンUI',     'TODO / IN_PROGRESS / DONE の3列。ドラッグ&ドロップでステータスを変更できるようにする。', 'IN_PROGRESS', 'HIGH',   2, '2026-05-25', 1),
  (1, 'タスク検索の絞り込み',       'キーワード、ステータス、優先度、担当者、期限範囲で絞り込みできるようにする。', 'TODO',        'MEDIUM', 2, '2026-06-10', 1),
  (1, 'タスク削除ボタンの追加',     'タスク詳細モーダルから削除できるようにする。誤操作防止に確認ダイアログを出すこと。', 'TODO',        'LOW',    NULL, '2026-06-20', 1),
  (1, 'ロード時のスピナー表示',     '初回ロード時にカンバンが描画されるまでローディング表示を出す。', 'TODO',        'LOW',    NULL, NULL,         2),
  (2, '春の新キャンペーン企画',     'ターゲット層別の訴求軸を3つに絞る。SNS と LP の構成案も用意。', 'IN_PROGRESS', 'HIGH',   3, '2026-05-20', 1),
  (2, '広告クリエイティブのレビュー', '3案を比較し、CTR の高いコピーをチームで選ぶ。', 'TODO',        'MEDIUM', 3, '2026-05-22', 1),
  (2, '前期アンケート結果のまとめ', 'NPS の伸び要因と低下要因をスライドにまとめる。タスク検索で「アンケート」と探せるようにタイトルにキーワードを残しておく。', 'DONE',        'LOW',    1, '2026-04-15', 1),
  (2, '次回オフライン展示の準備',   'ブースのレイアウト案、配布資料、当日シフトを準備する。', 'TODO',        'MEDIUM', NULL, '2026-07-01', 1);
