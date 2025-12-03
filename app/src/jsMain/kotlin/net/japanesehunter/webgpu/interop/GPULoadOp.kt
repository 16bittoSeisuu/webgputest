package net.japanesehunter.webgpu.interop

value class GPULoadOp private constructor(
  private val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Load: GPULoadOp = GPULoadOp("load")
    val Clear: GPULoadOp = GPULoadOp("clear")
  }
}
