"""Authoring script for PyPath course content.

Edit the data below and run:
    python content/build_course.py
It writes app/src/main/assets/course/course.json, which the app loads at runtime.
Adding a level or sub-level here requires NO Kotlin changes.
"""
import json, pathlib, textwrap

def code(src, output=None, explain=(), caption=None):
    b = {"type": "code", "code": textwrap.dedent(src).strip("\n")}
    if output is not None: b["output"] = textwrap.dedent(output).strip("\n")
    if explain: b["explanation"] = list(explain)
    if caption: b["caption"] = caption
    return b

def text(t): return {"type": "text", "text": " ".join(t.split())}
def bullets(*items): return {"type": "bullets", "items": list(items)}
def tip(t, title="Tip"): return {"type": "tip", "title": title, "text": " ".join(t.split())}
def key(t): return {"type": "keypoint", "text": " ".join(t.split())}
def page(title, *blocks): return {"title": title, "blocks": list(blocks)}

def q(qid, prompt, options, correct, explanation, code_=None):
    assert len(options) == 4
    d = {"id": qid, "prompt": prompt, "options": options, "correctIndex": correct, "explanation": explanation}
    if code_: d["code"] = textwrap.dedent(code_).strip("\n")
    return d

def sub(sid, code_, title, summary, minutes, pages, questions, pass_percent=60):
    return {"id": sid, "code": code_, "title": title, "summary": summary, "estimatedMinutes": minutes,
            "steps": ["learn", "quiz"], "lesson": {"pages": pages},
            "quiz": {"questions": questions, "passPercent": pass_percent}}

def outline(sid, code_, title, summary):
    return {"id": sid, "code": code_, "title": title, "summary": summary, "steps": ["learn", "quiz"]}

# ───────────────────────────── LEVEL 1 ─────────────────────────────
level1 = {
  "id": "l1", "number": 1, "title": "Python Basics",
  "description": "Meet Python, store values in variables, explore data types and talk to the user.",
  "subLevels": [
    sub("l1s1", "1.1", "What is Python?", "Your first look at Python and your very first line of code.", 4, [
        page("Meet Python",
             text("""Python is a programming language — a way to give instructions to a computer
                  using words that are close to plain English."""),
             bullets("Easy to read and write", "Free and works on every computer",
                     "Used by Google, Netflix, NASA and millions of developers"),
             key("A program is just a list of instructions the computer follows from top to bottom.")),
        page("Your first program",
             text("The classic first program prints a message on the screen."),
             code('print("Hello, World!")', output="Hello, World!", explain=[
                 "print is a built-in function that shows something on the screen.",
                 "The text inside the quotes is what gets shown.",
                 "The parentheses ( ) hold what you want to print."])),
        page("Printing more",
             text("You can call print as many times as you like. Each call starts on a new line."),
             code('''
                 print("I am learning Python")
                 print(2 + 3)
             ''', output='''
                 I am learning Python
                 5
             ''', explain=["Text needs quotes.", "Numbers and maths don't — Python works out 2 + 3 for you."]),
             tip("Python reads your code line by line, starting at the top.")),
        page("Comments",
             text("Lines starting with # are comments. Python ignores them — they are notes for humans."),
             code('''
                 # This line is a comment
                 print("Comments are ignored")  # notes can go here too
             ''', output="Comments are ignored", explain=["Use comments to explain why your code does something."])),
    ], [
        q("l1s1q1", "What does print() do in Python?",
          ["Takes user input", "Displays output", "Creates a variable", "Ends the program"], 1,
          "print() shows whatever you put inside the parentheses on the screen."),
        q("l1s1q2", "What will this code display?", ["2 + 3", "\"5\"", "5", "Error"], 2,
          "Without quotes Python treats 2 + 3 as maths and prints the result, 5.", code_="print(2 + 3)"),
        q("l1s1q3", "Which symbol starts a comment in Python?", ["//", "#", "--", "/*"], 1,
          "In Python, everything after # on a line is a comment."),
        q("l1s1q4", "In what order does Python run your lines of code?",
          ["Random order", "Bottom to top", "Top to bottom", "Shortest line first"], 2,
          "Python runs instructions one after another, from the top of the file to the bottom."),
    ]),
    sub("l1s2", "1.2", "Variables", "Give names to values so you can reuse them.", 5, [
        page("What is a variable?",
             text("""A variable is a named box that stores a value. You create one with the
                  equals sign ="""),
             code('''
                 name = "Asha"
                 age = 21
                 print(name)
                 print(age)
             ''', output='''
                 Asha
                 21
             ''', explain=["name stores the text \"Asha\".", "age stores the number 21.",
                           "print(name) shows the value inside the box, not the word name."])),
        page("Changing a value",
             text("Variables can change. The newest value replaces the old one."),
             code('''
                 score = 10
                 score = 15
                 print(score)
             ''', output="15", explain=["The second line overwrites the first value.", "Only the latest value is kept."]),
             key("= means \"store this value\", not \"is equal to\".")),
        page("Naming rules",
             bullets("Use letters, numbers and underscores: user_name, level2",
                     "Can't start with a number: 2name is not allowed",
                     "No spaces: use first_name, not first name",
                     "Names are case-sensitive: Age and age are different"),
             tip("Pick names that describe the value, like total_price instead of x.")),
        page("Using variables together",
             code('''
                 apples = 4
                 oranges = 3
                 total = apples + oranges
                 print(total)
             ''', output="7", explain=["Python looks up the values of apples and oranges.",
                                       "It adds them and stores the result in total."])),
    ], [
        q("l1s2q1", "Which line correctly creates a variable?",
          ["city = \"Pune\"", "\"Pune\" = city", "city == \"Pune\"", "var city = \"Pune\""], 0,
          "The variable name goes on the left, the value on the right of a single =."),
        q("l1s2q2", "What does this code print?", ["10", "15", "25", "score"], 1,
          "The second assignment replaces 10 with 15.", code_="score = 10\nscore = 15\nprint(score)"),
        q("l1s2q3", "Which is a valid variable name?", ["2fast", "my name", "user_age", "class-1"], 2,
          "Names can use letters, digits and underscores but can't start with a digit or contain spaces/hyphens."),
        q("l1s2q4", "What is printed?", ["a + b", "34", "7", "Error"], 2,
          "a is 3 and b is 4, so a + b is 7.", code_="a = 3\nb = 4\nprint(a + b)"),
    ]),
    sub("l1s3", "1.3", "Data Types", "Text, whole numbers, decimals and True/False.", 5, [
        page("Every value has a type",
             text("Python keeps track of what kind of value something is. The four you'll use most:"),
             bullets("str — text, like \"hello\"", "int — whole numbers, like 42",
                     "float — decimal numbers, like 3.14", "bool — True or False")),
        page("Seeing the type",
             text("Use type() to ask Python what type a value is."),
             code('''
                 print(type("hi"))
                 print(type(7))
                 print(type(2.5))
                 print(type(True))
             ''', output='''
                 <class 'str'>
                 <class 'int'>
                 <class 'float'>
                 <class 'bool'>
             ''', explain=["type() returns the kind of value.", "Don't worry about the word class yet."])),
        page("Why types matter",
             text("The same symbol can behave differently depending on the type."),
             code('''
                 print(2 + 3)
                 print("2" + "3")
             ''', output='''
                 5
                 23
             ''', explain=["With numbers, + adds.", "With text, + joins strings together."]),
             key("\"5\" (text) and 5 (number) are different things in Python.")),
        page("Converting types",
             code('''
                 age_text = "20"
                 age = int(age_text)
                 print(age + 1)
             ''', output="21", explain=["int() turns the text \"20\" into the number 20.",
                                        "Now maths works on it. str() and float() convert too."])),
    ], [
        q("l1s3q1", "What is the type of 3.14?", ["int", "str", "float", "bool"], 2,
          "Numbers with a decimal point are floats."),
        q("l1s3q2", "What does this code print?", ["5", "23", "2 + 3", "Error"], 1,
          "Both values are strings, so + joins them into \"23\".", code_='print("2" + "3")'),
        q("l1s3q3", "Which value is a bool?", ["\"True\"", "True", "1.0", "'yes'"], 1,
          "True (without quotes) is a boolean. With quotes it would be a string."),
        q("l1s3q4", "Which function converts \"42\" into a whole number?", ["str()", "float()", "type()", "int()"], 3,
          "int() converts a value to an integer (whole number)."),
    ]),
    sub("l1s4", "1.4", "Input and Output", "Ask the user a question and respond to them.", 6, [
        page("Getting input",
             text("input() pauses the program and waits for the user to type something."),
             code('''
                 name = input("What is your name? ")
                 print("Hello,", name)
             ''', output='''
                 What is your name? Ravi
                 Hello, Ravi
             ''', explain=["The text inside input() is the question shown to the user.",
                           "Whatever they type is stored in name.",
                           "print can show several things separated by commas."])),
        page("Input is always text",
             text("Even if the user types a number, input() gives you a string."),
             code('''
                 age = input("Age: ")
                 print(type(age))
             ''', output='''
                 Age: 18
                 <class 'str'>
             '''),
             key("Convert input with int() or float() before doing maths.")),
        page("Doing maths with input",
             code('''
                 age = int(input("Age: "))
                 print("Next year you will be", age + 1)
             ''', output='''
                 Age: 18
                 Next year you will be 19
             ''', explain=["input() reads the text \"18\".", "int() turns it into the number 18.",
                           "Now age + 1 works."])),
        page("Formatting output",
             text("f-strings let you place variables directly inside text. Put f before the quotes and wrap variables in { }."),
             code('''
                 name = "Meera"
                 level = 1
                 print(f"{name} is on level {level}")
             ''', output="Meera is on level 1"),
             tip("f-strings are the cleanest way to mix text and values.")),
    ], [
        q("l1s4q1", "What does input() return?", ["A number", "A string", "A bool", "Nothing"], 1,
          "input() always returns what the user typed as a string."),
        q("l1s4q2", "How do you turn the user's answer into a whole number?",
          ["int(input())", "input(int())", "number(input())", "input().int"], 0,
          "Wrap input() inside int() to convert the result."),
        q("l1s4q3", "What does this print?", ["{x} apples", "x apples", "5 apples", "f5 apples"], 2,
          "In an f-string, {x} is replaced with the value of x.", code_='x = 5\nprint(f"{x} apples")'),
        q("l1s4q4", "What is shown to the user by input(\"Age: \")?",
          ["Nothing", "The word input", "Age: ", "An error"], 2,
          "The text inside input() is shown as a prompt before the user types."),
    ]),
  ],
}

# ───────────────────────────── LEVEL 2 ─────────────────────────────
level2 = {
  "id": "l2", "number": 2, "title": "Conditions",
  "description": "Teach your programs to make decisions with if, else and elif.",
  "subLevels": [
    sub("l2s1", "2.1", "Comparisons", "Compare values and get True or False.", 4, [
        page("Asking questions",
             text("Comparison operators compare two values. The answer is always True or False."),
             bullets("==  equal to", "!=  not equal to", ">  greater than", "<  less than",
                     ">=  greater or equal", "<=  less or equal")),
        page("Try them",
             code('''
                 print(5 > 3)
                 print(4 == 5)
                 print("a" != "b")
             ''', output='''
                 True
                 False
                 True
             ''', explain=["5 is greater than 3, so True.", "4 is not equal to 5, so False."]),
             key("== compares. A single = stores a value.")),
    ], [
        q("l2s1q1", "What does 7 >= 7 give?", ["True", "False", "7", "Error"], 0,
          ">= is True when the left side is greater than OR equal to the right."),
        q("l2s1q2", "Which operator checks if two values are equal?", ["=", "==", "!=", "=>"], 1,
          "== compares values; = assigns a value to a variable."),
        q("l2s1q3", "What is printed?", ["True", "False", "10", "Error"], 1,
          "10 is not less than 3, so the comparison is False.", code_="print(10 < 3)"),
    ]),
    sub("l2s2", "2.2", "if and else", "Run code only when a condition is True.", 5, [
        page("The if statement",
             code('''
                 age = 20
                 if age >= 18:
                     print("You can vote")
             ''', output="You can vote", explain=["The condition age >= 18 is True.",
                                                  "So the indented line underneath runs.",
                                                  "Notice the colon : at the end of the if line."])),
        page("Indentation matters",
             text("Python uses indentation (4 spaces) to know which lines belong to the if."),
             tip("Forgetting the colon or the indentation is the most common beginner error.", title="Watch out")),
        page("Adding else",
             code('''
                 temp = 15
                 if temp > 25:
                     print("It's hot")
                 else:
                     print("It's cool")
             ''', output="It's cool", explain=["15 > 25 is False, so the if block is skipped.",
                                               "The else block runs instead."])),
    ], [
        q("l2s2q1", "What must end an if line?", ["A semicolon ;", "A colon :", "A period .", "Nothing"], 1,
          "Every if, else and elif line ends with a colon."),
        q("l2s2q2", "What is printed?", ["Big", "Small", "Big and Small", "Nothing"], 1,
          "3 > 10 is False, so the else branch runs.",
          code_='n = 3\nif n > 10:\n    print("Big")\nelse:\n    print("Small")'),
        q("l2s2q3", "How does Python know which lines belong to an if?",
          ["Curly braces { }", "Indentation", "The word end", "Line numbers"], 1,
          "Python groups code by indentation, usually 4 spaces."),
    ]),
    sub("l2s3", "2.3", "elif", "Check several conditions in order.", 5, [
        page("More than two choices",
             text("elif (short for else if) lets you test another condition when the first one is False."),
             code('''
                 marks = 72
                 if marks >= 90:
                     print("Grade A")
                 elif marks >= 60:
                     print("Grade B")
                 else:
                     print("Keep practising")
             ''', output="Grade B", explain=["72 >= 90 is False, so Python moves on.",
                                             "72 >= 60 is True — Grade B is printed.",
                                             "Once a branch runs, the rest are skipped."])),
        page("Order matters",
             key("Python checks conditions from top to bottom and runs only the first one that is True."),
             tip("Put the most specific condition first.")),
    ], [
        q("l2s3q1", "What does elif mean?", ["else if", "end if", "elevate if", "either if"], 0,
          "elif is short for else if."),
        q("l2s3q2", "What is printed?", ["High", "Medium", "Low", "High and Medium"], 1,
          "50 > 80 is False; 50 > 30 is True, so Medium prints and the rest are skipped.",
          code_='x = 50\nif x > 80:\n    print("High")\nelif x > 30:\n    print("Medium")\nelse:\n    print("Low")'),
        q("l2s3q3", "How many branches of an if/elif/else chain can run?",
          ["All of them", "Exactly one at most", "Two", "None ever"], 1,
          "Only the first branch whose condition is True runs (or else if none are)."),
    ]),
  ],
}

def soon(lid, n, title, desc, subs):
    return {"id": lid, "number": n, "title": title, "description": desc, "available": False,
            "subLevels": [outline(f"{lid}s{i+1}", f"{n}.{i+1}", t, s) for i, (t, s) in enumerate(subs)]}

course = {
  "id": "python-beginner", "title": "Python for Beginners", "schemaVersion": 1,
  "levels": [
    level1, level2,
    soon("l3", 3, "Loops", "Repeat actions with for and while loops.",
         [("for loops", "Repeat over a range."), ("while loops", "Repeat until a condition changes."),
          ("break and continue", "Control the flow of loops.")]),
    soon("l4", 4, "Functions", "Package code into reusable building blocks.",
         [("Defining functions", "Write your own functions."), ("Parameters", "Pass values in."),
          ("Return values", "Send results back.")]),
    soon("l5", 5, "Lists", "Store many values in a single variable.",
         [("Creating lists", "Square-bracket basics."), ("Indexing", "Access items by position."),
          ("List methods", "append, remove and more.")]),
  ],
}

out = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/course/course.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(course, indent=2, ensure_ascii=False))
print("wrote", out)
