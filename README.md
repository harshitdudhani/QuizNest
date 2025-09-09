# QuizNest

QuizNest is a **Java-based interactive quiz application** where questions are stored in a text file and dynamically loaded at runtime.  
It allows users to test their knowledge, get instant feedback, and track their score.  
This project demonstrates **Java fundamentals** such as file I/O, collections, OOP, and exception handling.

---

## 📖 Project Overview

- Reads questions and answers from a `questions.txt` file.
- Displays questions with multiple-choice options.
- Accepts user input and checks answers.
- Provides instant feedback (Correct / Wrong).
- Tracks and displays the final score.
- Handles invalid inputs and missing files gracefully.

---

## ⚙️ Technologies & Concepts Used

- **Java File I/O** → Reading questions from a text file (`BufferedReader`, `FileReader`).
- **Collections Framework** → Storing questions and answers (`ArrayList`, `HashMap`).
- **Object-Oriented Programming (OOP)** → `Question` class, `QuizManager` class.
- **Exception Handling** → Managing errors like *file not found* or *invalid input*.
- **User Input Handling** → Using `Scanner` for answer collection.

---

## 🚀 How It Works

1. Program starts and loads questions from `questions.txt`.
2. Each question is displayed with its options.
3. User provides an answer (e.g., `a`, `b`, `c`).
4. Answer is checked against the correct option.
   - If correct → score increases.
   - If wrong → shows correct answer.
5. Process repeats until all questions are answered.
6. Final score is displayed at the end.

---

## 📂 Question File Format (`questions.txt`)

