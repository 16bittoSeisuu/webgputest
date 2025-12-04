package net.japanesehunter.webgpu.interop

value class GPUCompareFunction private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Never = GPUCompareFunction("never")
    val Less = GPUCompareFunction("less")
    val Equal = GPUCompareFunction("equal")
    val LessEqual = GPUCompareFunction("less-equal")
    val Greater = GPUCompareFunction("greater")
    val NotEqual = GPUCompareFunction("not-equal")
    val GreaterEqual = GPUCompareFunction("greater-equal")
    val Always = GPUCompareFunction("always")
  }
}
