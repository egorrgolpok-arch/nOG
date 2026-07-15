package com.example.data

import android.util.Log
import kotlin.random.Random

object MarkovChainGenerator {
    private const val TAG = "MarkovChainGenerator"
    
    // Trigram model (Key: "word1 word2", Value: List of next words)
    private val trigramChain = java.util.concurrent.ConcurrentHashMap<String, MutableList<String>>()
    
    // Bigram model (Key: "word1", Value: List of next words)
    private val bigramChain = java.util.concurrent.ConcurrentHashMap<String, MutableList<String>>()
    
    // Start keys for trigram generation
    private val startKeysTrigram = java.util.concurrent.CopyOnWriteArrayList<String>()
    
    // All known words for unigram fallback
    private val allWords = java.util.concurrent.CopyOnWriteArrayList<String>()

    fun train(texts: List<String>, clearExisting: Boolean = false) {
        if (texts.isEmpty()) return
        if (clearExisting) {
            trigramChain.clear()
            bigramChain.clear()
            startKeysTrigram.clear()
            allWords.clear()
        }

        for (rawText in texts) {
            val text = rawText.trim()
            if (text.isEmpty()) continue
            // Split into words while keeping punctuation attached
            val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) continue
            
            allWords.addAll(words)

            if (words.size >= 2) {
                // Record trigram start keys
                val firstTrigramKey = "${words[0].lowercase()} ${words[1].lowercase()}"
                startKeysTrigram.add(firstTrigramKey)
            }

            // Train Bigram Chain
            for (i in 0 until words.size - 1) {
                val key = words[i].lowercase()
                val next = words[i+1]
                bigramChain.getOrPut(key) { java.util.concurrent.CopyOnWriteArrayList() }.add(next)
            }

            // Train Trigram Chain
            for (i in 0 until words.size - 2) {
                val key = "${words[i].lowercase()} ${words[i+1].lowercase()}"
                val next = words[i+2]
                trigramChain.getOrPut(key) { java.util.concurrent.CopyOnWriteArrayList() }.add(next)
            }
        }
        Log.d(TAG, "Trained Upgraded Adaptive Markov Chain. Words: ${allWords.size}, Trigrams: ${trigramChain.size}, Bigrams: ${bigramChain.size}")
    }

    fun generate(maxLength: Int = 20): String {
        if (allWords.isEmpty()) return ""
        
        // Start with a trigram key if possible, else a bigram word, else a random word
        val result = mutableListOf<String>()
        val startKey = startKeysTrigram.randomOrNull()
        
        if (startKey != null) {
            val startWords = startKey.split(" ")
            result.addAll(startWords)
        } else {
            val startWord = allWords.randomOrNull() ?: return ""
            result.add(startWord)
        }

        var count = result.size
        var consecutiveFallbacks = 0

        while (count < maxLength) {
            var nextWord: String? = null

            // 1. Try Trigram prediction (needs at least 2 words in result)
            if (result.size >= 2) {
                val lastWord = result[result.size - 1].lowercase()
                val secondLastWord = result[result.size - 2].lowercase()
                val trigramKey = "$secondLastWord $lastWord"
                val possibilities = trigramChain[trigramKey]
                if (!possibilities.isNullOrEmpty()) {
                    nextWord = possibilities.random()
                    consecutiveFallbacks = 0
                }
            }

            // 2. Fallback to Bigram prediction
            if (nextWord == null && result.isNotEmpty()) {
                val lastWord = result[result.size - 1].lowercase()
                val possibilities = bigramChain[lastWord]
                if (!possibilities.isNullOrEmpty()) {
                    nextWord = possibilities.random()
                    consecutiveFallbacks++
                }
            }

            // 3. Fallback to Unigram prediction (random word but from neighbors context to avoid gibberish)
            if (nextWord == null) {
                // If we get stuck, grab a random word from the training set, but limit chain repetition
                if (consecutiveFallbacks > 3) {
                    break // Prevent infinite random loops
                }
                nextWord = allWords.randomOrNull()
                consecutiveFallbacks++
            }

            if (nextWord != null) {
                result.add(nextWord)
                count++

                // Natural end-of-sentence checking
                val isPunctuationEnd = nextWord.endsWith(".") || nextWord.endsWith("!") || nextWord.endsWith("?") || nextWord.endsWith("...")
                if (isPunctuationEnd && count >= 8 && Random.nextInt(100) < 45) {
                    break
                }
            } else {
                break
            }
        }

        // Clean up formatting
        var sentence = result.joinToString(" ").trim()
        
        // Remove orphan trailing punctuation or prepositions
        val trailingPrepositionsRu = setOf("и", "в", "во", "на", "с", "со", "о", "об", "а", "но", "у", "к", "ко", "за", "под", "из")
        val trailingPrepositionsEn = setOf("and", "with", "at", "by", "for", "in", "of", "on", "to", "a", "an", "the")
        val words = sentence.split(" ")
        if (words.isNotEmpty()) {
            val lastWordClean = words.last().lowercase().replace(Regex("[^a-zA-Zа-яА-ЯёЁ]"), "")
            if (trailingPrepositionsRu.contains(lastWordClean) || trailingPrepositionsEn.contains(lastWordClean)) {
                sentence = words.dropLast(1).joinToString(" ")
            }
        }

        if (sentence.isEmpty()) return ""

        // Ensure proper ending punctuation
        if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?") && !sentence.endsWith("...")) {
            sentence += listOf(".", "!", "...", "!?").random()
        }

        // Capitalize the first letter nicely
        return sentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }

    fun mutatePostContent(original: String): String {
        val words = original.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        if (words.isEmpty()) return ""
        
        val result = mutableListOf<String>()
        var i = 0
        val size = words.size
        
        while (i < size) {
            // With 30% probability, we mutate the word sequence using bigram probabilities or splicing short chains
            if (Random.nextDouble() < 0.3 && i < size - 1) {
                val key = words[i].lowercase()
                val possibilities = bigramChain[key]
                if (!possibilities.isNullOrEmpty() && Random.nextDouble() < 0.7) {
                    result.add(words[i])
                    result.add(possibilities.random())
                    i += 2 // skip next word for stylistic shift
                } else {
                    val shortChain = generate(Random.nextInt(1, 4))
                    if (shortChain.isNotEmpty()) {
                        result.add(shortChain)
                    }
                    result.add(words[i])
                    i++
                }
            } else {
                result.add(words[i])
                i++
            }
        }
        
        var sentence = result.joinToString(" ").trim()
        if (sentence.isEmpty()) return ""
        
        // Formating cleanup
        if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?") && !sentence.endsWith("...")) {
            sentence += listOf(".", "!", "...", "!?").random()
        }
        return sentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
}
