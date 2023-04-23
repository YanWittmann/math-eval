# Basic Types and Values

Menter includes a variety of basic data types such as numbers, strings, booleans, lists and maps/dicts/objects.  
Values are the smallest building block everything else is made of: Every expression can be evaluated into a single
value, even control structures like `if`, `for`, and so on.

```result=42
40 + if (true) 2 else 10
```

For a list of all operators that can be applied to values, see [Operators](Core_Language_operators.html).

Although the types are referred to as 'primitive' or 'basic' types, they are all objects that can have methods or
properties that can be called using the dot `.` syntax.

## Numbers `number`

Unlike most languages, numbers in Menter can grow as large as you need them to be, as they are internally represented by
`BigDecimal` instances. They have a default precision of 20 digits, but can be increased programmatically.

```result=42;;;265252859812191058636308480000000
42;;;30!
```

## Strings `string`

Strings are sequences of characters. They are written by enclosing the characters in double quotes (`""`) and can be
concatenated using the `+` operator.

```result="Hello World"
"Hello " + "World"
```

## Objects/Lists `object`

Create lists by enclosing a comma-separated list of values in square brackets (`[]`) and objects by enclosing a
comma-separated list of key-value pairs (`key: value`) in curly braces (`{}`).

```result=[1, 2, 3];;;{a: 1, b: 3}
[1, 2, 3];;;{a: 1, b: 2 + 1}
```

At a first glance, it may seem that lists and objects are two different constructs. Like in JavaScript however, this is
not the case in Menter. Internally, both objects and lists are stored in the same data structure.

This can be seen when creating a list and setting a non-integer key to a value:

```result=[1, 2, 3];;;7;;;{0: 1, 1: 2, 2: 3, key: 7}
listOrObj = [1, 2, 3];;;listOrObj["key"] = 7;;;listOrObj
```

You may note that if you try to assign a value to a key in an object that does not exist yet, the according keys are
created automatically. This even applies over multiple layers of nested objects, as long as the type is an object, the
according keys are created:

```result=Yan;;;[];;;"Programming"
person.name = "Yan";;;person.interests = []  # required for the next line to work;;;person.interests[0] = "Programming"
```

There are more features connected to objects, but more on those in the [Types](Core_Language_types.html) section.

## Functions `function`, `value_function`, `native_function`, `reflective_function`

A function is a way to transform values. There are multiple ways functions can be defined in Menter, which are all
reduced to a single internal representation.

- Traditional functions: `name(a, b) { ... }`
- Expression assignment: `name(a, b) = ...`
- Arrow functions: `name = (a, b) -> ...`

```result=(a, b) -> { a + b };;;(a, b) -> { a + b };;;(a, b) -> { a + b }
add(a, b) { a + b };;;add(a, b) = a + b;;;add = (a, b) -> a + b
```

Functions are not named, but instead assigned to a variable and called via that variable. This makes it possible to pass
functions as arguments to other functions.

```result=(a, b) -> { a + b };;;(f, a, b) -> { f(a, b) };;;3
add = (a, b) -> a + b;;;apply = (f, a, b) -> f(a, b);;;apply(add, 1, 2)
```
