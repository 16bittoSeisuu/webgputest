
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.yield
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.ShaderCompiler
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.createShaderCompiler
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUBuffer
import net.japanesehunter.webgpu.interop.GPUBufferDescriptor
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCanvasContext
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUIndexFormat
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPURenderBundle
import net.japanesehunter.webgpu.interop.GPURenderBundleDescriptor
import net.japanesehunter.webgpu.interop.GPURenderBundleEncoder
import net.japanesehunter.webgpu.interop.GPURenderBundleEncoderDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassEncoder
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUVertexAttribute
import net.japanesehunter.webgpu.interop.GPUVertexBufferLayout
import net.japanesehunter.webgpu.interop.GPUVertexFormat
import net.japanesehunter.webgpu.interop.navigator.gpu
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
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
      val vertexPosBuffer = createVertexPosBuffer()
      val vertexColorBuffer = createVertexColorBuffer()
      val indexBuffer = createIndexBuffer()
      val renderBundle =
        recordRenderBundle {
          setPipeline(pipeline)
          setVertexBuffer(0, vertexPosBuffer)
          setVertexBuffer(1, vertexColorBuffer)
          setIndexBuffer(indexBuffer, GPUIndexFormat.Uint16)
          drawIndexed(3)
        }
      while (true) {
        frame {
          executeBundles(arrayOf(renderBundle))
        }
        yield()
      }
    }
  }

// region helper

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

context(device: GPUDevice)
private fun createVertexPosBuffer(): GPUBuffer {
  val vertices =
    floatArrayOf(
      0f,
      0.5f,
      0f, // vertex 0
      -0.5f,
      -0.5f,
      0f, // vertex 1
      0.5f,
      -0.5f,
      0f, // vertex 2
    )
  return device
    .createBuffer(
      GPUBufferDescriptor(
        size = vertices.size * Float.SIZE_BYTES,
        usage = GPUBufferUsage.Vertex,
        mappedAtCreation = true,
        label = "Vertex Buffer",
      ),
    ).apply {
      val floatArray = Float32Array(getMappedRange())
      for (i in vertices.indices) {
        floatArray[i] = vertices[i]
      }
      unmap()
    }
}

context(device: GPUDevice)
private fun createVertexColorBuffer(): GPUBuffer {
  val colors =
    floatArrayOf(
      1f,
      1f,
      0f,
      1f, // vertex 0
      0f,
      1f,
      1f,
      1f, // vertex 1
      1f,
      0f,
      1f,
      1f, // vertex 2
    )
  return device
    .createBuffer(
      GPUBufferDescriptor(
        size = colors.size * Float.SIZE_BYTES,
        usage = GPUBufferUsage.Vertex,
        mappedAtCreation = true,
        label = "Color Buffer",
      ),
    ).apply {
      val floatArray = Float32Array(getMappedRange())
      for (i in colors.indices) {
        floatArray[i] = colors[i]
      }
      unmap()
    }
}

context(device: GPUDevice)
private fun createIndexBuffer(): GPUBuffer {
  val indices = shortArrayOf(0, 1, 2)
  return device
    .createBuffer(
      GPUBufferDescriptor(
        size = 8, // 3 indices * 2 bytes, rounded up to multiple of 4
        usage = GPUBufferUsage.Index,
        mappedAtCreation = true,
        label = "Index Buffer",
      ),
    ).apply {
      val array = getMappedRange()
      val uint16Array = Uint16Array(array)
      for (i in indices.indices) {
        uint16Array[i] = indices[i]
      }
      unmap()
    }
}

context(compiler: ShaderCompiler)
private suspend fun compileTriangleShader(): GPURenderPipeline {
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

context(gpu: GPU, device: GPUDevice)
private inline fun recordRenderBundle(
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
      finish(GPURenderBundleDescriptor(label))
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
  
  @vertex
  fn vs_main(
    @location(0) pos: vec3f,
    @location(1) color: vec4f,
  ) -> VsOut {
    var out: VsOut;
    out.position = vec4f(pos, 1.0);
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

// endregion
