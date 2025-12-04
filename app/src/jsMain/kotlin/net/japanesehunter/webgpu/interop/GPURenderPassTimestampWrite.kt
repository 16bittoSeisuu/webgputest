package net.japanesehunter.webgpu.interop

fun GPURenderPassTimestampWrite(
  querySet: GPUQuerySet,
  beginningOfPassWriteIndex: Int,
  endOfPassWriteIndex: Int,
): GPURenderPassTimestampWrite =
  js("{}").unsafeCast<GPURenderPassTimestampWrite>().apply {
    this.querySet = querySet
    this.beginningOfPassWriteIndex = beginningOfPassWriteIndex
    this.endOfPassWriteIndex = endOfPassWriteIndex
  }

external interface GPURenderPassTimestampWrite {
  var querySet: GPUQuerySet
  var beginningOfPassWriteIndex: Int
  var endOfPassWriteIndex: Int
}
