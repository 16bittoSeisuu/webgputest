@file:OptIn(ExperimentalAtomicApi::class)

package net.japanesehunter.webgpu

import kotlinx.coroutines.await
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.webgpu.interop.GPUBindGroupDescriptor
import net.japanesehunter.webgpu.interop.GPUBindGroupEntry
import net.japanesehunter.webgpu.interop.GPUBindingResource
import net.japanesehunter.webgpu.interop.GPUColorTargetState
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUFragmentState
import net.japanesehunter.webgpu.interop.GPURenderBundle
import net.japanesehunter.webgpu.interop.GPURenderBundleDescriptor
import net.japanesehunter.webgpu.interop.GPURenderBundleEncoderDescriptor
import net.japanesehunter.webgpu.interop.GPURenderPipeline
import net.japanesehunter.webgpu.interop.GPURenderPipelineDescriptor
import net.japanesehunter.webgpu.interop.GPUSampler
import net.japanesehunter.webgpu.interop.GPUShaderModuleDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureView
import net.japanesehunter.webgpu.interop.GPUVertexAttribute
import net.japanesehunter.webgpu.interop.GPUVertexBufferLayout
import net.japanesehunter.webgpu.interop.GPUVertexState
import net.japanesehunter.webgpu.interop.GPUVertexStepMode
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

context(device: GPUDevice)
suspend inline fun buildRenderBundle(
  label: String? = null,
  action: GpuRenderBundleEncoder.() -> GpuRenderBundleEncoder.FragmentCodeDone,
): GPURenderBundle =
  GpuRenderBundleEncoder().run {
    action()
    build(device, label)
  }

class GpuRenderBundleEncoder
  @PublishedApi
  internal constructor() {
    private val indexBuffer: AtomicReference<IndexGpuBuffer?> =
      AtomicReference(null)
    private val headerCode: AtomicReference<(() -> String)?> =
      AtomicReference(null)
    private val vertexCode: AtomicReference<(() -> String)?> =
      AtomicReference(null)
    private val vsOuts: MutableList<VsOut> = mutableListOf()
    private val layout: MutableMap<
      VertexGpuBuffer,
      GPUVertexBufferLayout,
    > = mutableMapOf()
    private val vertexBuffers: MutableList<VertexGpuBuffer> = mutableListOf()
    private val fragmentCode: AtomicReference<(() -> String)?> =
      AtomicReference(null)
    private val targets: MutableList<Pair<String, GPUTextureFormat>> =
      mutableListOf()
    private val bindings: MutableList<Binding> = mutableListOf()

    fun UniformGpuBuffer.asUniform(type: String): PropertyDelegateProvider<
      Any?,
      ReadOnlyProperty<Any?, Binding>,
    > =
      PropertyDelegateProvider { _, property ->
        val name = property.name
        val bindIndex = bindings.size
        val code =
          "@group(0) @binding($bindIndex) var<uniform> $name: $type;"
        val binding = Binding(name, this.asBinding(), code)
        bindings += binding
        ReadOnlyProperty { _, _ -> binding }
      }

    operator fun GPUTextureView.provideDelegate(
      thisRef: Any?,
      property: KProperty<*>,
    ): ReadOnlyProperty<Any?, Binding> {
      val name = property.name
      val bindIndex = bindings.size
      val code =
        "@group(0) @binding($bindIndex) var $name: texture_2d<f32>;"
      val binding = Binding(name, this, code)
      bindings += binding
      return ReadOnlyProperty { _, _ -> binding }
    }

    operator fun GPUSampler.provideDelegate(
      thisRef: Any?,
      property: KProperty<*>,
    ): ReadOnlyProperty<Any?, Binding> {
      val name = property.name
      val bindIndex = bindings.size
      val code =
        "@group(0) @binding($bindIndex) var $name: sampler;"
      val binding = Binding(name, this, code)
      bindings += binding
      return ReadOnlyProperty { _, _ -> binding }
    }

    fun vsOut(
      type: String,
      interpolation: String? = null,
    ): PropertyDelegateProvider<
      Any?,
      ReadOnlyProperty<Any?, VsOut>,
    > =
      PropertyDelegateProvider { _, property ->
        val name = property.name
        val ip = interpolation?.let { "@interpolation($it) " } ?: ""
        val ret =
          VsOut(
            name,
            "$ip@location(${vsOuts.size}) $name: $type,",
          ).also { vsOuts += it }
        ReadOnlyProperty { _, _ -> ret }
      }

    fun header(action: () -> String) {
      headerCode.store(action)
    }

    fun vertex(
      indices: IndexGpuBuffer? = null,
      action: VertexScope.() -> String,
    ) {
      indexBuffer.store(indices)
      val scope = VertexScope()
      val code = scope.action().trimIndent()
      vertexCode.store {
        """
        struct VsOut {
          @builtin(position) ${scope.position.removePrefix("out.")}: vec4f,
          ${vsOuts.joinToString("\n") { it.code }}
        }
        
        @vertex
        fn vs_main(
          @builtin(vertex_index) ${scope.vertexIndex}: u32,
          @builtin(instance_index) ${scope.instanceIndex}: u32,
          ${scope.attributes.joinToString("\n")}
        ) -> VsOut {
          var out: VsOut;
          $code
          return out;
        }
        """.trimIndent()
      }
    }

    fun fragment(code: FragmentScope.() -> String): FragmentCodeDone =
      FragmentCodeDone.also {
        val scope = FragmentScope()
        vsOuts.forEach(VsOut::setFragmentMode)
        val fragmentCodeStr = scope.code().trimIndent()
        targets += scope.targets
        fragmentCode.store {
          """
          struct FsOut {
            ${
            scope.targets.mapIndexed { i, (name) ->
              "@location($i) ${name.removePrefix("out.")}: vec4f,"
            }.joinToString("\n")
          }
          }
            
          @fragment
          fn fs_main(
            ${vsOuts.joinToString("\n") { it.code }}
          ) -> FsOut {
            var out: FsOut;
            $fragmentCodeStr
            return out;
          }
          """.trimIndent()
        }
      }

    @PublishedApi
    internal suspend fun build(
      device: GPUDevice,
      label: String?,
    ): GPURenderBundle =
      device
        .createRenderBundleEncoder(
          GPURenderBundleEncoderDescriptor(
            colorFormats = targets.map { (_, format) -> format }.toTypedArray(),
            label = label?.let { "$it-bundle-encoder" },
          ),
        ).run {
          val pipeline = createPipeline(device)
          setPipeline(pipeline)
          vertexBuffers.forEachIndexed { i, v ->
            setVertexBuffer(i, v.raw, offset = v.offset, size = v.size)
          }
          val bindGroup =
            device.createBindGroup(
              GPUBindGroupDescriptor(
                layout = pipeline.getBindGroupLayout(0),
                entries =
                  bindings
                    .mapIndexed { i, binding ->
                      GPUBindGroupEntry(
                        binding = i,
                        resource = binding.resource,
                      )
                    }.toTypedArray(),
              ),
            )
          setBindGroup(0, bindGroup)
          val indexBuf = indexBuffer.load()
          val instanceCount =
            vertexBuffers
              .filterIsInstance<InstanceGpuBuffer>()
              .minOfOrNull { it.size / it.stride }
              ?: 1
          if (indexBuf != null) {
            setIndexBuffer(
              indexBuf.raw,
              indexBuf.indexFormat,
              indexBuf.offset,
              indexBuf.size,
            )
            drawIndexed(
              indexBuf.indexCount,
              instanceCount = instanceCount,
            )
          } else {
            draw(
              vertexCount =
                vertexBuffers.minOfOrNull {
                  it.size / it.stride
                } ?: 3,
              instanceCount = instanceCount,
            )
          }
          finish(
            GPURenderBundleDescriptor(
              label = label,
            ),
          )
        }

    private suspend fun createPipeline(device: GPUDevice): GPURenderPipeline {
      val header =
        headerCode.load()?.invoke() ?: ""
      val bindingCode = bindings.joinToString("\n") { it.code }
      val vertexCode =
        vertexCode.load()?.invoke()
          ?: error("Vertex shader code is not defined")
      val fragmentCode =
        fragmentCode.load()?.invoke()
          ?: error("Fragment shader code is not defined")
      val code = header + bindingCode + vertexCode + "\n" + fragmentCode
      val module =
        device.createShaderModule(
          GPUShaderModuleDescriptor(code = code),
        )
      val vertexState =
        GPUVertexState(
          module = module,
          buffers = layout.values.toTypedArray(),
        )
      val fragmentState =
        GPUFragmentState(
          module = module,
          targets =
            targets
              .map { (_, format) ->
                GPUColorTargetState(format = format)
              }.toTypedArray(),
        )
      return device
        .createRenderPipelineAsync(
          GPURenderPipelineDescriptor(
            vertex = vertexState,
            fragment = fragmentState,
          ),
        ).await()
    }

    sealed interface FragmentCodeDone {
      private companion object Default : FragmentCodeDone
    }

    class VsOut internal constructor(
      val name: String,
      internal val code: String,
    ) {
      internal var prefix: String = "out."
        private set

      override fun toString(): String = "$prefix$name"

      internal fun setFragmentMode() {
        prefix = ""
      }
    }

    class Binding internal constructor(
      val name: String,
      internal val resource: GPUBindingResource,
      internal val code: String,
    ) {
      override fun toString(): String = name
    }

    inner class VertexScope internal constructor() {
      val position: String = "out._position"
      val vertexIndex: String = "_vertex_index"
      val instanceIndex: String = "_instance_index"
      val location: AtomicInt = AtomicInt(0)
      internal val attributes: MutableList<String> = mutableListOf()
      internal val bufferAttrIndex: MutableMap<VertexGpuBuffer, Int> =
        mutableMapOf()

      operator fun VertexGpuBuffer.provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
      ): ReadOnlyProperty<Any?, String> =
        provideDelegateInternal(
          name = "vertexAttr${property.name}",
          stepMode = GPUVertexStepMode.Vertex,
        )

      operator fun InstanceGpuBuffer.provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
      ): ReadOnlyProperty<Any?, String> =
        provideDelegateInternal(
          name = "instanceAttr${property.name}",
          stepMode = GPUVertexStepMode.Instance,
        )

      private fun VertexGpuBuffer.provideDelegateInternal(
        name: String,
        stepMode: GPUVertexStepMode,
      ): ReadOnlyProperty<Any?, String> {
        val index = bufferAttrIndex.getOrElse(this) { 0 }
        bufferAttrIndex[this] = index + 1
        check(index < formats.size) {
          "VertexGpuBuffer '${this.label}' has only ${formats.size} attributes, " +
            "but tried to access attribute index $index"
        }
        val location = location.fetchAndIncrement()
        val type = formats[index].type
        val currentLayout =
          layout.getOrElse(this) {
            GPUVertexBufferLayout(
              arrayStride = formats.sumOf { it.sizeInBytes },
              attributes = emptyArray(),
            )
          }
        layout[this] =
          GPUVertexBufferLayout(
            arrayStride = currentLayout.arrayStride,
            stepMode = stepMode,
            attributes =
              currentLayout.attributes +
                GPUVertexAttribute(
                  format = formats[index].raw,
                  offset = offsets[index],
                  shaderLocation = location,
                ),
          )
        attributes += "@location($location) $name: $type,"
        vertexBuffers += this
        return ReadOnlyProperty { _, _ -> name }
      }

      private val GpuVertexFormat.type
        get() =
          when (this) {
            GpuVertexFormat.Uint8x2 -> "vec2<u8>"
            GpuVertexFormat.Uint8x4 -> "vec4<u8>"
            GpuVertexFormat.Sint8x2 -> "vec2<i8>"
            GpuVertexFormat.Sint8x4 -> "vec4<i8>"
            GpuVertexFormat.Unorm8x2 -> "vec2f"
            GpuVertexFormat.Unorm8x4 -> "vec4f"
            GpuVertexFormat.Snorm8x2 -> "vec2f"
            GpuVertexFormat.Snorm8x4 -> "vec4f"
            GpuVertexFormat.Uint16x2 -> "vec2<u16>"
            GpuVertexFormat.Uint16x4 -> "vec4<u16>"
            GpuVertexFormat.Sint16x2 -> "vec2<i16>"
            GpuVertexFormat.Sint16x4 -> "vec4<i16>"
            GpuVertexFormat.Unorm16x2 -> "vec2f"
            GpuVertexFormat.Unorm16x4 -> "vec4f"
            GpuVertexFormat.Snorm16x2 -> "vec2f"
            GpuVertexFormat.Snorm16x4 -> "vec4f"
            GpuVertexFormat.Float16x2 -> "vec2<f16>"
            GpuVertexFormat.Float16x4 -> "vec4<f16>"
            GpuVertexFormat.Float32 -> "f32"
            GpuVertexFormat.Float32x2 -> "vec2f"
            GpuVertexFormat.Float32x3 -> "vec3f"
            GpuVertexFormat.Float32x4 -> "vec4f"
            GpuVertexFormat.Uint32 -> "u32"
            GpuVertexFormat.Uint32x2 -> "vec2u"
            GpuVertexFormat.Uint32x3 -> "vec3u"
            GpuVertexFormat.Uint32x4 -> "vec4u"
            GpuVertexFormat.Sint32 -> "i32"
            GpuVertexFormat.Sint32x2 -> "vec2i"
            GpuVertexFormat.Sint32x3 -> "vec3i"
            GpuVertexFormat.Sint32x4 -> "vec4i"
            GpuVertexFormat.Unorm10_10_10_2 -> "u32"
          }
    }

    class FragmentScope {
      val targets: MutableList<Pair<String, GPUTextureFormat>> =
        mutableListOf()

      context(canvas: CanvasContext)
      val canvas: PropertyDelegateProvider<
        Any?,
        ReadOnlyProperty<Any?, String>,
      > get() = target(canvas.preferredFormat)

      fun target(format: GPUTextureFormat): PropertyDelegateProvider<
        Any?,
        ReadOnlyProperty<Any?, String>,
      > =
        PropertyDelegateProvider { _, property ->
          val name = "out.${property.name}"
          targets += name to format
          ReadOnlyProperty { _, _ -> name }
        }
    }
  }
