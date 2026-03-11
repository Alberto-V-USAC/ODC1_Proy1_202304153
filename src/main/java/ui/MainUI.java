package ui;

import org.comp.db.DbManager;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainUI {
    private JPanel mainPanel;
    private JButton queryButton;
    private JButton reportsButton;
    private JTextArea inputArea;
    private JTextArea outputArea;

    public MainUI() {
        JFrame frame = new JFrame();
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                DbManager.getInstance().persist();
                System.exit(0);
            }
        });

        frame.setVisible(true);

        queryButton.addActionListener(e -> onQueryBtn());
    }

    private void onQueryBtn() {
        DbManager mng = DbManager.getInstance();

        try {
            mng.query(inputArea.getText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        outputArea.setText(mng.getLatestLog());
    }
}
