package net.japanesehunter.webgpu.interop

fun GPUShaderModuleDescriptor(
  code: String,
  label: String? = null,
): GPUShaderModuleDescriptor {
  val obj = {}.unsafeCast<GPUShaderModuleDescriptor>()
  obj.code = code
  if (label != null) obj.label = label
  return obj
}

external interface GPUShaderModuleDescriptor {
  var code: String
  var label: String?
}
