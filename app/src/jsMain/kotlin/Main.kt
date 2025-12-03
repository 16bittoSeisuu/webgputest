
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.yield
import net.japanesehunter.webgpu.ShaderCompiler
import net.japanesehunter.webgpu.createShaderCompiler
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
import net.japanesehunter.webgpu.interop.navigator.gpu
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
    window.onresize = { canvas.fit() }

    webgpuContext(canvas) {
      val pipeline = compileTriangleShader()
      while (true) {
        frame {
          setPipeline(pipeline)
          draw(3)
        }
        yield()
      }
    }
  }

private suspend inline fun <R> webgpuContext(
  canvas: HTMLCanvasElement,
  action: context(
    GPU, GPUAdapter, GPUDevice, ShaderCompiler, GPUCanvasContext
  ) () -> R,
): R {
  val gpu = gpu ?: throw UnsupportedBrowserException()
  val adapter =
    gpu.requestAdapter().await() ?: throw UnsupportedGPUDriverException()
  val device =
    adapter.requestDevice().await() ?: throw UnsupportedGPUDriverException()
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
  return context(gpu, adapter, device, compiler, surfaceContext) {
    action()
  }
}

context(compiler: ShaderCompiler)
private suspend fun compileTriangleShader(): GPURenderPipeline =
  compiler.compile(
    vertexCode = code,
    fragmentCode = code,
    label = "Triangle Pipeline",
  )

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

private class UnsupportedGPUDriverException :
  Exception(
    message = "WebGPU Adapter or Device could not be obtained",
  )

private val logger = KotlinLogging.logger("Main")

private val code =
  """
  struct VsOut {
    @builtin(position) position : vec4f,
    @location(0) color : vec4f,
  }
  
  @vertex
  fn vs_main(
    @builtin(vertex_index) vertexIndex : u32
  ) -> VsOut {
    var positions = array<vec2f, 3>(
      vec2f(0.0, 0.5),
      vec2f(-0.5, -0.5),
      vec2f(0.5, -0.5)
    );
    var colors = array<vec4f, 3>(
      vec4f(1.0, 1.0, 0.0, 1.0),
      vec4f(0.0, 1.0, 1.0, 1.0),
      vec4f(1.0, 0.0, 1.0, 1.0)
    );
    let pos = positions[vertexIndex];
    let color = colors[vertexIndex];
    var out: VsOut;
    out.position = vec4f(pos, 0.0, 1.0);
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

private fun canvas(): HTMLCanvasElement? =
  document
    .getElementById("canvas")
    .unsafeCast<HTMLCanvasElement?>()

private fun HTMLCanvasElement.fit() {
  width = window.innerWidth
  height = window.innerHeight
}
