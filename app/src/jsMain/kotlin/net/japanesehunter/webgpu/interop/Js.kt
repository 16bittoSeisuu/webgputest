package net.japanesehunter.webgpu.interop

import org.w3c.dom.ImageBitmap
import org.w3c.files.Blob
import kotlin.js.Promise

external fun requestAnimationFrame(callback: (timestamp: Double) -> Unit): Int

external fun setTimeout(
  callback: () -> Unit,
  delay: Double,
)

external fun createImageBitmap(image: Blob): Promise<ImageBitmap>
