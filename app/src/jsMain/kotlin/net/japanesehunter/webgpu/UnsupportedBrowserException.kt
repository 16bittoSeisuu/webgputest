package net.japanesehunter.webgpu

class UnsupportedBrowserException :
  Exception(
    message = "WebGPU is not supported on this browser",
  )
