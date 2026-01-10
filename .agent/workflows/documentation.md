---
description: シグネチャ、KDocによって仕様を決めるワークフロー
---



# Kotlin Specification Definition Workflow

このワークフローは、Kotlinでプログラムを作成する際の最初のステップである「仕様定義」の手順を規定します。

## 重要：文法制約

**注意！ context receiverは使用できません！**
近年のKotlinではcontext receiverはコンパイルエラーとなります。これの代替として、文法上**context parameterのみ**が使用可能です。

```kotlin
context(resource: ResourceScope)
fun openConnection(): DatabaseConnection {
  // ...
  resource.install(connection)
  // ...
}
```

## 1. シグネチャの作成

対象（class, interface, object, fun, val/var）のシグネチャを作成します。

- **アクセス修飾子**: `public` キーワードを省略してください。Kotlinでは省略するとデフォルトで `public` になります。
- **Context Parameter**: 上記の通り、必ず `context parameter` を使用してください。
- **命名規則**: 以下の規則に従ってください。
    - パラメーター名、プロパティ名、フィールド名において、以下の「一時オブジェクト」を想起させる名前の使用を禁止します。
        - 禁止例: `tmp`, `temp`, `a`, `value`
    - **Enum エントリー**: 全て `PascalCase` で記述してください（例: `Active`, `InProgress`）。
- **実装のスタブ化**: 関数の本体は `TODO()` で記述し、この段階では具体的なロジックを実装しないでください。

## 2. KDocによる仕様定義

すべての public/protected なメンバーに対し、包括的な KDoc を英語で作成します。

### 構成要素と優先順位

1. **正確性 (最優先)**: 論理的に発生し得ない事項を記述しないでください。
2. **必要十分性**: 読み手がこれだけでテストを書ける状態を目指します。
3. **簡潔性**: 型システムから自明な事項は省略してください。

### 記述項目 (該当する場合のみ)

- `@receiver`: レシーバーが満たすべき性質と、満たされない時の挙動。
- `@param`: 入力値の意味、制約、および制約違反時の挙動。
- `@return`: 戻り値の意味と、型で表現されない性質（例: 文字列が非空であること、ソート済みであること）。
- `@throws`: 例外が投げられる条件。

### エッジケース・チェックリスト

対象の型に応じて、以下の項目を網羅しているか必ず確認してください。

#### プリミティブ・値型
- **整数**: 0、負数、最小値/最大値、オーバーフローの可能性。
- **実数・Duration**: NaN、無限大、0、最小値/最大値、オーバーフローの可能性。
- **文字列**: 空文字、非常に長い文字列、サロゲートペア、改行（パーサーの場合）。
- **Boolean**: 真偽両方のケース。

#### コレクション・データ構造
- **共通**: 空のコレクション、要素が1つの場合、非常に大きなコレクション。
- **List**: 重複要素の扱い、順序。
- **Map**: キーが存在しない場合。

#### クラス・オブジェクト
- あり得る状態の組み合わせ（直積）を考慮した振る舞い。

### フォーマット規則
- **言語**: 英語のみを使用。
- **スタイル**: ヘッダー `#` や箇条書き `-` を活用して構造化する。空行を適切に入れて読みやすくする。
- **整合性**: `@param` や `@return` の制約は、必ず `@throws` の条件と一致させる。

#### 具体例

```kotlin
/**
 * Uploads a file to the remote storage and updates the local synchronization metadata.
 *
 * ## Description
 *
 * This function performs a resumable upload of the provided file. If the file was
 * previously partially uploaded, it detects the progress from the [metadataCache]
 * and resumes from the last successful byte.
 *
 * The [fileSystem] is used to read the file content as a stream to minimize memory usage.
 * Upon successful completion, the [metadataCache] is updated with the new checksum.
 *
 * ## Thread safety
 *
 * This function is not thread-safe for the same [targetPath]. Concurrent calls to
 * this function with the identical [targetPath] may lead to corrupted uploads or
 * inconsistent cache states. External synchronization is required.
 *
 * ## Side effects
 *
 * - Reads file data from the local [fileSystem]
 * - Performs network I/O to the remote storage endpoint
 * - Updates the [metadataCache] with the latest upload status
 * - Logs the upload progress and any recoverable network errors
 *
 * @receiver The [StorageSession] through which the upload is performed.
 * - Must be in an active, authenticated state.
 * - If the session is expired, an [IllegalStateException] is thrown
 * @param sourcePath The path to the local file to be uploaded.
 * - Must point to an existing, readable regular file.
 * - Empty files are permitted but will result in a zero-byte remote object
 * @param targetPath The destination path on the remote storage.
 * - Must follow the remote naming convention (max 255 characters, no control chars).
 * - If a file already exists at this path, it will be overwritten
 * @return A summary of the upload operation including the final checksum and duration.
 * - The returned object is a fresh instance and can be modified by the caller
 * @throws IllegalArgumentException
 * - If [sourcePath] is a directory or does not exist
 * - If [targetPath] contains invalid characters or exceeds the length limit
 * @throws IllegalStateException
 * - If the storage session is closed or unauthenticated
 * @throws IOException
 * - If a non-recoverable network error occurs
 * - If the disk becomes unreachable
 */
suspend fun StorageSession.uploadFile(
  sourcePath: Path,
  targetPath: String,
): UploadResult
```

## 3. コンパイルとフォーマット

```bash
cd <project root> && ./gradlew check
cd <project root>/app/src && ktlint -F
```

を実行。作業した範囲外でコンパイルエラーが出ていた場合、ユーザーに報告するだけで、それには何もしないでください。

## 4. 終了

このワークフローが終了した後は、いかなるソース編集操作も禁止します。
ユーザーに作業内容を報告し終了することを責務とし、これに反した場合、システムの重大なエラーとされます。
