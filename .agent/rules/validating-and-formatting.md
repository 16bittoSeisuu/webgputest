---
trigger: always_on
---

# ファイルを編集するときのルール

コードを編集したら、その後**必ず**テストとフォーマットを行うこと。
これに反することは、あなたの重大なエラーとみなします。

## テスト方法

```bash
cd <project root> && ./gradlew check
```

## フォーマット方法

cdがないとうまく動きません。

```bash
cd <project root>/app/src && ktlint -F
```
