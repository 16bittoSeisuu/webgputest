package net.japanesehunter.webgpu.interop

value class PredefinedColorSpace private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Srgb = PredefinedColorSpace("srgb")
    val DisplayP3 = PredefinedColorSpace("display-p3")
  }
}

value class GPUCanvasAlphaMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Opaque = GPUCanvasAlphaMode("opaque")
    val Premultiplied = GPUCanvasAlphaMode("premultiplied")
  }
}

value class GPUCanvasToneMappingMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Standard = GPUCanvasToneMappingMode("standard")
    val Extended = GPUCanvasToneMappingMode("extended")
  }
}
