package net.japanesehunter.math

import korlibs.datastructure.WeakMap
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@OptIn(ExperimentalAtomicApi::class)
internal class WeakProperty<V>(
  value: V,
) : PropertyDelegateProvider<Any, ReadWriteProperty<Any, V?>> {
  private var tmp: AtomicReference<V?> = AtomicReference(value)
  private val map = WeakMap<Any, V>()

  override fun provideDelegate(
    thisRef: Any,
    property: KProperty<*>,
  ): ReadWriteProperty<Any, V?> {
    tmp.update {
      if (it != null) {
        map[thisRef] = it
      }
      null
    }
    return object : ReadWriteProperty<Any, V?> {
      override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
      ): V? = map[thisRef]

      override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: V?,
      ) {
        if (value == null) {
          map.remove(thisRef)
        } else {
          map[thisRef] = value
        }
      }
    }
  }
}
