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

```result=(a, b) -> { a + b; };;;(a, b) -> { a + b; };;;(a, b) -> { a + b; }
add(a, b) { a + b };;;add(a, b) = a + b;;;add = (a, b) -> a + b
```

Functions are not named, but instead assigned to a variable and called via that variable. This makes it possible to pass
functions as arguments to other functions.

```result=(a, b) -> { a + b; };;;(f, a, b) -> { f(a, b); };;;3
add = (a, b) -> a + b;;;apply = (f, a, b) -> f(a, b);;;apply(add, 1, 2)
```