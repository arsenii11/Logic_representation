# Kotlin Forward Chaining Inference Engine

This project is a simple forward-chaining rule-based reasoning system implemented in a **single Kotlin file** (`main.kt`). It supports first-order logic predicates and unification.

## 🔍 What It Does

- Stores facts like `human(Socrates)`
- Applies rules like `human(?X) => mortal(?X)`
- Uses unification to apply rules with variables
- Derives new facts automatically using forward chaining

## 📂 File Structure

- `main.kt` – contains everything: data structures, unification logic, inference engine, and test functions (`main()`)

## ▶️ How to Run

### 🟢 Option 1: Online

1. Go to [https://play.kotlinlang.org](https://play.kotlinlang.org)
2. Paste the contents of `main.kt`
3. Click **Run**

### 💻 Option 2: IntelliJ IDEA or Android Studio

1. Create a new Kotlin project
2. Paste the code into `main.kt`
3. Run the `main()` function

### 🖥 Option 3: Terminal with Kotlin Installed

1. Save the file as `main.kt`
2. Compile:
   ```bash
   kotlinc main.kt -includejava -jar engine.jar
-runtime -d engine.jar

## ✅ Example Output

```text
✅ testHumanMortalInference passed.
✅ testPhilosopherIsHuman passed.
✅ testStudentOfDerivedFromTeacherOf passed.

✅ All tests passed.
```

## 👨‍💻 Author  
Arsenii Dragunkin  
AD21132

