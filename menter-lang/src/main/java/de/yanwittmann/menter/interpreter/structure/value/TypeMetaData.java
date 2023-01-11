package de.yanwittmann.menter.interpreter.structure.value;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TypeMetaData {
    String typeName();
    String moduleName();
}
