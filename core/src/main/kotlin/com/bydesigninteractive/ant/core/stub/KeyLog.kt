package com.bydesigninteractive.ant.core.stub

/** The newest key lines for the overlay. Written from the UI thread, read from the GL thread. */
class KeyLog(private val capacity: Int = 6) {
    private val recent = ArrayDeque<String>()
    var total = 0
        private set

    @Synchronized
    fun add(line: String) {
        recent.addLast(line)
        while (recent.size > capacity) recent.removeFirst()
        total++
    }

    @Synchronized
    fun lines(): List<String> = recent.toList()
}
