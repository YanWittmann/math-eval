Features:

- [ ] while loops
- [ ] stack trace line numbers
- [ ] `return`, `break` and `continue` statements via special subclass of value which is detected and unwrapped in eval
  tree
- [ ] "safe-mode" in which no global variables can be changed
- [ ] write documentation
- [ ] input history for more than one element in documentation
- [ ] static symbols in custom types (java)
- [ ] if ...: ... elif ...: ... else: ...
- [ ] for ...: ...
- [ ] while ...: ...
- [ ] custom types in menter lang directly via map; must think more about a useful syntax for this.
- [ ] import will search for files in the same directory with a fitting name/export, for which the files in the
  directory have to be indexed before parsing them, to prevent execution of the file. i.e. rewrite the import system
- [ ] reflection module that allows for calling methods by name, inherit values from other values, etc.
- [ ] operator operation with both sides as list, where each side must have the same keys in order for the operation to
  be valid
- [ ] rewrite iterator system to work on 'lists' and not 'key'-'value' maps and default to values if only one argument
  is required. (Do not use the size of the object?)
- [ ] string output with quotes "", two separate rendering functions (?)

```
Person = (name, alter) -> {
    private.name = name
    private.alter = alter
    public.getName = () -> private.name
    public.getAlter = () -> private.alter
    public.altern = () -> private.alter = private.alter + 1
    public.setName = name -> private.name = name
    public.execute = (f) -> f(private)
    public
}

yan = Person("Yan", 22)
print(yan.getName())
yan.execute(person -> person.name = "Thomas")
print(yan.getName())
```

```
isPrimeUtil(n, i) { if (n == 2) true elif (n < 2) false elif (n % i == 0) false elif (i * i > n) true else
isPrimeUtil(n, i + 1) }
isPrime(n) { n >| isPrimeUtil(2) }
range(1, 100).filter(isPrime)
range(2, 100).filter(isPrime).size()
```

```
foldl = (funct, acc, list) -> { if (list.size() == 0) acc else foldl(funct, funct(list.head(), acc), list.tail()) }
```

Bugs:

```
{test:[1], hey: 4}[0]
accessors on maps
import system inline --> sleep(1000) --> Cannot resolve symbol 'sleep' on [sleep]
```

```
?? NUMBER_LITERAL: 3 on documentation
```
