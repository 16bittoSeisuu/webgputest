package net.japanesehunter.webgpu

class UnsupportedAdapterException :
  Exception(
    message = "WebGPU Adapter could not be obtained",
  )
