Features:

- [ ] while loops
- [ ] stack trace line numbers
- [ ] write documentation
- [ ] input history for more than one element in documentation
- [ ] if ...: ... elif ...: ... else: ...
- [ ] for ...: ...
- [ ] while ...: ...
- [ ] string output with quotes "", two separate rendering functions (?)
- [ ] fix `??` when printing syntax trees (also in documentation)
- [ ] documentation: info/warning/error boxes

To be thought about:

- [ ] custom types in menter lang directly via map; must think more about a useful syntax for this.
- [ ] rethink iterator system
- [ ] live preview "safe-mode" in which no global variables can be changed

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
```
