---
trigger: always_on
description: Kotlinソースコードを編集するとき。
---

コードを編集したら、次でテストとフォーマットを行うこと。
特に、ktlintを走らせるときのcdはないとうまく動きません。

```bash
cd <project root> && ./gradlew check
cd <project root>/app/src && ktlint -F
```
