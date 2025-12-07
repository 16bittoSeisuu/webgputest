package net.japanesehunter.webgpu.interop

external fun requestAnimationFrame(callback: (timestamp: Double) -> Unit): Int

external fun setTimeout(
  callback: () -> Unit,
  delay: Double,
)
