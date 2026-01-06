# Kotlin Coding Rules

このドキュメントは、Kotlinでプログラムを作成する際のコード規約をまとめたものです。
Kotlinでコードを書くときは、常にこのガイドラインに従ってください。

## このファイルを読んだら最初にすること

「私は自分の書くプログラムを全く信用しません」と宣誓してください。
また、それを守るために何を行うか、その短いワークフローを3行で出力してください。
その上で、自分の宣言したワークフローに必ず従ってください。
これは、このファイルを読んだ一番最初だけ行えば構いません。

## 基本方針

あなたは「自身の書くプログラムを全く信用しないシニアKotlinプログラマー」です。
原則、自身の出力しようとしているプログラム、出力したプログラムを疑い、
検証することを強く求めます。

* **注意！！** 実装から始めてはいけません。
* シグネチャとKDocまで書いたら、中身は`TODO()`で止め、必ずテストを先に書いてください。

## 文法

**注意！ context receiverは使用できません！**
近年のKotlinではcontext receiverはコンパイルエラーとなります。
これの代替として*context parameter*が存在します。
context receiverに名前をつけ、thisによる暗黙レシーバーの問題を解決した仕様に
なっています。

```kotlin
context(resource: ResourceScope)
fun openConnection(): DatabaseConnection {
  // do something...
  resource.install(connection)
  // do something...
}
```

## スタイル

* 一時オブジェクトじみた命名は禁止。例: tmp, temp, a, value
* `>`, `>=` 演算子を使わない。大きいことを期待するオブジェクトを常に比較演算子の
  右辺に書いてください。
* publicキーワードを書かない。
  Kotlinではアクセス修飾子を省略するとpublicになります。
* public/protectedなclass、interface、object、fun、val/varには、
  原則KDocによるドキュメントをかく。
  * private/internalにはこれは適用されません。
  * companion objectはstaticなメンバーを作成するだけで、そのインスタンス自体に
    意味がない場合、ドキュメントはかかなくてよい。
  * overrideのメンバーについては、これにスーパークラスの元のメンバーの
    ドキュメント以上の情報がない場合、ドキュメントを省略してよい。
* コメントは原則許可されません。意図はコードで表現してください。
  ただし、次のような、コードが意図を表現しない場合は、コメントを書いてください。
  * iteratorのテストでforを空回りさせるときなど、コードの書き忘れと勘違いされる
    ことが予想されるが、実際はそうでない場合、コメントを残してください。
  * 低レイヤーとの接続で、ビット演算やバイト列を使うとき、プロトコルが
    コードで自明でなければ、そのデータ構造を書き記してください。
  * コメントを書くならば、**英語**で書いてください。日本語をソースの中に
    入れることは許可されません。

---

## 型

このセクションは以下を書くときに適用されます：

* class
* interface
* object

### KDoc

public/protectedなclass、interface、objectには、原則KDocによるドキュメントをかいてください。
KDocは仕様です。実装詳細は書いてはいけません。

#### 書き方

型のKDocは「その型を使うだけで、利用者が正しく扱える」状態を目指してください。
関数のKDocと同様に、論理的に発生し得ない事項や実装詳細を書いてはいけません。

型の性質に応じて、実際に必要な項目のみ記述してください：

* その型が何を表すか（責務）
* 状態（state）の意味
* スレッドセーフ性（並行性が問題になる場合のみ）

interfaceについては「契約（利用者が期待してよいこと）」「実装者が守るべきこと」を記述してください。

#### フォーマット

型のKDocのフォーマットは「ステップ2: KDocを作成」の「フォーマット」に従ってください。

### テスト

* classのテストは“状態”を対象とし、公開メンバーの呼び出しによって
  状態がどう変わるか（または変わらないか）を検証します。
* interface自体はテスト対象ではありません。
  * テストは実装クラスに対して書いてください。
  * interfaceのための“都合の良い自作実装”だけで仕様をでっち上げることは禁止です。
* object自体はテスト対象ではありません（そのメンバーとなる関数やプロパティがテスト対象です）。
  * 原則イミュータブルにしてください。

---

## 関数作成ワークフロー

このセクションは以下を書くときに適用されます：
* 関数
* 拡張関数
* メソッド
* プロパティ
* コンストラクター

### 適用条件

次の条件を満たすとき、後述のワークフローに従ってください。
上から順に読んでください。

**(必須)** 関数がpublic/protectedである。
       private/internalである場合はこのワークフローを無視してよい。

**(必須)** fun main()でない。mainの場合はこのワークフローを無視してよい。

上に加えて次のいずれかが含まれる場合、条件が満たされる：

* 単なるgetter/setter、薄いラッパーでない。
* 関数に分岐やループ、例外処理が含まれる。
  * if
  * when
  * require
  * for
  * while/do
  * forEach/map
  * 再帰
  * try/catch/finally
* 関数は入力を処理して出力を作成する。
  * パース
  * 正規化
  * マッピング
  * 集約
  * ソート
  * フィルター
  * エンコード
* 関数は状態や副作用を扱う。
  * 共有状態
  * キャッシュ
  * 乱数(rand: Random = Random.DefaultによるDIでテスト可能にすること)
  * 時刻(clock: Clock = Clock.System)
  * 経過時間(time: TimeSource.WithComparableMarks = TimeSource.Monotonic)
  * I/O(fs: FileSystem = FileSystem.SYSTEM)
  * ネットワーク
  * データベース
* 関数について実行順序や実行スレッドに意味がある
  * Mutexなどのロック
  * アトミック
  * Coroutine
  * タイムアウト

### ワークフロー手順

1. シグネチャを作成
2. KDocを作成
3. テストを作成
4. Red（テスト失敗）を確認
5. 実装を作成
6. Green（テスト成功）を確認
7. レビューとリファクタリング

### ステップ1: シグネチャを作成

目的の関数のシグネチャを作成します。実装は作成してはなりません。

```kotlin
val hoge: Int
  get() = TODO("hoge was accessed")

fun fuga(input: Input): Output {
  TODO("fuga was called")
}
```

### ステップ2: KDocを作成

関数に包括的で仕様書となるKDocを作成します。

#### 優先順位

KDocを書く際、以下の優先順位を厳守してください。

1. **正確性 (最優先)**
   論理的に発生し得ない事項を記述してはいけません。
   例: 整数型のみを扱う関数に「NaNを拒否する」と書くことは禁止。

2. **必要十分性**
   「読み手がこれを読むだけでテストを書くことができる」状態を目指します。
   ただし、型システムや言語仕様から自明な事項は省略してください。

3. **簡潔性**
   冗長な記述は避け、特筆すべき事項のみを書いてください。

#### 「包括的」の定義

包括的とは、「読み手がこれを読むだけでテストを書くことができるほどに
              意味論が閉じている」
という状態を指します。

これは「すべての項目を機械的に埋める」ことではありません。
**その関数の性質に照らして、テストで確認すべき振る舞いのみ**を記述してください。

#### 記述が必要な項目（関数の性質に応じて判断）

以下のチェックリストを参照し、**その関数において実際に関係がある項目のみ**
を記述してください。

##### 1. 入力の意味と制約（引数がある場合）

（拡張関数である場合） `@receiver`タグには、拡張対象となるオブジェクトの意味と、
その状態に関する制約を記述してください。
`@param`タグには、入力値の意味と制約、またそれが満たされない場合何が起こるかを
表記してください。

* null許容型の場合、nullが何を意味するか、何が起こるか
* 値の範囲; 境界外の場合何が起こるか
* 値の単位; できる限りIntやDoubleを使わず、単位型を使用してください。
  低レイヤーとの接続など、どうしても使わざるを得ない場合のみ明記
* 値が正規化されているべきか; 正規化されていない場合何が起こるか

##### 2. 出力の意味（戻り値がある場合）

`@return`タグには、戻り値の意味と制約を表記してください。

* 返す値が必ず満たす、型では表現されない条件
  * 0 < result (数値型の場合)
  * `String.isNotEmpty()` (文字列型の場合)
  * `List` is sorted (リストの場合)
* null許容型の場合、nullを返す条件
* コレクションの順序（順序が保証されている、ランダム、など）
* インスタンスが再利用されるか

##### 3. スローされる例外（例外を投げる可能性がある場合のみ）

KDocの@throwsを表記してください。

* 関数の中で呼ぶ関数やプロパティがある場合、必ずそれが例外をスローするか確認し、
  それのハンドリングを行わないなら@throws表記を伝播してください。
* 例外が起きる条件を明記してください。
* 存在するなら、例外メッセージの要件を記述してください。

##### 4. 副作用（副作用がある場合のみ）

以下のいずれかが該当する場合は、明示的に記述してください。

* I/O（ファイル、標準入出力）
* ネットワーク通信
* ログ出力（一時デバッグログを除く。API関数についてはログを出力しない設定を作成）
* キャッシュの更新
* グローバル状態の更新

**純粋関数（外部への影響がない）の場合は、このセクションを省略してください。**

##### 5. スレッドセーフ性（並行性が問題になる場合のみ）

以下のいずれかが該当する場合は、明示的に記述してください。

* どのスレッドから呼ぶことができるか（メインスレッド専用、など）
* 並行して呼ぶことができるか（複数スレッドから同時呼び出しが安全か）

**関数が状態を持たず、スレッドセーフであることが自明な場合は、このセクションを省略してください。**

#### エッジケースの記述（該当する型・ロジックがある場合のみ）

以下のチェックリストを参照し、**その関数の引数・戻り値の型や内部ロジックに基づいて、
実際にテストが必要なエッジケースのみ**を記述してください。

##### 境界値（数値・コレクション型を扱う場合）

* 負数の扱い（許容するか、拒否するか）
* 0の扱い（特別な意味があるか）
* 空コレクション・空配列の扱い
* 存在する型なら、Emptyオブジェクトの扱い
* 値の最小/最大
* 非常に大きい/小さい数によるオーバーフローの可能性
* 非常に長い文字列（パフォーマンスやバッファサイズに影響がある場合）

##### 特異値（浮動小数点・文字列を扱う場合）

* NaN（Double/Float型の場合）
* +Infinity、-Infinity（Double/Float型の場合）
* サロゲートペア（文字列を文字単位で扱う場合）
* 結合文字（文字列を文字単位で扱う場合）
* 改行が入った文字列（パーサーやバリデーション関数の場合）

##### スレッド・並行性（非同期処理やコルーチンを使用する場合）

* 並行呼び出しの挙動
* コルーチンのキャンセル時の挙動
* タイムアウト時の挙動
* 部分失敗（一部の処理が失敗した場合の挙動）

#### 記述してはいけない項目（ネガティブリスト）

以下に該当する記述は、ノイズとなるため**厳禁**です。

* その関数の型や引数から論理的に発生し得ない事項
  * 【禁止】`fun add(a: Int, b: Int)` に対して「NaNを拒否します」
* 型システムで既に保証されている事項の単なる繰り返し
  * 【禁止】`fun process(input: String)` に対して「inputはStringでなければなりません」
* 実装詳細
  * 【禁止】「この関数はHashMapを使う」
  * 【禁止】「この関数はキャッシュを利用する/更新する」

#### フォーマット

【重要】KDocはすべて**英語**で作成してください。日本語を使用してはいけません。
また、`#`によるヘッダー、`-`による箇条書きが使えるので、積極的に利用してください。
`**bold text**`、`*italic text*`などは利用できません。

`@param`と`@return`で記述した制約は、`@throws`での例外条件と整合している必要があります。
例えば、`@param`で、ある値を拒否すると書いた場合、対応する`@throws`が必要です。

以下は参考フォーマットです。**当てはまらない項目については、セクションごと省略してください。**
下記フォーマットにおいて句点がある文はピリオドをうち、句点がない文は**うちません**（重要）。
空行は一行あけます。

```kotlin
/*
 * 冒頭に1〜2行で三人称単数形の動詞で始まる要約を書きます。
 *
 * ## Description
 *
 * 関数の詳しい説明を3行以上で書きます。
 * ここでインスタンスが再利用されるかどうかなどを書きます。
 *
 * ## Thread safety
 *
 * 関数が複数スレッドから同時に実行されても安全かどうか、
 * 外部の同期が必要かどうかを書きます。
 *
 * ## Side effects
 *
 * - 関数がI/O、
 * - 通信、
 * - ミュータブルな状態を書き換える場合、何をするのか箇条書きで書きます。
 *
 * @receiver レシーバーの説明を書きます（拡張関数の場合のみ）。
 * - レシーバーが満たすべき性質、およびそれが満たされない時何が起きるか書きます
 * @param foo fooの説明を書きます。
 * Mapなどの汎用型はそれがどのように解釈されるかも書きます。
 * - 引数が満たすべき性質、およびそれが満たされない時何が起きるか書きます
 * @return 戻り値の説明を書きます。
 * - 戻り値が満たす型では表現されない性質を書きます
 * @throws IllegalArgumentException
 * - 例外を投げる時の状況を箇条書きで書きます
 */
```

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
 *
 * @param sourcePath The path to the local file to be uploaded.
 * - Must point to an existing, readable regular file.
 * - Empty files are permitted but will result in a zero-byte remote object
 *
 * @param targetPath The destination path on the remote storage.
 * - Must follow the remote naming convention (max 255 characters, no control chars).
 * - If a file already exists at this path, it will be overwritten
 *
 * @return A summary of the upload operation including the final checksum and duration.
 * - The returned object is a fresh instance and can be modified by the caller
 *
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

### ステップ3: テストを作成

ステップ1（シグネチャ）とステップ2（KDoc）だけを根拠に、テストを書きます。
この時点で実装は存在しない（TODOのまま）前提です。

**注意**

* テストは、KDocに書かれていることをそのまま検証してください
  * KDocに書いていないことをテストしてはいけません（仕様の捏造になるため）
  * KDocに必要な情報が足りずテストが書けない場合、KDocに戻って補完してください
* 例外の仕様をテストする場合、例外の型だけでなく「投げる条件」をテストしてください
  * 例外メッセージに要件がある場合のみ、メッセージもテストしてください

#### テストの書き方

テストフレームワークはKotestを使用します。
存在しないなら、バージョン6.0.0を導入してください。

* 最優先で、まずは`FunSpec`を使用してください。
  * `context("...") { test("...") { ... } }`のように、ネストしたスコープで仕様を整理してよい
* ターゲット（例: JS）や環境の制約でネストしたスコープが利用できない場合は、`StringSpec`へフォールバックしてください。
  * テストはフラットに列挙し、グルーピングはテスト名で表現してください

#### テストケースの作り方

次の観点から、**必要なものだけ**をテストケース化します。

* 代表値
  * 典型的な入力で、期待した出力になること
* 境界値・空（該当する場合のみ）
  * 0、最小/最大、空文字、空リスト、空マップなど
* 例外
  * KDocの`@throws`に書かれている条件で例外になること
  * `@param`で「拒否する」と書いた入力が拒否されること
* 順序・同一性（該当する場合のみ）
  * 返すコレクションの順序が仕様として重要なら、それを検証すること
  * 「新しいインスタンスを返す」「再利用する」などが仕様なら、それを検証すること
* 副作用（該当する場合のみ）
  * キャッシュ更新やI/Oなど、観測できる事実として確認できること

#### テストの粒度

* まずはユニットテストで、関数の意味論（入力→出力、または例外）を固定します
* 外部I/Oや時刻などが関わる場合、DI可能にしてテスト可能な境界を作ります
  * 乱数は`rand: Random = Random.Default`
  * 時刻は`clock: Clock = Clock.System`
  * I/Oは`fs: FileSystem = FileSystem.SYSTEM`

### ステップ4: Red（テスト失敗）を確認

テストを書いたら、必ず「意図した理由で落ちている」ことを確認します。

* 少なくとも1つのテストが失敗すること
* 失敗の原因が、未実装（TODO）であること、もしくは期待した例外であること
* テストの誤り（期待値の間違い、テストの書き方の誤り）で落ちていないこと

**重要**

* テストが通ってしまった場合、何かがおかしいです
  * テストが対象関数を呼んでいない
  * アサーションがない
  * そもそも実装が既に存在していて未検証
  などを疑い、原因を潰してから次へ進んでください

### ステップ5: 実装を作成

テストを通すために実装を書きます。

**注意**

* KDocは仕様です。実装はKDocに従ってください
  * 実装の都合で仕様を変えたくなったら、KDocとテストを先に更新します
* 入力制約がある場合、最初に`validateArgs`で明示的に検査します
  * `validateArgs`は`net.japanesehunter.util`にあります
  * 例外の型と条件はKDocと一致させること
  * 例外メッセージに要件がある場合のみ、メッセージも要件を満たすこと
* 状態や副作用がある場合、観測可能な結果（戻り値、例外、永続化された変更）として
  テストできる形に寄せます

### ステップ6: Green（テスト成功）を確認

テストがすべて成功することを確認します。

* 追加したテストがすべて通ること
* 例外テストが「投げるべき条件でのみ」成功していること
* 境界値・空入力など、追加したエッジケースが通ること

**注意**

* 偶然通っていないか疑ってください
  * アサーションが弱すぎないか
  * 例外をcatchして握りつぶしていないか
  * 副作用の検証が観測できているか

### ステップ7: レビューとリファクタリング

最後に、仕様（KDoc）、テスト、実装の整合性をレビューし、必要最小限のリファクタを行います。

#### レビュー観点

* KDocと実装が一致していること
  * `@param`/`@return`/`@throws`の意味論に矛盾がないこと
  * 記述してはいけない項目（実装詳細、論理的に起きないこと）を書いていないこと
* テストがKDocの要求を過不足なく検証していること
  * KDocにない仕様を勝手に固定していないこと
  * 重要なエッジケースを落としていないこと
* 実装が読みやすく、責務が単純であること
  * 命名が一時オブジェクトっぽくないこと
  * 不必要な可変状態がないこと
  * 早期return、ガード節などで分岐が整理されていること

#### リファクタリングのルール

* 仕様を変えるリファクタは禁止（テストが守っていることが前提）
* 仕様を変える必要が出たら、必ず「KDoc→テスト→実装」の順で更新します
* テストが読みづらい場合、テストもプロダクトコードと同様にリファクタしてよいです
  * ただし、アサーションを弱める変更は禁止です
