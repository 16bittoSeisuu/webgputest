
import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.await
import kotlinx.coroutines.yield
import net.japanesehunter.math.Camera
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.meters
import net.japanesehunter.math.z
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CameraGpuBuffer
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.ShaderCompiler
import net.japanesehunter.webgpu.UniformGpuBuffer
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.asBinding
import net.japanesehunter.webgpu.camera
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.createShaderCompiler
import net.japanesehunter.webgpu.drawIndexed
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCanvasContext
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
import net.japanesehunter.webgpu.interop.GPUVertexFormat
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.pos3D
import net.japanesehunter.webgpu.recordRenderBundle
import net.japanesehunter.webgpu.rgbaColor
import net.japanesehunter.webgpu.setBindGroup
import net.japanesehunter.webgpu.setVertexBuffer
import net.japanesehunter.webgpu.u16
import org.w3c.dom.HTMLCanvasElement

fun main() =
  application {
    val canvas =
      canvas()?.apply {
        fit()
      } ?: run {
        logger.error { "Canvas element not found" }
        return@application
      }
    val camera =
      MovableCamera(
        fov = Fov.fromDegrees(60.0),
        nearFar = NearFar(0.1.meters, 128.meters),
        aspect = canvas.width.toDouble() / canvas.height,
      ).apply {
        z = 2.meters
      }
    window.onresize = {
      canvas.fit()
      camera.aspect = canvas.width.toDouble() / canvas.height
    }

    webgpuContext(canvas) {
      val pipeline = compileTriangleShader()
      val vertexPosBuffer = VertexGpuBuffer.pos3D(vertexPos).bind()
      val vertexColorBuffer = VertexGpuBuffer.rgbaColor(vertexColor).bind()
      val indexBuffer = IndexGpuBuffer.u16(0, 1, 2, 1, 3, 2).bind()
      val cameraBuf = createCameraBuffer(camera)
      val renderBundle =
        recordRenderBundle {
          val pipeline = pipeline.await()
          setPipeline(pipeline)
          setVertexBuffer(listOf(vertexPosBuffer, vertexColorBuffer))
          setBindGroup(listOf(listOf(cameraBuf.asBinding()))) {
            pipeline.getBindGroupLayout(it)
          }
          drawIndexed(indexBuffer)
        }
      while (true) {
        cameraBuf.update()
        frame {
          executeBundles(arrayOf(renderBundle))
        }
        yield()
      }
    }
  }

// region helper

context(resource: ResourceScope)
private suspend inline fun <R> webgpuContext(
  canvas: HTMLCanvasElement,
  action: context(
    GPU, GPUAdapter, GPUDevice, ShaderCompiler, GPUCanvasContext, BufferAllocator
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
  val surfaceContext =
    canvas.getContext("webgpu").unsafeCast<GPUCanvasContext>()
  val preferredFormat = gpu.getPreferredCanvasFormat()
  surfaceContext.configure(
    GPUCanvasConfiguration(
      device = device,
      format = preferredFormat,
    ),
  )
  val compiler = device.createShaderCompiler(preferredFormat)
  val allocator = device.createBufferAllocator()
  return context(gpu, adapter, device, compiler, surfaceContext, allocator) {
    action()
  }
}

context(compiler: ShaderCompiler, coroutine: CoroutineScope)
private fun compileTriangleShader(): Deferred<GPURenderPipeline> {
  val layout0 =
    GPUVertexBufferLayout(
      arrayStride = 3 * Float.SIZE_BYTES,
      attributes =
        arrayOf(
          GPUVertexAttribute(
            shaderLocation = 0,
            offset = 0,
            format = GPUVertexFormat.Float32x3,
          ),
        ),
    )
  val layout1 =
    GPUVertexBufferLayout(
      arrayStride = 4 * Float.SIZE_BYTES,
      attributes =
        arrayOf(
          GPUVertexAttribute(
            shaderLocation = 1,
            offset = 0,
            format = GPUVertexFormat.Float32x4,
          ),
        ),
    )
  return compiler.compile(
    vertexCode = code,
    fragmentCode = code,
    vertexAttributes = arrayOf(layout0, layout1),
    label = "Triangle Pipeline",
  )
}

context(bufAlloc: BufferAllocator, resource: ResourceScope)
private suspend fun createCameraBuffer(camera: Camera): CameraGpuBuffer =
  with(resource) {
    UniformGpuBuffer.camera(camera).bind()
  }

context(device: GPUDevice, surface: GPUCanvasContext)
private inline fun frame(action: GPURenderPassEncoder.() -> Unit) {
  val surfaceTexture = surface.getCurrentTexture()
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

private class UnsupportedBrowserException :
  Exception(
    message = "WebGPU is not supported on this browser",
  )

private class UnsupportedAdapterException :
  Exception(
    message = "WebGPU Adapter could not be obtained",
  )

private val logger = KotlinLogging.logger("Main")

private val code =
  """
  struct VsOut {
    @builtin(position) position : vec4f,
    @location(0) color : vec4f,
  }
  
  @group(0) @binding(0) var<uniform> viewProj : mat4x4f;
  
  @vertex
  fn vs_main(
    @location(0) pos: vec3f,
    @location(1) color: vec4f,
  ) -> VsOut {
    var out: VsOut;
    out.position = viewProj * vec4f(pos, 1.0);
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

private fun canvas(): HTMLCanvasElement? =
  document
    .getElementById("canvas")
    .unsafeCast<HTMLCanvasElement?>()

private fun HTMLCanvasElement.fit() {
  width = window.innerWidth
  height = window.innerHeight
}

// endregion
