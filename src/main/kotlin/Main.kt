package org.example

import kotlin.collections.HashMap
import kotlin.collections.HashSet

// --- Data Structures for Logic Representation ---

// Base interface for all terms (Variable, Constant, Predicate)
sealed interface Term

// Represents a variable (e.g., X, Y)
// Using data class for equals/hashCode based on name
data class Variable(val name: String) : Term {
    override fun toString(): String = "?$name" // Display variables with '?' prefix
}

// Represents a constant value (e.g., "Socrates", "human")
// Using data class for equals/hashCode based on value
data class Constant(val value: Any) : Term {
    override fun toString(): String = value.toString()
}

// Represents a structured term or predicate (e.g., human(Socrates), mortal(?X))
// Using data class for equals/hashCode based on functor and arguments
data class Predicate(val functor: String, val args: List<Term>) : Term {
    override fun toString(): String {
        return if (args.isEmpty()) {
            functor
        } else {
            "$functor(${args.joinToString(", ")})"
        }
    }
}

// Type alias for substitutions (bindings from variables to terms)
typealias Substitution = MutableMap<Variable, Term>

// Type alias for a Fact (a ground Predicate - no variables)
// While technically a Predicate can represent a fact, this alias emphasizes its role.
typealias Fact = Predicate

// Represents a rule: IF antecedents THEN consequent
// Antecedents and Consequent are Predicates (which can contain Variables)
data class Rule(val antecedents: List<Predicate>, val consequent: Predicate) {
    override fun toString(): String {
        return "${antecedents.joinToString(", ")} => $consequent"
    }
}

// --- Unification Implementation ---
fun unify(x: Term, y: Term, subst: Substitution): Boolean {
    when {
        // 1. If x and y are identical, unification succeeds with the current substitution.
        x == y -> return true

        // 2. If x is a variable:
        x is Variable -> return unifyVariable(x, y, subst)

        // 3. If y is a variable:
        y is Variable -> return unifyVariable(y, x, subst) // Symmetric case

        // 4. If x and y are predicates (structured terms):
        x is Predicate && y is Predicate -> {
            // Must have the same functor and the same number of arguments.
            if (x.functor != y.functor || x.args.size != y.args.size) {
                return false
            }
            // Recursively unify all corresponding arguments.
            // The substitution must be consistent across all arguments.
            for (i in x.args.indices) {
                if (!unify(x.args[i], y.args[i], subst)) {
                    return false // Unification failed for an argument pair
                }
            }
            return true // All arguments unified successfully
        }

        // 5. If x and y are constants (handled by x == y check if they are equal)
        // or if types mismatch (e.g., Constant vs Predicate), they cannot unify.
        else -> return false
    }
}


private fun unifyVariable(variable: Variable, term: Term, subst: Substitution): Boolean {
    // 1. If the variable is already bound in the substitution:
    if (variable in subst) {
        // Unify the term the variable is bound to with the other term.
        return unify(subst.getValue(variable), term, subst)
    }

    // 2. If the term is a variable and already bound:
    if (term is Variable && term in subst) {
        // Unify the current variable with the term the other variable is bound to.
        return unify(variable, subst.getValue(term), subst)
    }

    // 3. Occurs Check: Prevent infinite recursion (e.g., unifying ?X with predicate(?X))
    if (occursCheck(variable, term, subst)) {
        println("Warning: Occurs check failed for $variable in $term")
        return false
    }

    // 4. Bind the variable to the term in the substitution.
    subst[variable] = term
    return true
}

private fun occursCheck(variable: Variable, term: Term, subst: Substitution): Boolean {
    return when {
        // If term is the same variable, it occurs.
        variable == term -> true

        // If term is another variable that is bound, check the term it's bound to.
        term is Variable && term in subst -> occursCheck(variable, subst.getValue(term), subst)

        // If term is a predicate, check recursively in its arguments.
        term is Predicate -> term.args.any { occursCheck(variable, it, subst) }

        // Otherwise (term is a Constant or an unbound Variable different from 'variable'), it doesn't occur.
        else -> false
    }
}

fun applySubstitution(term: Term, subst: Substitution): Term {
    return when {
        // If term is a variable and is bound, return its binding (recursively applying substitution to it).
        term is Variable && term in subst -> applySubstitution(subst.getValue(term), subst)

        // If term is a variable but not bound, return it as is.
        term is Variable -> term

        // If term is a predicate, apply substitution recursively to its arguments.
        term is Predicate -> Predicate(term.functor, term.args.map { applySubstitution(it, subst) })

        // If term is a constant, return it as is.
        term is Constant -> term

        // Should not happen with sealed interface, but added for completeness
        else -> term
    }
}


// --- Forward Chaining Engine with Unification ---
class ForwardChainingEngineWithUnification {
    private val knownFacts = HashSet<Fact>() // Set of known facts (ground predicates)
    private val rules = HashSet<Rule>()     // Set of rules

    fun addFact(fact: Fact) {
        // Ensure added facts are ground (contain no variables)
        if (containsVariable(fact)) {
            println("Warning: Cannot add non-ground fact: $fact")
            return
        }
        knownFacts.add(fact)
    }

    fun addRule(rule: Rule) {
        rules.add(rule)
    }

    // Helper to check if a term contains any variables recursively
    private fun containsVariable(term: Term): Boolean {
        return when (term) {
            is Variable -> true
            is Predicate -> term.args.any { containsVariable(it) }
            is Constant -> false
            else -> false
        }
    }

    fun infer(): Set<Fact> {
        val inferredFacts = HashSet(knownFacts) // Start with initial facts
        var newFactDerived: Boolean
        var iteration = 0 // Safety break for potential infinite loops in complex cases

        println("--- Starting Inference ---")
        println("Initial Facts: ${inferredFacts.joinToString()}")

        do {
            iteration++
            if (iteration > 100) { // Safety break
                println("Warning: Inference stopped after 100 iterations (potential loop or complex derivation).")
                break
            }

            newFactDerived = false
            val newlyDerivedInThisPass = HashSet<Fact>() // Store facts derived in the current pass

            for (rule in rules) {
                // Try to find all possible instantiations of the rule's antecedents
                // using the currently known facts. This requires a search/backtracking mechanism.
                val initialBindings = mutableMapOf<Variable, Term>()
                findAndApplyRuleInstantiations(rule, 0, initialBindings, inferredFacts, newlyDerivedInThisPass)
            }

            // Add the newly derived facts to the main set
            if (newlyDerivedInThisPass.isNotEmpty()) {
                val actuallyAdded = newlyDerivedInThisPass.filter { inferredFacts.add(it) } // Add only if not already present
                if (actuallyAdded.isNotEmpty()) {
                    newFactDerived = true
                    println("Iteration $iteration: Derived ${actuallyAdded.size} new facts: ${actuallyAdded.joinToString()}")
                } else {
                    println("Iteration $iteration: No new facts added (derived facts already known).")
                }

            } else {
                println("Iteration $iteration: No new facts derived in this pass.")
            }

        } while (newFactDerived) // Repeat until no new facts are derived in a full pass

        println("--- Inference Complete ---")
        return inferredFacts
    }

    private fun findAndApplyRuleInstantiations(
        rule: Rule,
        antecedentIndex: Int,
        currentSubst: Substitution,
        factsToCheck: Set<Fact>,
        newlyDerived: MutableSet<Fact>
    ) {
        // Base case: All antecedents have been successfully matched
        if (antecedentIndex == rule.antecedents.size) {
            // Apply the final substitution to the consequent to get a new fact
            val newFactCandidate = applySubstitution(rule.consequent, currentSubst)

            // Ensure the derived fact is ground (no variables left)
            if (newFactCandidate is Fact && !containsVariable(newFactCandidate)) {
                // Add to the set of facts derived in *this pass*
                // We check against the main inferredFacts set later to avoid issues with iteration order
                newlyDerived.add(newFactCandidate)
                // println("  -> Found instantiation for rule $rule, derived potential fact: $newFactCandidate with bindings: $currentSubst")
            } else {
                // This might happen if the rule consequent had variables not present in antecedents
                // Or if unification resulted in variables in the bindings (shouldn't happen with proper facts)
                // println("  -> Found instantiation for rule $rule, but consequent $newFactCandidate is not a ground fact. Bindings: $currentSubst")
            }
            return // Found one complete instantiation, return from this path
        }

        // Recursive step: Try to match the current antecedent
        val currentAntecedentPattern = rule.antecedents[antecedentIndex]

        // Iterate through all known facts to see if one unifies with the current antecedent pattern
        for (fact in factsToCheck) {
            // Create a copy of the current substitution to backtrack correctly
            val potentialSubst = HashMap(currentSubst)

            // Attempt to unify the pattern with the fact using the potential substitution
            if (unify(currentAntecedentPattern, fact, potentialSubst)) {
                // If successful, recursively try to match the *next* antecedent
                // using the *updated* substitution (potentialSubst)
                findAndApplyRuleInstantiations(rule, antecedentIndex + 1, potentialSubst, factsToCheck, newlyDerived)
                // We continue the loop to find ALL possible matches for the current antecedent
            }
            // If unification fails, just try the next fact (backtracking implicitly happens
            // because we used a *copy* of the substitution).
        }
    }


    fun reset() {
        knownFacts.clear()
        rules.clear()
    }
}

fun testHumanMortalInference() {
    val engine = ForwardChainingEngineWithUnification()
    val varX = Variable("X")
    val socrates = Constant("Socrates")

    // Fact: human(Socrates)
    engine.addFact(Predicate("human", listOf(socrates)))

    // Rule: human(?X) => mortal(?X)
    engine.addRule(
        Rule(
            listOf(Predicate("human", listOf(varX))),
            Predicate("mortal", listOf(varX))
        )
    )

    val result = engine.infer()
    val expected = Predicate("mortal", listOf(socrates))
    assert(expected in result) { "Expected $expected to be derived." }

    println("✅ testHumanMortalInference passed.")
}

fun testPhilosopherIsHuman() {
    val engine = ForwardChainingEngineWithUnification()
    val varX = Variable("X")
    val plato = Constant("Plato")

    // Fact: philosopher(Plato)
    engine.addFact(Predicate("philosopher", listOf(plato)))

    // Rule: philosopher(?X) => human(?X)
    engine.addRule(
        Rule(
            listOf(Predicate("philosopher", listOf(varX))),
            Predicate("human", listOf(varX))
        )
    )

    val result = engine.infer()
    val expected = Predicate("human", listOf(plato))
    assert(expected in result) { "Expected $expected to be derived." }

    println("✅ testPhilosopherIsHuman passed.")
}

fun testStudentOfDerivedFromTeacherOf() {
    val engine = ForwardChainingEngineWithUnification()
    val varX = Variable("X")
    val varY = Variable("Y")
    val socrates = Constant("Socrates")
    val plato = Constant("Plato")

    // Fact: teacherOf(Socrates, Plato)
    engine.addFact(Predicate("teacherOf", listOf(socrates, plato)))

    // Rule: teacherOf(?X, ?Y) => studentOf(?Y, ?X)
    engine.addRule(
        Rule(
            listOf(Predicate("teacherOf", listOf(varX, varY))),
            Predicate("studentOf", listOf(varY, varX))
        )
    )

    val result = engine.infer()
    val expected = Predicate("studentOf", listOf(plato, socrates))
    assert(expected in result) { "Expected $expected to be derived." }

    println("✅ testStudentOfDerivedFromTeacherOf passed.")
}

// --- Example Usage ---

fun main() {
    testHumanMortalInference()
    testPhilosopherIsHuman()
    testStudentOfDerivedFromTeacherOf()
}


