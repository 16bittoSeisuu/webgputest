package net.japanesehunter.webgpu

import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPURenderBundle
import net.japanesehunter.webgpu.interop.GPURenderBundleDescriptor
import net.japanesehunter.webgpu.interop.GPURenderBundleEncoder
import net.japanesehunter.webgpu.interop.GPURenderBundleEncoderDescriptor

context(device: GPUDevice, gpu: GPU)
inline fun recordRenderBundle(
  label: String? = null,
  action: GPURenderBundleEncoder.() -> Unit,
): GPURenderBundle =
  device
    .createRenderBundleEncoder(
      GPURenderBundleEncoderDescriptor(
        colorFormats = arrayOf(gpu.getPreferredCanvasFormat()),
      ),
    ).run {
      action()
      finish(GPURenderBundleDescriptor(label = label))
    }

fun GPURenderBundleEncoder.setVertexBuffer(vertexBuffers: List<VertexGpuBuffer>) {
  vertexBuffers.forEachIndexed { index, vertexBuffer ->
    setVertexBuffer(
      index,
      vertexBuffer.raw,
      vertexBuffer.offset,
      vertexBuffer.size,
    )
  }
}

fun GPURenderBundleEncoder.drawIndexed(indexBuffer: IndexGpuBuffer) {
  setIndexBuffer(
    indexBuffer.raw,
    indexBuffer.indexFormat,
    indexBuffer.offset,
    indexBuffer.size,
  )
  drawIndexed(indexCount = indexBuffer.indexCount)
}
