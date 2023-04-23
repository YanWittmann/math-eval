# Indentation and Statements

Menter combines parts of the indentation-based syntax of Python with the semicolon-based syntax of other languages like
Java.

## Statement separation

### Semicolon-based syntax

Using this syntax, you can separate statements using semicolons.

```result=20;;;20
result = 5 + 10; result += 5;;;result = 5 + 10;
result += 5;
```

This allows you to write code in a single line, or spread it out across multiple lines.

```result=45;;;45
result = 0;
for (i in range(1, 10)) { result += i; result -= 1 };;;result = 0;
for (i in range(1, 10)) {
  result += i; 
  result -= 1;
}
```

### Newline-based syntax

Almost all newlines are treated as statement finishers, meaning in a case like this, you can leave away the
semicolons:

```result=20
result = 5 + 10
result += 5
```

But why only almost all of them?
There are exceptions when a newline is not considered a statement separator: if the next line starts with one of

- `.`
- `elif` or `else`
- any operator
- or has an indentation that is greater than the line before it

the line will not be considered a new statement. See how the lines are joined into a single statement, as each line is
more indented than the last one:

```result=15
result =
  5 +
    10
```

As you can see in the next example, the operators and `.` make the lines go into a single statement, even though they
have the same indentation:

```result=15;;;[4, 16, 36, 64, 100]
result
= 5
+ 10;;;range(1, 10)
.map(x -> x ** 2)
.filter(x -> x % 4 == 0)
```

## Example

This small constructor function does not use semicolons to separate the statements. The first line `person.name = name`
is the only line in the demo that is more indented than the previous line, meaning only it will be joined with the
line before it. This previous line ends in a `{` which means that the joining does not make a difference, as `{ }` are
considered statement separators in of themselves.

```result=(name, alter) -> { person.name = name; person.alter = alter; person };;;{name: Yan, alter: 22};;;Yan
Person = (name, alter) -> {
    person.name = name
    person.alter = alter
    person
};;;yan = Person("Yan", 22);;;yan.name
```

This is also a great case where you can see how the statements are parsed and stored internally, as you can see, all
indentation and newline-based separations are mapped to the semicolon and curly bracket syntax:

```static
(name, alter) -> { person.name = name; person.alter = alter; person; }
```

## Code blocks

Code blocks are defined by curly brackets `{ }`. The opening and closing brackets are considered statement separators,
so there is no need to add a `;` before or after them.

Code blocks can be used anywhere, but **will NOT open a new variable scope**. Code blocks will return the last evaluated
value in them, meaning there is no need to call an anonymous function and return the value manually, if a new scope is
not needed.

```result=10;;;10;;;5
value = (() -> { # anonymous function, new scope
  result = 5
  result + 5
})();;;value = { # code block, parent scope
  result = 5
  result + 5
};;;result # is 5, as `result + 5` was never assigned to `result`
```

## Reasoning

If you are new to Menter, it is recommended to start using the semicolon syntax, as the newline-based one might be
confusing at first.

Although it may seem unusual, the newline-based syntax was adopted in Menter because the language was designed to
replace the old expression evaluator in the [Launch Anything Bar](https://github.com/YanWittmann/launch-anything), where 
code needed to be written in a single line.  
However, I also wanted to allow Menter to be used for other purposes, such as writing small scripts. The newline-based
separation of statements is an interesting approach to solve the statement separation problem, even though it is not
perfect. Therefore, Menter allows the use of both newlines and semicolons.

As Menter was a major learning experience for me, I wanted to experiment with as many new things as possible, including
the alternative syntax.