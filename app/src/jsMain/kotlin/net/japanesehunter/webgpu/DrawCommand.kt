@file:OptIn(ExperimentalAtomicApi::class)

package net.japanesehunter.webgpu

import arrow.fx.coroutines.ResourceScope
import net.japanesehunter.math.Color
import net.japanesehunter.webgpu.interop.GPUColor
import net.japanesehunter.webgpu.interop.GPUCommandEncoder
import net.japanesehunter.webgpu.interop.GPUCullMode
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPULoadOp
import net.japanesehunter.webgpu.interop.GPURenderBundle
import net.japanesehunter.webgpu.interop.GPURenderPassColorAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDepthStencilAttachment
import net.japanesehunter.webgpu.interop.GPURenderPassDescriptor
import net.japanesehunter.webgpu.interop.GPUStoreOp
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureView
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

context(device: GPUDevice)
suspend inline fun buildDrawCommand(
  noinline colorTargetTexture: () -> GPUTextureView,
  noinline clearColor: () -> Color,
  noinline multisampleTexture: (() -> GPUTexture)? = null,
  noinline depthStencilTexture: (() -> GPUTexture)? = null,
  noinline label: () -> String = { "Draw Command" },
  cullMode: GPUCullMode = GPUCullMode.None,
  sampleCount: Int =
    multisampleTexture
      ?.invoke()
      ?.sampleCount ?: 1,
  depthStencilFormat: GPUTextureFormat? =
    depthStencilTexture
      ?.invoke()
      ?.format,
  record: GpuRenderBundleEncoder.() -> GpuRenderBundleEncoder.FragmentCodeDone,
): DrawCommand {
  val renderBundle =
    buildRenderBundle(
      sampleCount = sampleCount,
      depthStencilFormat = depthStencilFormat,
      cullMode = cullMode,
      action = record,
    )
  return DrawCommandImpl(
    colorTargetTexture = colorTargetTexture,
    clearColor = clearColor,
    msaa = multisampleTexture?.let { { it().createView() } },
    depthStencil = depthStencilTexture?.let { { it().createView() } },
    label = label,
    raw = arrayOf(renderBundle),
  )
}

context(device: GPUDevice, canvas: CanvasContext, resource: ResourceScope)
suspend inline fun buildDrawCommand(
  noinline clearColor: () -> Color,
  sampleCount: Int = 4,
  depthStencilFormat: GPUTextureFormat = GPUTextureFormat.Depth24PlusStencil8,
  noinline multisampleTexture: (() -> GPUTexture)? =
    createMsaaTexture(sampleCount),
  noinline depthStencilTexture: (() -> GPUTexture)? =
    createDepthStencilTexture(sampleCount, depthStencilFormat),
  noinline label: () -> String = { "Main Draw" },
  cullMode: GPUCullMode = GPUCullMode.Back,
  record: GpuRenderBundleEncoder.() -> GpuRenderBundleEncoder.FragmentCodeDone,
): DrawCommand =
  buildDrawCommand(
    {
      canvas
        .getCurrentTexture()
        .createView()
    },
    clearColor = clearColor,
    multisampleTexture = multisampleTexture,
    depthStencilTexture = depthStencilTexture,
    label = label,
    cullMode = cullMode,
    sampleCount = sampleCount,
    depthStencilFormat = depthStencilFormat,
    record = record,
  )

interface DrawCommand {
  val raw: Array<GPURenderBundle>

  operator fun plus(
    other: DrawCommand,
  ): DrawCommand

  context(commandEncoder: GPUCommandEncoder)
  fun dispatch()
}

@PublishedApi
internal class DrawCommandImpl(
  private val colorTargetTexture: () -> GPUTextureView,
  private val clearColor: () -> Color,
  private val msaa: (() -> GPUTextureView)?,
  private val depthStencil: (() -> GPUTextureView)?,
  private val label: () -> String,
  override val raw: Array<GPURenderBundle>,
  initialFrame: Int = 0,
) : DrawCommand {
  private val frames = AtomicInt(initialFrame)

  override fun plus(
    other: DrawCommand,
  ): DrawCommand =
    DrawCommandImpl(
      colorTargetTexture = this.colorTargetTexture,
      msaa = this.msaa,
      depthStencil = this.depthStencil,
      label = this.label,
      raw = this.raw + other.raw,
      initialFrame =
        this.frames
          .load(),
      clearColor = this.clearColor,
    )

  context(commandEncoder: GPUCommandEncoder)
  override fun dispatch() {
    val color =
      run {
        val c = clearColor()
        GPUColor(
          r =
            c.r
              .toDouble(),
          g =
            c.g
              .toDouble(),
          b =
            c.b
              .toDouble(),
          a =
            c.a
              .toDouble(),
        )
      }
    val colorAttachment =
      if (msaa == null) {
        GPURenderPassColorAttachment(
          view = colorTargetTexture(),
          loadOp = GPULoadOp.Clear,
          storeOp = GPUStoreOp.Store,
          clearValue = color,
        )
      } else {
        GPURenderPassColorAttachment(
          view = msaa(),
          resolveTarget = colorTargetTexture(),
          loadOp = GPULoadOp.Clear,
          storeOp = GPUStoreOp.Store,
          clearValue = color,
        )
      }
    val depthStencilAttachment =
      depthStencil?.let {
        GPURenderPassDepthStencilAttachment(
          view = depthStencil(),
          depthLoadOp = GPULoadOp.Clear,
          depthStoreOp = GPUStoreOp.Store,
          depthClearValue = 1.0,
          stencilLoadOp = GPULoadOp.Clear,
          stencilStoreOp = GPUStoreOp.Store,
          stencilClearValue = 0.0,
        )
      }
    val pass =
      commandEncoder.beginRenderPass(
        GPURenderPassDescriptor(
          colorAttachments = arrayOf(colorAttachment),
          depthStencilAttachment = depthStencilAttachment,
          label = "${label()} #${frames.fetchAndIncrement()}",
        ),
      )
    pass.executeBundles(raw)
    pass.end()
  }
}
