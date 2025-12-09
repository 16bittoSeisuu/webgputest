package net.japanesehunter.webgpu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import net.japanesehunter.webgpu.interop.GPUColorTargetState
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUFragmentState
import net.japanesehunter.webgpu.interop.GPUMultisampleState
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPURenderPipelineDescriptor
import net.japanesehunter.webgpu.interop.GPUShaderModuleDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUVertexBufferLayout
import net.japanesehunter.webgpu.interop.GPUVertexState
import kotlin.js.Promise

fun GPUDevice.createShaderCompiler(surfaceFormat: GPUTextureFormat): ShaderCompiler =
  ShaderCompilerImpl(
    device = this,
    surfaceFormat = surfaceFormat,
  )

interface ShaderCompiler {
  context(coroutine: CoroutineScope)
  fun compile(
    vertexCode: String,
    vertexAttributes: List<GPUVertexBufferLayout> = emptyList(),
    fragmentCode: String? = null,
    sampleCount: Int? = null,
    label: String? = null,
  ): Deferred<GPURenderPipeline>
}

private class ShaderCompilerImpl(
  private val device: GPUDevice,
  private val surfaceFormat: GPUTextureFormat,
) : ShaderCompiler {
  context(coroutine: CoroutineScope)
  override fun compile(
    vertexCode: String,
    vertexAttributes: List<GPUVertexBufferLayout>,
    fragmentCode: String?,
    sampleCount: Int?,
    label: String?,
  ): Deferred<GPURenderPipeline> {
    val vertexModule =
      device.createShaderModule(
        GPUShaderModuleDescriptor(
          code = vertexCode,
          label = label?.let { "$label-vertex-shader" },
        ),
      )
    val fragmentModule =
      fragmentCode?.let {
        device.createShaderModule(
          GPUShaderModuleDescriptor(
            code = it,
            label = label?.let { "$label-fragment-shader" },
          ),
        )
      } ?: vertexModule
    val vertexState = GPUVertexState(vertexModule, vertexAttributes.toTypedArray())
    val fragmentState =
      GPUFragmentState(
        module = fragmentModule,
        targets = arrayOf(GPUColorTargetState(format = surfaceFormat)),
      )
    return device
      .createRenderPipelineAsync(
        GPURenderPipelineDescriptor(
          vertex = vertexState,
          fragment = fragmentState,
          multisample =
            sampleCount?.let {
              GPUMultisampleState(sampleCount)
            },
        ),
      ).toDeferred()
  }
}

context(coroutine: CoroutineScope)
private fun <T> Promise<T>.toDeferred(): Deferred<T> = coroutine.async { await() }
