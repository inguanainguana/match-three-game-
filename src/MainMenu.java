import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.sound.sampled.*;

public class MainMenu extends JFrame {

    private Font gameFont;
    private Clip backgroundMusic;
    private JLayeredPane layeredPane;
    private CustomModal currentModal = null;

    private CardLayout cardLayout;
    private JPanel contentStack;
    private BackgroundPanel menuPanel;
    private Game gameScreen;

    // Глобальные настройки для игры
    public static float currentVolume = 0f;
    public static boolean showGrid = true;
    public static int gameSpeed = 1; // 0 - Медленно, 1 - Нормально, 2 - Быстро

    // Палитра
    private final Color GOLD_BASE = new Color(218, 165, 32);
    private final Color GOLD_HOVER = new Color(255, 215, 0);
    private final Color SAND_BASE = new Color(139, 115, 85);
    private final Color SAND_HOVER = new Color(180, 150, 110);
    private final Color ACCENT_BASE = new Color(160, 82, 45);
    private final Color ACCENT_HOVER = new Color(190, 100, 60);

    public MainMenu() {
        setTitle("Золото Ра");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        gameFont = loadCustomFont("resources/fonts/4040-font.ttf", 20f);

        layeredPane = new JLayeredPane();
        setContentPane(layeredPane);

        cardLayout = new CardLayout();
        contentStack = new JPanel(cardLayout);
        contentStack.setBounds(0, 0, 800, 600);

        menuPanel = new BackgroundPanel("resources/img/фон.jpg");

        setupMenuLayout();

        // 1 - Выход в меню, 2 - Открытие настроек
        gameScreen = new Game(gameFont,
                () -> {
                    cardLayout.show(contentStack, "MENU_SCREEN");
                    if (backgroundMusic != null && !backgroundMusic.isRunning()) {
                        backgroundMusic.setFramePosition(0);
                        backgroundMusic.start();
                    }
                },
                () -> showModal("НАСТРОЙКИ")
        );

        contentStack.add(menuPanel, "MENU_SCREEN");
        contentStack.add(gameScreen, "GAME_SCREEN");

        layeredPane.add(contentStack, JLayeredPane.DEFAULT_LAYER);

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                contentStack.setBounds(0, 0, getWidth(), getHeight());
                if (currentModal != null) centerModal();
                contentStack.revalidate();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { showModal("ВЫХОД"); }
        });

        playMusic("resources/audio/OST match-three-game.wav");
    }

    private void setupMenuLayout() {
        menuPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel logoLabel = new JLabel("ЗОЛОТО РА");
        logoLabel.setFont(gameFont.deriveFont(Font.BOLD, 64f));
        logoLabel.setForeground(GOLD_HOVER);
        gbc.gridx = 0; gbc.gridy = 0;
        menuPanel.add(logoLabel, gbc);

        RoundedButton playButton = new RoundedButton("ИГРАТЬ ▶", GOLD_BASE, GOLD_HOVER);
        RoundedButton settingsButton = new RoundedButton("НАСТРОЙКИ", SAND_BASE, SAND_HOVER);
        RoundedButton exitButton = new RoundedButton("ВЫХОД", ACCENT_BASE, ACCENT_HOVER);

        Dimension btnDim = new Dimension(250, 60);
        playButton.setPreferredSize(btnDim);
        settingsButton.setPreferredSize(btnDim);
        exitButton.setPreferredSize(btnDim);

        playButton.setFont(gameFont.deriveFont(24f));
        settingsButton.setFont(gameFont.deriveFont(20f));
        exitButton.setFont(gameFont.deriveFont(20f));

        gbc.gridy = 1; menuPanel.add(playButton, gbc);
        gbc.gridy = 2; menuPanel.add(settingsButton, gbc);
        gbc.gridy = 3; menuPanel.add(exitButton, gbc);

        playButton.addActionListener(e -> {
            if (backgroundMusic != null) backgroundMusic.stop();
            cardLayout.show(contentStack, "GAME_SCREEN");
        });

        settingsButton.addActionListener(e -> showModal("НАСТРОЙКИ"));
        exitButton.addActionListener(e -> showModal("ВЫХОД"));
    }

    private void showModal(String type) {
        if (currentModal != null) return;
        currentModal = new CustomModal(type);
        centerModal();

        if (type.equals("ВЫХОД")) {
            setupExitContent();
        } else {
            setupSettingsContent();
        }

        layeredPane.add(currentModal, JLayeredPane.PALETTE_LAYER);
        layeredPane.repaint();
    }

    private void centerModal() {
        if (currentModal != null) {
            int w = 550;
            int h = 420;
            currentModal.setBounds((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
        }
    }

    private void setupExitContent() {
        JTextArea msg = new JTextArea("Открылся проход в твою эпоху. Уверен ли ты, что готов расстаться с этим древним миром и вернуться в своё время?");
        msg.setFont(gameFont.deriveFont(18f));
        msg.setForeground(new Color(45, 30, 10));
        msg.setBackground(new Color(252, 245, 220));
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        currentModal.add(msg, BorderLayout.CENTER);

        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        p.setOpaque(false);

        RoundedButton y = new RoundedButton("ДА, ВЕРНУТЬСЯ", GOLD_BASE, GOLD_HOVER);
        RoundedButton n = new RoundedButton("НЕТ, ОСТАТЬСЯ", ACCENT_BASE, ACCENT_HOVER);

        y.setPreferredSize(new Dimension(160, 50));
        n.setPreferredSize(new Dimension(160, 50));
        y.setFont(gameFont.deriveFont(16f));
        n.setFont(gameFont.deriveFont(16f));

        y.addActionListener(e -> System.exit(0));
        n.addActionListener(e -> closeModal());

        p.add(y); p.add(n);
        currentModal.add(p, BorderLayout.SOUTH);
    }

    private void setupSettingsContent() {
        JLabel title = new JLabel("НАСТРОЙКИ", SwingConstants.CENTER);
        title.setFont(gameFont.deriveFont(26f));
        title.setForeground(new Color(45, 30, 10));
        currentModal.add(title, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(3, 2, 10, 20));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Громкость
        JLabel volLabel = new JLabel("ГРОМКОСТЬ:");
        volLabel.setFont(gameFont.deriveFont(18f));
        JSlider slider = new JSlider(-40, 6);
        slider.setOpaque(false);
        slider.setValue((int) currentVolume);
        slider.addChangeListener(e -> {
            currentVolume = slider.getValue();
            if (backgroundMusic != null && backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(currentVolume);
            }
            if (gameScreen != null) gameScreen.updateVolume(currentVolume);
        });
        mainPanel.add(volLabel);
        mainPanel.add(slider);

        // 2. Сетка
        JLabel gridLabel = new JLabel("СЕТКА:");
        gridLabel.setFont(gameFont.deriveFont(18f));
        RoundedButton gridBtn = new RoundedButton(showGrid ? "ВКЛЮЧЕНА" : "ВЫКЛЮЧЕНА", SAND_BASE, SAND_HOVER);
        gridBtn.setFont(gameFont.deriveFont(16f));
        gridBtn.addActionListener(e -> {
            showGrid = !showGrid;
            gridBtn.setText(showGrid ? "ВКЛЮЧЕНА" : "ВЫКЛЮЧЕНА");
            if (gameScreen != null) gameScreen.repaint();
        });
        mainPanel.add(gridLabel);
        mainPanel.add(gridBtn);

        // 3. Скорость
        JLabel speedLabel = new JLabel("СКОРОСТЬ:");
        speedLabel.setFont(gameFont.deriveFont(18f));
        String[] speedTexts = {"МЕДЛЕННО", "НОРМАЛЬНО", "БЫСТРО"};
        RoundedButton speedBtn = new RoundedButton(speedTexts[gameSpeed], SAND_BASE, SAND_HOVER);
        speedBtn.setFont(gameFont.deriveFont(16f));
        speedBtn.addActionListener(e -> {
            gameSpeed = (gameSpeed + 1) % 3;
            speedBtn.setText(speedTexts[gameSpeed]);
        });
        mainPanel.add(speedLabel);
        mainPanel.add(speedBtn);

        currentModal.add(mainPanel, BorderLayout.CENTER);

        RoundedButton close = new RoundedButton("СОХРАНИТЬ", GOLD_BASE, GOLD_HOVER);
        close.setPreferredSize(new Dimension(180, 50));
        close.setFont(gameFont.deriveFont(18f));
        close.addActionListener(e -> closeModal());

        JPanel p = new JPanel(); p.setOpaque(false); p.add(close);
        currentModal.add(p, BorderLayout.SOUTH);
    }

    private void closeModal() {
        if (currentModal != null) {
            layeredPane.remove(currentModal);
            currentModal = null;
            layeredPane.repaint();
            if (contentStack.getComponents()[1].isVisible() && gameScreen != null) {
                gameScreen.resumeGame();
            }
        }
    }

    class CustomModal extends JPanel {
        public CustomModal(String title) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(252, 245, 220));
            g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 40, 40);
            g2.setColor(new Color(190, 150, 60));
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 40, 40);
            g2.dispose();
        }
    }

    private void playMusic(String path) {
        try {
            File musicPath = new File(path);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInput);

                if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(currentVolume);
                }

                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
        } catch (Exception ex) { }
    }

    private Font loadCustomFont(String path, float size) {
        try { return Font.createFont(Font.TRUETYPE_FONT, new File(path)).deriveFont(size);
        } catch (Exception e) { return new Font("Serif", Font.PLAIN, (int) size); }
    }

    class BackgroundPanel extends JPanel {
        private Image img;
        public BackgroundPanel(String path) { this.img = new ImageIcon(path).getImage(); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}

class RoundedButton extends JButton {
    private Color baseColor, hoverColor;
    private float alpha = 0f;
    private Timer timer;

    public RoundedButton(String text, Color base, Color hover) {
        super(text);
        this.baseColor = base;
        this.hoverColor = hover;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        timer = new Timer(20, e -> {
            if (getModel().isRollover()) alpha = Math.min(1f, alpha + 0.1f);
            else alpha = Math.max(0f, alpha - 0.1f);
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { timer.start(); }
            public void mouseExited(MouseEvent e) { timer.start(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), arc = 35;
        g2.setColor(baseColor);
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(hoverColor);
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        g2.setComposite(AlphaComposite.SrcOver);
        GradientPaint goldGlare = new GradientPaint(0, 0, new Color(255, 215, 0, 70), 0, h / 3, new Color(255, 200, 0, 20));
        g2.setPaint(goldGlare);
        g2.fillRoundRect(0, 0, w, h / 2, arc, arc);
        GradientPaint darkGlare = new GradientPaint(0, h / 2, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 50));
        g2.setPaint(darkGlare);
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        g2.setColor(new Color(255, 220, 100, 30));
        g2.fillRoundRect(5, 5, w - 10, h / 4, arc / 2, arc / 2);
        g2.setFont(getFont());
        g2.setColor(new Color(45, 30, 10));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}