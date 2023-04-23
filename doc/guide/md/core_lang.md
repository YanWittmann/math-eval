# Core language

Let's start with a quick overview of the core language. The following chapters are made to make you familiar with the
syntax and principles of Menter.

A few things to note about the code boxes, as seen below:

- You can make them interactive using a [local Menter sever](execute_code.html)
- Input code that spans multiple lines is indicated using `|` pipe symbols. This code is then sent to the server as a
  single input (but still as separate lines)
- Hold down `shift` and press `enter` to enter multiple lines of code yourself
- The result of an expression is displayed behind an `->`
- If the expression printed something to the output stream, it is displayed below the expression

```result=(funct, acc, list) -> { if (list.size() == 0) acc else { foldl(funct, funct(list.head(), acc), list.tail()) } };;;6;;;Result: 6
foldl(funct, acc, list) {
    if (list.size() == 0) acc
    else foldl(funct, funct(list.head(), acc), list.tail())
};;;val = [1, 2, 3] |> foldl((*), 1);;;print("Result: " + val)
```

But now go check out the subchapters and start with [Primitive Types](Core_Language_primitive_types.html) by clicking
the link or using the right arrow key.
