
import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.github.oshai.kotlinlogging.Level
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import net.japanesehunter.math.Color
import net.japanesehunter.math.Direction16
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Fov
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.NearFar
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.blue
import net.japanesehunter.math.currentDirection16
import net.japanesehunter.math.down
import net.japanesehunter.math.east
import net.japanesehunter.math.lookAt
import net.japanesehunter.math.meters
import net.japanesehunter.math.north
import net.japanesehunter.math.south
import net.japanesehunter.math.up
import net.japanesehunter.math.west
import net.japanesehunter.math.x
import net.japanesehunter.math.y
import net.japanesehunter.math.z
import net.japanesehunter.math.zero
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
import net.japanesehunter.worldcreate.CameraNavigator
import net.japanesehunter.worldcreate.CameraNavigator.Settings
import net.japanesehunter.worldcreate.GreedyQuad
import net.japanesehunter.worldcreate.MaterialKey
import net.japanesehunter.worldcreate.QuadShape
import net.japanesehunter.worldcreate.toGpuBuffer
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

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
          x = 2.meters
          y = 2.meters
          z = 2.meters
          lookAt(Point3.zero)
          autoFit()
        }
      val navigatorSettings = Settings()
      val navigator = CameraNavigator(currentCanvasElement(), camera, navigatorSettings)
      val controls = createNavigatorControls(navigatorSettings)
      onClose {
        controls.close()
        navigator.close()
      }
      webgpuContext {
        debugPrintLimits()
        val cameraHud = createCameraDirectionHud()
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

            val (vBuf, iBuf) = quads.toGpuBuffer()
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
            navigator.update()
            cameraHud.update(camera.currentDirection16())
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

private fun createNavigatorControls(settings: Settings): NavigatorControls {
  val body = document.body ?: error("Document body is not available")
  val container =
    (document.getElementById("camera-controls") as? HTMLDivElement)
      ?: (document.createElement("div") as HTMLDivElement).also {
        it.id = "camera-controls"
        body.appendChild(it)
      }
  container.style.apply {
    position = "fixed"
    top = "12px"
    right = "12px"
    width = "fit-content"
    padding = "0.75rem"
    backgroundColor = "rgba(0, 0, 0, 0.68)"
    color = "#FFF"
    borderRadius = "0.5rem"
    fontFamily = "monospace, system-ui"
    fontSize = "0.85rem"
    zIndex = "2"
  }

  fun createSectionTitle(text: String): HTMLDivElement =
    (document.createElement("div") as HTMLDivElement).apply {
      this.textContent = text
      style.marginBottom = "0.35rem"
      style.fontWeight = "700"
    }

  fun createSliderRow(
    label: String,
    min: String,
    max: String,
    step: String,
    initial: Double,
    suffix: String,
    onChange: (Double) -> Unit,
  ): Pair<HTMLDivElement, () -> Unit> {
    val row = document.createElement("div") as HTMLDivElement
    row.style.marginBottom = "0.6rem"
    val title = document.createElement("div") as HTMLDivElement
    title.textContent = label
    title.style.marginBottom = "0.2rem"
    val valueLabel = document.createElement("div") as HTMLDivElement
    valueLabel.style.marginTop = "0.2rem"
    val slider = document.createElement("input") as HTMLInputElement
    slider.type = "range"
    slider.min = min
    slider.max = max
    slider.step = step
    slider.value = initial.toString()
    slider.style.width = "75%"

    fun updateLabel(value: Double) {
      valueLabel.textContent = "${value.toString().take(5)} $suffix"
    }
    updateLabel(initial)
    val handler: (Event) -> Unit = {
      slider.value.toDoubleOrNull()?.let { value ->
        onChange(value)
        updateLabel(value)
      }
    }
    slider.addEventListener("input", handler)
    row.appendChild(title)
    row.appendChild(slider)
    row.appendChild(valueLabel)
    return row to { slider.removeEventListener("input", handler) }
  }

  fun keyLabel(code: String): String = code.removePrefix("Key").removePrefix("Digit").ifEmpty { code }

  fun createKeyRow(
    label: String,
    getter: () -> String,
    setter: (String) -> Unit,
  ): Pair<HTMLDivElement, () -> Unit> {
    val row = document.createElement("div") as HTMLDivElement
    row.style.marginBottom = "0.5rem"
    val title = document.createElement("div") as HTMLDivElement
    title.textContent = label
    title.style.marginBottom = "0.2rem"
    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = keyLabel(getter())
    button.style.padding = "0.35rem 0.5rem"
    button.style.borderRadius = "0.35rem"
    button.style.border = "1px solid rgba(255, 255, 255, 0.25)"
    button.style.background = "rgba(255, 255, 255, 0.08)"
    button.style.color = "#FFF"
    button.style.cursor = "pointer"
    var pending: ((Event) -> Unit)? = null
    button.onclick = {
      button.textContent = "Press key..."
      pending?.let {
        window.removeEventListener("keydown", it)
      }
      var capture: ((Event) -> Unit)? = null
      capture =
        listener@{ event ->
          val keyEvent = event as? KeyboardEvent ?: return@listener
          keyEvent.preventDefault()
          keyEvent.stopPropagation()
          setter(keyEvent.code)
          button.textContent = keyLabel(keyEvent.code)
          capture?.let { window.removeEventListener("keydown", it) }
          pending = null
        }
      pending = capture
      capture.let { window.addEventListener("keydown", it) }
    }
    row.appendChild(title)
    row.appendChild(button)
    return row to {
      pending?.let {
        window.removeEventListener("keydown", it)
        pending = null
      }
    }
  }

  val teardown: MutableList<() -> Unit> = mutableListOf()

  fun addRow(
    row: HTMLDivElement,
    close: () -> Unit = {},
  ) {
    container.appendChild(row)
    teardown.add(close)
  }

  container.appendChild(createSectionTitle("Movement Keys"))
  createKeyRow("Move Forward", { settings.forwardKey }, { settings.forwardKey = it }).also { (row, close) ->
    addRow(row, close)
  }
  createKeyRow("Move Backward", { settings.backwardKey }, { settings.backwardKey = it }).also { (row, close) ->
    addRow(row, close)
  }
  createKeyRow("Strafe Left", { settings.leftKey }, { settings.leftKey = it }).also { (row, close) ->
    addRow(row, close)
  }
  createKeyRow("Strafe Right", { settings.rightKey }, { settings.rightKey = it }).also { (row, close) ->
    addRow(row, close)
  }
  createKeyRow("Move Up", { settings.upKey }, { settings.upKey = it }).also { (row, close) ->
    addRow(row, close)
  }
  createKeyRow("Move Down", { settings.downKey }, { settings.downKey = it }).also { (row, close) ->
    addRow(row, close)
  }

  container.appendChild(createSectionTitle("Speed"))
  createSliderRow(
    label = "Horizontal",
    min = "0",
    max = "10",
    step = "0.1",
    initial = settings.horizontalSpeedMetersPerSecond,
    suffix = "m/s",
    onChange = { settings.horizontalSpeedMetersPerSecond = it },
  ).also { (row, close) ->
    addRow(row, close)
  }
  createSliderRow(
    label = "Vertical",
    min = "0",
    max = "10",
    step = "0.1",
    initial = settings.verticalSpeedMetersPerSecond,
    suffix = "m/s",
    onChange = { settings.verticalSpeedMetersPerSecond = it },
  ).also { (row, close) ->
    addRow(row, close)
  }

  container.appendChild(createSectionTitle("Mouse"))
  createSliderRow(
    label = "Sensitivity",
    min = "0",
    max = "5",
    step = "0.01",
    initial = settings.mouseSensitivityDegPerDot,
    suffix = "deg/dot",
    onChange = { settings.mouseSensitivityDegPerDot = it },
  ).also { (row, close) ->
    addRow(row, close)
  }

  return NavigatorControls(
    container = container,
    teardown = teardown,
  )
}

private class NavigatorControls(
  private val container: HTMLDivElement,
  private val teardown: List<() -> Unit>,
) : AutoCloseable {
  override fun close() {
    teardown.forEach { it.invoke() }
    container.remove()
  }
}

private val logger = logger("Main")

context(canvas: CanvasContext)
private val canvasAspect get() = canvas.width.toDouble() / canvas.height

context(canvas: CanvasContext)
private fun currentCanvasElement(): HTMLCanvasElement = canvas.canvas

context(canvas: CanvasContext)
private fun MovableCamera.autoFit(): AutoCloseable =
  canvas.onResize {
    aspect = canvasAspect
  }

private val quads =
  run {
    fun quad(
      min: Point3,
      normal: Direction3,
      tangent: Direction3,
    ) = GreedyQuad(
      shape =
        QuadShape(
          min = min,
          normal = normal,
          tangent = tangent,
          sizeU = 1.meters,
          sizeV = 1.meters,
        ),
      aoLeftBottom = Proportion.ONE,
      aoRightBottom = Proportion.ONE,
      aoLeftTop = Proportion.ONE,
      aoRightTop = Proportion.ONE,
      repeatU = 1,
      repeatV = 1,
      material = MaterialKey.vanilla("doge"),
    )
    listOf(
      // UP
      quad(Point3(0.meters, 1.meters, 0.meters), Direction3.up, Direction3.east),
      // NORTH
      quad(Point3(1.meters, 1.meters, 0.meters), Direction3.north, Direction3.west),
      // EAST
      quad(Point3(1.meters, 1.meters, 1.meters), Direction3.east, Direction3.north),
      // SOUTH
      quad(Point3(0.meters, 1.meters, 1.meters), Direction3.south, Direction3.east),
      // WEST
      quad(Point3(0.meters, 1.meters, 0.meters), Direction3.west, Direction3.south),
      // BOTTOM
      quad(Point3(1.meters, 0.meters, 0.meters), Direction3.down, Direction3.west),
    )
  }

// endregion
