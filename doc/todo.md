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
isPrimeUtil(n, i) {
    if (n == 2) true
    elif (n < 2) false
    elif (n % i == 0) false
    elif (i * i > n) true
    else isPrimeUtil(n, i + 1)
}
isPrime(n) = n >| isPrimeUtil(2)
range(1, 100).filter(isPrime)
range(2, 100).filter(isPrime).size()

# alternative without recursion
isPrime(x) {
    if(x <= 3) return x > 1
    elif (x % 2 == 0 || x % 3 == 0) return false
    else {
        limit = round(x ^^ 0.5)
		if (limit >= 5)
            for (i in range(5, limit, 6)) {
                if (x % i == 0 || x % (i+2) == 0) return false
            }
        return true
    }
}
nextPrime(x) {
    for (i in range(1,100))
        if (isPrime(x + i)) return x + i
}
```

```
plot(space(-10, 10), x -> 0.4*x^3, x -> x^2, x -> 3 * x - 20, x -> 16 * sin(x) + 62, x -> 10 * cos(x * 2) - 37)
```

```
namen = ["Yan", "Nils", "Holger", "Ute", "Thomas", "Jonas", "Eren"]
createEntry(min, max) = {name: namen[floor(random(0, namen.size()))], age: round(random(min, max))}
db ::= range(1, 100).map(x -> createEntry(12, 26))
db ::= range(1, 100).map(x -> createEntry(27, 32))

db.filter(x -> x.age > 20).size()
db.filter(x -> x.age > 20).map(x -> x.age).avg()
```

Bugs:

```
{test:[1], hey: 4}[0]
accessors on maps
```

```
x^^2 + x^^3
```
