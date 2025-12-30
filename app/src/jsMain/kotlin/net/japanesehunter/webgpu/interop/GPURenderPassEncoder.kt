@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external interface GPURenderPassEncoder : GPUObjectBase {
  override var label: String

  fun setPipeline(
    pipeline: GPURenderPipeline,
  )

  fun setVertexBuffer(
    slot: Int,
    buffer: GPUBuffer,
    offset: Int = definedExternally,
    size: Int = definedExternally,
  )

  fun setIndexBuffer(
    buffer: GPUBuffer,
    indexFormat: GPUIndexFormat, // "uint16" | "uint32"
    offset: Int = definedExternally,
    size: Int = definedExternally,
  )

  fun setBindGroup(
    index: Int,
    bindGroup: GPUBindGroup,
    dynamicOffsets: Array<Int> = definedExternally,
    dynamicOffsetsDataStart: Int = definedExternally,
    dynamicOffsetsDataLength: Int = definedExternally,
  )

  fun draw(
    vertexCount: Int,
    instanceCount: Int = definedExternally,
    firstVertex: Int = definedExternally,
    firstInstance: Int = definedExternally,
  )

  fun drawIndexed(
    indexCount: Int,
    instanceCount: Int = definedExternally,
    firstIndex: Int = definedExternally,
    baseVertex: Int = definedExternally,
    firstInstance: Int = definedExternally,
  )

  fun drawIndirect(
    indirectBuffer: GPUBuffer,
    indirectOffset: Int,
  )

  fun drawIndexedIndirect(
    indirectBuffer: GPUBuffer,
    indirectOffset: Int,
  )

  fun executeBundles(
    bundles: Array<GPURenderBundle>,
  )

  fun end()
}
