# Modules

Menter allows for exporting modules, which can be imported by other files. This allows for code reuse and a modular
structure.

Import and export statements are discovered by Menter before any code is executed, for several reasons:

- Import resolution order
- Discover available modules before executing them

More on those topics later.

This also means, that you cannot use evaluate-able expressions in import and export statements.

## Exporting Modules

A module is a named list of symbols (variables, functions, ...) from the current file/context. The module name is the
name of the module that will be created, which is used to import the module from other files.

```static
export [<name>, <...>] as <module_name>
```

Only the exported symbols will be available in the module, making these an interface for the module. This means, that
if done correctly, you can change the implementation of the module without breaking other files that import it.

## Importing Modules

To import a module, use the `import` statement:

```static
import <module_name>;;;import <module_name> inline;;;import <module_name> as <alias>
```

By renaming the module, you bind the module name to the alias. This allows you to use the alias instead of the module.
This will not allow you to import the same module twice with different aliases.

Importing a module inline will import all symbols from the module into the current context. This is useful for small
modules with functions like `print` or `cos`.

## Example

The upper code box exports a module called `listModule` with the symbols `push` and `pop`. The lower code box imports
the module as `lm` and uses it to modify the list `data`.

```result=() -> { data.pop() };;;null---id=listModule1
data = []
push = data.push
pop = data.pop;;;export [push, pop] as listModule
```

```after=listModule1
import listModule as lm;;;lm.push(6);;;lm.push(8);;;lm.pop();;;lm.pop()
```

Try it yourself by modifying the `data` array in the upper code box and see how the lower code box `pop` data changes.

## Import Resolution Order

On startup, as already mentioned in some of the chapters before this one, Menter will first find all export statements
from all `*.mtr` files that are either in the current directory or in the `MENTER_HOME` directory.

Whenever a module is then imported, Menter will check if the module is already loaded. If it is not, Menter will search
for the module in the index it created on startup. If it is found, Menter will repeat this process with the module that
was found: It will check if any imports are missing and load them recursively.

The order in which the modules are loaded is therefore from bottom to top. This means, that the module that is imported
first will be loaded last.

## Next chapters

The next chapters will cover the core modules that Menter provides by default. These are always available and can be
imported no matter where you currently are.

These modules will show a code box at the top that shows the source code of the module. This is useful to see how the
module is implemented and how you can use it.
