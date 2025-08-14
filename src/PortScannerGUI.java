import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortScannerGUI extends JFrame {
    private JTextField hostField;
    private JButton scanButton;
    private JTextArea resultArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private static final int THREADS = 100;
    private static final int TIMEOUT = 100;
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;

    public PortScannerGUI() {
        setTitle("Сканер портов");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создаем компоненты
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Адрес хоста:"));
        hostField = new JTextField("localhost");
        inputPanel.add(hostField);
        inputPanel.add(new JLabel("Диапазон портов:"));
        inputPanel.add(new JLabel(MIN_PORT + " - " + MAX_PORT));

        scanButton = new JButton("Начать сканирование");
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        progressBar = new JProgressBar(MIN_PORT, MAX_PORT);
        progressBar.setStringPainted(true);

        statusLabel = new JLabel("Готов к сканированию");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Расположение компонентов
        setLayout(new BorderLayout(5, 5));
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(progressBar, BorderLayout.NORTH);
        southPanel.add(statusLabel, BorderLayout.CENTER);
        southPanel.add(scanButton, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        // Обработчик кнопки
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = hostField.getText().trim();
                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(PortScannerGUI.this,
                            "Введите адрес хоста", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                scanButton.setEnabled(false);
                resultArea.setText("");
                new Thread(() -> scanHost(host)).start();
            }
        });
    }

    private void scanHost(String host) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Сканирование " + host + "...");
            progressBar.setValue(0);
        });

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        int totalPorts = MAX_PORT - MIN_PORT + 1;

        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            final int currentPort = port;
            executor.execute(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, currentPort), TIMEOUT);
                    String message = "Порт " + currentPort + " открыт\n";
                    SwingUtilities.invokeLater(() -> {
                        resultArea.append(message);
                    });
                } catch (IOException ignored) {
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        int progress = currentPort - MIN_PORT;
                        progressBar.setValue(currentPort);
                        statusLabel.setText(String.format("Прогресс: %.1f%%",
                                (double)progress / totalPorts * 100));
                    });
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Сканирование завершено");
            scanButton.setEnabled(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PortScannerGUI gui = new PortScannerGUI();
            gui.setVisible(true);
        });
    }
}