package net.japanesehunter.math.test

interface Dimension {
  sealed interface Times<D1 : Dimension, D2 : Dimension> : Dimension

  sealed interface Divides<D1 : Dimension, D2 : Dimension> : Dimension
}
