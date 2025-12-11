
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resource
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import net.japanesehunter.math.Angle
import net.japanesehunter.math.Direction16
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.currentDirection16
import net.japanesehunter.math.degrees
import net.japanesehunter.math.east
import net.japanesehunter.math.lookAt
import net.japanesehunter.math.meters
import net.japanesehunter.math.south
import net.japanesehunter.math.x
import net.japanesehunter.math.y
import net.japanesehunter.math.z
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.webgpu.GpuBuffer
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.StorageGpuBuffer
import net.japanesehunter.webgpu.UnsupportedAdapterException
import net.japanesehunter.webgpu.UnsupportedBrowserException
import net.japanesehunter.webgpu.buildRenderBundle
import net.japanesehunter.webgpu.canvasContext
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.createMsaaTexture
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUAddressMode
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUCullMode
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
import net.japanesehunter.webgpu.interop.GPUSampler
import net.japanesehunter.webgpu.interop.GPUSamplerDescriptor
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureDimension
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureUsage
import net.japanesehunter.webgpu.interop.GPUTextureView
import net.japanesehunter.webgpu.interop.createImageBitmap
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.interop.requestAnimationFrame
import net.japanesehunter.webgpu.u16
import net.japanesehunter.worldcreate.Quad
import net.japanesehunter.worldcreate.toGpuBuffer
import net.japanesehunter.worldcreate.toIndicesGpuBuffer
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.ImageBitmap
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
          z = 2.meters
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
              normal = Direction3.south,
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
        val directionHud = createCameraDirectionHud()
        val cameraBuf = camera.toGpuBuffer().bind()
        val msaaTexture = createMsaaTexture()
        val renderBundle =
          buildRenderBundle(
            sampleCount = msaaTexture.sampleCount,
            cullMode = GPUCullMode.Back,
          ) {
            val indexBuffer = IndexGpuBuffer.u16(0, 1, 2, 1, 3, 2).bind()
            val quadIndexBuffer = quads.toIndicesGpuBuffer().bind()
            val textureBuffer =
              createTexture(
                "assets/vanilla/textures/doge.png",
              ).await().createView()

            header {
              """
              struct CameraUniform {
                projection: mat4x4f,
                rotation: mat3x3f,
                block_pos: vec3i,
                pad0: u32,
                local_pos: vec3f,
                pad1: f32,
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
              
              struct Material {
                uv_min: vec2f,
                uv_max: vec2f,
              }
              
              const corners =
                array<vec2f, 4>(
                  vec2f(-0.5, -0.5),
                  vec2f(0.5, -0.5),
                  vec2f(-0.5, 0.5),
                  vec2f(0.5, 0.5),
                );
              
              const uvs =
                array<vec2f, 4>(
                  vec2f(0.0, 1.0),
                  vec2f(1.0, 1.0),
                  vec2f(0.0, 0.0),
                  vec2f(1.0, 0.0),
                );
              """
            }

            val uv by vsOut("vec2f")
            val ao by vsOut("f32")
            val matId by vsOut("u32", interpolation = "flat")
            val camera by cameraBuf.asUniform(type = "CameraUniform")
            val quadsInst by quads.toGpuBuffer().bind().asStorage("array<Quad>")
            val material by createMaterialBuffer().bind().asStorage("array<Material>")
            val texture by textureBuffer
            val samp by createSampler()
            vertex(indexBuffer) {
              val quadIndices by quadIndexBuffer
              """
              let quad = $quadsInst[$quadIndices];
              
              let normal = normalize(quad.normal);
              let tangent = normalize(quad.tangent);
              let bitangent = normalize(cross(normal, tangent));
              let u_offset = corners[$vertexIndex].x * quad.size_u;
              let v_offset = corners[$vertexIndex].y * quad.size_v;
              
              let relative_block_pos = quad.block_pos - camera.block_pos;
              let relative_local_pos = quad.local_pos - camera.local_pos;
              let relative_base_pos = 
                vec3f(relative_block_pos) + relative_local_pos;
              let relative_pos = 
                relative_base_pos +
                (tangent * u_offset) +
                (bitangent * v_offset);
              let camera_pos = camera.rotation * relative_pos;
              $position = $camera.projection * vec4f(camera_pos, 1.0);
              $uv = uvs[$vertexIndex];
              $ao = f32((quad.ao >> ($vertexIndex * 8)) & 0xFF) / 255.0;
              """
            }
            fragment {
              val out by canvas
              """
              
              let material = $material[$matId];
              let uv_scaled = 
                mix(
                  material.uv_min,
                  material.uv_max,
                  $uv,
                );
              let base_color = textureSample($texture, $samp, uv_scaled);
              let color = vec4f(base_color.rgb * $ao, base_color.a);
              $out = color;
              """
            }
          }
        val time = TimeSource.Monotonic.markNow()
        val done = Job()

        fun loop() {
          try {
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
            directionHud.update(camera.currentDirection16())
            cameraBuf.update()
            frame(view = msaaTexture.provide()) {
              executeBundles(arrayOf(renderBundle))
            }
            if (end.isCompleted) {
              done.complete()
              return
            }
            requestAnimationFrame { loop() }
          } catch (e: Throwable) {
            logger.error(e) { "Error in main loop" }
            done.completeExceptionally(e)
          }
        }
        loop()
        done.join()
      }
    }
  }

// region helper

context(canvas: CanvasContext, resource: ResourceScope)
private suspend inline fun <R> webgpuContext(
  action: context(
    GPUAdapter, GPUDevice, BufferAllocator
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
  val allocator = device.createBufferAllocator()
  return context(adapter, device, allocator) {
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

context(coroutine: CoroutineScope)
private fun loadImageBitmap(path: String): Deferred<ImageBitmap> =
  coroutine.async {
    val path = path.removePrefix("/")
    val resp = window.fetch("/$path").await()
    val blob = resp.blob().await()
    createImageBitmap(blob).await()
  }

context(alloc: BufferAllocator)
private fun createMaterialBuffer(): Resource<StorageGpuBuffer> {
  val res =
    alloc.static(
      data = floatArrayOf(0f, 0f, 1f, 1f),
      usage = GPUBufferUsage.Storage,
      label = "Material Buffer",
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
private inline fun frame(
  view: GPUTextureView? = null,
  action: GPURenderPassEncoder.()
  -> Unit,
) {
  val surfaceTexture = canvas.getCurrentTexture()
  val commandEncoder = device.createCommandEncoder()
  val renderPassEncoder =
    commandEncoder.beginRenderPass(
      GPURenderPassDescriptor(
        colorAttachments =
          arrayOf(
            view?.let {
              GPURenderPassColorAttachment(
                view = view,
                resolveTarget = surfaceTexture.createView(),
                clearValue = GPUColor(0.8, 0.8, 0.8, 1.0),
                loadOp = GPULoadOp.Clear,
                storeOp = GPUStoreOp.Store,
              )
            } ?: GPURenderPassColorAttachment(
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

private fun createCameraDirectionHud(): CameraDirectionHud {
  val body = document.body ?: error("Document body is not available")
  val container =
    (document.getElementById("camera-direction") as? HTMLDivElement)
      ?: (document.createElement("div") as HTMLDivElement).also {
        it.id = "camera-direction"
        body.appendChild(it)
      }
  container.style.apply {
    position = "fixed"
    top = "12px"
    left = "12px"
    padding = "0.35rem 0.65rem"
    backgroundColor = "rgba(0, 0, 0, 0.75)"
    color = "#FFF"
    borderRadius = "0.35rem"
    fontFamily = "monospace, system-ui"
    fontSize = "0.85rem"
    setProperty("pointer-events", "none")
    zIndex = "1"
  }
  return CameraDirectionHud(container)
}

private class CameraDirectionHud(
  private val container: HTMLDivElement,
) {
  fun update(direction: Direction16) {
    val label = direction.displayName()
    val text = "Direction: $label"
    if (container.textContent == text) return
    container.textContent = text
  }
}

private fun Direction16.displayName(): String =
  name
    .split('_')
    .joinToString(" ") { segment ->
      segment.lowercase().replaceFirstChar(Char::titlecaseChar)
    }

private val logger = logger("Main")

context(canvas: CanvasContext)
private val canvasAspect get() = canvas.width.toDouble() / canvas.height

context(canvas: CanvasContext)
private fun MovableCamera.autoFit(): AutoCloseable =
  canvas.onResize {
    aspect = canvasAspect
  }

// endregion
