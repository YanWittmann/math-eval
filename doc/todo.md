Features:

- [ ] for loops (?)
- [ ] while loops (?)
- [ ] different types of assignments (e.g. `+=`, `-=`, `*=`, `/=`, `%=`)

Bugs:

```
>> foo(x) = x + x
(x) -> { x + x; }
>> mapper(f, val) = f(val)
(f, val) -> { f; }
>> mapper(foo, 3)
6
>> [mapper(foo, 3)].keys()
Error: Cannot resolve symbol 'keys' on
[mapper].keys()
```

```
Make sure order is correct
>> [1, 2, 3, 4].entries()
[{"key": 0, "value": 1}, {"key": 3, "value": 4}, {"key": 2, "value": 3}, {"key": 1, "value": 2}]
```
