package net.japanesehunter.webgpu.interop

value class GPUStoreOp private constructor(val value: String) {
  companion object {
    val Store: GPUStoreOp = GPUStoreOp("store")
    val Discard: GPUStoreOp = GPUStoreOp("discard")
  }
}
