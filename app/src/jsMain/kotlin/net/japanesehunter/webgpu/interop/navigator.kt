@file:Suppress("ClassName", "ktlint:standard:class-naming", "ktlint:standard:filename")

package net.japanesehunter.webgpu.interop

external object navigator {
  val gpu: GPU?
}
