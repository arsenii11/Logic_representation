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

/**
 * Attempts to unify two terms, finding a substitution that makes them identical.
 *
 * @param x The first term.
 * @param y The second term.
 * @param subst The current substitution map (will be modified on success).
 * @return True if unification is successful, false otherwise. The substitution map is updated in place.
 */
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

/**
 * Helper function to unify a variable with a term.
 *
 * @param variable The variable to unify.
 * @param term The term to unify the variable with.
 * @param subst The current substitution map (modified on success).
 * @return True if unification succeeds, false otherwise.
 */
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

/**
 * Performs the occurs check: verifies that the variable does not occur within the term
 * it is being bound to, considering the current substitution. This prevents infinite loops.
 *
 * @param variable The variable being bound.
 * @param term The term the variable is being bound to.
 * @param subst The current substitution.
 * @return True if the variable occurs within the term, false otherwise.
 */
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

/**
 * Applies a substitution to a term, replacing variables with their bound values.
 * If a variable is encountered that is not in the substitution, it remains unchanged.
 *
 * @param term The term to apply the substitution to.
 * @param subst The substitution map.
 * @return A new term with variables replaced according to the substitution.
 */
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

    /**
     * Recursive helper function to find rule instantiations via backtracking.
     * Tries to unify each antecedent of the rule with known facts, maintaining consistent bindings.
     *
     * @param rule The rule being processed.
     * @param antecedentIndex The index of the current antecedent being matched.
     * @param currentSubst The substitution built up so far by matching previous antecedents.
     * @param factsToCheck The set of currently known facts to match against.
     * @param newlyDerived Set to add newly derived consequents to (if a full match is found).
     */
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


// --- Example Usage ---

fun main() {
    val engine = ForwardChainingEngineWithUnification()

    // Define Variables
    val varX = Variable("X")
    val varY = Variable("Y")
    val varZ = Variable("Z")

    // Define Constants
    val socrates = Constant("Socrates")
    val plato = Constant("Plato")
    val aristotle = Constant("Aristotle")
    val human = Constant("human")
    val mortal = Constant("mortal")
    val philosopher = Constant("philosopher")
    val teacherOf = Constant("teacherOf")
    val studentOf = Constant("studentOf")


    // Add Initial Facts (must be ground Predicates)
    engine.addFact(Predicate(human.value.toString(), listOf(socrates))) // human(Socrates)
    engine.addFact(Predicate(human.value.toString(), listOf(plato)))    // human(Plato)
    engine.addFact(Predicate(philosopher.value.toString(), listOf(socrates))) // philosopher(Socrates)
    engine.addFact(Predicate(teacherOf.value.toString(), listOf(socrates, plato))) // teacherOf(Socrates, Plato)
    engine.addFact(Predicate(teacherOf.value.toString(), listOf(plato, aristotle))) // teacherOf(Plato, Aristotle)


    // Add Rules (can contain Variables)
    // Rule 1: If X is human, then X is mortal.
    engine.addRule(Rule(
        antecedents = listOf(Predicate(human.value.toString(), listOf(varX))), // human(?X)
        consequent = Predicate(mortal.value.toString(), listOf(varX))          // mortal(?X)
    ))

    // Rule 2: If X is a philosopher, then X is human. (Implicit assumption made explicit)
    engine.addRule(Rule(
        antecedents = listOf(Predicate(philosopher.value.toString(), listOf(varX))), // philosopher(?X)
        consequent = Predicate(human.value.toString(), listOf(varX))                // human(?X) - This might re-derive existing facts, engine handles duplicates.
    ))

    // Rule 3: Transitivity of teaching: If X taught Y, and Y taught Z, then X taught Z indirectly.
    // NOTE: Simple forward chaining might loop infinitely if not careful with derived facts.
    // This engine adds derived facts and checks existence, preventing trivial loops for *identical* facts.
    // More complex loops (e.g., generating infinitely many different facts) are not prevented beyond the iteration limit.
    /* // Commented out for simplicity - transitivity can cause issues without careful state management
       engine.addRule(Rule(
           antecedents = listOf(
               Predicate(teacherOf.value.toString(), listOf(varX, varY)), // teacherOf(?X, ?Y)
               Predicate(teacherOf.value.toString(), listOf(varY, varZ))  // teacherOf(?Y, ?Z)
           ),
           consequent = Predicate(teacherOf.value.toString(), listOf(varX, varZ)) // teacherOf(?X, ?Z)
       ))
    */

    // Rule 4: If X is the teacher of Y, then Y is the student of X.
    engine.addRule(Rule(
        antecedents = listOf(Predicate(teacherOf.value.toString(), listOf(varX, varY))), // teacherOf(?X, ?Y)
        consequent = Predicate(studentOf.value.toString(), listOf(varY, varX))          // studentOf(?Y, ?X)
    ))


    // Perform Inference
    val allDerivedFacts = engine.infer()

    // Print results
    println("\n--- Final Inferred Facts (${allDerivedFacts.size}) ---")
    allDerivedFacts.sortedBy { it.toString() }.forEach { println("- $it") }

    // Check specific facts
    val isSocratesMortal = Predicate(mortal.value.toString(), listOf(socrates))
    if (isSocratesMortal in allDerivedFacts) {
        println("\nVerified: $isSocratesMortal")
    }

    val isPlatoMortal = Predicate(mortal.value.toString(), listOf(plato))
    if (isPlatoMortal in allDerivedFacts) {
        println("Verified: $isPlatoMortal")
    }

    val isAristotleMortal = Predicate(mortal.value.toString(), listOf(aristotle))
    if (isAristotleMortal in allDerivedFacts) {
        println("Verified: $isAristotleMortal") // This should NOT be derived unless we add human(Aristotle)
    } else {
        println("\nNot Derived: $isAristotleMortal (Aristotle's humanity/mortality wasn't established)")
    }

    val isPlatoStudentOfSocrates = Predicate(studentOf.value.toString(), listOf(plato, socrates))
    if (isPlatoStudentOfSocrates in allDerivedFacts) {
        println("Verified: $isPlatoStudentOfSocrates")
    }

    val isAristotleStudentOfPlato = Predicate(studentOf.value.toString(), listOf(aristotle, plato))
    if (isAristotleStudentOfPlato in allDerivedFacts) {
        println("Verified: $isAristotleStudentOfPlato")
    }
}