# Kotlin Forward Chaining Engine with Unification

This project provides a basic implementation of a forward-chaining rule-based inference engine in Kotlin, contained within a **single source file**. It includes a unification mechanism to support rules with variables.

## Purpose

The engine aims to deduce new facts from an initial set of known facts and a collection of inference rules. It mimics a simple form of logical reasoning used in AI and expert systems. This implementation was created as an example solution for a task related to Logic and Computation, demonstrating AI reasoning principles.

## Core Concepts

1.  **Facts**: Represented as ground `Predicate` objects (structured terms with no variables). Examples: `human(Socrates)`, `teacherOf(Socrates, Plato)`.
2.  **Rules**: Defined by the `Rule` data class, consisting of a list of antecedent `Predicate`s and a single consequent `Predicate`. Antecedents and consequents can contain `Variable`s. Example: `IF human(?X) THEN mortal(?X)`.
3.  **Terms**: The basic building blocks:
    * `Variable`: Represents a placeholder (e.g., `?X`).
    * `Constant`: Represents a fixed value (e.g., `Socrates`, `human`).
    * `Predicate`: Represents a structured term or relation (e.g., `human(Socrates)`).
4.  **Unification**: The process of finding a substitution (a set of variable bindings) that makes two terms (potentially containing variables) identical. Implemented in the `unify` function. It includes an "occurs check" to prevent infinite loops.
5.  **Substitution**: A mapping from variables to terms. Used by unification and applied to rule consequents to generate new facts using `applySubstitution`.
6.  **Forward Chaining**: The inference strategy used. The engine iteratively applies rules to the known facts:
    * It searches for combinations of facts that unify with the antecedents of a rule under a consistent substitution.
    * If such an instantiation is found, the substitution is applied to the rule's consequent.
    * The resulting ground predicate is added to the set of known facts if it's novel.
    * This process repeats until no new facts can be derived in a full pass over the rules.

## Code Structure (Single File)

All the following components are defined within the single Kotlin source file (e.g., `InferenceEngine.kt`):

* **Data Structures**:
    * `Term`: Sealed interface for `Variable`, `Constant`, `Predicate`.
    * `Variable`: Represents variables (e.g., `?X`).
    * `Constant`: Represents constants (e.g., `"Socrates"`).
    * `Predicate`: Represents structured data like `functor(arg1, arg2, ...)`.
    * `Fact`: Type alias for `Predicate` when used as a ground fact.
    * `Rule`: Data class holding `antecedents: List<Predicate>` and `consequent: Predicate`.
    * `Substitution`: Type alias for `MutableMap<Variable, Term>`.
* **Unification Logic**:
    * `unify(Term, Term, Substitution): Boolean`: Attempts to unify two terms, updating the substitution.
    * `unifyVariable(Variable, Term, Substitution): Boolean`: Helper for unifying a variable.
    * `occursCheck(Variable, Term, Substitution): Boolean`: Prevents binding a variable to a term containing itself.
    * `applySubstitution(Term, Substitution): Term`: Applies bindings to a term to create a new (potentially ground) term.
* **Inference Engine**:
    * `ForwardChainingEngineWithUnification`: The main class orchestrating the inference process.
    * `addFact(Fact)`: Adds an initial ground fact.
    * `addRule(Rule)`: Adds an inference rule.
    * `infer(): Set<Fact>`: Runs the forward-chaining algorithm.
    * `findAndApplyRuleInstantiations(...)`: Internal recursive function to find rule matches.
    * `reset()`: Clears facts and rules.
* **Example Usage**:
    * `main()`: Function demonstrating how to use the engine with example facts and rules.

## How to Run

1.  **Save:** Save the entire Kotlin code block into a single file named `InferenceEngine.kt` (or any other `.kt` name).
2.  **Compile:** You need the Kotlin compiler (`kotlinc`). Compile the file:
    ```bash
    kotlinc InferenceEngine.kt -include-runtime -d engine.jar
    ```
3.  **Run:** Execute the compiled JAR file:
    ```bash
    java -jar engine.jar
    ```
    Alternatively, if you are using an IDE like IntelliJ IDEA:
    * Create a new Kotlin project.
    * Paste the code into a `.kt` file within the project.
    * Run the `main` function directly from the IDE.

## Example Output

Running the example `main` function will produce output similar to this (order might vary slightly):
