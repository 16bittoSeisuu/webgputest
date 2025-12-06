@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

/**
 * Records render commands for reuse inside multiple render passes.
 *
 * Created via [GPUDevice.createRenderBundleEncoder], this encoder captures fixed render
 * state (pipeline, vertex/index buffers, and optional indirect draw calls) into an
 * immutable [GPURenderBundle]. Bundles are played back later through
 * [GPURenderPassEncoder.executeBundles], reducing CPU overhead when submitting similar
 * sequences of draws repeatedly.
 */
external interface GPURenderBundleEncoder : GPUObjectBase {
  /**
   * Optional developer-assigned label surfaced by browser tooling while encoding.
   */
  override var label: String

  /**
   * Associates a render pipeline with subsequent draw calls recorded in the bundle.
   */
  fun setPipeline(pipeline: GPURenderPipeline)

  /**
   * Binds a pre-created group of resources to a shader slot.
   *
   * Dynamic offsets allow overriding buffer binding offsets at execution time.
   *
   * @param index Bind group slot to target.
   * @param bindGroup Collection of buffers, textures, and samplers to attach.
   * @param dynamicOffsets Optional sequence of dynamic offsets applied to buffers.
   * @param dynamicOffsetsDataStart Starting element when reading from a typed array.
   * @param dynamicOffsetsDataLength Number of elements to read from a typed array.
   */
  fun setBindGroup(
    index: Int,
    bindGroup: GPUBindGroup,
    dynamicOffsets: Array<Int> = definedExternally,
    dynamicOffsetsDataStart: Int = definedExternally,
    dynamicOffsetsDataLength: Int = definedExternally,
  )

  /**
   * Binds a vertex buffer slot for use by upcoming draw calls.
   *
   * @param slot Vertex buffer slot index in the current pipeline layout.
   * @param buffer Source buffer containing vertex attributes.
   * @param offset Byte offset to the first vertex element in [buffer]. Defaults to 0.
   * @param size Byte length of the bound range. Defaults to `buffer.size - offset`.
   */
  fun setVertexBuffer(
    slot: Int,
    buffer: GPUBuffer,
    offset: Int = definedExternally,
    size: Int = definedExternally,
  )

  /**
   * Binds the index buffer used by [drawIndexed] and [drawIndexedIndirect].
   *
   * @param buffer Buffer containing index data.
   * @param indexFormat Format of individual indices.
   * @param offset Byte offset to the first index in [buffer]. Defaults to 0.
   * @param size Byte length of the bound range. Defaults to `buffer.size - offset`.
   */
  fun setIndexBuffer(
    buffer: GPUBuffer,
    indexFormat: GPUIndexFormat,
    offset: Int = definedExternally,
    size: Int = definedExternally,
  )

  /**
   * Records a non-indexed draw call into the bundle.
   */
  fun draw(
    vertexCount: Int,
    instanceCount: Int = definedExternally,
    firstVertex: Int = definedExternally,
    firstInstance: Int = definedExternally,
  )

  /**
   * Records an indexed draw call into the bundle.
   */
  fun drawIndexed(
    indexCount: Int,
    instanceCount: Int = definedExternally,
    firstIndex: Int = definedExternally,
    baseVertex: Int = definedExternally,
    firstInstance: Int = definedExternally,
  )

  /**
   * Records an indirect draw whose arguments are sourced from [indirectBuffer].
   */
  fun drawIndirect(
    indirectBuffer: GPUBuffer,
    indirectOffset: Int,
  )

  /**
   * Records an indexed indirect draw whose arguments are sourced from [indirectBuffer].
   */
  fun drawIndexedIndirect(
    indirectBuffer: GPUBuffer,
    indirectOffset: Int,
  )

  /**
   * Finalizes encoding and produces an immutable [GPURenderBundle].
   *
   * After calling this, the encoder becomes invalid; further calls throw validation
   * errors in JavaScript. The returned bundle can be executed multiple times in future
   * render passes as long as the target formats and sample count match.
   *
   * @param descriptor Optional label for the resulting bundle.
   * @return A pre-recorded bundle of render commands ready for submission.
   */
  fun finish(descriptor: GPURenderBundleDescriptor = definedExternally): GPURenderBundle
}
