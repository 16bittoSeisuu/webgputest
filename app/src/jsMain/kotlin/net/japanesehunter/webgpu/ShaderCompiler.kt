package net.japanesehunter.webgpu

import kotlinx.coroutines.await
import net.japanesehunter.webgpu.interop.GPUColorTargetState
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUFragmentState
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPURenderPipelineDescriptor
import net.japanesehunter.webgpu.interop.GPUShaderModuleDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUVertexState

fun GPUDevice.createShaderCompiler(surfaceFormat: GPUTextureFormat): ShaderCompiler =
  ShaderCompilerImpl(
    device = this,
    surfaceFormat = surfaceFormat,
  )

interface ShaderCompiler {
  suspend fun compile(
    vertexCode: String,
    fragmentCode: String? = null,
    label: String? = null,
  ): GPURenderPipeline
}

private class ShaderCompilerImpl(
  private val device: GPUDevice,
  private val surfaceFormat: GPUTextureFormat,
) : ShaderCompiler {
  override suspend fun compile(
    vertexCode: String,
    fragmentCode: String?,
    label: String?,
  ): GPURenderPipeline {
    val vertexModule =
      device.createShaderModule(
        GPUShaderModuleDescriptor(
          code = vertexCode,
          label = label?.let { "$it-vertex-shader" },
        ),
      )
    val fragmentModule =
      fragmentCode?.let {
        device.createShaderModule(
          GPUShaderModuleDescriptor(
            code = it,
            label = label?.let { "$it-fragment-shader" },
          ),
        )
      } ?: vertexModule
    val vertexState = GPUVertexState(vertexModule)
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
        ),
      ).await()
  }
}
