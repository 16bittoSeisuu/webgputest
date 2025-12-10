
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.UnsupportedAdapterException
import net.japanesehunter.webgpu.UnsupportedBrowserException
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.buildRenderBundle
import net.japanesehunter.webgpu.canvasContext
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPassEncoder
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUTextureView
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.interop.requestAnimationFrame
import net.japanesehunter.webgpu.pos3D
import net.japanesehunter.webgpu.rgbaColor
import net.japanesehunter.webgpu.u16

val end = Job()

fun main() =
  application(loggerLevel = Level.DEBUG) {
    canvasContext {
//      val camera =
//        MovableCamera(
//          fov = Fov.fromDegrees(60.0),
//          nearFar = NearFar(0.1.meters, 128.meters),
//          aspect = canvasAspect,
//        ).apply {
//          autoFit()
//        }
//      val quads =
//        List(101) { x ->
//          List(101) { y ->
//            Quad(
//              pos =
//                Point3(
//                  x = (x - 50).meters,
//                  y = (y - 50).meters,
//                  z = 0.meters,
//                ),
//              normal = Direction3.south,
//              tangent = Direction3.east,
//              aoLeftBottom = Proportion.QUARTER,
//              aoRightBottom = Proportion.HALF,
//              aoLeftTop = Proportion.HALF,
//              aoRightTop = Proportion.ONE,
//              sizeU = 0.9.meters,
//              sizeV = 0.9.meters,
//              materialId = 0,
//            )
//          }
//        }.flatten()

      webgpuContext {
        debugPrintLimits()
//        val directionHud = createCameraDirectionHud()
        val indexBuffer = IndexGpuBuffer.u16(0, 1, 2, 1, 3, 2).bind()
//        val cameraBuf = camera.toGpuBuffer().bind()
        val renderBundle =
          buildRenderBundle {
            val vertexPosBuffer = vertexPosBuffer()
            val vertexColorBuffer = vertexColorBuffer()
            val color by vsOut("vec4f")
            vertex(indexBuffer) {
              val posBuf by vertexPosBuffer
              val colorBuf by vertexColorBuffer
              """
              $position = vec4f($posBuf, 1.0);
              $color = $colorBuf;
              """
            }
            fragment {
              val out by canvas
              """
              $out = $color;
              """
            }
          }
//        val time = TimeSource.Monotonic.markNow()
        val done = Job()

        fun loop() {
          try {
//            camera.x = 1.meters * time.rad(perSec = 15.0.degrees).sin()
//            camera.y = 1.meters * time.rad(perSec = 30.0.degrees).cos()
//            camera.z = 5.meters * time.rad(perSec = 10.0.degrees).cos()
//            val point =
//              run {
//                val rad = time.rad(perSec = 20.0.degrees)
//                Point3(
//                  x = 1.meters * rad.cos(),
//                  y = 1.meters * rad.sin(),
//                  z = 0.meters,
//                )
//              }
//            camera.lookAt(point)
//            directionHud.update(camera.currentDirection16())
//            cameraBuf.update()
            frame {
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

context(alloc: BufferAllocator)
private val vertexPosBuffer: Resource<VertexGpuBuffer>
  get() =
    VertexGpuBuffer.pos3D(
      floatArrayOf(
        -0.5f,
        -0.5f,
        0.0f, // left-bottom
        0.5f,
        -0.5f,
        0.0f, // right-bottom
        -0.5f,
        0.5f,
        0.0f, // left-top
        0.5f,
        0.5f,
        0.0f, // right-top
      ),
    )

context(alloc: BufferAllocator)
private val vertexColorBuffer: Resource<VertexGpuBuffer>
  get() =
    VertexGpuBuffer.rgbaColor(
      floatArrayOf(
        1.0f,
        0.0f,
        1.0f,
        1.0f, // magenta
        1.0f,
        1.0f,
        1.0f,
        1.0f, // white
        0.0f,
        1.0f,
        1.0f,
        1.0f, // cyan
        1.0f,
        1.0f,
        0.0f,
        1.0f, // yellow
      ),
    )

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

// context(coroutine: CoroutineScope)
// private fun loadImageBitmap(path: String): Deferred<ImageBitmap> =
//  coroutine.async {
//    val path = path.removePrefix("/")
//    val resp = window.fetch("/$path").await()
//    val blob = resp.blob().await()
//    createImageBitmap(blob).await()
//  }

// context(alloc: BufferAllocator)
// private fun createUvsBuffer(): Resource<StorageGpuBuffer> {
//  val res =
//    alloc.static(
//      data = floatArrayOf(0f, 0f, 1f, 1f),
//      usage = GPUBufferUsage.Storage,
//      label = "UVs Buffer",
//    )
//  return resource {
//    val buf = res.bind()
//    object : StorageGpuBuffer, GpuBuffer by buf {}
//  }
// }
//
// context(device: GPUDevice, coroutine: CoroutineScope)
// private fun createTexture(path: String): Deferred<GPUTexture> =
//  coroutine.async {
//    val bitmap = loadImageBitmap(path).await()
//    val textureSize = GPUExtent3D(bitmap.width, bitmap.height, 1)
//    val descriptor =
//      GPUTextureDescriptor(
//        size = textureSize,
//        mipLevelCount = 1,
//        sampleCount = 1,
//        dimension = GPUTextureDimension.D2,
//        format = GPUTextureFormat.Rgba8UnormSrgb,
//        usage =
//          GPUTextureUsage.TextureBinding + GPUTextureUsage.CopyDst +
//            GPUTextureUsage.RenderAttachment,
//      )
//    val texture = device.createTexture(descriptor)
//    device.queue.copyExternalImageToTexture(
//      GPUImageCopyExternalImage(
//        source = bitmap,
//        origin = GPUOrigin3D(),
//        flipY = false,
//        premultipliedAlpha = true,
//      ),
//      GPUImageCopyTextureTagged(
//        texture = texture,
//        mipLevel = 0,
//        origin = GPUOrigin3D(),
//      ),
//      textureSize,
//    )
//    texture
//  }
//
// context(device: GPUDevice)
// private fun createSampler(): GPUSampler {
//  val descriptor =
//    GPUSamplerDescriptor(
//      magFilter = GPUFilterMode.Nearest,
//      minFilter = GPUFilterMode.Nearest,
//      mipmapFilter = GPUMipmapFilterMode.Nearest,
//      addressModeU = GPUAddressMode.Repeat,
//      addressModeV = GPUAddressMode.Repeat,
//      addressModeW = GPUAddressMode.Repeat,
//    )
//  return device.createSampler(descriptor)
// }
//
// context(device: GPUDevice, canvas: CanvasContext, resource: ResourceScope)
// private fun createMsaaTexture(sampleCount: Int): GPUTexture {
//  val textureDescriptor =
//    GPUTextureDescriptor(
//      size = GPUExtent3D(canvas.width, canvas.height, 1),
//      sampleCount = sampleCount,
//      format = canvas.preferredFormat,
//      usage = GPUTextureUsage.RenderAttachment,
//    )
//  val ret = device.createTexture(textureDescriptor)
//  resource.onClose {
//    ret.destroy()
//  }
//  return ret
// }

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
//
// private fun TimeMark.rad(perSec: Angle): Angle {
//  val elapsed = this.elapsedNow()
//  val seconds = elapsed.inWholeMilliseconds / 1000.0
//  return perSec * seconds
// }
//
// private fun createCameraDirectionHud(): CameraDirectionHud {
//  val body = document.body ?: error("Document body is not available")
//  val container =
//    (document.getElementById("camera-direction") as? HTMLDivElement)
//      ?: (document.createElement("div") as HTMLDivElement).also {
//        it.id = "camera-direction"
//        body.appendChild(it)
//      }
//  container.style.apply {
//    position = "fixed"
//    top = "12px"
//    left = "12px"
//    padding = "0.35rem 0.65rem"
//    backgroundColor = "rgba(0, 0, 0, 0.75)"
//    color = "#FFF"
//    borderRadius = "0.35rem"
//    fontFamily = "monospace, system-ui"
//    fontSize = "0.85rem"
//    setProperty("pointer-events", "none")
//    zIndex = "9000"
//  }
//  return CameraDirectionHud(container)
// }

// private class CameraDirectionHud(
//  private val container: HTMLDivElement,
// ) {
//  fun update(direction: Direction16) {
//    val label = direction.displayName()
//    container.textContent = "Direction: $label"
//  }
// }
//
// private fun Direction16.displayName(): String =
//  name
//    .split('_')
//    .joinToString(" ") { segment ->
//      segment.lowercase().replaceFirstChar(Char::titlecaseChar)
//    }

private val logger = logger("Main")
//
// context(canvas: CanvasContext)
// private val canvasAspect get() = canvas.width.toDouble() / canvas.height
//
// context(canvas: CanvasContext)
// private fun MovableCamera.autoFit(): AutoCloseable =
//  canvas.onResize {
//    aspect = canvasAspect
//  }

// endregion
