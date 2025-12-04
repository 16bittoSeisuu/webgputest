@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

import org.khronos.webgl.ArrayBuffer
import kotlin.js.Promise

/**
 * Represents a block of GPU-visible memory allocated by [GPUDevice.createBuffer].
 *
 * A buffer's lifetime is controlled through [mapAsync], [getMappedRange], and [unmap] when
 * CPU access is required, and through [destroy] when the memory is no longer needed.
 * Buffers are always created with an immutable [size] and set of [usage] flags.
 */
external interface GPUBuffer : GPUObjectBase {
  /**
   * Optional developer-assigned label surfaced by browser tooling to help debug GPU workloads.
   */
  override var label: String

  /**
   * Byte length of the buffer as declared at creation time.
   *
   * The value is fixed for the lifetime of the buffer and caps the valid ranges for
   * [mapAsync] and [getMappedRange].
   */
  val size: Int

  /**
   * Bitwise combination of [GPUBufferUsage] flags describing how the buffer may be used.
   *
   * Mapping requires the corresponding `MAP_READ` or `MAP_WRITE` flag to have been set.
   * Attempting to use the buffer with an unsupported operation results in a validation error
   * surfaced as an `OperationError` in JavaScript.
   */
  val usage: GPUBufferUsage

  /**
   * Current mapping state of the buffer: `unmapped`, `pending` (waiting on [mapAsync]),
   * or `mapped` (CPU-visible via [getMappedRange]).
   */
  val mapState: GPUBufferMapState

  /**
   * Requests CPU access to a subrange of the buffer, resolving once the mapping is ready.
   *
   * @param mode Specifies read or write intent. Must match the corresponding `MAP_READ`
   * or `MAP_WRITE` flag set in [usage].
   * @param offset Byte offset of the range to map. Must be a multiple of 8, >= 0,
   * and <= `GPUBuffer.size`. Defaults to 0.
   * @param size Number of bytes to map. Must be a multiple of 4, >= 0, and the sum
   * of `offset + size` must not exceed `GPUBuffer.size`. Defaults to `GPUBuffer.size - offset`.
   * @return A [Promise] that resolves when the buffer transitions to `mapped`.
   * @throws Throwable An `OperationError` is thrown in JavaScript if the buffer is already
   * mapped, destroyed, lacks the required usage flags, or if the requested range is invalid.
   */
  fun mapAsync(
    mode: GPUMapMode,
    offset: Int = definedExternally,
    size: Int = definedExternally,
  ): Promise<Unit>

  /**
   * Returns an [ArrayBuffer] view of the buffer's currently mapped contents.
   *
   * The buffer must already be mapped (for read or write) via [mapAsync]; otherwise an
   * `OperationError` is thrown by the underlying WebGPU implementation. The returned
   * `ArrayBuffer` aliases GPU-visible memory; it does not copy. The view becomes detached
   * and unusable after [unmap].
   *
   * @param offset Byte offset of the mapped range to expose. Must be a multiple of 8, >= 0,
   * and <= `GPUBuffer.size`. Defaults to 0.
   * @param size Number of bytes to expose. Must be a multiple of 4, >= 0, and the sum of
   * `offset + size` must not exceed `GPUBuffer.size`. Defaults to `GPUBuffer.size - offset`.
   * @return An [ArrayBuffer] referencing the requested subrange. Multiple calls with
   * overlapping ranges return views into the same mapped memory.
   * @throws Throwable An `OperationError` is thrown in JavaScript if the buffer is not in
   * the `mapped` state, if mapping has not completed, if the buffer is destroyed, or if the
   * requested range violates alignment or bounds.
   */
  fun getMappedRange(
    offset: Int = definedExternally,
    size: Int = definedExternally,
  ): ArrayBuffer

  /**
   * Completes a pending mapping, commits any writes to the GPU-visible buffer, and
   * returns the buffer to the `unmapped` state.
   *
   * Any [ArrayBuffer] instances returned by [getMappedRange] become detached and
   * unusable after this call.
   */
  fun unmap()

  /**
   * Immediately releases the GPU memory owned by this buffer.
   *
   * The buffer transitions to the destroyed state; it cannot be mapped, bound, or queued
   * for future operations. It is safe to call multiple times.
   */
  fun destroy()
}
