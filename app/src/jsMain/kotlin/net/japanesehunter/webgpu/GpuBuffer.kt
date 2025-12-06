package net.japanesehunter.webgpu

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.japanesehunter.webgpu.interop.GPUBuffer
import net.japanesehunter.webgpu.interop.GPUBufferDescriptor
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUCommandEncoder
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUQueue
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

fun GPUDevice.createBufferAllocator(): BufferAllocator = BufferAllocatorImpl(this)

// region interface

/**
 * A view over a [GPUBuffer], optionally narrowed via [offset]/[size].
 */
interface GpuBuffer {
  /**
   * The underlying WebGPU buffer backing this view.
   */
  val raw: GPUBuffer

  /**
   * Byte offset of this view relative to [raw].
   */
  val offset: Int

  /**
   * Byte length of this view.
   */
  val size: Int

  /**
   * Developer-assigned label surfaced by browser tooling.
   */
  var label: String

  /**
   * Bitwise combination of [GPUBufferUsage] flags describing how the buffer may be used.
   */
  val usage: GPUBufferUsage

  /**
   * Creates another view into the same [raw] buffer narrowed to the given range.
   */
  fun slice(
    offset: Int,
    size: Int,
  ): GpuBuffer
}

/**
 * A GPU buffer view that can be used as a copy source.
 */
interface CopySrcGpuBuffer : GpuBuffer {
  /**
   * Enqueues a buffer-to-buffer copy on the given [GPUCommandEncoder] context.
   */
  context(encoder: GPUCommandEncoder)
  fun writeTo(dst: MutableGpuBuffer)
}

/**
 * A GPU buffer view whose contents can be updated via [write], backed by queue.writeBuffer.
 */
interface MutableGpuBuffer : GpuBuffer {
  /**
   * Writes array data into this buffer view using `GPUQueue.writeBuffer`.
   * May perform the work asynchronously.
   */
  fun write(data: ByteArray)

  fun write(data: FloatArray)

  fun write(data: ShortArray)

  fun write(data: IntArray)
}

interface MutableCopySrcGpuBuffer :
  MutableGpuBuffer,
  CopySrcGpuBuffer

/**
 * Creates GPU buffers backed by [Resource] to ensure `destroy()` is called automatically.
 *
 * Data uploads use `GPUQueue.writeBuffer`; buffer-to-buffer copies are enqueued on a
 * caller-provided [GPUCommandEncoder] via context parameters.
 */
interface BufferAllocator {
  /**
   * Allocates an immutable buffer view of [size] bytes with the given [usage] flags.
   */
  fun static(
    size: Int,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<GpuBuffer>

  /**
   * Allocates an immutable buffer initialized with [data] via `queue.writeBuffer`.
   */
  fun static(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<GpuBuffer>

  fun static(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<GpuBuffer>

  fun static(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<GpuBuffer>

  fun static(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<GpuBuffer>

  /**
   * Allocates a copy-source buffer view of [size] bytes.
   */
  fun staticCopySrc(
    size: Int,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<CopySrcGpuBuffer>

  /**
   * Allocates a copy-source buffer initialized with [data] via `queue.writeBuffer`.
   */
  fun staticCopySrc(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<CopySrcGpuBuffer>

  fun staticCopySrc(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<CopySrcGpuBuffer>

  fun staticCopySrc(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<CopySrcGpuBuffer>

  fun staticCopySrc(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<CopySrcGpuBuffer>

  /**
   * Allocates a CPU-writable buffer view of [size] bytes. Writes use `queue.writeBuffer`.
   */
  fun mutable(
    size: Int,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableGpuBuffer>

  /**
   * Allocates a CPU-writable buffer initialized with [data] via `queue.writeBuffer`.
   */
  fun mutable(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableGpuBuffer>

  fun mutable(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableGpuBuffer>

  fun mutable(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableGpuBuffer>

  fun mutable(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableGpuBuffer>

  /**
   * Allocates a CPU-writable buffer that can also act as a copy source.
   */
  fun mutableCopySrc(
    size: Int,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableCopySrcGpuBuffer>

  /**
   * Allocates a CPU-writable copy-source buffer initialized with [data].
   */
  fun mutableCopySrc(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableCopySrcGpuBuffer>

  fun mutableCopySrc(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableCopySrcGpuBuffer>

  fun mutableCopySrc(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableCopySrcGpuBuffer>

  fun mutableCopySrc(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String = "",
  ): Resource<MutableCopySrcGpuBuffer>
}

// endregion

// region implementation

private class BufferAllocatorImpl(
  private val device: GPUDevice,
) : BufferAllocator {
  private val queue: GPUQueue = device.queue

  override fun static(
    size: Int,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<GpuBuffer> {
    val res = allocate(size, usage, label)
    return resource {
      val raw = res.bind()
      StaticBufferImpl(
        raw = raw,
        offset = 0,
        size = size,
      )
    }
  }

  override fun static(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<GpuBuffer> {
    val res = allocate(data.size, usage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticBufferImpl(
        raw = raw,
        offset = 0,
        size = data.size,
      )
    }
  }

  override fun static(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<GpuBuffer> {
    val byteSize = data.byteSize
    val res = allocate(byteSize, usage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun static(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<GpuBuffer> {
    val byteSize = data.byteSize
    val res = allocate(byteSize, usage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun static(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<GpuBuffer> {
    val byteSize = data.byteSize
    val res = allocate(byteSize, usage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun staticCopySrc(
    size: Int,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<CopySrcGpuBuffer> {
    val effectiveUsage = usage + GPUBufferUsage.CopySrc
    val res = allocate(size, effectiveUsage, label)
    return resource {
      val raw = res.bind()
      StaticCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = size,
      )
    }
  }

  override fun staticCopySrc(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<CopySrcGpuBuffer> {
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc
    val res = allocate(data.size, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = data.size,
      )
    }
  }

  override fun staticCopySrc(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<CopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun staticCopySrc(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<CopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun staticCopySrc(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<CopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      StaticCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
      )
    }
  }

  override fun mutable(
    size: Int,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableGpuBuffer> {
    val effectiveUsage = usage + GPUBufferUsage.CopyDst
    val res = allocate(size, effectiveUsage, label)
    return resource {
      val raw = res.bind()
      MutableBufferImpl(
        raw = raw,
        offset = 0,
        size = size,
        queue = queue,
      )
    }
  }

  override fun mutable(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableGpuBuffer> {
    val effectiveUsage =
      usage + GPUBufferUsage.CopyDst
    val res = allocate(data.size, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableBufferImpl(
        raw = raw,
        offset = 0,
        size = data.size,
        queue = queue,
      )
    }
  }

  override fun mutable(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  override fun mutable(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  override fun mutable(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  override fun mutableCopySrc(
    size: Int,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableCopySrcGpuBuffer> {
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc + GPUBufferUsage.CopyDst
    val res = allocate(size, effectiveUsage, label)
    return resource {
      val raw = res.bind()
      MutableCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = size,
        queue = queue,
      )
    }
  }

  override fun mutableCopySrc(
    data: ByteArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableCopySrcGpuBuffer> {
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc + GPUBufferUsage.CopyDst
    val res = allocate(data.size, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = data.size,
        queue = queue,
      )
    }
  }

  override fun mutableCopySrc(
    data: FloatArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableCopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  override fun mutableCopySrc(
    data: ShortArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableCopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  override fun mutableCopySrc(
    data: IntArray,
    usage: GPUBufferUsage,
    label: String,
  ): Resource<MutableCopySrcGpuBuffer> {
    val byteSize = data.byteSize
    val effectiveUsage =
      usage + GPUBufferUsage.CopySrc + GPUBufferUsage.CopyDst
    val res = allocate(byteSize, effectiveUsage, label, map = true)
    return resource {
      val raw = res.bind()
      data.writeTo(raw.getMappedRange())
      raw.unmap()
      MutableCopySrcBufferImpl(
        raw = raw,
        offset = 0,
        size = byteSize,
        queue = queue,
      )
    }
  }

  private fun allocate(
    size: Int,
    usage: GPUBufferUsage,
    label: String,
    map: Boolean = false,
  ): Resource<GPUBuffer> =
    resource {
      withContext(Dispatchers.Default) {
        device
          .createBuffer(
            GPUBufferDescriptor(
              size = size,
              usage = usage,
              label = label,
              mappedAtCreation = map,
            ),
          ).apply {
            onClose {
              destroy()
            }
          }
      }
    }
}

private open class StaticBufferImpl(
  final override val raw: GPUBuffer,
  final override val offset: Int,
  final override val size: Int,
) : GpuBuffer {
  override var label: String
    get() = raw.label
    set(value) {
      raw.label = value
    }
  override val usage: GPUBufferUsage get() = raw.usage

  override fun slice(
    offset: Int,
    size: Int,
  ): GpuBuffer {
    requireRange(offset, size)
    return StaticBufferImpl(
      raw = raw,
      offset = this.offset + offset,
      size = size,
    )
  }
}

private class StaticCopySrcBufferImpl(
  raw: GPUBuffer,
  offset: Int,
  size: Int,
) : StaticBufferImpl(raw, offset, size),
  CopySrcGpuBuffer {
  context(encoder: GPUCommandEncoder)
  override fun writeTo(dst: MutableGpuBuffer) {
    require(dst.size >= size) {
      "Destination buffer is too small: dst.size=${dst.size}, src.size=$size"
    }
    encoder.copyBufferToBuffer(raw, offset, dst.raw, dst.offset, size)
  }
}

private open class MutableBufferImpl(
  raw: GPUBuffer,
  offset: Int,
  size: Int,
  private val queue: GPUQueue,
) : StaticBufferImpl(raw, offset, size),
  MutableGpuBuffer {
  override fun write(data: ByteArray) {
    data.requireFits(size)
    val buffer = data.toArrayBuffer()
    queue.writeBuffer(raw, offset, buffer, 0, data.byteSize)
  }

  override fun write(data: FloatArray) {
    data.requireFits(size)
    val buffer = data.toArrayBuffer()
    queue.writeBuffer(raw, offset, buffer, 0, data.byteSize)
  }

  override fun write(data: ShortArray) {
    data.requireFits(size)
    val buffer = data.toArrayBuffer()
    queue.writeBuffer(raw, offset, buffer, 0, data.byteSize)
  }

  override fun write(data: IntArray) {
    data.requireFits(size)
    val buffer = data.toArrayBuffer()
    queue.writeBuffer(raw, offset, buffer, 0, data.byteSize)
  }
}

private class MutableCopySrcBufferImpl(
  raw: GPUBuffer,
  offset: Int,
  size: Int,
  queue: GPUQueue,
) : MutableBufferImpl(raw, offset, size, queue),
  MutableCopySrcGpuBuffer {
  context(encoder: GPUCommandEncoder)
  override fun writeTo(dst: MutableGpuBuffer) {
    require(dst.size >= size) {
      "Destination buffer is too small: dst.size=${dst.size}, src.size=$size"
    }
    encoder.copyBufferToBuffer(raw, offset, dst.raw, dst.offset, size)
  }
}

private fun ByteArray.toArrayBuffer(): ArrayBuffer {
  val array = Uint8Array(size)
  for (i in indices) {
    array.asDynamic()[i] = this[i]
  }
  return array.buffer
}

private fun FloatArray.toArrayBuffer(): ArrayBuffer {
  val array = Float32Array(size)
  for (i in indices) {
    array.asDynamic()[i] = this[i]
  }
  return array.buffer
}

private fun ShortArray.toArrayBuffer(): ArrayBuffer {
  val array = Int16Array(size)
  for (i in indices) {
    array[i] = this[i]
  }
  return array.buffer
}

private fun IntArray.toArrayBuffer(): ArrayBuffer {
  val array = Int32Array(size)
  for (i in indices) {
    array[i] = this[i]
  }
  return array.buffer
}

private fun ByteArray.writeTo(buffer: ArrayBuffer) {
  val array = Uint8Array(buffer)
  for (i in indices) {
    array[i] = this[i]
  }
}

private fun FloatArray.writeTo(buffer: ArrayBuffer) {
  val array = Float32Array(buffer)
  for (i in indices) {
    array[i] = this[i]
  }
}

private fun ShortArray.writeTo(buffer: ArrayBuffer) {
  val array = Int16Array(buffer)
  for (i in indices) {
    array[i] = this[i]
  }
}

private fun IntArray.writeTo(buffer: ArrayBuffer) {
  val array = Int32Array(buffer)
  for (i in indices) {
    array[i] = this[i]
  }
}

private val ByteArray.byteSize: Int get() = size
private val FloatArray.byteSize: Int get() = size * Float.SIZE_BYTES
private val ShortArray.byteSize: Int get() = size * Short.SIZE_BYTES
private val IntArray.byteSize: Int get() = size * Int.SIZE_BYTES

private fun GpuBuffer.requireRange(
  offset: Int,
  size: Int,
) {
  require(offset >= 0) { "offset must be non-negative, but got: $offset" }
  require(size >= 0) { "size must be non-negative, but got: $size" }
  require(offset + size <= this.size) {
    "offset + size must be <= view size (${this.size}), but got offset=$offset, size=$size"
  }
}

private fun ByteArray.requireFits(limit: Int) = requireFits(byteSize, limit, "ByteArray")

private fun FloatArray.requireFits(limit: Int) = requireFits(byteSize, limit, "FloatArray")

private fun ShortArray.requireFits(limit: Int) = requireFits(byteSize, limit, "ShortArray")

private fun IntArray.requireFits(limit: Int) = requireFits(byteSize, limit, "IntArray")

private fun requireFits(
  dataSize: Int,
  limit: Int,
  typeName: String,
) {
  require(dataSize <= limit) {
    "$typeName byteSize ($dataSize) must be <= buffer view size ($limit)"
  }
}

// endregion
