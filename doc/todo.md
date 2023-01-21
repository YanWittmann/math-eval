Features:

- [ ] while loops
- [ ] stack trace line numbers
- [ ] `return`, `break` and `continue` statements via special subclass of value which is detected and unwrapped in eval tree
- [ ] "safe-mode" in which no global variables can be changed
- [ ] write documentation
- [ ] input history for more than one element in documentation
- [ ] 'Object' type return empty if not exists on throw if not exists
- [ ] isPrimeUtil(n, i) { if (n == 2) true elif (n < 2) false elif (n % i == 0) false elif (i * i > n) true else isPrimeUtil(n, i + 1) }  
      isPrime(n) { n >| isPrimeUtil(2) }  
      range(1, 100).filter(isPrime)  
      range(2, 100).filter(isPrime).size()
- [ ] foldl = (funct, acc, list) -> { if (list.size() == 0) acc else foldl(funct, funct(list.head(), acc), list.tail()) }
- [ ] null --> empty value
- [ ] static symbols in custom types (java)
- [ ] if ...: ... elif ...: ... else: ...
- [ ] for ...: ...
- [ ] while ...: ...
- [ ] x instanceof y --> x.type() == y
- [ ] range(10, 1) --> downto
- [ ] range(1, 10, 2) --> step
- [ ] different types of assignments (e.g. `+=`, `-=`, `*=`, `/=`, `%=`)  
      *= += via a combined operator, is detected as + and = and then combined
- [ ] custom types in menter lang directly via map; must think more about a useful syntax for this.
- [ ] empty pass statement, like in python

Bugs:

```
{test:[1], hey: 4}[0]
accessors on maps
import system inline --> sleep(1000) --> Cannot resolve symbol 'sleep' on [sleep]
```

```
creator(a) {
test.test = a * 3;

f.setTest(a) { test.test = a };
f.getTest() { test.test };
f
}

created = creator(4)
print(created.getTest())
print(created.setTest(34))
print(created.getTest())
```

```
?? NUMBER_LITERAL: 3 on documentation
```