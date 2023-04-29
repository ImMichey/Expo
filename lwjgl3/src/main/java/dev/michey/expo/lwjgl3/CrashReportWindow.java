package dev.michey.expo.lwjgl3;

import dev.michey.expo.log.ExpoLogger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CrashReportWindow {

    private final JFrame frame;
    private final JPanel panel;
    private final JLabel label;
    private final JButton button;
    private final JButton buttonExit;

    public CrashReportWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        frame = new JFrame("Expo Crash Report");
        frame.setSize(800, 300);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        {
            panel = new JPanel();
            panel.setBackground(Color.decode("#262632"));
            panel.setSize(800, 300);
            panel.setLayout(null);
        }

        {
            label = new JLabel("<html><div style='text-align: center;'>Expo crashed :(<br><br>A logfile was created at: " + ExpoLogger.LOG_FILE_ABSOLUTE_PATH + "</div></html>") {

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    super.paintComponent(g);
                }

            };
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Lucida Sans", Font.PLAIN, 24));
            label.setSize(800, 200);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            frame.add(label);
        }

        {
            button = new JButton("Click to open log file location");
            button.setSize(368, 40);
            button.setLocation(16, 200);
            button.addActionListener(e -> {
                try {
                    if(ExpoLogger.LOG_FOLDER_ABSOLUTE_PATH.length() <= 1) return;
                    Desktop.getDesktop().open(new File(ExpoLogger.LOG_FOLDER_ABSOLUTE_PATH));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            frame.add(button);
        }

        {
            buttonExit = new JButton("Close");
            buttonExit.setSize(368, 40);
            buttonExit.setLocation(400, 200);
            buttonExit.addActionListener(e -> System.exit(0));
            frame.add(buttonExit);
        }

        frame.add(panel);
    }

    public void show() {
        frame.setVisible(true);
    }

}
