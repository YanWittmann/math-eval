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
There are exceptions when a newline is not considered a statement separator: if the next line starts with

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

In the next two cases, it would obviously make sense to indent the follow-up lines by a bit, but for the demonstration,
they all hava the same and are still treated as single statements:

```result=15;;;[4, 16, 36, 64, 100]
result
= 5
+ 10;;;range(1, 10)
.map(x -> x ** 2)
.filter(x -> x % 4 == 0)
```

## Example

This small constructor function does not use semicolons to separate the statements. The first line `person.name = name`
is the only line in the demo that is more incremented than the previous line, meaning only it will be joined with the
line before it. This previous line ends in a `{` which means that the joining does not make a difference, as `{ }` are
considered statement separators in of themselves.

```result=(name, alter) -> { person.name = name; person.alter = alter; person; };;;{name: Yan, alter: 22};;;Yan
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

## Reasoning

When you are new to the language, I'd recommend using the semicolon syntax, as the indentation and newline based one
might be confusing at first.

It may seem strange at first, but seeing where this language comes from and the use-case it was made for, it makes a bit
more sense:

Code in Menter needs to be able to be able to be written in a single line, since the main reason it was developed was
that it should replace the old expression evaluator in the
[Launch Anything Bar](https://github.com/YanWittmann/launch-anything). On the other hand, I wanted to allow other use
cases for the language, such as writing small scripts. I find that newline separation is an interesting approach to
solve the statement separation problem, even if it is not perfect. This is why Menter allows both newlines and
semicolons.

Also, this language is has been a major learning experience for me and I wanted to try out as many new things as I
could. This alternate syntax is one of the few things that actually made sense (at least, for me) and therefore I left
it in.
