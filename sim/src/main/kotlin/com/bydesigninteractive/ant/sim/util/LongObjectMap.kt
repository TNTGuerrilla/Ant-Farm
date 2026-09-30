package com.bydesigninteractive.ant.sim.util

/**
 * A hash map from primitive longs to objects: open addressing with linear probing and
 * backward-shift deletion, so lookups never box a key. Iterate by slot with [capacity],
 * [valueAt] and [keyAt]; the order is deterministic for the same sequence of operations.
 */
class LongObjectMap<V : Any>(initialCapacity: Int = 64) {
    private var keys = LongArray(roundUp(initialCapacity))
    private var values = arrayOfNulls<Any>(keys.size)
    private var used = BooleanArray(keys.size)
    private var mask = keys.size - 1

    var size = 0
        private set

    fun capacity(): Int = keys.size

    fun keyAt(slot: Int): Long = keys[slot]

    @Suppress("UNCHECKED_CAST")
    fun valueAt(slot: Int): V? = if (used[slot]) values[slot] as V else null

    @Suppress("UNCHECKED_CAST")
    fun get(key: Long): V? {
        var i = home(key)
        while (used[i]) {
            if (keys[i] == key) return values[i] as V
            i = (i + 1) and mask
        }
        return null
    }

    fun put(key: Long, value: V) {
        if ((size + 1) * 4 > keys.size * 3) grow()
        var i = home(key)
        while (used[i]) {
            if (keys[i] == key) {
                values[i] = value
                return
            }
            i = (i + 1) and mask
        }
        used[i] = true
        keys[i] = key
        values[i] = value
        size++
    }

    @Suppress("UNCHECKED_CAST")
    fun remove(key: Long): V? {
        var i = home(key)
        while (used[i]) {
            if (keys[i] == key) {
                val old = values[i] as V
                deleteAt(i)
                size--
                return old
            }
            i = (i + 1) and mask
        }
        return null
    }

    /** Backward-shift deletion: pull later entries of the probe run into the hole. */
    private fun deleteAt(slot: Int) {
        var hole = slot
        used[hole] = false
        values[hole] = null
        var j = hole
        while (true) {
            j = (j + 1) and mask
            if (!used[j]) return
            val k = home(keys[j])
            val stays = if (hole <= j) k in (hole + 1)..j else k > hole || k <= j
            if (!stays) {
                keys[hole] = keys[j]
                values[hole] = values[j]
                used[hole] = true
                used[j] = false
                values[j] = null
                hole = j
            }
        }
    }

    private fun grow() {
        val oldKeys = keys
        val oldValues = values
        val oldUsed = used
        keys = LongArray(oldKeys.size * 2)
        values = arrayOfNulls(keys.size)
        used = BooleanArray(keys.size)
        mask = keys.size - 1
        size = 0
        for (i in oldKeys.indices) {
            @Suppress("UNCHECKED_CAST")
            if (oldUsed[i]) put(oldKeys[i], oldValues[i] as V)
        }
    }

    private fun home(key: Long): Int = mix64(key).toInt() and mask

    private companion object {
        fun roundUp(n: Int): Int {
            var c = 4
            while (c < n) c *= 2
            return c
        }
    }
}
