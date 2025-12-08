@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUExtent3D(
  width: Int,
  height: Int,
  depthOrArrayLayers: Int = 1,
): GPUExtent3D =
  js("{}").unsafeCast<GPUExtent3D>().apply {
    this.width = width
    this.height = height
    this.depthOrArrayLayers = depthOrArrayLayers
  }

external interface GPUExtent3D {
  var width: Int
  var height: Int
  var depthOrArrayLayers: Int
}
