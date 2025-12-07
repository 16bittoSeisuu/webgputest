package net.japanesehunter.worldcreate

open class Key(
  val namespace: String,
  val path: String,
) {
  init {
    require(Regex("^[a-zA-Z0-9_]+$").matches(namespace)) {
      "Invalid namespace: $namespace"
    }
    require(Regex("^[a-zA-Z0-9_/]+$").matches(path)) {
      "Invalid path: $path"
    }
  }

  override fun toString(): String = "$namespace:$path"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Key) return false

    if (namespace != other.namespace) return false
    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    var result = namespace.hashCode()
    result = 31 * result + path.hashCode()
    return result
  }

  companion object
}

class MaterialKey(
  namespace: String,
  path: String,
) : Key(namespace, path) {
  companion object {
    fun vanilla(path: String): MaterialKey = MaterialKey("vanilla", path)
  }
}
