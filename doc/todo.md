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
