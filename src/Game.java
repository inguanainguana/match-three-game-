import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.File;

public class Game extends JPanel {
    private final int ROWS = 8;
    private final int COLS = 8;
    private int[][] board = new int[ROWS][COLS];
    private Image[] gemImages = new Image[5];
    private Image bgImage;

    private int score = 0;
    private int timeLeft = 120;
    private final int WIN_SCORE = 1200;
    private boolean isIntro = true;
    private boolean isGameOver = false;
    private boolean didWin = false;

    // Анимации
    private boolean isAnimating = false;
    private Point animSource = null, animTarget = null;
    private double animProgress = 0.0;
    private javax.swing.Timer animTimer;

    private boolean isFalling = false;
    private double[][] fallOffsets = new double[ROWS][COLS];
    private javax.swing.Timer fallingTimer;

    private javax.swing.Timer gameTimer;
    private Point selectedGem = null;
    private Font gameFont;
    private Clip gameMusic;
    private Runnable backToMenu;
    private Runnable openSettings;

    private RoundedButton settingsBtn;
    private RoundedButton backBtn;

    // Параметры интерфейса
    private final int SIDEBAR_WIDTH = 260;
    private final Color GOLD = new Color(218, 165, 32);
    private final Color SAND_BASE = new Color(139, 115, 85);
    private final Color SAND_HOVER = new Color(180, 150, 110);

    public Game(Font font, Runnable backToMenuAction, Runnable openSettingsAction) {
        this.gameFont = font;
        this.backToMenu = backToMenuAction;
        this.openSettings = openSettingsAction;
        setLayout(null);
        loadAssets();

        gameTimer = new javax.swing.Timer(1000, e -> {
            if (!isIntro && !isGameOver && !isFalling && timeLeft > 0) {
                timeLeft--;
                if (timeLeft <= 0) endGame();
                repaint();
            }
        });

        animTimer = new javax.swing.Timer(8, e -> {
            // Читаем глобальную скорость из MainMenu
            double step = MainMenu.gameSpeed == 0 ? 0.08 : (MainMenu.gameSpeed == 1 ? 0.18 : 0.35);
            animProgress += step;
            if (animProgress >= 1.0) {
                animProgress = 1.0;
                animTimer.stop();
                finalizeSwap();
            }
            repaint();
        });

        fallingTimer = new javax.swing.Timer(8, e -> {
            boolean stillFalling = false;
            // Читаем глобальную скорость гравитации из MainMenu
            double fallStep = MainMenu.gameSpeed == 0 ? 0.12 : (MainMenu.gameSpeed == 1 ? 0.22 : 0.45);
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (fallOffsets[r][c] > 0) {
                        fallOffsets[r][c] -= fallStep;
                        if (fallOffsets[r][c] < 0) fallOffsets[r][c] = 0;
                        stillFalling = true;
                    }
                }
            }
            if (!stillFalling) {
                fallingTimer.stop();
                isFalling = false;
                checkAndRemoveMatches(true);
            }
            repaint();
        });

        initBoard();

        // КНОПКА НАСТРОЕК
        settingsBtn = new RoundedButton("НАСТРОЙКИ", SAND_BASE, SAND_HOVER);
        settingsBtn.setFont(gameFont.deriveFont(18f));
        settingsBtn.addActionListener(e -> {
            pauseGame();
            openSettings.run();
        });
        add(settingsBtn);

        // КНОПКА МЕНЮ
        backBtn = new RoundedButton("В МЕНЮ", SAND_BASE, SAND_HOVER);
        backBtn.setFont(gameFont.deriveFont(18f));
        backBtn.addActionListener(e -> {
            exitGameAndReset();
            backToMenuAction.run();
        });
        add(backBtn);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                settingsBtn.setBounds(40, getHeight() - 170, 180, 50);
                backBtn.setBounds(40, getHeight() - 110, 180, 50);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isIntro) {
                    isIntro = false;
                    startGame();
                }
                else if (isGameOver) handleGameOverClick(e.getPoint());
                else if (!isAnimating && !isFalling) handleMouseClick(e.getPoint());
            }
        });
    }

    // Методы для управления паузой из MainMenu
    public void pauseGame() {
        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
    }

    public void resumeGame() {
        if (!isIntro && !isGameOver && !isFalling && !isAnimating) {
            if (gameTimer != null) gameTimer.start();
        }
    }

    // Метод для обновления громкости на лету
    public void updateVolume(float volume) {
        if (gameMusic != null && gameMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) gameMusic.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(volume);
        }
    }

    private void exitGameAndReset() {
        if (gameTimer != null) gameTimer.stop();
        if (animTimer != null) animTimer.stop();
        if (fallingTimer != null) fallingTimer.stop();
        stopMusic();
        score = 0;
        timeLeft = 120;
        isIntro = true;
        isGameOver = false;
        selectedGem = null;
        initBoard();
        repaint();
    }

    private void loadAssets() {
        try {
            bgImage = new ImageIcon("resources/img/фон.jpg").getImage();
            for (int i = 0; i < 5; i++) {
                File f = new File("resources/img/sprites/" + (i + 1) + ".png");
                if (f.exists()) gemImages[i] = new ImageIcon(f.getAbsolutePath()).getImage();
            }
        } catch (Exception e) {}
    }

    private void initBoard() {
        Random rand = new Random();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) board[r][c] = rand.nextInt(5);
        }
        while (checkAndRemoveMatches(false)) refillBoardInstantly();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (bgImage != null) g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
        drawSidebar(g2);
        drawBoard(g2);
        if (isIntro) drawIntro(g2);
        if (isGameOver) drawGameOver(g2);
        g2.dispose();
    }

    private void drawSidebar(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 215));
        g2.fillRect(0, 0, SIDEBAR_WIDTH, getHeight());
        g2.setColor(GOLD);
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH, getHeight());
        g2.setFont(gameFont.deriveFont(30f));
        g2.setColor(GOLD);
        g2.drawString("СЧЕТ", 40, 90);
        g2.setColor(Color.WHITE);
        g2.drawString(score + " / " + WIN_SCORE, 40, 130);
        g2.setColor(GOLD);
        g2.drawString("ВРЕМЯ", 40, 220);
        g2.setColor(timeLeft <= 15 ? Color.RED : Color.WHITE);
        String timeStr = String.format("%02d:%02d", timeLeft/60, timeLeft%60);
        g2.drawString(timeStr, 40, 260);
    }

    private void drawBoard(Graphics2D g2) {
        int cellSize = getCellSize();
        int offX = getBoardOffsetX();
        int offY = getBoardOffsetY();

        // СЕТКА - ЧИТАЕТ ГЛОБАЛЬНУЮ НАСТРОЙКУ
        if (MainMenu.showGrid) {
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(218, 165, 32, 80));
            for (int r = 0; r <= ROWS; r++) {
                g2.drawLine(offX, offY + r * cellSize, offX + COLS * cellSize, offY + r * cellSize);
            }
            for (int c = 0; c <= COLS; c++) {
                g2.drawLine(offX + c * cellSize, offY, offX + c * cellSize, offY + ROWS * cellSize);
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (isAnimating && ((r == animSource.x && c == animSource.y) || (r == animTarget.x && c == animTarget.y))) continue;
                int x = offX + c * cellSize;
                int y = offY + (int)((r - fallOffsets[r][c]) * cellSize);
                if (y >= offY - cellSize/2) drawGemAt(g2, board[r][c], x, y, cellSize, (selectedGem != null && selectedGem.x == r && selectedGem.y == c));
            }
        }
        if (isAnimating) {
            int x1 = offX + animSource.y * cellSize; int y1 = offY + animSource.x * cellSize;
            int x2 = offX + animTarget.y * cellSize; int y2 = offY + animTarget.x * cellSize;
            int curX1 = (int)(x1 + (x2 - x1) * animProgress); int curY1 = (int)(y1 + (y2 - y1) * animProgress);
            int curX2 = (int)(x2 + (x1 - x2) * animProgress); int curY2 = (int)(y2 + (y1 - y2) * animProgress);
            drawGemAt(g2, board[animSource.x][animSource.y], curX1, curY1, cellSize, false);
            drawGemAt(g2, board[animTarget.x][animTarget.y], curX2, curY2, cellSize, false);
        }
    }

    private void drawGemAt(Graphics2D g2, int type, int x, int y, int size, boolean sel) {
        if (type == -1) return;
        if (sel) { g2.setColor(new Color(255, 255, 255, 60)); g2.fillRoundRect(x+3, y+3, size-6, size-6, 12, 12); }
        int p = size / 8;
        if (gemImages[type] != null) g2.drawImage(gemImages[type], x+p, y+p, size-p*2, size-p*2, null);
    }

    private void drawIntro(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 240));
        g2.fillRect(SIDEBAR_WIDTH, 0, getWidth() - SIDEBAR_WIDTH, getHeight());
        int cx = SIDEBAR_WIDTH + (getWidth() - SIDEBAR_WIDTH) / 2;
        int cy = getHeight() / 2;
        g2.setColor(GOLD);
        g2.setFont(gameFont.deriveFont(58f));
        String title = "ЗОЛОТО РА";
        g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title)/2, cy - 60);
        g2.setFont(gameFont.deriveFont(22f));
        g2.setColor(new Color(230, 200, 150));
        g2.drawString("Легенды гласят: золото Ра ждет разгадавшего тайну камней.", cx - g2.getFontMetrics().stringWidth("Легенды гласят: золото Ра ждет разгадавшего тайну камней.")/2, cy + 10);
        g2.setFont(gameFont.deriveFont(18f));
        g2.setColor(Color.WHITE);
        g2.drawString("[ НАЖМИТЕ, ЧТОБЫ НАЧАТЬ ИГРУ ]", cx - g2.getFontMetrics().stringWidth("[ НАЖМИТЕ, ЧТОБЫ НАЧАТЬ ИГРУ ]")/2, cy + 110);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 245));
        g2.fillRect(SIDEBAR_WIDTH, 0, getWidth() - SIDEBAR_WIDTH, getHeight());
        int cx = SIDEBAR_WIDTH + (getWidth() - SIDEBAR_WIDTH) / 2;
        int cy = getHeight() / 2;
        g2.setFont(gameFont.deriveFont(54f));
        g2.setColor(didWin ? Color.GREEN : Color.RED);
        String t = didWin ? "ПОБЕДА!" : "ПОРАЖЕНИЕ";
        g2.drawString(t, cx - g2.getFontMetrics().stringWidth(t)/2, cy - 60);
        g2.setFont(gameFont.deriveFont(22f));
        g2.setColor(GOLD);
        g2.drawString("[ ЕЩЁ РАЗ ]", cx - g2.getFontMetrics().stringWidth("[ ЕЩЁ РАЗ ]")/2, cy + 40);
        g2.drawString("[ В МЕНЮ ]", cx - g2.getFontMetrics().stringWidth("[ В МЕНЮ ]")/2, cy + 90);
    }

    private void handleMouseClick(Point p) {
        int cellSize = getCellSize();
        int col = (p.x - getBoardOffsetX()) / cellSize;
        int row = (p.y - getBoardOffsetY()) / cellSize;
        if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
            if (selectedGem == null) selectedGem = new Point(row, col);
            else {
                if (isAdjacent(selectedGem, new Point(row, col))) {
                    animSource = new Point(selectedGem.x, selectedGem.y);
                    animTarget = new Point(row, col);
                    animProgress = 0.0; isAnimating = true; animTimer.start();
                }
                selectedGem = null;
            }
            repaint();
        }
    }

    private void finalizeSwap() {
        isAnimating = false;
        swapGems(animSource.x, animSource.y, animTarget.x, animTarget.y);
        if (!checkAndRemoveMatches(true)) swapGems(animSource.x, animSource.y, animTarget.x, animTarget.y);
        repaint();
    }

    private boolean checkAndRemoveMatches(boolean addScore) {
        if (isFalling) return false;
        boolean[][] toRemove = new boolean[ROWS][COLS];
        boolean found = false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS - 2; c++) {
                if (board[r][c] != -1 && board[r][c] == board[r][c+1] && board[r][c] == board[r][c+2]) {
                    toRemove[r][c] = toRemove[r][c+1] = toRemove[r][c+2] = true; found = true;
                }
            }
        }
        for (int r = 0; r < ROWS - 2; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != -1 && board[r][c] == board[r+1][c] && board[r][c] == board[r+2][c]) {
                    toRemove[r][c] = toRemove[r+1][c] = toRemove[r+2][c] = true; found = true;
                }
            }
        }
        if (found) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (toRemove[r][c]) { board[r][c] = -1; if (addScore) score += 10; }
                }
            }
            applyGravity();
        }
        return found;
    }

    private void applyGravity() {
        isFalling = true;
        Random rand = new Random();
        for (int c = 0; c < COLS; c++) {
            int emptySpot = ROWS - 1;
            for (int r = ROWS - 1; r >= 0; r--) {
                if (board[r][c] != -1) {
                    if (emptySpot != r) { board[emptySpot][c] = board[r][c]; fallOffsets[emptySpot][c] = emptySpot - r; board[r][c] = -1; }
                    emptySpot--;
                }
            }
            for (int r = emptySpot; r >= 0; r--) { board[r][c] = rand.nextInt(5); fallOffsets[r][c] = emptySpot + 0.8; }
        }
        fallingTimer.start();
    }

    private void refillBoardInstantly() {
        Random rand = new Random();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) if (board[r][c] == -1) board[r][c] = rand.nextInt(5);
        }
    }

    private void handleGameOverClick(Point p) {
        int cx = SIDEBAR_WIDTH + (getWidth() - SIDEBAR_WIDTH) / 2;
        int cy = getHeight() / 2;
        if (p.y > cy + 20 && p.y < cy + 60 && Math.abs(p.x - cx) < 100) {
            initBoard();
            startGame();
        }
        if (p.y > cy + 70 && p.y < cy + 110 && Math.abs(p.x - cx) < 100) {
            exitGameAndReset();
            backToMenu.run();
        }
    }

    private void startGame() {
        score = 0;
        timeLeft = 120;
        isGameOver = false;
        gameTimer.start();
        playMusic("resources/audio/OST match-three-game.wav");
        repaint();
    }

    private void endGame() {
        gameTimer.stop();
        isGameOver = true;
        didWin = (score >= WIN_SCORE);
        repaint();
    }

    private int getCellSize() { return Math.min((getWidth() - SIDEBAR_WIDTH - 100) / COLS, (getHeight() - 100) / ROWS); }
    private int getBoardOffsetX() { return SIDEBAR_WIDTH + (getWidth() - SIDEBAR_WIDTH - (COLS * getCellSize())) / 2; }
    private int getBoardOffsetY() { return (getHeight() - (ROWS * getCellSize())) / 2; }
    private boolean isAdjacent(Point p1, Point p2) { return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y) == 1; }
    private void swapGems(int r1, int c1, int r2, int c2) { int t = board[r1][c1]; board[r1][c1] = board[r2][c2]; board[r2][c2] = t; }

    private void playMusic(String path) {
        try {
            stopMusic();
            File musicFile = new File(path);
            if (!musicFile.exists()) return;
            AudioInputStream ai = AudioSystem.getAudioInputStream(musicFile);
            gameMusic = AudioSystem.getClip();
            gameMusic.open(ai);

            // Громкость музыки сразу при запуске
            if (gameMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) gameMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(MainMenu.currentVolume);
            }

            gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
            gameMusic.start();
        } catch (Exception ex) {}
    }

    private void stopMusic() {
        if (gameMusic != null) {
            if (gameMusic.isRunning()) gameMusic.stop();
            gameMusic.close(); gameMusic = null;
        }
    }
}