package de.yanwittmann.menter.interpreter.type;

public class NativeTestClass {

    private int testValue = 0;

    public NativeTestClass() {
    }

    public NativeTestClass(int testValue) {
        this.testValue = testValue;
    }

    public void setTestValue(int testValue) {
        this.testValue = testValue;
    }

    public int getTestValue() {
        return testValue;
    }

    public void setIfPasswordCorrect(String password, int testValue) {
        if (password.equals("test")) {
            this.testValue = testValue;
        }
    }
}
