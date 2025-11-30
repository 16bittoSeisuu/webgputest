
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.VertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.canvasContextRenderer
import io.ygdrasil.webgpu.writeInto

external fun setInterval(
  callback: () -> Unit,
  ms: Int,
): Int

fun main() =
  application(loggerLevel = Level.DEBUG) {
    val ctx = canvasContextRenderer()
    val device = ctx.wgpuContext.device
    ctx.wgpuContext.surface.configure(
      SurfaceConfiguration(
        device = device,
        format = ctx.wgpuContext.renderingContext.textureFormat,
      ),
    )
    val shader =
      run {
        val module =
          device.createShaderModule(
            ShaderModuleDescriptor(
              label = "Hello triangle shader",
              code = code,
            ),
          )
        val vertexState =
          VertexState(
            module = module,
            entryPoint = "vs_main",
            buffers =
              listOf(
                VertexBufferLayout(
                  arrayStride = (Float.SIZE_BYTES * 3).toULong(),
                  attributes =
                    listOf(
                      VertexAttribute(
                        format = GPUVertexFormat.Float32x3,
                        offset = 0u,
                        shaderLocation = 0u,
                      ),
                    ),
                ),
                VertexBufferLayout(
                  arrayStride = (Float.SIZE_BYTES * 4).toULong(),
                  attributes =
                    listOf(
                      VertexAttribute(
                        format = GPUVertexFormat.Float32x4,
                        offset = 0u,
                        shaderLocation = 1u,
                      ),
                    ),
                ),
              ),
          )
        val fragmentState =
          FragmentState(
            module = module,
            entryPoint = "fs_main",
            targets =
              listOf(
                ColorTargetState(
                  format = ctx.wgpuContext.renderingContext.textureFormat,
                ),
              ),
          )
        device.createRenderPipeline(
          RenderPipelineDescriptor(
            label = "Hello triangle pipeline",
            vertex = vertexState,
            fragment = fragmentState,
            primitive =
              PrimitiveState(
                topology = GPUPrimitiveTopology.TriangleList,
                cullMode = GPUCullMode.Back,
              ),
          ),
        )
      }
    val vertexBuffer0 =
      device
        .createBuffer(
          BufferDescriptor(
            label = "Vertex pos buffer",
            size = (Float.SIZE_BYTES * 3 * 3).toULong(),
            usage = setOf(GPUBufferUsage.Vertex),
            mappedAtCreation = true,
          ),
        ).apply {
          floatArrayOf(
            0.0f,
            0.5f,
            0.0f, // Vertex 1 position
            -0.5f,
            -0.5f,
            0.0f, // Vertex 2 position
            0.5f,
            -0.5f,
            0.0f, // Vertex 3 positions
          ).writeInto(getMappedRange())
          unmap()
        }
    val vertexBuffer1 =
      device
        .createBuffer(
          BufferDescriptor(
            label = "Vertex color buffer",
            size = (Float.SIZE_BYTES * 4 * 3).toULong(),
            usage = setOf(GPUBufferUsage.Vertex),
            mappedAtCreation = true,
          ),
        ).apply {
          floatArrayOf(
            1.0f,
            1.0f,
            0.0f,
            1.0f, // Vertex 1 color
            0.0f,
            1.0f,
            1.0f,
            1.0f, // Vertex 2 color
            1.0f,
            0.0f,
            1.0f,
            1.0f, // Vertex 3 color
          ).writeInto(getMappedRange())
          unmap()
        }
    setInterval({
      ctx.wgpuContext.renderingContext
        .getCurrentTexture()
        .use { surface ->
          device.createCommandEncoder().use { cmdEnc ->
            cmdEnc.beginRenderPass(
              RenderPassDescriptor(
                label = "Hello triangle render pass",
                colorAttachments =
                  listOf(
                    RenderPassColorAttachment(
                      view = surface.createView(),
                      loadOp = GPULoadOp.Clear,
                      storeOp = GPUStoreOp.Store,
                      clearValue = Color(0.8, 0.8, 0.8, 1.0),
                    ),
                  ),
              ),
            ) {
              setPipeline(shader)
              setVertexBuffer(0u, vertexBuffer0)
              setVertexBuffer(1u, vertexBuffer1)
              draw(vertexCount = 3u)
              end()
            }
            cmdEnc.finish().use {
              device.queue.submit(listOf(it))
            }
          }
          ctx.wgpuContext.surface.present()
        }
    }, 16)
  }

private val logger = KotlinLogging.logger("Main")

private val code =
  """
  struct VsOut {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>,
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
    @location(0) color: vec4<f32>,
  ) -> @location(0) vec4<f32> {
    return color;
  }
  """.trimIndent()
