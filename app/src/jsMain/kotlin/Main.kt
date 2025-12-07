
import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.await
import kotlinx.coroutines.yield
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.MutableTransform
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.meters
import net.japanesehunter.math.mutateScale
import net.japanesehunter.math.mutateTranslation
import net.japanesehunter.math.z
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.InstanceGpuBuffer
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
import net.japanesehunter.webgpu.interop.GPUVertexStepMode
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.pos3D
import net.japanesehunter.webgpu.recordRenderBundle
import net.japanesehunter.webgpu.rgbaColor
import net.japanesehunter.webgpu.setBindGroup
import net.japanesehunter.webgpu.setVertexBuffer
import net.japanesehunter.webgpu.transforms
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

    val transforms =
      List(3) { x ->
        List(3) { y ->
          MutableTransform().apply {
            mutateTranslation {
              dx = (x - 1).meters
              dy = (y - 1).meters
              dz = (-2).meters
            }
            mutateScale {
              sx = 0.5
              sy = 0.5
              sz = 0.5
            }
          }
        }
      }.flatten()

    webgpuContext(canvas) {
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

private fun canvas(): HTMLCanvasElement? =
  document
    .getElementById("canvas")
    .unsafeCast<HTMLCanvasElement?>()

private fun HTMLCanvasElement.fit() {
  width = window.innerWidth
  height = window.innerHeight
}

// endregion
