@file:OptIn(ExperimentalAtomicApi::class)

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resource
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import net.japanesehunter.math.Angle
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.degrees
import net.japanesehunter.math.east
import net.japanesehunter.math.lookAt
import net.japanesehunter.math.meters
import net.japanesehunter.math.north
import net.japanesehunter.math.x
import net.japanesehunter.math.y
import net.japanesehunter.math.z
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.webgpu.GpuBuffer
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.InstanceGpuBuffer
import net.japanesehunter.webgpu.ShaderCompiler
import net.japanesehunter.webgpu.StorageGpuBuffer
import net.japanesehunter.webgpu.UnsupportedAdapterException
import net.japanesehunter.webgpu.UnsupportedBrowserException
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.asBinding
import net.japanesehunter.webgpu.canvasContext
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.createShaderCompiler
import net.japanesehunter.webgpu.drawIndexed
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUAddressMode
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUExtent3D
import net.japanesehunter.webgpu.interop.GPUFilterMode
import net.japanesehunter.webgpu.interop.GPUImageCopyExternalImage
import net.japanesehunter.webgpu.interop.GPUImageCopyTextureTagged
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPUMipmapFilterMode
import net.japanesehunter.webgpu.interop.GPUOrigin3D
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassEncoder
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPUSampler
import net.japanesehunter.webgpu.interop.GPUSamplerDescriptor
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureDimension
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureUsage
import net.japanesehunter.webgpu.interop.GPUVertexAttribute
import net.japanesehunter.webgpu.interop.GPUVertexBufferLayout
import net.japanesehunter.webgpu.interop.GPUVertexStepMode
import net.japanesehunter.webgpu.interop.createImageBitmap
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.interop.requestAnimationFrame
import net.japanesehunter.webgpu.recordRenderBundle
import net.japanesehunter.webgpu.setBindGroup
import net.japanesehunter.webgpu.setVertexBuffer
import net.japanesehunter.webgpu.u16
import net.japanesehunter.worldcreate.Quad
import net.japanesehunter.worldcreate.toGpuBuffer
import net.japanesehunter.worldcreate.toIndicesGpuBuffer
import org.w3c.dom.ImageBitmap
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeMark
import kotlin.time.TimeSource

val end = Job()

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
      val quads =
        List(101) { x ->
          List(101) { y ->
            Quad(
              pos =
                Point3(
                  x = (x - 50).meters,
                  y = (y - 50).meters,
                  z = 0.meters,
                ),
              normal = Direction3.north,
              tangent = Direction3.east,
              aoLeftBottom = Proportion.QUARTER,
              aoRightBottom = Proportion.HALF,
              aoLeftTop = Proportion.HALF,
              aoRightTop = Proportion.ONE,
              sizeU = 0.9.meters,
              sizeV = 0.9.meters,
              materialId = 0,
            )
          }
        }.flatten()

      webgpuContext {
        debugPrintLimits()
        val indexBuffer = IndexGpuBuffer.u16(0, 1, 2, 1, 3, 2).bind()
        val cameraBuf = camera.toGpuBuffer().bind()
        val quadInst = quads.toGpuBuffer().bind()
        val quadIndices = quads.toIndicesGpuBuffer().bind()
        val uvs = createUvsBuffer().bind()
        val textureDeferred =
          createTexture(
            "assets/vanilla/textures/doge.png",
          )
        val samp = createSampler()
        val pipeline =
          compileTriangleShader(quadIndices)
        val renderBundle =
          recordRenderBundle {
            val pipeline = pipeline.await()
            setPipeline(pipeline)
            setVertexBuffer(
              listOf(
                quadIndices,
              ),
            )
            val texture = textureDeferred.await().createView()
            setBindGroup(
              listOf(
                listOf(cameraBuf.asBinding()),
                listOf(uvs.asBinding(), texture, samp),
                listOf(quadInst.asBinding()),
              ),
            ) { pipeline.getBindGroupLayout(it) }
            drawIndexed(indexBuffer, instanceCount = quads.size)
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
          if (end.isCompleted) {
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

context(coroutine: CoroutineScope)
private fun loadImageBitmap(path: String): Deferred<ImageBitmap> =
  coroutine.async {
    val path = path.removePrefix("/")
    val resp = window.fetch("/$path").await()
    val blob = resp.blob().await()
    createImageBitmap(blob).await()
  }

context(alloc: BufferAllocator)
private fun createUvsBuffer(): Resource<StorageGpuBuffer> {
  val res =
    alloc.static(
      data = floatArrayOf(0f, 0f, 1f, 1f),
      usage = GPUBufferUsage.Storage,
      label = "UVs Buffer",
    )
  return resource {
    val buf = res.bind()
    object : StorageGpuBuffer, GpuBuffer by buf {}
  }
}

context(device: GPUDevice, coroutine: CoroutineScope)
private fun createTexture(path: String): Deferred<GPUTexture> =
  coroutine.async {
    val bitmap = loadImageBitmap(path).await()
    val textureSize = GPUExtent3D(bitmap.width, bitmap.height, 1)
    val descriptor =
      GPUTextureDescriptor(
        size = textureSize,
        mipLevelCount = 1,
        sampleCount = 1,
        dimension = GPUTextureDimension.D2,
        format = GPUTextureFormat.Rgba8UnormSrgb,
        usage =
          GPUTextureUsage.TextureBinding + GPUTextureUsage.CopyDst +
            GPUTextureUsage.RenderAttachment,
      )
    val texture = device.createTexture(descriptor)
    device.queue.copyExternalImageToTexture(
      GPUImageCopyExternalImage(
        source = bitmap,
        origin = GPUOrigin3D(),
        flipY = false,
        premultipliedAlpha = true,
      ),
      GPUImageCopyTextureTagged(
        texture = texture,
        mipLevel = 0,
        origin = GPUOrigin3D(),
      ),
      textureSize,
    )
    texture
  }

context(device: GPUDevice)
private fun createSampler(): GPUSampler {
  val descriptor =
    GPUSamplerDescriptor(
      magFilter = GPUFilterMode.Nearest,
      minFilter = GPUFilterMode.Nearest,
      mipmapFilter = GPUMipmapFilterMode.Nearest,
      addressModeU = GPUAddressMode.Repeat,
      addressModeV = GPUAddressMode.Repeat,
      addressModeW = GPUAddressMode.Repeat,
    )
  return device.createSampler(descriptor)
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
    @location(0) @interpolate(flat) mat_id: u32,
    @location(1) uv: vec2f,
    @location(2) ao: f32,
  }
  
  struct CameraUniform {
    projection: mat4x4f,  // 64
    rotation: mat3x3f,    // 48
    block_pos: vec3i,     // 12
    _pad0: u32,           // 4
    local_pos: vec3f,     // 12
    _pad1: u32,           // 4
  }
  
  struct Material {
    uv_min: vec2f,      // 8
    uv_max: vec2f,      // 8
  }
  
  struct Quad {
    block_pos: vec3i,     // 12
    size_u: f32,          // 4
    local_pos: vec3f,     // 12
    size_v: f32,          // 4
    normal: vec3f,        // 12
    ao: u32,              // 4
    tangent: vec3f,       // 12
    mat_id: u32,          // 4
  }
  
  @group(0) @binding(0) var<uniform> camera: CameraUniform;
  @group(1) @binding(0) var<storage, read> materials: array<Material>;
  @group(1) @binding(1) var tex: texture_2d<f32>;
  @group(1) @binding(2) var samp: sampler;
  @group(2) @binding(0) var<storage, read> quad_instances: array<Quad>;
  
  @vertex
  fn vs_main(
    @builtin(vertex_index) vertex_index: u32,
    @location(0) quad_index: u32,
  ) -> VsOut {
    let quad = quad_instances[quad_index];
    let relative_block_pos = vec3f(quad.block_pos - camera.block_pos);
    let relative_base_pos =
      relative_block_pos 
        + quad.local_pos
        - camera.local_pos;
    
    let normal = normalize(quad.normal);
    let tangent = normalize(quad.tangent);
    let bitangent = normalize(cross(normal, tangent));
    let corner = corners[vertex_index];
    let offset = 
      tangent * (corner.x * quad.size_u) +
      bitangent * (corner.y * quad.size_v);
    let pos = relative_base_pos + offset;
    let pos_relative_camera = camera.rotation * pos;
    let clip_pos = camera.projection * vec4f(pos_relative_camera, 1.0);
    
    var out: VsOut;
    out.position = clip_pos;
    out.mat_id = quad.mat_id;
    out.uv = uvs[vertex_index];
    out.ao = f32((quad.ao >> (vertex_index * 8)) & 0xFF) / 255.0;
    return out;
  }

  @fragment
  fn fs_main(
    @location(0) @interpolate(flat) mat_id: u32,
    @location(1) uv: vec2f,
    @location(2) ao: f32,
  ) -> @location(0) vec4f {
    let material = materials[mat_id];
    let uv_scaled =
      mix(
        material.uv_min,
        material.uv_max,
        uv,
      );
    let baseColor = textureSample(tex, samp, uv_scaled);
    let color = vec4f(
      baseColor.rgb * ao,
      baseColor.a,
    );
    return color;
  }
  
  const corners: array<vec2f, 4> = 
    array<vec2f, 4>(
      vec2f(-0.5, -0.5), // left-bottom
      vec2f( 0.5, -0.5), // right-bottom
      vec2f(-0.5,  0.5), // left-top
      vec2f( 0.5,  0.5), // right-top
    );
    
  const uvs: array<vec2f, 4> = 
    array<vec2f, 4>(
      vec2f(0.0, 0.0), // left-bottom
      vec2f(1.0, 0.0), // right-bottom
      vec2f(0.0, 1.0), // left-top
      vec2f(1.0, 1.0), // right-top
    );
  """.trimIndent()

context(canvas: CanvasContext)
private val canvasAspect get() = canvas.width.toDouble() / canvas.height

context(canvas: CanvasContext)
private fun MovableCamera.autoFit(): AutoCloseable =
  canvas.onResize {
    aspect = canvasAspect
  }

// endregion
