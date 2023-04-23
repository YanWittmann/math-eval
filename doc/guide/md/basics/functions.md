# Functions

If you visited the chapters before this one, you may have seen how functions work in Menter. Seeing how Menter is a
functional language, functions are first-class citizens and can therefore be stored in variables or passed around as
arguments to other functions.

## Function declaration

Functions can be created in three different ways, which are all mapped to the same internal representation.

- Traditional functions: `name(a, b) { ... }`
- Expression assignment: `name(a, b) = ...`
- Arrow functions: `name = (a, b) -> ...`

As with all control structures, the function body can only contain one statement. If you need more than one statement,
use `{ }` to group multiple statements into one.

```result=(a, b) -> { a + b };;;(a, b) -> { a + b };;;(a, b) -> { a + b }
add(a, b) { a + b };;;add(a, b) = a + b;;;add = (a, b) -> a + b
```

Functions are not named, but can be assigned to a variable and called via that variable. This makes it possible to pass
functions as arguments to other functions.

```result=(a, b) -> { a + b };;;(f, a, b) -> { f(a, b) };;;3
add = (a, b) -> a + b;;;apply = (f, a, b) -> f(a, b);;;apply(add, 1, 2)
```

## Argument list

The values passed into the function are called arguments and are separated by a `,` comma. The arguments are separated
by commas and can be of any type, as Menter is a dynamically typed language. You could say that the above function `add`
is a function that takes two arguments.

The traditional and expression assignment form always require a pair of parenthesis around the argument list, even if
only one argument is passed. The arrow function has three states:

- no arguments: `() -> ...`
- one argument: `x -> ...`
- multiple arguments: `(x, y, ...) -> ...`

## Return value

In Menter, the result of an expression is always the last value that was evaluated. That means, if not specified
otherwise via a `return` statement, the result of a function is the result of the last statement in the function body.

```result=3;;;3;;;2
doStuff = () -> { 1; 2; 3 }
doStuff();;;doStuff = () -> { 1; 2; return 3 }
doStuff();;;doStuff = () -> { 1; return 2; 3 }
doStuff()
```

To learn more about how this works internally, read the chapter about [value markers](Java_value_markers.html).

## Native functions

Menter allows for the creation of native functions, which are functions that are implemented in Java. This chapter will
not go into detail about how to create native functions, check out the [Java integration](java.html) chapters for that.

When visiting the [modules chapters](modules.html), you will find a lot of these kind of functions.

```static
native add()
```
