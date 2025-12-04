@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

/**
 * Configures a render bundle encoder created via [GPUDevice.createRenderBundleEncoder].
 *
 * Render bundles must be compatible with the render passes that execute them. The
 * attachment formats, sample count, and read-only depth/stencil state recorded here
 * must exactly match the render pass descriptor used later with [GPURenderPassEncoder].
 *
 * @param colorFormats Color attachment formats the bundle will target. Required.
 * @param depthStencilFormat Depth/stencil format expected by the bundle; omit if unused.
 * @param sampleCount Multisample count; must match the render pass. Defaults to 1.
 * @param depthReadOnly Whether depth is read-only inside the bundle. Defaults to false.
 * @param stencilReadOnly Whether stencil is read-only inside the bundle. Defaults to false.
 * @param label Optional developer label shown in tooling.
 */
fun GPURenderBundleEncoderDescriptor(
  colorFormats: Array<GPUTextureFormat>,
  depthStencilFormat: GPUTextureFormat? = null,
  sampleCount: Int? = null,
  depthReadOnly: Boolean? = null,
  stencilReadOnly: Boolean? = null,
  label: String? = null,
): GPURenderBundleEncoderDescriptor =
  js("{}").unsafeCast<GPURenderBundleEncoderDescriptor>().apply {
    this.colorFormats = colorFormats
    if (depthStencilFormat != null) this.depthStencilFormat = depthStencilFormat
    if (sampleCount != null) this.sampleCount = sampleCount
    if (depthReadOnly != null) this.depthReadOnly = depthReadOnly
    if (stencilReadOnly != null) this.stencilReadOnly = stencilReadOnly
    if (label != null) this.label = label
  }

external interface GPURenderBundleEncoderDescriptor {
  /**
   * Formats of the color attachments the bundle will render to.
   *
   * Must match the `colorAttachments[i].view` formats supplied to the render passes
   * that execute the finished bundle.
   */
  var colorFormats: Array<GPUTextureFormat>

  /**
   * Optional depth/stencil attachment format expected by commands in the bundle.
   *
   * Omit for bundles that do not touch depth/stencil state.
   */
  var depthStencilFormat: GPUTextureFormat?

  /**
   * Multisample count expected by draw calls within the bundle. Defaults to 1.
   */
  var sampleCount: Int

  /**
   * Whether depth is treated as read-only within the bundle. Defaults to false.
   */
  var depthReadOnly: Boolean

  /**
   * Whether stencil is treated as read-only within the bundle. Defaults to false.
   */
  var stencilReadOnly: Boolean

  /**
   * Optional developer-assigned label surfaced by browser tooling.
   */
  var label: String?
}
