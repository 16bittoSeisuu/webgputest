
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
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
import net.japanesehunter.webgpu.interop.GPURenderPipelineDescriptor
import net.japanesehunter.webgpu.interop.GPUShaderModuleDescriptor
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUTextureView
import net.japanesehunter.webgpu.interop.GPUVertexState
import net.japanesehunter.webgpu.interop.navigator
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

    val gpu =
      navigator.gpu ?: run {
        logger.error { "WebGPU is not supported in this browser." }
        return@application
      }
    val adapter = gpu.requestAdapter().await()
    val device = adapter.requestDevice().await()
    val preferredFormat = gpu.getPreferredCanvasFormat()
    val context = canvas.getContext("webgpu").unsafeCast<GPUCanvasContext>()
    context.configure(
      GPUCanvasConfiguration(
        device = device,
        format = preferredFormat,
      ),
    )
    val pipeline =
      run {
        val module =
          device.createShaderModule(GPUShaderModuleDescriptor(code = code))
        val vertexState = GPUVertexState(module = module)
        val fragmentState =
          GPUFragmentState(
            module = module,
            targets =
              arrayOf(GPUColorTargetState(format = preferredFormat)),
          )
        device
          .createRenderPipelineAsync(
            GPURenderPipelineDescriptor(
              vertex = vertexState,
              fragment = fragmentState,
            ),
          ).await()
      }
    val surfaceTexture = context.getCurrentTexture()
    val surfaceView = surfaceTexture.createView()
    context(device, surfaceView) {
      frame {
        setPipeline(pipeline)
        draw(3)
      }
    }
    surfaceTexture.destroy()
  }

context(device: GPUDevice, surface: GPUTextureView)
private inline fun frame(action: GPURenderPassEncoder.() -> Unit) {
  val commandEncoder = device.createCommandEncoder()
  val renderPassEncoder =
    commandEncoder.beginRenderPass(
      GPURenderPassDescriptor(
        colorAttachments =
          arrayOf(
            GPURenderPassColorAttachment(
              view = surface,
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
