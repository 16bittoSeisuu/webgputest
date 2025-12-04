@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUVertexBufferLayout(
  arrayStride: Int,
  stepMode: GPUVertexStepMode? = null,
  attributes: Array<GPUVertexAttribute>,
): GPUVertexBufferLayout =
  js("{}").unsafeCast<GPUVertexBufferLayout>().apply {
    this.arrayStride = arrayStride
    if (stepMode != null) this.stepMode = stepMode
    this.attributes = attributes
  }

external interface GPUVertexBufferLayout {
  var arrayStride: Int
  var stepMode: GPUVertexStepMode
  var attributes: Array<GPUVertexAttribute>
}
