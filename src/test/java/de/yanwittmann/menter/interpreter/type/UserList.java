package de.yanwittmann.menter.interpreter.type;

import de.yanwittmann.menter.interpreter.structure.value.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TypeMetaData(typeName = "UserList", moduleName = "users")
public class UserList extends CustomType {

    private final List<Value> users = new ArrayList<>();

    public UserList(List<Value> parameters) {
        super(parameters);
    }

    @TypeFunction
    public Value addUser(List<Value> parameters) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.STRING.getType(), PrimitiveValueType.NUMBER.getType()},
                {"User"}
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                final User user = new User(Collections.emptyList());
                user.setName(Collections.singletonList(parameters.get(0)));
                user.setAge(Collections.singletonList(parameters.get(1)));
                users.add(new Value(user));
                break;
            case 1:
                users.add(parameters.get(0));
                break;
            case -1:
                throw invalidParameterCombinationException(getClass().getSimpleName(), "registerUser", parameters, parameterCombinations);
        }

        return Value.empty();
    }

    @TypeFunction
    public Value getUsers(List<Value> parameters) {
        return new Value(users);
    }

    @Override
    public Value iterator() {
        return new Value(users.iterator());
    }
}
