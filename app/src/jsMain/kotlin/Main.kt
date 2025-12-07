@file:OptIn(ExperimentalAtomicApi::class)

import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import net.japanesehunter.math.Angle
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.MutableTransform
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.Point3
import net.japanesehunter.math.degrees
import net.japanesehunter.math.lookAt
import net.japanesehunter.math.meters
import net.japanesehunter.math.mutateScale
import net.japanesehunter.math.mutateTranslation
import net.japanesehunter.math.x
import net.japanesehunter.math.y
import net.japanesehunter.math.z
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.InstanceGpuBuffer
import net.japanesehunter.webgpu.ShaderCompiler
import net.japanesehunter.webgpu.UniformGpuBuffer
import net.japanesehunter.webgpu.UnsupportedAdapterException
import net.japanesehunter.webgpu.UnsupportedBrowserException
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.asBinding
import net.japanesehunter.webgpu.camera
import net.japanesehunter.webgpu.canvasContext
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.createShaderCompiler
import net.japanesehunter.webgpu.drawIndexed
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassEncoder
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUVertexAttribute
import net.japanesehunter.webgpu.interop.GPUVertexBufferLayout
import net.japanesehunter.webgpu.interop.GPUVertexStepMode
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.interop.requestAnimationFrame
import net.japanesehunter.webgpu.pos3D
import net.japanesehunter.webgpu.recordRenderBundle
import net.japanesehunter.webgpu.rgbaColor
import net.japanesehunter.webgpu.setBindGroup
import net.japanesehunter.webgpu.setVertexBuffer
import net.japanesehunter.webgpu.transforms
import net.japanesehunter.webgpu.u16
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeMark
import kotlin.time.TimeSource

val end = AtomicBoolean(false)

fun main() =
  application(loggerLevel = Level.DEBUG) {
    canvasContext {
      val camera =
        MovableCamera(
          fov = Fov.fromDegrees(60.0),
          nearFar = NearFar(0.1.meters, 128.meters),
          aspect = canvasAspect,
        ).apply {
          autoFit()
        }
      val transforms =
        List(101) { x ->
          List(101) { y ->
            MutableTransform().apply {
              mutateTranslation {
                dx = (x - 50).meters
                dy = (y - 50).meters
              }
              mutateScale {
                sx = 0.5
                sy = 0.5
                sz = 0.5
              }
            }
          }
        }.flatten()

      webgpuContext {
        debugPrintLimits()
        val transformBuffer = InstanceGpuBuffer.transforms(transforms).bind()
        val vertexPosBuffer = VertexGpuBuffer.pos3D(vertexPos).bind()
        val vertexColorBuffer = VertexGpuBuffer.rgbaColor(vertexColor).bind()
        val indexBuffer = IndexGpuBuffer.u16(0, 1, 2, 1, 3, 2).bind()
        val cameraBuf = UniformGpuBuffer.camera(camera).bind()
        val pipeline =
          compileTriangleShader(
            transformBuffer,
            vertexPosBuffer,
            vertexColorBuffer,
          )
        val renderBundle =
          recordRenderBundle {
            val pipeline = pipeline.await()
            setPipeline(pipeline)
            setVertexBuffer(
              listOf(
                transformBuffer,
                vertexPosBuffer,
                vertexColorBuffer,
              ),
            )
            setBindGroup(listOf(listOf(cameraBuf.asBinding()))) {
              pipeline.getBindGroupLayout(it)
            }
            drawIndexed(indexBuffer, instanceCount = transforms.size)
          }
        val time = TimeSource.Monotonic.markNow()
        val done = Job()

        fun loop() {
          camera.x = 1.meters * time.rad(perSec = 15.0.degrees).sin()
          camera.y = 1.meters * time.rad(perSec = 30.0.degrees).cos()
          camera.z = 5.meters * time.rad(perSec = 10.0.degrees).cos()
          val point =
            run {
              val rad = time.rad(perSec = 20.0.degrees)
              Point3(
                x = 1.meters * rad.cos(),
                y = 1.meters * rad.sin(),
                z = 0.meters,
              )
            }
          camera.lookAt(point)
          cameraBuf.update()
          frame {
            executeBundles(arrayOf(renderBundle))
          }
          if (end.load()) {
            done.complete()
            return
          }
          requestAnimationFrame { loop() }
        }
        requestAnimationFrame {
          loop()
        }
        done.join()
      }
    }
  }

// region helper

context(canvas: CanvasContext, resource: ResourceScope)
private suspend inline fun <R> webgpuContext(
  action: context(
    GPU, GPUAdapter, GPUDevice, ShaderCompiler, BufferAllocator
  ) () -> R,
): R {
  val gpu = gpu ?: throw UnsupportedBrowserException()
  val adapter =
    gpu.requestAdapter().await() ?: throw UnsupportedAdapterException()
  val device =
    adapter.requestDevice().await()
  resource.onClose {
    device.destroy()
  }
  canvas.configure(
    GPUCanvasConfiguration(
      device = device,
      format = canvas.preferredFormat,
    ),
  )
  val compiler = device.createShaderCompiler(canvas.preferredFormat)
  val allocator = device.createBufferAllocator()
  return context(gpu, adapter, device, compiler, allocator) {
    action()
  }
}

context(device: GPUDevice)
private fun debugPrintLimits() {
  logger.debug {
    val limits = device.limits
    buildString {
      appendLine("Device Limits:")
      appendLine("Max texture dimension 1D: ${limits.maxTextureDimension1D}")
      appendLine("Max texture dimension 2D: ${limits.maxTextureDimension2D}")
      appendLine("Max texture dimension 3D: ${limits.maxTextureDimension3D}")
      appendLine("Max texture array layers: ${limits.maxTextureArrayLayers}")
      appendLine("Max bind groups: ${limits.maxBindGroups}")
      appendLine(
        "Max bind groups plus vertex buffers: " +
          "${limits.maxBindGroupsPlusVertexBuffers}",
      )
      appendLine("Max bindings per bind group: ${limits.maxBindingsPerBindGroup}")
      appendLine(
        "Max dynamic uniform buffers per pipeline layout: " +
          "${limits.maxDynamicUniformBuffersPerPipelineLayout}",
      )
      appendLine(
        "Max dynamic storage buffers per pipeline layout: " +
          "${limits.maxDynamicStorageBuffersPerPipelineLayout}",
      )
      appendLine(
        "Max sampled textures per shader stage: " +
          "${limits.maxSampledTexturesPerShaderStage}",
      )
      appendLine("Max samplers per shader stage: ${limits.maxSamplersPerShaderStage}")
      appendLine(
        "Max storage buffers per shader stage: " +
          "${limits.maxStorageBuffersPerShaderStage}",
      )
      appendLine(
        "Max storage textures per shader stage: " +
          "${limits.maxStorageTexturesPerShaderStage}",
      )
      appendLine(
        "Max uniform buffers per shader stage: " +
          "${limits.maxUniformBuffersPerShaderStage}",
      )
      appendLine(
        "Max uniform buffer binding size: " +
          "${limits.maxUniformBufferBindingSize}",
      )
      appendLine(
        "Max storage buffer binding size: " +
          "${limits.maxStorageBufferBindingSize}",
      )
      appendLine(
        "Min uniform buffer offset alignment: " +
          "${limits.minUniformBufferOffsetAlignment}",
      )
      appendLine(
        "Min storage buffer offset alignment: " +
          "${limits.minStorageBufferOffsetAlignment}",
      )
      appendLine("Max vertex buffers: ${limits.maxVertexBuffers}")
      appendLine("Max buffer size: ${limits.maxBufferSize}")
      appendLine("Max vertex attributes: ${limits.maxVertexAttributes}")
      appendLine("Max vertex buffer array stride: ${limits.maxVertexBufferArrayStride}")
      appendLine(
        "Max inter-stage shader variables: " +
          "${limits.maxInterStageShaderVariables}",
      )
      appendLine("Max color attachments: ${limits.maxColorAttachments}")
      appendLine(
        "Max color attachment bytes per sample: " +
          "${limits.maxColorAttachmentBytesPerSample}",
      )
      appendLine(
        "Max compute workgroup storage size: " +
          "${limits.maxComputeWorkgroupStorageSize}",
      )
      appendLine(
        "Max compute invocations per workgroup: " +
          "${limits.maxComputeInvocationsPerWorkgroup}",
      )
      appendLine("Max compute workgroup size X: ${limits.maxComputeWorkgroupSizeX}")
      appendLine("Max compute workgroup size Y: ${limits.maxComputeWorkgroupSizeY}")
      appendLine("Max compute workgroup size Z: ${limits.maxComputeWorkgroupSizeZ}")
      appendLine(
        "Max compute workgroups per dimension: " +
          "${limits.maxComputeWorkgroupsPerDimension}",
      )
    }
  }
}

context(compiler: ShaderCompiler, coroutine: CoroutineScope)
private fun compileTriangleShader(vararg vertexBuffers: VertexGpuBuffer): Deferred<GPURenderPipeline> {
  var shaderLocation = 0
  val layouts =
    vertexBuffers.map { buf ->
      GPUVertexBufferLayout(
        arrayStride = buf.stride,
        attributes =
          buf.formats
            .mapIndexed { index, format ->
              GPUVertexAttribute(
                shaderLocation = shaderLocation++,
                offset = buf.offsets[index],
                format = format.raw,
              )
            }.toTypedArray(),
        stepMode =
          if (buf is InstanceGpuBuffer) {
            GPUVertexStepMode.Instance
          } else {
            GPUVertexStepMode.Vertex
          },
      )
    }
  return compiler.compile(
    vertexCode = code,
    fragmentCode = code,
    vertexAttributes = layouts,
    label = "Triangle Pipeline",
  )
}

context(device: GPUDevice, canvas: CanvasContext)
private inline fun frame(action: GPURenderPassEncoder.() -> Unit) {
  val surfaceTexture = canvas.getCurrentTexture()
  val commandEncoder = device.createCommandEncoder()
  val renderPassEncoder =
    commandEncoder.beginRenderPass(
      GPURenderPassDescriptor(
        colorAttachments =
          arrayOf(
            GPURenderPassColorAttachment(
              view = surfaceTexture.createView(),
              clearValue = GPUColor(0.8, 0.8, 0.8, 1.0),
              loadOp = GPULoadOp.Clear,
              storeOp = GPUStoreOp.Store,
            ),
          ),
      ),
    )
  renderPassEncoder.action()
  renderPassEncoder.end()
  device.queue.submit(arrayOf(commandEncoder.finish()))
}

private fun TimeMark.rad(perSec: Angle): Angle {
  val elapsed = this.elapsedNow()
  val seconds = elapsed.inWholeMilliseconds / 1000.0
  return perSec * seconds
}

private fun Angle.sin(): Double = sin(toDouble(AngleUnit.RADIAN))

private fun Angle.cos(): Double = cos(toDouble(AngleUnit.RADIAN))

private val logger = logger("Main")

private val code =
  """
  struct VsOut {
    @builtin(position) position : vec4f,
    @location(0) color : vec4f,
  }
  
  @group(0) @binding(0) var<uniform> viewProj : mat4x4f;
  
  @vertex
  fn vs_main(
    @location(0) model0: vec4f,
    @location(1) model1: vec4f,
    @location(2) model2: vec4f,
    @location(3) model3: vec4f,
    @location(4) pos: vec3f,
    @location(5) color: vec4f,
  ) -> VsOut {
    let model = mat4x4f(
      model0,
      model1,
      model2,
      model3,
    );
    var out: VsOut;
    out.position = viewProj * model * vec4f(pos, 1.0);
    out.color = color;
    return out;
  }

  @fragment
  fn fs_main(
    @location(0) color: vec4f,
  ) -> @location(0) vec4f {
    return color;
  }
  """.trimIndent()

private val vertexPos =
  floatArrayOf(
    -0.5f,
    -0.5f,
    0.0f, // vertex 0
    0.5f,
    -0.5f,
    0.0f, // vertex 1
    -0.5f,
    0.5f,
    0.0f, // vertex 2
    0.5f,
    0.5f,
    0.0f, // vertex 3
  )

private val vertexColor =
  floatArrayOf(
    1.0f,
    1.0f,
    0.0f,
    1.0f, // vertex 0
    0.0f,
    1.0f,
    1.0f,
    1.0f, // vertex 1
    1.0f,
    0.0f,
    1.0f,
    1.0f, // vertex 2
    1.0f,
    1.0f,
    1.0f,
    1.0f, // vertex 3
  )

context(canvas: CanvasContext)
private val canvasAspect get() = canvas.width.toDouble() / canvas.height

context(canvas: CanvasContext)
private fun MovableCamera.autoFit(): AutoCloseable =
  canvas.onResize {
    aspect = canvasAspect
  }

// endregion
