package de.yanwittmann.menter;

import de.yanwittmann.menter.interpreter.MenterInterpreter;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.operator.Operators;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;

public class UiTest extends JFrame {

    public UiTest() {
        super("UiTest");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);

        // single text input above output text label
        final JPanel panel = new JPanel();
        panel.add(new JLabel("Text:"));
        final JTextArea in = new JTextArea(10, 70);
        panel.add(in);
        panel.add(new JLabel("Output:"));
        final JTextComponent out = new JTextField(70);
        panel.add(out);
        add(panel);

        final MenterInterpreter interpreter = new MenterInterpreter(new Operators());

        interpreter.getModuleOptions().addAutoImport("system inline");
        interpreter.getModuleOptions().addAutoImport("math inline");

        in.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                final String input = in.getText();
                try {
                    final Value result = interpreter.evaluateInContextOf(input, "UiTest");
                    out.setText(result.toDisplayString());
                } catch (Exception e) {
                    out.setText(e.getMessage()
                            .replace(" +", " ")
                            .replace("\t", "  ")
                    );
                }
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new UiTest();
    }
}
