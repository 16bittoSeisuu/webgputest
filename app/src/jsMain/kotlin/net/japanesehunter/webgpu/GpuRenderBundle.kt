package net.japanesehunter.webgpu

import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUBindGroup
import net.japanesehunter.webgpu.interop.GPUBindGroupDescriptor
import net.japanesehunter.webgpu.interop.GPUBindGroupEntry
import net.japanesehunter.webgpu.interop.GPUBindGroupLayout
import net.japanesehunter.webgpu.interop.GPUBindingResource
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

fun GPURenderBundleEncoder.setBindGroup(bindGroups: List<GPUBindGroup>) {
  bindGroups.forEachIndexed { index, bindGroup ->
    setBindGroup(
      index,
      bindGroup,
    )
  }
}

context(device: GPUDevice)
fun GPURenderBundleEncoder.setBindGroup(
  entries: List<List<GPUBindingResource>>,
  layout: (layoutIndex: Int) -> GPUBindGroupLayout,
) {
  entries.forEachIndexed { index, resourceList ->
    val bindGroup =
      device.createBindGroup(
        GPUBindGroupDescriptor(
          layout = layout(index),
          entries =
            resourceList
              .mapIndexed { entryIndex, resource ->
                GPUBindGroupEntry(
                  binding = entryIndex,
                  resource = resource,
                )
              }.toTypedArray(),
        ),
      )
    setBindGroup(
      index,
      bindGroup,
    )
  }
}

fun GPURenderBundleEncoder.drawIndexed(
  indexBuffer: IndexGpuBuffer,
  instanceCount: Int = 1,
) {
  setIndexBuffer(
    indexBuffer.raw,
    indexBuffer.indexFormat,
    indexBuffer.offset,
    indexBuffer.size,
  )
  drawIndexed(
    indexCount = indexBuffer.indexCount,
    instanceCount = instanceCount,
  )
}
