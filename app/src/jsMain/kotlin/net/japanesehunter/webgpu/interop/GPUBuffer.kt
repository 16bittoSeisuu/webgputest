@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

import org.khronos.webgl.ArrayBuffer
import kotlin.js.Promise

external interface GPUBuffer : GPUObjectBase {
  override var label: String
  val size: Int
  val usage: Int
  val mapState: GPUBufferMapState

  fun mapAsync(
    mode: Int,
    offset: Int = definedExternally,
    size: Int = definedExternally,
  ): Promise<Unit>

  fun getMappedRange(
    offset: Int = definedExternally,
    size: Int = definedExternally,
  ): ArrayBuffer

  fun unmap()

  fun destroy()
}
