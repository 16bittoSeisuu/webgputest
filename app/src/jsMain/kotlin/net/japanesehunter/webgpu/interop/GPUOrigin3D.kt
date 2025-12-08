@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUOrigin3D(
  x: Int? = null,
  y: Int? = null,
  z: Int? = null,
): GPUOrigin3D =
  js("{}").unsafeCast<GPUOrigin3D>().apply {
    if (x != null) this.x = x
    if (y != null) this.y = y
    if (z != null) this.z = z
  }

external interface GPUOrigin3D {
  var x: Int?
  var y: Int?
  var z: Int?
}
