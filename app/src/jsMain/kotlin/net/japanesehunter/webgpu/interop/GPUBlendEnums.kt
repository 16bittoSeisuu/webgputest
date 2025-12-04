package net.japanesehunter.webgpu.interop

value class GPUBlendOperation private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Add = GPUBlendOperation("add")
    val Subtract = GPUBlendOperation("subtract")
    val ReverseSubtract = GPUBlendOperation("reverse-subtract")
    val Min = GPUBlendOperation("min")
    val Max = GPUBlendOperation("max")
  }
}

value class GPUBlendFactor private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Zero = GPUBlendFactor("zero")
    val One = GPUBlendFactor("one")
    val Src = GPUBlendFactor("src")
    val OneMinusSrc = GPUBlendFactor("one-minus-src")
    val SrcAlpha = GPUBlendFactor("src-alpha")
    val OneMinusSrcAlpha = GPUBlendFactor("one-minus-src-alpha")
    val Dst = GPUBlendFactor("dst")
    val OneMinusDst = GPUBlendFactor("one-minus-dst")
    val DstAlpha = GPUBlendFactor("dst-alpha")
    val OneMinusDstAlpha = GPUBlendFactor("one-minus-dst-alpha")
    val SrcAlphaSaturated = GPUBlendFactor("src-alpha-saturated")
    val Constant = GPUBlendFactor("constant")
    val OneMinusConstant = GPUBlendFactor("one-minus-constant")
    val Src1 = GPUBlendFactor("src1")
    val OneMinusSrc1 = GPUBlendFactor("one-minus-src1")
    val Src1Alpha = GPUBlendFactor("src1-alpha")
    val OneMinusSrc1Alpha = GPUBlendFactor("one-minus-src1-alpha")
  }
}
