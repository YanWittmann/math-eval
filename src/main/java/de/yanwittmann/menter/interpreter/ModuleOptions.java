package de.yanwittmann.menter.interpreter;

import java.util.ArrayList;
import java.util.List;

public class ModuleOptions {

    private final List<String> forbiddenImports = new ArrayList<>();

    private final List<String> autoImports = new ArrayList<>();

    public ModuleOptions addForbiddenImport(String forbiddenImport) {
        forbiddenImports.add(forbiddenImport);
        return this;
    }

    public boolean isImportForbidden(String importName) {
        return forbiddenImports.contains(importName);
    }

    public void clearForbiddenImports() {
        forbiddenImports.clear();
    }

    public ModuleOptions addAutoImport(String importStatement) {
        autoImports.add(importStatement.startsWith("import ") ? importStatement.replace(";", "") : "import " + importStatement.replace(";", ""));
        return this;
    }

    public void clearAutoImports() {
        autoImports.clear();
    }

    public String getAutoImportsAsString() {
        return autoImports.size() > 0 ? String.join(";", autoImports) + ";" : "";
    }

    public boolean hasAutoImports() {
        return autoImports.size() > 0;
    }
}
