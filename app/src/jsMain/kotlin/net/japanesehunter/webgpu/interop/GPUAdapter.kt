package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPUAdapter {
  // always non-null
  fun requestDevice(options: dynamic = definedExternally): Promise<GPUDevice>
}
