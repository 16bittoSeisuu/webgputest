
import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import net.japanesehunter.math.Aabb
import net.japanesehunter.math.Color
import net.japanesehunter.math.Fov
import net.japanesehunter.math.ImmutableAabb
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.Point3
import net.japanesehunter.math.blue
import net.japanesehunter.math.currentDirection16
import net.japanesehunter.math.intersects
import net.japanesehunter.math.meters
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.webgpu.UnsupportedAdapterException
import net.japanesehunter.webgpu.UnsupportedBrowserException
import net.japanesehunter.webgpu.buildDrawCommand
import net.japanesehunter.webgpu.canvasContext
import net.japanesehunter.webgpu.createBufferAllocator
import net.japanesehunter.webgpu.interop.GPUAdapter
import net.japanesehunter.webgpu.interop.GPUAddressMode
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCommandEncoder
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUExtent3D
import net.japanesehunter.webgpu.interop.GPUFilterMode
import net.japanesehunter.webgpu.interop.GPUImageCopyExternalImage
import net.japanesehunter.webgpu.interop.GPUImageCopyTextureTagged
import net.japanesehunter.webgpu.interop.GPUMipmapFilterMode
import net.japanesehunter.webgpu.interop.GPUOrigin3D
import net.japanesehunter.webgpu.interop.GPUSampler
import net.japanesehunter.webgpu.interop.GPUSamplerDescriptor
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureDimension
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureUsage
import net.japanesehunter.webgpu.interop.createImageBitmap
import net.japanesehunter.webgpu.interop.navigator.gpu
import net.japanesehunter.webgpu.interop.requestAnimationFrame
import net.japanesehunter.worldcreate.BlockState
import net.japanesehunter.worldcreate.FullBlockState
import net.japanesehunter.worldcreate.MaterialKey
import net.japanesehunter.worldcreate.PlayerController
import net.japanesehunter.worldcreate.World
import net.japanesehunter.worldcreate.controller
import net.japanesehunter.worldcreate.entity.Player
import net.japanesehunter.worldcreate.hud.CameraHud
import net.japanesehunter.worldcreate.hud.PlayerHud
import net.japanesehunter.worldcreate.input.inputContext
import net.japanesehunter.worldcreate.toGpuBuffer
import net.japanesehunter.worldcreate.toMeshGpuBuffer
import net.japanesehunter.worldcreate.world.BlockAccess
import net.japanesehunter.worldcreate.world.createFixedStepTickSource
import org.w3c.dom.ImageBitmap
import kotlin.time.Duration.Companion.milliseconds

val end = Job()

fun main() =
  application(loggerLevel = Level.DEBUG) {
    canvasContext {
      inputContext {
        val camera =
          MovableCamera(
            fov = Fov.fromDegrees(60.0),
            nearFar = NearFar(0.1.meters, 128.meters),
            aspect = canvasAspect,
          ).apply {
            autoFit()
          }
        val cameraHud = CameraHud()

        val (tickSource, tickSink) = createFixedStepTickSource(targetStep = 20.milliseconds)
        val blockAccess = ChunkBlockAccess(chunk)
        val player =
          Player(
            tickSource = tickSource,
            blockAccess = blockAccess,
            initialPosition =
              Point3(
                x = 20.meters,
                y = 20.meters,
                z = 20.meters,
              ),
          )
        val playerHud = PlayerHud(player)
        val controllerSettings = PlayerController.Settings()
        val controller = camera.controller(player, controllerSettings)
        var lastFrameTime = window.performance.now()

        webgpuContext {
          debugPrintLimits()
          val cameraBuf = camera.toGpuBuffer().bind()
          val draw =
            buildDrawCommand(clearColor = { Color.blue }) {
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

              val camera by cameraBuf.asUniform(type = "CameraUniform")
              val texture by textureBuffer
              val samp by createSampler()

              val (vBuf, iBuf) = chunk.toMeshGpuBuffer()
              vertex(iBuf) {
                val aBlockPos by vBuf
                val aLocalPos by vBuf
                val aUv by vBuf
                """
              let relative_block_pos = vec3f($aBlockPos - $camera.block_pos);
              let relative_local_pos = $aLocalPos - $camera.local_pos;
              let relative_pos = relative_block_pos + relative_local_pos;
              let pos = $camera.rotation * relative_pos;
              $position = $camera.projection * vec4f(pos, 1.0);
              $uv = $aUv;
              """
              }
              fragment {
                val out by canvas
                """

              let uv_interpolated = $uv;
              let base_color = textureSample($texture, $samp, uv_interpolated);
              let color = vec4f(base_color.rgb, base_color.a);
              $out = color;
              """
              }
            }
          val done = Job()

          fun loop() {
            try {
              val currentTime = window.performance.now()
              val frameDelta = (currentTime - lastFrameTime).milliseconds
              lastFrameTime = currentTime
              tickSink.onEvent(frameDelta)

              controller.update()
              cameraHud.update(camera.currentDirection16())
              playerHud.update()
              cameraBuf.update()

              gpuCommand {
                draw.dispatch()
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

context(device: GPUDevice)
private inline fun gpuCommand(action: GPUCommandEncoder.() -> Unit) {
  val commandEncoder = device.createCommandEncoder()
  commandEncoder.action()
  device.queue.submit(arrayOf(commandEncoder.finish()))
}

private val logger = logger("Main")

context(canvas: CanvasContext)
private val canvasAspect get() = canvas.width.toDouble() / canvas.height

context(canvas: CanvasContext)
private fun MovableCamera.autoFit(): AutoCloseable =
  canvas.onResize {
    aspect = canvasAspect
  }

private val chunk =
  run {
    val doge = FullBlockState(MaterialKey.vanilla("doge"))
    val air = BlockState.Air
    List(World.CHUNK_LENGTH_BLOCKS * 16) { x ->
      List(World.CHUNK_LENGTH_BLOCKS) { y ->
        List(World.CHUNK_LENGTH_BLOCKS * 16) { z ->
          val x = x % World.CHUNK_LENGTH_BLOCKS
          val z = z % World.CHUNK_LENGTH_BLOCKS
          if (y == x + z) {
            doge
          } else {
            air
          }
        }
      }
    }
  }

private class ChunkBlockAccess(
  private val chunk: List<List<List<BlockState>>>,
) : BlockAccess {
  private val sizeX = chunk.size
  private val sizeY = if (chunk.isNotEmpty()) chunk[0].size else 0
  private val sizeZ = if (sizeY > 0 && chunk[0].isNotEmpty()) chunk[0][0].size else 0

  override fun getCollisions(region: Aabb): List<Aabb> {
    val minBlockX = (region.min.x.inWholeMeters - 1).coerceIn(0L, sizeX.toLong() - 1).toInt()
    val maxBlockX = (region.max.x.inWholeMeters + 1).coerceIn(0L, sizeX.toLong() - 1).toInt()
    val minBlockY = (region.min.y.inWholeMeters - 1).coerceIn(0L, sizeY.toLong() - 1).toInt()
    val maxBlockY = (region.max.y.inWholeMeters + 1).coerceIn(0L, sizeY.toLong() - 1).toInt()
    val minBlockZ = (region.min.z.inWholeMeters - 1).coerceIn(0L, sizeZ.toLong() - 1).toInt()
    val maxBlockZ = (region.max.z.inWholeMeters + 1).coerceIn(0L, sizeZ.toLong() - 1).toInt()

    val collisions = mutableListOf<ImmutableAabb>()

    for (x in minBlockX..maxBlockX) {
      for (y in minBlockY..maxBlockY) {
        for (z in minBlockZ..maxBlockZ) {
          val block = chunk.getOrNull(x)?.getOrNull(y)?.getOrNull(z) ?: continue
          if (block == BlockState.Air) continue

          val blockBox =
            Aabb(
              min = Point3(x = x.meters, y = y.meters, z = z.meters),
              max = Point3(x = (x + 1).meters, y = (y + 1).meters, z = (z + 1).meters),
            )

          if (region.intersects(blockBox)) {
            collisions.add(blockBox)
          }
        }
      }
    }

    return collisions
  }
}

// endregion
