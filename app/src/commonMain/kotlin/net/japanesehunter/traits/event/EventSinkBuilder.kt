package net.japanesehunter.traits.event

import net.japanesehunter.traits.Entity
import net.japanesehunter.traits.EntityQuery
import net.japanesehunter.traits.TraitKey
import net.japanesehunter.worldcreate.world.EventSink
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty1

/**
 * Builds an [EventSink] with declarative trait bindings resolved per event.
 *
 * The builder allows declaring trait requirements from entities referenced in
 * events. Bindings are resolved when each event arrives, not at declaration time.
 * If all required bindings resolve successfully, [onEach] executes. Otherwise,
 * the handler silently skips without throwing exceptions.
 *
 * Accessing bound trait values outside [onEach] throws [IllegalStateException].
 *
 * Implementations are not thread-safe. Build the sink on a single thread, then
 * use the resulting [EventSink] according to its own thread-safety contract.
 *
 * @param Ev the event type this builder handles
 */
interface EventSinkBuilder<out Ev> {
  /**
   * Registers the handler invoked for each event when all bindings resolve.
   *
   * The handler receives the event instance. Bound trait values are accessible
   * only within this block.
   *
   * @param handler the event processing logic
   */
  fun onEach(handler: (Ev) -> Unit)

  /**
   * Declares a required read-only trait binding from an entity property.
   *
   * The binding resolves when each event arrives. If the entity lacks the trait,
   * the event handler silently skips. Accessing the bound value outside [onEach]
   * throws [IllegalStateException].
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key
   * @return a property delegate providing the read-only view
   */
  fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R>

  /**
   * Declares an optional read-only trait binding from an entity property.
   *
   * The binding resolves when each event arrives. If the entity lacks the trait,
   * the delegate returns null without skipping the event handler. Accessing the
   * bound value outside [onEach] throws [IllegalStateException].
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key
   * @return a property delegate providing the read-only view or null
   */
  fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.readOptional(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R?>

  /**
   * Declares a trait binding with a default value provider.
   *
   * The binding resolves when each event arrives. If the entity lacks the trait,
   * the default value provider is evaluated during [onEach] execution. The provider
   * is not evaluated if the trait exists. Accessing the bound value outside [onEach]
   * throws [IllegalStateException].
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key
   * @param defaultValue the provider for the default read-only view
   * @return a property delegate providing the read-only view
   */
  fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.readOrDefault(
    key: TraitKey<R, W>,
    defaultValue: () -> R,
  ): ReadOnlyProperty<Any?, R>
}

/**
 * Builds an [EventSink] with entity query support for declarative trait binding resolution.
 *
 * This builder extends [EventSinkBuilder] with the ability to query entities from a registry.
 * Query results are evaluated per event and can be filtered using event-bound trait values.
 *
 * @param Ev the event type this builder handles
 */
interface QueryingEventSinkBuilder<out Ev> : EventSinkBuilder<Ev> {
  /**
   * Creates a query that will be evaluated when each event arrives.
   *
   * The query builds a plan that is executed against the entity registry for each event.
   * Filters in the query can reference event-bound trait values, which are resolved at
   * event processing time.
   *
   * @return a query builder for declaring entity requirements
   */
  fun query(): QueryBuilder
}

/**
 * Builds a query for selecting entities from a registry.
 *
 * Query plans are constructed during the builder phase and executed per event.
 * Results are cached within a single event processing cycle to avoid repeated evaluation.
 */
interface QueryBuilder : Sequence<Entity> {
  /**
   * Requires entities to have a specific trait, optionally filtered.
   *
   * The filter is evaluated during event processing and can reference event-bound values.
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key
   * @param filter optional predicate evaluated per entity
   * @return this query builder for chaining
   */
  fun <R : Any, W : Any> has(
    key: TraitKey<R, W>,
    filter: ((R) -> Boolean)? = null,
  ): QueryBuilder

  /**
   * Creates a binding to read a trait from the current query result entity.
   *
   * The binding resolves to the trait value of whichever entity is being iterated
   * in the onEach block. Access outside of iteration throws [IllegalStateException].
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key to read
   * @return a property delegate that provides the trait value during iteration
   */
  fun <R : Any, W : Any> read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R>
}

/**
 * Builds an [EventSink] with declarative trait binding resolution.
 *
 * Events flow through the returned sink. For each event, the builder resolves
 * all declared bindings. If every required binding succeeds, the [onEach]
 * handler executes. If any required binding fails, the event is silently skipped.
 *
 * @param Ev the event type
 * @param block the builder configuration
 * @return an event sink that processes events according to the configured bindings
 */
inline fun <Ev> buildEventSink(block: EventSinkBuilder<Ev>.() -> Unit): EventSink<Ev> {
  val builder = EventSinkBuilderImpl<Ev>()
  builder.block()
  return builder.build()
}

/**
 * Builds an [EventSink] with entity query support and declarative trait binding resolution.
 *
 * The returned function accepts an [EntityQuery] and produces an [EventSink]. Events flow
 * through the sink, and for each event, the builder resolves all declared bindings and
 * executes registered queries. If every required binding succeeds, the [onEach] handler
 * executes. If any required binding fails, the event is silently skipped.
 *
 * @param Ev the event type
 * @param block the builder configuration with query support
 * @return a function that produces an event sink given an entity query
 */
inline fun <Ev> buildQueryingEventSink(block: QueryingEventSinkBuilder<Ev>.() -> Unit): (EntityQuery) -> EventSink<Ev> {
  val builder = QueryingEventSinkBuilderImpl<Ev>()
  builder.block()
  return builder::build
}

@PublishedApi
internal class EventSinkBuilderImpl<Ev> : EventSinkBuilder<Ev> {
  private var handler: ((Ev) -> Unit)? = null
  private val requiredBindings = mutableListOf<Binding<Ev>>()

  override fun onEach(handler: (Ev) -> Unit) {
    this.handler = handler
  }

  override fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R> {
    val binding = ReadBinding(this, key)
    requiredBindings.add(binding)
    return ReadOnlyProperty { _, _ ->
      binding.resolvedReadView ?: error("Trait binding can only be accessed during onEach execution")
    }
  }

  override fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.readOptional(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R?> {
    val binding = ReadOptionalBinding(this, key)
    requiredBindings.add(binding)
    return ReadOnlyProperty { _, _ ->
      if (binding.isResolved) {
        binding.resolvedReadView
      } else {
        error("Trait binding can only be accessed during onEach execution")
      }
    }
  }

  override fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.readOrDefault(
    key: TraitKey<R, W>,
    defaultValue: () -> R,
  ): ReadOnlyProperty<Any?, R> {
    val binding = ReadOrDefaultBinding(this, key, defaultValue)
    requiredBindings.add(binding)
    return ReadOnlyProperty { _, _ ->
      binding.resolvedReadView ?: error("Trait binding can only be accessed during onEach execution")
    }
  }

  fun build(): EventSink<Ev> {
    val h = handler
    val bindings = requiredBindings.toList()
    return EventSink { event ->
      if (!bindings.all { it.tryResolve(event) }) {
        return@EventSink
      }
      try {
        h?.invoke(event)
      } finally {
        bindings.forEach { it.clear() }
      }
    }
  }

  /**
   * Attempts to resolve all bindings for the given event.
   *
   * @return true if all required bindings were resolved, false if any required binding failed
   */
  fun tryResolveBindings(event: Ev): Boolean = requiredBindings.all { it.tryResolve(event) }

  /**
   * Invokes the handler with the given event.
   * Bindings must be resolved before calling this method.
   */
  fun invokeHandler(event: Ev) {
    handler?.invoke(event)
  }

  /**
   * Clears all resolved binding values.
   */
  fun clearBindings() {
    requiredBindings.forEach { it.clear() }
  }

  private interface Binding<Ev> {
    fun tryResolve(event: Ev): Boolean

    fun clear()
  }

  private class ReadBinding<Ev, R : Any, W : Any>(
    val property: KProperty1<in Ev, Entity>,
    val key: TraitKey<R, W>,
  ) : Binding<Ev> {
    var resolvedReadView: R? = null
      private set

    override fun tryResolve(event: Ev): Boolean {
      val entity = property.get(event)
      val trait = entity.get(key.writableType) ?: return false
      resolvedReadView = key.provideReadonlyView(trait)
      return true
    }

    override fun clear() {
      resolvedReadView = null
    }
  }

  private class ReadOptionalBinding<Ev, R : Any, W : Any>(
    val property: KProperty1<in Ev, Entity>,
    val key: TraitKey<R, W>,
  ) : Binding<Ev> {
    var resolvedReadView: R? = null
      private set
    var isResolved: Boolean = false
      private set

    override fun tryResolve(event: Ev): Boolean {
      val entity = property.get(event)
      val trait = entity.get(key.writableType)
      resolvedReadView = trait?.let { key.provideReadonlyView(it) }
      isResolved = true
      return true
    }

    override fun clear() {
      resolvedReadView = null
      isResolved = false
    }
  }

  private class ReadOrDefaultBinding<Ev, R : Any, W : Any>(
    val property: KProperty1<in Ev, Entity>,
    val key: TraitKey<R, W>,
    val defaultValue: () -> R,
  ) : Binding<Ev> {
    var resolvedReadView: R? = null
      private set

    override fun tryResolve(event: Ev): Boolean {
      val entity = property.get(event)
      val trait = entity.get(key.writableType)
      resolvedReadView =
        if (trait != null) {
          key.provideReadonlyView(trait)
        } else {
          defaultValue()
        }
      return true
    }

    override fun clear() {
      resolvedReadView = null
    }
  }
}

@PublishedApi
internal class QueryingEventSinkBuilderImpl<Ev>(
  private val delegate: EventSinkBuilderImpl<Ev> = EventSinkBuilderImpl(),
) : QueryingEventSinkBuilder<Ev>,
  EventSinkBuilder<Ev> by delegate {
  private val queries = mutableListOf<QueryBuilderImpl>()

  override fun query(): QueryBuilder {
    val builder = QueryBuilderImpl()
    queries.add(builder)
    return builder
  }

  fun build(registry: EntityQuery): EventSink<Ev> {
    val queryExecutions = queries.map { QueryExecution(it.requirements, registry) }
    queries.zip(queryExecutions).forEach { (builder, execution) -> builder.setExecution(execution) }
    return EventSink { event ->
      if (!delegate.tryResolveBindings(event)) {
        return@EventSink
      }
      queryExecutions.forEach { it.execute() }
      try {
        delegate.invokeHandler(event)
      } finally {
        queries.forEach { it.clearAllState() }
        queryExecutions.forEach { it.clear() }
        delegate.clearBindings()
      }
    }
  }
}

@PublishedApi
internal class QueryBuilderImpl : QueryBuilder {
  val requirements: QueryPlan = mutableListOf()
  private var execution: QueryExecution? = null
  private val iteratorStack = mutableListOf<QueryIterator>()
  private val readBindings = mutableListOf<QueryReadBinding<*, *>>()

  override fun <R : Any, W : Any> has(
    key: TraitKey<R, W>,
    filter: ((R) -> Boolean)?,
  ): QueryBuilder {
    requirements.add(QueryRequirement(key, filter))
    return this
  }

  override fun <R : Any, W : Any> read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R> {
    val binding = QueryReadBinding(key)
    readBindings.add(binding)
    return ReadOnlyProperty { _, _ ->
      binding.resolvedReadView ?: error("Query trait binding can only be accessed during iteration")
    }
  }

  override fun iterator(): Iterator<Entity> {
    val exec = execution ?: error("Query can only be accessed during onEach execution")
    val iter = QueryIterator(exec.results.iterator(), this)
    iteratorStack.add(iter)
    return iter
  }

  internal fun setExecution(execution: QueryExecution?) {
    this.execution = execution
  }

  internal fun popIteratorsAbove(target: QueryIterator) {
    while (iteratorStack.isNotEmpty() && iteratorStack.last() !== target) {
      iteratorStack.removeLast()
    }
  }

  internal fun popIterator(target: QueryIterator) {
    if (iteratorStack.isNotEmpty() && iteratorStack.last() === target) {
      iteratorStack.removeLast()
    }
    restoreOrClearBindings()
  }

  internal fun updateBindings(entity: Entity) {
    readBindings.forEach { it.resolve(entity) }
  }

  internal fun clearAllState() {
    iteratorStack.clear()
    readBindings.forEach { it.clear() }
  }

  private fun restoreOrClearBindings() {
    val parentIterator = iteratorStack.lastOrNull()
    val parentEntity = parentIterator?.currentEntity
    if (parentEntity != null) {
      readBindings.forEach { it.resolve(parentEntity) }
    } else {
      readBindings.forEach { it.clear() }
    }
  }
}

internal class QueryIterator(
  private val delegate: Iterator<Entity>,
  private val builder: QueryBuilderImpl,
) : Iterator<Entity> {
  var currentEntity: Entity? = null
    private set

  override fun hasNext(): Boolean {
    val result = delegate.hasNext()
    if (!result) {
      builder.popIterator(this)
    }
    return result
  }

  override fun next(): Entity {
    builder.popIteratorsAbove(this)
    val entity = delegate.next()
    currentEntity = entity
    builder.updateBindings(entity)
    return entity
  }
}

private class QueryReadBinding<R : Any, W : Any>(
  private val key: TraitKey<R, W>,
) {
  var resolvedReadView: R? = null
    private set

  fun resolve(entity: Entity) {
    val trait = entity.get(key.writableType)
    resolvedReadView =
      if (trait != null) {
        key.provideReadonlyView(trait)
      } else {
        null
      }
  }

  fun clear() {
    resolvedReadView = null
  }
}

internal typealias QueryPlan = MutableList<QueryRequirement<*, *>>

internal class QueryRequirement<R : Any, W : Any>(
  val key: TraitKey<R, W>,
  val filter: ((R) -> Boolean)?,
)

internal class QueryExecution(
  private val requirements: QueryPlan,
  private val registry: EntityQuery,
) {
  var results: List<Entity> = emptyList()
    private set

  fun execute() {
    val types = requirements.map { it.key.writableType }.toTypedArray()
    val entities = registry.query(*types)
    results =
      entities
        .filter { entity ->
          requirements.all { req ->
            checkRequirement(entity, req)
          }
        }.toList()
  }

  private fun <R : Any, W : Any> checkRequirement(
    entity: Entity,
    req: QueryRequirement<R, W>,
  ): Boolean {
    val trait = entity.get(req.key.writableType) ?: return false

    val readView = req.key.provideReadonlyView(trait)

    val filter = req.filter
    return filter?.invoke(readView) != false
  }

  fun clear() {
    results = emptyList()
  }
}
