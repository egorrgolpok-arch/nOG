package com.example.data

import android.util.Log
import kotlin.random.Random

object MarkovChainGenerator {
    private const val TAG = "MarkovChainGenerator"
    private val chain = java.util.concurrent.ConcurrentHashMap<String, MutableList<String>>()
    private val startKeys = java.util.concurrent.CopyOnWriteArrayList<String>()

    fun train(texts: List<String>) {
        if (texts.isEmpty()) return
        chain.clear()
        startKeys.clear()

        for (rawText in texts) {
            val text = rawText.trim()
            if (text.isEmpty()) continue
            // Split into words, handling some basic punctuation or split by space
            val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.size < 2) continue

            // Record start bigrams (keys)
            val firstKey = "${words[0].lowercase()} ${words[1].lowercase()}"
            startKeys.add(firstKey)

            for (i in 0 until words.size - 2) {
                val key = "${words[i].lowercase()} ${words[i+1].lowercase()}"
                val nextWord = words[i+2]
                chain.getOrPut(key) { java.util.concurrent.CopyOnWriteArrayList() }.add(nextWord)
            }
        }
        Log.d(TAG, "Trained Markov Chain on ${texts.size} items. Start keys size: ${startKeys.size}, nodes in chain: ${chain.size}")
    }

    fun generate(maxLength: Int = 15): String {
        if (startKeys.isEmpty() || chain.isEmpty()) {
            return ""
        }
        val randomStart = startKeys.randomOrNull() ?: return ""
        val result = mutableListOf<String>()
        val startWords = randomStart.split(" ")
        result.addAll(startWords)

        var currentKey = randomStart
        var count = 2
        while (count < maxLength) {
            val possibleNext = chain[currentKey]
            if (possibleNext.isNullOrEmpty()) {
                break
            }
            val nextWord = possibleNext.random()
            result.add(nextWord)
            currentKey = "${result[result.size - 2].lowercase()} ${result[result.size - 1].lowercase()}"
            count++
            // Natural ending on punctuation with 30% chance
            if ((nextWord.endsWith(".") || nextWord.endsWith("!") || nextWord.endsWith("?")) && Random.nextInt(100) < 30) {
                break
            }
        }
        val generated = result.joinToString(" ").trim()
        if (generated.isEmpty()) return ""
        return generated.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
}
