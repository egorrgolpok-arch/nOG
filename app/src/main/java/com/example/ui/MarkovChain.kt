package com.example.ui

import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
import kotlin.random.Random

class MarkovChain {
    private val chain = ConcurrentHashMap<String, MutableList<String>>()

    fun train(text: String) {
        val words = text.split(" ").filter { it.isNotEmpty() }
        for (i in 0 until words.size - 1) {
            val word = words[i]
            val nextWord = words[i + 1]
            // Use computeIfAbsent to atomicaly initialize the list 
            // and use a synchronized list for its contents
            chain.computeIfAbsent(word) { Collections.synchronizedList(mutableListOf<String>()) }
                .add(nextWord)
        }
    }

    fun generate(startWord: String?, length: Int = 10): String {
        if (chain.isEmpty()) return ""
        
        // Safely get a snapshot of the keys for random selection
        val keys = chain.keys.toList()
        if (keys.isEmpty()) return ""

        var currentWord = startWord ?: keys.random()
        val result = mutableListOf(currentWord)
        
        for (i in 0 until length) {
            val nextWords = chain[currentWord]
            if (nextWords == null || nextWords.isEmpty()) break
            
            // nextWords is a synchronized list, so this is thread-safe
            val nextWord = nextWords.random()
            result.add(nextWord)
            currentWord = nextWord
        }
        
        return result.joinToString(" ")
    }
}
