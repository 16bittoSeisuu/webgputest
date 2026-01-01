# 作業ガイドライン

## Kotlin

このリポジトリでの作業は、まず [docs/kotlin-coding-rules.md](docs/kotlin-coding-rules.md) を前提に進めてください。
Kotlinの書式、命名、コメント、KDoc、テストの作り方などの規約は、基本的にこのドキュメントに集約しています。

### 最終確認

ソースコードに変更を入れたら、次を実行してください。

```bash
cd <project root> && ./gradlew check
cd <project root>/app/src && ktlint -F
```

## 自然言語

会話と作業プランは、やさしい日本語で書いてください。
変数名とコメントとドキュメントは英語で書いてください。

## git

gradlew.bat の変更は無視して、話題に出さないでください。
