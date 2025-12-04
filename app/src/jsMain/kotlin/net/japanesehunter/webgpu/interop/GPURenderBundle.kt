package net.japanesehunter.webgpu.interop

/**
 * Immutable collection of render commands encoded by [GPURenderBundleEncoder.finish].
 *
 * Render bundles can be replayed multiple times within compatible render passes via
 * [GPURenderPassEncoder.executeBundles], reducing validation and CPU cost for
 * repeated drawing sequences.
 */
external interface GPURenderBundle : GPUObjectBase {
  /**
   * Optional developer-assigned label surfaced by browser tooling.
   */
  override var label: String
}
