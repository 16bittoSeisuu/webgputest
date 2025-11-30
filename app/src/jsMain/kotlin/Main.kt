
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.canvasContextRenderer

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

  const vertex_pos = array<vec2<f32>, 3>(
    vec2<f32>(0.0, 0.5),
    vec2<f32>(-0.5, -0.5),
    vec2<f32>(0.5, -0.5),
  );
  
  const vertex_color = array<vec4<f32>, 3>(
    vec4<f32>(1.0, 1.0, 0.0, 1.0),
    vec4<f32>(0.0, 1.0, 1.0, 1.0),
    vec4<f32>(1.0, 0.0, 1.0, 1.0),
  );

  @vertex
  fn vs_main(
    @builtin(vertex_index) v: u32,
  ) -> VsOut {
    var out: VsOut;
    out.position = vec4f(vertex_pos[v], 0.0, 1.0);
    out.color = vertex_color[v];
    return out;
  }

  @fragment
  fn fs_main(
    @location(0) color: vec4<f32>,
  ) -> @location(0) vec4<f32> {
    return color;
  }
  """.trimIndent()
