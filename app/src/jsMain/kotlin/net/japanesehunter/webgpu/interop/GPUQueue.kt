package net.japanesehunter.webgpu.interop

import org.khronos.webgl.ArrayBuffer

external interface GPUQueue {
  fun submit(commandBuffers: Array<GPUCommandBuffer>)

  fun writeBuffer(
    buffer: GPUBuffer,
    bufferOffset: Int,
    data: ArrayBuffer,
    dataOffset: Int = definedExternally,
    size: Int = definedExternally,
  )
}
