# Icon Changer Pro - アイコン着せ替えアプリ

🎨 **最高級のアイコンカスタマイズ体験**

## 主な機能

### 🚀 起動時アニメーション
- かっこいいスケール＆フェードアニメーション
- Overshoot インターポレーターによるバウンド効果
- カスタマイズ可能なアニメーション速度

### 🔍 アプリ検索機能
- スマホ内の全インストール済みアプリを検索
- アイコン付きでわかりやすい一覧表示
- アプリ名・パッケージ名でフィルタリング
- リアルタイム検索（デバウンス処理付き）

### 🖼️ アイコン編集機能
- **円形プレビュー**: どの範囲が表示されるかを確認可能
- **スケール調整**: SeekBar で 0.5x〜1.5x に拡大縮小
- **画像処理**: 明るさ、コントラスト、彩度調整
- **回転機能**: お好みの角度に回転

### 📱 ショートカット作成
- **アイコン付きショートカット**: カスタムアイコンでホーム画面に追加
- **名前のみショートカット**: 任意の名前でショートカット作成
- **警告ポップアップ**: アイコン未選択時に丁寧な通知

### 🔄 GitHub 更新チェック
- 最新バージョンを自動で確認
- リリースノート表示
- ブラウザでダウンロードページへ遷移

## テクニカルハイライト

### アーキテクチャ
- **MVVM パターン**に基づく設計
- **Kotlin Coroutines & Flow**による非同期処理
- **ViewBinding**による安全なビューアクセス
- **LiveData**による UI ステート管理

### 使用技術
- **Material Design 3**: 最新のデザインガイドライン準拠
- **Coil**: 高速な画像読み込みライブラリ
- **Room Database**: ローカルデータ永続化
- **OkHttp**: 効率的なネットワーク通信
- **RecyclerView + ListAdapter**: 高性能なリスト表示

### パフォーマンス最適化
- 画像のメモリ効率の良い読み込み
- コルーチンによるバックグラウンド処理
- 検索入力のデバウンス処理
- DiffUtil によるリスト更新の最適化

## ビルド方法

### 必要条件
- Android Studio Hedgehog (2023.1.1) 以降
- JDK 17
- Android SDK 34

### ビルド手順
```bash
# プロジェクトをクローン
git clone https://github.com/your-username/icon-changer-pro.git

# Android Studio で開く
# またはコマンドラインでビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease
```

## 権限説明

| 権限 | 用途 |
|------|------|
| `INTERNET` | GitHub からの更新情報取得 |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | 画像選択によるアイコン設定 |
| `QUERY_ALL_PACKAGES` | インストール済みアプリの一覧取得 |
| `INSTALL_SHORTCUT` | ホーム画面へのショートカット追加 |

## プロジェクト構成

```
app/src/main/
├── java/com/example/iconchanger/
│   ├── ui/                  # アクティビティ・フラグメント
│   │   ├── MainActivity.kt
│   │   └── SplashActivity.kt
│   ├── model/               # データクラス
│   │   └── DataModels.kt
│   ├── adapter/             # RecyclerView アダプター
│   │   └── AppAdapters.kt
│   └── util/                # ユーティリティクラス
│       ├── AppUtils.kt
│       └── Utils.kt
├── res/
│   ├── layout/              # レイアウト XML
│   ├── drawable/            # ドローアブル
│   ├── values/              # 文字列・カラー・テーマ
│   └── anim/                # アニメーション
└── AndroidManifest.xml
```

## ライセンス

MIT License

## 貢献

プルリクエストを歓迎します！
大きな変更を加える場合は、事前に issue を立てて議論させてください。

---

**Icon Changer Pro** - あなたのホーム画面を美しく✨
