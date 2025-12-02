package net.japanesehunter.webgpu.interop

fun GPUColor(
  r: Double,
  g: Double,
  b: Double,
  a: Double,
): GPUColor =
  {}.unsafeCast<GPUColor>().apply {
    this.r = r
    this.g = g
    this.b = b
    this.a = a
  }

external interface GPUColor {
  var r: Double
  var g: Double
  var b: Double
  var a: Double
}
