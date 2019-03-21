/*
 * Copyright 2016-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.*

internal const val NO_MODIFICATION = 0
internal const val UPDATE_VALUE = 1
internal const val PUT_KEY_VALUE = 2
internal class ModificationWrapper(var value: Int = NO_MODIFICATION)


internal class PersistentHashMap<K, out V>(private val node: TrieNode<K, V>,
                                           override val size: Int): AbstractMap<K, V>(), PersistentMap<K, V> {

    override val keys: ImmutableSet<K>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val keys = mutableSetOf<K>()
            while (iterator.hasNext()) {
                keys.add(iterator.nextKey())
            }
            return keys.toImmutableSet()
        }

    override val values: ImmutableCollection<V>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val values = mutableListOf<V>()
            while (iterator.hasNext()) {
                values.add(iterator.nextValue())
            }
            return values.toImmutableList()
        }

    override val entries: ImmutableSet<Map.Entry<K, V>> get() = _entries ?: createEntries().apply { _entries = this }

    private var _entries: ImmutableSet<Map.Entry<K, V>>? = null

    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> {
        val iterator = PersistentHashMapIterator(node)
        val entries = mutableSetOf<Map.Entry<K, V>>()
        while (iterator.hasNext()) {
            entries.add(iterator.nextEntry())
        }
        return entries.toImmutableSet()
    }



    // TODO: compiler bug: this bridge should be generated automatically
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> = _entries ?: createEntries().apply { _entries = this }

    override fun containsKey(key: K): Boolean {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        return node.containsKey(keyHash, key, 0)
    }

    override fun get(key: K): V? {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        return node.get(keyHash, key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): PersistentMap<K, V> {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        val modification = ModificationWrapper()
        val newNode = node.put(keyHash, key, value, 0, modification)
        if (node === newNode) { return this }
        val sizeDelta = if (modification.value == PUT_KEY_VALUE) 1 else 0
        return PersistentHashMap(newNode, size + sizeDelta)
    }

    override fun remove(key: K): PersistentMap<K, V> {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        val newNode = node.remove(keyHash, key, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return persistentHashMapOf() }
        return PersistentHashMap(newNode, size - 1)
    }

    override fun remove(key: K, value: @UnsafeVariance V): PersistentMap<K, V> {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        val newNode = node.remove(keyHash, key, value, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return persistentHashMapOf() }
        return PersistentHashMap(newNode, size - 1)
    }

    override fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V> {
        return this.mutate { it.putAll(m) }
    }

    override fun clear(): PersistentMap<K, V> {
        return persistentHashMapOf()
    }

    override fun builder(): PersistentMap.Builder<K, @UnsafeVariance V> {
        return PersistentHashMapBuilder(node, size)
    }

    internal companion object {
        internal val EMPTY = PersistentHashMap(TrieNode.EMPTY, 0)
    }
}

fun <K, V> persistentHashMapOf(): PersistentMap<K, V> {
    return PersistentHashMap.EMPTY as PersistentHashMap<K, V>
}