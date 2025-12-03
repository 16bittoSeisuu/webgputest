import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCanvasContext
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUColorTargetState
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUFragmentState
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassEncoder
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPURenderPipelineDescriptor
import net.japanesehunter.webgpu.interop.GPUShaderModuleDescriptor
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUVertexState
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
      val pipeline = createPipeline()
      frame {
        setPipeline(pipeline)
        draw(3)
      }
    }
  }

private suspend inline fun <R> webgpuContext(
  canvas: HTMLCanvasElement,
  action: context(GPU, GPUAdapter, GPUDevice, GPUCanvasContext) () -> R,
): R {
  val gpu = gpu ?: throw UnsupportedBrowserException()
  val adapter = gpu.requestAdapter().await()
  val device = adapter.requestDevice().await()
  val surfaceContext =
    canvas.getContext("webgpu").unsafeCast<GPUCanvasContext>()
  surfaceContext.configure(
    GPUCanvasConfiguration(
      device = device,
      format = gpu.getPreferredCanvasFormat(),
    ),
  )
  return context(gpu, adapter, device, surfaceContext) {
    action()
  }
}

context(device: GPUDevice, gpu: GPU)
private suspend fun createPipeline(): GPURenderPipeline {
  val module =
    device.createShaderModule(GPUShaderModuleDescriptor(code = code))
  val vertexState = GPUVertexState(module = module)
  val fragmentState =
    GPUFragmentState(
      module = module,
      targets =
        arrayOf(GPUColorTargetState(format = gpu.getPreferredCanvasFormat())),
    )
  return device
    .createRenderPipelineAsync(
      GPURenderPipelineDescriptor(
        vertex = vertexState,
        fragment = fragmentState,
      ),
    ).await()
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
  surfaceTexture.destroy()
}

private class UnsupportedBrowserException :
  Exception(
    message = "WebGPU is not supported on this browser",
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
