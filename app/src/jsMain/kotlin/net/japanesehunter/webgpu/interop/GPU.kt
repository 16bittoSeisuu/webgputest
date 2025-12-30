@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPU {
  fun requestAdapter(
    options: dynamic = definedExternally,
  ): Promise<GPUAdapter?>

  fun getPreferredCanvasFormat(): GPUTextureFormat
}
