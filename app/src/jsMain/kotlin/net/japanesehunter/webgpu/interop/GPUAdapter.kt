package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPUAdapter {
  fun requestDevice(options: dynamic = definedExternally): Promise<GPUDevice?>
}
