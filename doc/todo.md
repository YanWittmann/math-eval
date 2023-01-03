Features:

- [ ] while loops
- [ ] different types of assignments (e.g. `+=`, `-=`, `*=`, `/=`, `%=`)
- [ ] stack trace line numbers
- [ ] `break` and `continue` statements
- [ ] `return` statement
- [ ] "safe-mode" in which no global variables can be changed

Bugs:

```
val = TestType(1)
val.addToList(5)
val.addToList(10)
sum = 0
if (val) {
  for (i in val) { sum = sum + i } # This should not need a code block
}
sum

= 15
```

There seems to be automatic type conversion going on here: "4" + 45  ->  49

```
fun(x) {
  if (x.type() == "number") x + 5
  else if (x.type() == "object") x.map(x -> x + 5)
  else 0
}
```

```
sub(x, y) = x - y
export [sub] as math

import math
val = x -> {math.sub(-2, x)}
val(4)
```
Cannot resolve symbol 'x' on x.
Did you mean 'add', 'val', 'test'?
in [codebox-763038.val] at x
in [codebox-763038.val] at math.sub(-2, x)
in [codebox-763038    ] at val(4)
Local symbols:  sub (function)
Global symbols: add (function), test (number), val (function)
-> null
