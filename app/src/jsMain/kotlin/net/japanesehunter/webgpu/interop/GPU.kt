package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPU {
  fun requestAdapter(options: dynamic = definedExternally): Promise<GPUAdapter?>

  fun getPreferredCanvasFormat(): String
}
