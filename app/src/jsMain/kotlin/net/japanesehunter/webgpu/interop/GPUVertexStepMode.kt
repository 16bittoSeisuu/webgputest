package net.japanesehunter.webgpu.interop

value class GPUVertexStepMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Vertex = GPUVertexStepMode("vertex")
    val Instance = GPUVertexStepMode("instance")
  }
}
