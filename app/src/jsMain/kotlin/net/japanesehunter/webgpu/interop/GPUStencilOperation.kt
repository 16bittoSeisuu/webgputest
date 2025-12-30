package net.japanesehunter.webgpu.interop

value class GPUStencilOperation private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Keep = GPUStencilOperation("keep")
    val Zero = GPUStencilOperation("zero")
    val Replace = GPUStencilOperation("replace")
    val Invert = GPUStencilOperation("invert")
    val IncrementClamp = GPUStencilOperation("increment-clamp")
    val DecrementClamp = GPUStencilOperation("decrement-clamp")
    val IncrementWrap = GPUStencilOperation("increment-wrap")
    val DecrementWrap = GPUStencilOperation("decrement-wrap")
  }
}
