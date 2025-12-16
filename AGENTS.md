# Agent Guideline

## Coding Rules

- Follow .editorconfig
- Strive for code which is readable even without comments;
  do not write unnecessary comments. Only write them when the
  context of the code is not obvious, such as with bitwise operations
  or when a protocol is defined.
- Make sure to write KDocs.
- When treating a Double as a finite value, always validate it with isFinite.

## KDoc Rules

KDoc is a top-down specification document, not an implementation note.

Documentation must be readable sequentially from top to bottom, with
information ordered as:

Overview -> Constraints -> Additional specification

Descriptions must read as natural prose. Mechanical templates,
checklist-style phrasing, or sentence fragments should be avoided,
especially colons are prohibited.

### Scopes

Only applies to `public` things.
It is not required to write KDocs to `private`, `protected`, `internal`,
or `override` methods.

### Interfaces

Must not describe concrete behavior tied to a specific implementation.

#### Format

1. Opening summary

- If the interface represents a domain concept or a value, begin with
    `Represents ...`
- If it represents behavior or a role, begin with a third-person
    singular verb.

2. Semantics and contract

- Describe what the interface guarantees to callers.
- Describe what implementers are required to uphold.
- State any behavioral expectation that apply across all methods as a whole.

3. Thread-safety expectations

- Clearly state whether implementations are required to be thread-safe,
  conditionally thread-safe, or not thread-safe.

#### Must not include

- Method-level or property-level specifications.
- Implementation-specific state or algorithms.
- Caching, pooling, reuse, or lifecycle strategies.

### Classes

Describes the *concrete behavior, constraints, and guarantees* of a specific
implementation.

#### Format

1. Opening summary

- FOllow the same rules as interface summaries.

2. Primary constructor parameters

- Primary constructor parameters may be documented using
  `@param` entries in the class KDoc.
- Any `range` or `null` constraints must be expressed using the standard
  `@param` specification format.

3. Invariants

- Describe conditions that always hold after construction completes
  successfully.
- These invariants must remain true for the entire observable lifetime
  of the instance.

4. Thread-safety

- Describe the thread-safety characteristics of the class as a whole.
- Explanations should be comparable in granularity to standard library
  types such as `HashMap` or `ConcurrentHashMap`.

#### Must not include

- Member-level documentation.
- Method-level synchronization or concurrency details.
- Instance reuse, pooling, or caching behavior.

### Functions

#### Format

1. Opening summary

- Must begin with a third-person singular verb.
- The sentence must describe the observable effect of the function.

2. Time complexity

- If time complexity can grow quadratically or worse with respect to input
  size, describe the complexity.
- Constant or linear behavior does not need to be mentioned.

3. Side effects

- Describe any observable side effects.
- If side effects are clearly implied by the function name, this
  section may be omitted.
  When omitted, the following report must be provided at the end of the task:

  ```
  Side effects omitted:
  - reason: implied by function name
  - function: <function name>
  ```

4. Parameters

- For `Float` or `Double`, specify behavior for the following when relevant:
  - `NaN`
  - `Infinity`
- For `String` when relevant:
  - `""`

```kotlin
@param count the number of items.
  range: 1 <= count <= 100
```

5. Return value

- Required unless the return type is `Unit` or `Nothing`.

```kotlin
@return the calculated value.
  null: return when no result is produced
```

6. Exceptions

- All possible thrown exceptions must be documented using `@throws`.
- Fatal errors such as `OutOfMemoryError` are excluded.

### Properties

#### Format

1. Meaning of the value

- Describe what the value represents conceptually.

2. Constraints or invariants

- State any valid ranges, consistency requirements.

3. Mutation rules

- *Required only for mutable properties*
- Describe when, how, and under what conditions the value may change.

### Constants

#### Format

1. Meaning of the value
2. Unit or scale, can be omitted if can be implied from type

### @param and @return rules

#### Description line

- Must be a noun phrase.
- Must not start with a verb.
- Must end with a period.

#### Specification lines

- `range`: Specify the parameter/return value allowed range
- `null`: What `null` in the context means
- `NaN`: What `NaN` in the context means
- `Infinity`: What `Infinity` in the context means
- Must not end with a period.
- Exactly one specification per line.

## Language

Use *gentle and kind* Japanese when:

- Chatting with the user
- Showing the current coding plan

Use English when:

- Naming variables
- Writing comments
- Documenting

Focus on kindness, not cleverness.
Do not repeat bullet points, use language flows naturally.
However, make sure to take advantage of markdown notation.
Try not to use parenthesis, colons in the response.

## Final Check

Whenever you make changes, always run

```bash
./gradlew check
cd app/src && ktlint -F
```

## git

Ignore gradlew.bat changes, and don't mention it in the response.
