package com.example.data

import kotlin.random.Random

class MarkovChain(val order: Int = 2) {
    private val chain = mutableMapOf<List<String>, MutableList<String>>()

    fun train(texts: List<String>) {
        texts.forEach { text ->
            val words = text.split(" ").filter { it.isNotBlank() }
            if (words.size > order) {
                for (i in 0 until words.size - order) {
                    val state = words.subList(i, i + order)
                    val next = words[i + order]
                    chain.getOrPut(state) { mutableListOf() }.add(next)
                }
            }
        }
    }

    fun generate(): String {
        if (chain.isEmpty()) return ""
        var state = chain.keys.toList().random()
        val result = state.toMutableList()
        
        // Generate up to 50 words to avoid infinite loops and keep it snappy
        repeat(50) {
            val nextOptions = chain[state]
            if (nextOptions.isNullOrEmpty()) return@repeat
            
            val next = nextOptions.random()
            result.add(next)
            state = result.takeLast(order)
        }
        return result.joinToString(" ")
    }
}
