/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import org.partiql.lang.util.codePointSequence
import java.util.ArrayList
import java.util.HashSet


@Suppress("UNCHECKED_CAST")
private fun <T> Iterable<T>.fastToMutableHashSet(): MutableSet<T> =
    when(this) {
        // `HashSet<T>.clone()` is faster than constructing a new HashSet<T> and calling .addAll()
        is HashSet<T> -> this.clone() as HashSet<T>
        else -> HashSet<T>().also { it.addAll(this) }
    }

private infix fun <T> Iterable<T>.fastUnion(other: Iterable<T>): MutableSet<T> {
    val set = this.fastToMutableHashSet()
    set.addAll(other)
    return set
}


/**
 * Enumeration of Alphabet letters which can be one of
 *
 * - Any one character -- SQL `_` that maps to `.` in RegeExp
 * - Zero or more characters -- SQL `%` that maps to `.*` in RegeExp
 * - Epsilon -- denotes the epsilon (empty) transitions in an NFA
 * - Character -- denotes any single character
 */
private sealed class Alphabet {
    data class Letter(val codePoint: Int) : Alphabet()
    object AnyOneChar : Alphabet()
    object AnyZeroOrMoreChars : Alphabet()
    object Epsilon : Alphabet()
}

/**
 * Represents the dead DFA State.
 * This state is terminal-- has no outgoing transitions and it is neither a start State nor a Final state
 */
val  DFADeadState : IDFAState =  DFAState(HashSet(), HashMap())

// Represents the DFA of the empty pattern
val DFAEmptyPattern = object :IDFAState {
    override fun isAccepting(): Boolean {
        return false
    }

    override fun run(word: String?): Boolean =
        word?.let {
            word.isEmpty() // SQL92 pp. 216 Case 5)b)
        } ?:  false


    override fun step(codePoint: Int): IDFAState? {
        return DFADeadState
    }

}

interface IDFAState {
    fun isAccepting(): Boolean
    /**
     * Given a possibly `null` string, starting from `this` DFA state run the automaton and return
     * `true` if we exhaust [word] and we are in a an accepting state, false otherwise.
     *
     * @param word input to the DFA
     *
     * @return true if the DFA accepts the input, false otherwise
     */
    fun run(word: String?): Boolean

    /**
     * Given a character, take a step in our DFA starting with `this` state and possible transitions that match [codePoint].
     *
     * @param codePoint character to match against possible valid transitions
     *
     * @return next DFA state
     */
    fun step(codePoint: Int): IDFAState?
}

/**
 * Represents a DFA State where
 *
 * - [nfaStates] set of NFA states that correspond to this DFA state
 * - [outgoing] map of transitions to DFA states
 * - [accepting] true if this is a Final state, false otherwise
 * - [start] true if this is a Start state, false otherwise
 */
private open class DFAState(
    val nfaStates: MutableSet<NFAState>,
    val outgoing: MutableMap<Alphabet, DFAState>,
    var accepting: Boolean = nfaStates.any { it.isAccepting },
    var start: Boolean = nfaStates.any { it.isStartState }
) : IDFAState {

    fun addTransition(transition: Alphabet, target: DFAState) {
        if (transition == Alphabet.Epsilon) errNoContext("DFA cannot have epsilon transitions: $transition, $target", internal = true)
        when (outgoing.containsKey(transition)) {
            true -> if (target != outgoing[transition])
                errNoContext("DFA cannot have a transition that maps to different targets : $transition -> $target AND $transition  -> $outgoing.get(transition)", internal = true)
            false -> outgoing.put(transition, target)
        }
    }

    fun addNFAStates(nfaState: NFAState) {
        nfaStates.add(nfaState)
        accepting = accepting || nfaState.isAccepting
        start = start || nfaState.isStartState
    }

    override fun isAccepting(): Boolean = accepting

     /**
     * Given a character, take a step in our DFA starting with `this` state and possible transitions that match [codePoint].
     *
     * @param codePoint character to match against possible valid transitions
     *
     * @return next DFA state
     */
    override fun step(codePoint: Int): IDFAState? {
        val trans = Alphabet.Letter(codePoint)
        when (outgoing.containsKey(trans)) {
            true -> return outgoing[trans]
            else -> {
                if (outgoing.containsKey(Alphabet.AnyOneChar)) return outgoing[Alphabet.AnyOneChar]
                else return DFADeadState
            }
        }
    }


    /**
     * Given a possibly `null` string, starting from `this` DFA state run the automaton and return
     * `true` if we exhaust [word] and we are in a an accepting state, false otherwise.
     *
     * @param word input to the DFA
     *
     * @return true if the DFA accepts the input, false otherwise
     */
    override fun run(word: String?): Boolean {
        var currentState: IDFAState = this

        word?.let {
            it.codePointSequence().forEach { ele ->
                val newState: IDFAState? = currentState.step(ele)
                when (newState) {
                    null -> return false
                    DFADeadState -> return false
                    else -> currentState = newState
                }
            }
        }

        return currentState.isAccepting()
    }
}



/**
 * Represents a state in an NFA where
 *
 * - [stateNumber] is a number used for this state
 * - [isAccepting] true when this State is a Final state, false otherwise
 * - [isStartState] true when this State is a Start state, false othewise
 * - [outgoing] map of alphabet letter to NFA State
 */
private class NFAState(
    val stateNumber: Int,
    val isAccepting: Boolean,
    val isStartState: Boolean,
    val outgoing: MutableMap<Alphabet, MutableSet<NFAState>> = HashMap()) {

    fun get(transition: Alphabet): Set<NFAState> =
        outgoing[transition] ?: hashSetOf()

    fun addTransition(label: Alphabet, target: NFAState) {
        when (outgoing.containsKey(label)) {
            true -> outgoing[label]?.add(target) ?: HashSet<NFAState>().apply { add(target) }
            false -> outgoing[label] = HashSet<NFAState>().apply { add(target) }
        }
    }

    /**
     * Given a letter from the NFA's alphabet return the letter-closure from `this` state.
     *
     * @param alpha letter from the NFA's alphabet
     *
     * @return set of NFA states that make up the letter-closure--reachable states from `this` state
     * through a combination of 1 transition of `alpha` and any sequence of 1 or more epsilon transitions.
     *
     */
    fun getOutgoingStates(alpha: Alphabet): Set<NFAState> =
        when (alpha) {
            is Alphabet.Letter -> getOutgoingStates(alpha.codePoint)
            is Alphabet.Epsilon -> epsilonClosure()
            is Alphabet.AnyOneChar,
            is Alphabet.AnyZeroOrMoreChars -> {
                val startSet = epsilonClosure().fastUnion(get(alpha))
                startSet.fold(get(alpha)) { acc, nfaState -> acc.fastUnion(nfaState.epsilonClosure()) }
            }
        }

    /**
     * Given a character return the set of NFA States that are the character-closure for `this` node.
     *
     * The character closure is the set of all NFA State reachable through `this` state by
     * following any combination of epsilon transitions and *one* non-epsilon transition that
     * matches [codePoint].
     *
     *
     * @param codePoint character to check for transitions
     *
     * @return set of NFA states reachable though any combination of epsilon transitions and *one*
     * non-epsilon transitions that matches the input character.

     */
    fun getOutgoingStates(codePoint: Int): Set<NFAState> =
        closure(codePoint)

    /**
     * Given a character return all states reachable from `this` state by 1 non-epsilon transition.
     *
     * @param codePoint character to check for transitions
     *
     * @return set of NFA States reachable from `this` state by 1 non-epsilon transition.
     */
    fun getNonEpsilonTransitionTargets(codePoint: Int): Set<NFAState> =
        get(Alphabet.Letter(codePoint)).fastUnion(get(Alphabet.AnyOneChar))


    /**
     * Given a code point for a character return the character-closure for `this` node.
     * The character closure is the set of all NFA State reachable through `this` state by
     * following any combination of epsilon transitions and *one* non-epsilon transition that
     * matches [codePoint].
     *
     *
     * @param codePoint character to check for transitions
     *
     * @return set of NFA states reachable though any combination of epsilon transitions and *one*
     * non-epsilon transitions that matches the input character.
     */
    fun closure(codePoint: Int): Set<NFAState> {
        val reachableThroughEpsilon = epsilonClosure()
        val reachableThroughNonEpsilon = getNonEpsilonTransitionTargets(codePoint).let {
            it.fold(it, { acc, state -> acc.fastUnion(state.epsilonClosure()) })
        }
        return reachableThroughEpsilon.fastUnion(reachableThroughNonEpsilon)
    }


    /**
     * Returns the espilon-closure of this NFA State. All states reachable from `this` state by using one or more
     * epsilon transitions in succession.
     *
     * @return the set of NFA states that make the epsilon closure of `this` NFA state
     */
    fun epsilonClosure(): Set<NFAState> =
        get(Alphabet.Epsilon).let {
            it.fold(it, { acc, state ->
                acc.fastUnion(state.epsilonClosure())
            })
        }
}


/**
 * Given the search pattern, possible escape character used in the search pattern and the size of the search pattern,
 * build a DFA recognizer. The recognizer builds an NFA that then translates to a DFA.
 *
 * PRE-CONDITION: [pattern] is a valid LIKE pattern, i.e, the result of `checkPattern` function
 *
 *
 * @param pattern valid search pattern as a [String]
 * @param escape possible escape character
 * @param patternSize size of the pattern
 *
 * @return DFA that accepts inputs which match [pattern]
 */
fun buildDfaFromPattern(pattern: String, escape: Int?, patternSize: Int): IDFAState {
    escape?.let {
        val patternAsNfaLetters = patternToSequenceOfNfaLetters(pattern, it)
        val dfaAlpha = patternAsNfaLetters.map(nfaLettersToDfaAlphabet()).toHashSet()
        return nfaToDfa(dfaAlpha, buildNfa(patternAsNfaLetters, patternSize))
    }
    val patternAsNfaLetters = patternToSequenceOfNfaLetters(pattern)
    val dfaAlpha = patternAsNfaLetters.map(nfaLettersToDfaAlphabet()).toHashSet()
    return nfaToDfa(dfaAlpha, buildNfa(patternAsNfaLetters, patternSize))
}

/**
 * Given a search pattern and an escape character possibly used in the pattern, return the sequence
 * of letters in the NFA's alphabet that correspond to the characters in the pattern.
 *
 * @param pattern search pattern
 * @param escapeChar escape character
 *
 * @return sequence of letters in the NFA's alphabet that correspond to the characters in the pattern
 */
private fun patternToSequenceOfNfaLetters(pattern: String, escapeChar: Int): Sequence<Alphabet> {
    val codePointIter = pattern.codePointSequence().iterator()
    val result = ArrayList<Alphabet>()

    while (codePointIter.hasNext()) {
        val current = codePointIter.next()
        when (current) {
            escapeChar -> result.add(Alphabet.Letter(codePointIter.next())) // skip current, use successor as raw character
            else -> result.add(codePointToAlphabetLetter(current))
        }
    }
    return result.asSequence()
}

/**
 * Given the search pattern return a sequence of [Alphabet] that holds the corresponding [Alphabet] instance for
 * each character in the input pattern.
 *
 * @param pattern search pattern
 *
 * @return sequence of [Alphabet] for each character in the input
 */
private fun patternToSequenceOfNfaLetters(pattern: String): Sequence<Alphabet> =
    pattern.codePointSequence().map {
        codePointToAlphabetLetter(it)
    }

/**
 * Given a character, return its corresponding Alphabet Letter
 *
 * @param codePoint input character as a code point
 *
 * @return corresponding [Alphabet] instance for the input
 */
private fun codePointToAlphabetLetter(codePoint: Int): Alphabet {
    return when (codePoint) {
        '_'.toInt() -> Alphabet.AnyOneChar
        '%'.toInt() -> Alphabet.AnyZeroOrMoreChars
        else -> Alphabet.Letter(codePoint)
    }
}

/**
 * Function that given an instance of [Alphabet] for an NFA returns the appropriate [Alphabet] for the NFA's DFA.
 * Change all zero or more letter to any one char letter. All other elements of the input remain unchanged.
 *
 */
private fun nfaLettersToDfaAlphabet(): (Alphabet) -> Alphabet {
    return { a ->
        when (a) {
            Alphabet.AnyZeroOrMoreChars -> Alphabet.AnyOneChar
            else -> a
        }
    }
}

/**
 * Given the DFA alphabet and the start NFA state, return the DFA that simulates the NFA
 *
 * @param alphabet DFA alphabet
 * @param nfa NFA start state
 *
 * @return DFA that simulates the NFA with start state [nfa]
 */
private fun nfaToDfa(alphabet: Set<Alphabet>, nfa: NFAState) =
    buildDFA(alphabet, HashMap(), hashSetOf(nfa.epsilonClosure().fastUnion(hashSetOf(nfa))))


/**
 * Given the DFA alphabet, the current DFA delta and a set of sets of NFA State, process
 * the set of sets of NFA States and update the DFA.
 *
 * This function builds the table that simulates the NFA to create the DFA
 *
 * @param dfaAlphabet DFA Alphabet, the rows of the table
 * @param delta DFA delta function thus far
 * @param set of sets of NFA states to process
 *
 * @return DFA that simulates the NFA
 */
private fun buildDFA(
    dfaAlphabet: Set<Alphabet>,
    delta: MutableMap<Pair<Set<NFAState>, Alphabet>, Set<NFAState>>,
    todo: Set<Set<NFAState>>): DFAState {

    var unprocessed = todo.fastToMutableHashSet()
    val processed = HashSet<Set<NFAState>>()
    while (unprocessed.isNotEmpty()) {
        val nfaStates = unprocessed.first()
        unprocessed.remove(nfaStates)
        //  delta = (Q x \Sigma) -> Q
        //       where Q is \Set(NFAState)
        //  maps to the type
        // delta : Pair<Set<NFAState>, Alphabet>, Set<NFAState>
        val deltaUpdates: List<Pair<Pair<Set<NFAState>, Alphabet>, Set<NFAState>>> =
            dfaAlphabet.map {
                Pair(Pair(nfaStates, it),
                    HashSet<NFAState>().apply {
                        nfaStates.forEach { state ->
                            addAll(state.getOutgoingStates(it))

                            if (Thread.interrupted()) {
                                throw InterruptedException()
                            }
                        }
                    })
            }
        processed.add(nfaStates)
        updateDelta(delta, deltaUpdates)
        val newStates = deltaUpdates.map {
            it.second
        }.filter { s ->
            s.isNotEmpty() && !processed.contains(s)
        }.fastToMutableHashSet()
        unprocessed = unprocessed.fastUnion(newStates)
    }

    val nfaStateSetToDfaState = HashMap<Set<NFAState>, DFAState>()

    delta.forEach { nfaStateSetToDfaState[it.key.first] = DFAState(it.key.first.fastToMutableHashSet(), HashMap()) }
    delta.forEach { (nfaSet, alpha), target ->
        val targetDfa: DFAState = nfaStateSetToDfaState[target].let { it } ?: DFADeadState as DFAState
        nfaStateSetToDfaState[nfaSet]?.addTransition(alpha, targetDfa) ?: errNoContext("DFA state for $nfaSet does not exist", internal = true)
    }

    val dfaStartState = nfaStateSetToDfaState.values.filter { it.start }
    if (dfaStartState.size == 1) return dfaStartState.first()
    else errNoContext("DFA has more that 1 start state : $dfaStartState", internal = true)
}

/**
 * Given our current delta for the DFA and a list of updates, return the updated delta.
 *
 * @param delta current delta for the DFA
 * @param deltaUpdates list of updates to be processed
 *
 * @return update [delta] that incorporates changes in [deltaUpdates]
 */
private fun updateDelta(
    delta: MutableMap<Pair<Set<NFAState>, Alphabet>, Set<NFAState>>,
    deltaUpdates: List<Pair<Pair<Set<NFAState>, Alphabet>, Set<NFAState>>>) {
    deltaUpdates.forEach {
        if (delta.containsKey(it.first)) {
            if (delta[it.first] != it.second) {
                errNoContext("construction of DFA attempted to add the same transition with two distinct targets: $it.first, $it.second", internal = true)
            }
        } else {
            delta.put(it.first, it.second)
        }
    }
}


/**
 * Given the sequence of NFA letters  that correspond to the search string and the search string's length
 * build an NFA that accepts words that match [letters].
 *
 * @param letters sequence of NFA letters that correspond to the search string
 * @param patternSize size of the search string
 *
 * @return NFA that accepts words that match [letters]
 *
 */
private fun buildNfa(letters: Sequence<Alphabet>, patternSize: Int): NFAState =
    letters.foldIndexed(mutableListOf(NFAState(-1, 0 == patternSize, true)), { index, acc, transition ->
        alphabetToNFAStateAcc(transition, NFAState(index, index == (patternSize - 1), false), acc)
    }).first()

/**
 * Given the current letter in the NFA's alphabet, the new NFA state created and the list of already created
 * NFA states, add necessary transitions in the NFA states (new and old) to simulate a move of the NFA for the
 * input letter.
 *
 * @param letter new letter for the NFA
 * @param newState newly created NFA state
 * @param acc accumulator that holds previously processed NFA states.
 *
 * @return updated list of NFA states
 */
private fun alphabetToNFAStateAcc(letter: Alphabet, newState: NFAState, acc: MutableList<NFAState>): MutableList<NFAState> =
    when (letter) {
        is Alphabet.Letter, is Alphabet.AnyOneChar -> {
            acc.last().addTransition(letter, newState)
            acc.add(newState)
            acc
        }
        is Alphabet.AnyZeroOrMoreChars -> {
            acc.last().addTransition(Alphabet.Epsilon, newState)
            newState.addTransition(Alphabet.AnyOneChar, newState)
            acc.add(newState)
            acc
        }
        is Alphabet.Epsilon -> errNoContext("Found epsilon letter while processing pattern chars", internal = true)
    }
