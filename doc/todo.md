Features:

- [ ] Parser/Interpreter: while loops (?)
- [ ] different types of assignments (e.g. `+=`, `-=`, `*=`, `/=`, `%=`)

Bugs:
newline before EOF on lexer

if (val) {
  for (i in val) sum = sum + i
}
incorrectly assumes sum = sum + i is a STATEMENT
