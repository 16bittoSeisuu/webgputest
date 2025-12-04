package net.japanesehunter.webgpu.interop

value class GPUTextureAspect private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val All = GPUTextureAspect("all")
    val StencilOnly = GPUTextureAspect("stencil-only")
    val DepthOnly = GPUTextureAspect("depth-only")
  }
}
