import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

class Game extends JPanel implements Runnable, KeyListener {
    // Window constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;

    // Game state tracking
    private Thread gameThread;
    private volatile boolean isRunning = true; // Thread kill switch
    private String gameState = "PLAYING"; 
    private int timer = 60; 
    private int frameCount = 0; 
    private int maxRounds = 3;
    private int p1RoundsWon = 0;
    private int p2RoundsWon = 0;
    private int restartTimer = 0;

    // Background Image
    private Image backgroundImage = null;

    // Objects
    private Player player1;
    private Player player2;
    private Level currentLevel;
    private ArrayList<PowerUp> powerUps = new ArrayList<>();

    // Keyboard state tracking
    private boolean wPressed, aPressed, sPressed, dPressed;
    private boolean upPressed, leftPressed, downPressed, rightPressed;
    private boolean isPaused = false;

    private JFrame frame; // Reference to the game window
    private String levelName;
    private boolean isTournament = false;
    private int[] playerWins = new int[2];
    private String[] stageOrder = {"Street Side", "Subway", "Roof Top"};
    private int currentStage = 0;

    // Character Selections
    private String p1CharacterSelection;
    private String p2CharacterSelection;

    private static Clip jumpClip, punchClip, kickClip, menuMusicClip, levelMusicClip;

    // Helper method to get relative path that works on all computers
    public static String getResourcePath(String resourceName) {
        return "." + File.separator + resourceName;
    }

    public Game(String selection, String p1Char, String p2Char) {
        this.p1CharacterSelection = p1Char;
        this.p2CharacterSelection = p2Char;
        
        if (selection.equals("Best of Three")) {
            isTournament = true;
            levelName = "Street Side";
        } else {
            levelName = selection;
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(screenSize);
        setBackground(getBackgroundColor(levelName)); 
        setFocusable(true);
        addKeyListener(this);

        if (levelName.equals("Street Side")) {
            setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 1 Background.png"));
        }
        if (levelName.equals("Subway")) {
            setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 2 Background.png"));
        }
        if (levelName.equals("Roof Top")) {
            setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 3 Background.png"));
        }

        loadSounds();
        startNewMatch();
    }

    private void nextStage() {
        currentStage++;
        if (currentStage < 3) {
            levelName = stageOrder[currentStage];
            setBackground(getBackgroundColor(levelName));
            
            if (levelName.equals("Street Side")) {
                setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 1 Background.png"));
            } 
            else if (levelName.equals("Subway")) {
                setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 2 Background.png"));
            }
            else if (levelName.equals("Roof Top")) {
                setBackgroundImage(getResourcePath("Image Files" + File.separator + "Stage 3 Background.png"));
            }
            else {
                backgroundImage = null;
            }
            
            gameState = "PLAYING";
            restartTimer = 0;
            startNewMatch();
        }
    }

    private Color getBackgroundColor(String levelName) {
        switch (levelName) {
            case "joe": return Color.RED;
            case "Roof": return Color.BLUE;
            default: return new Color(173, 216, 230); // Light blue for Street Side
        }
    }

    public void setFrame(JFrame f) {
        this.frame = f;
    }

    public void setBackgroundImage(String imagePath) {
        try {
            backgroundImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Could not load background image at " + imagePath + ". Defaulting to light blue.");
            backgroundImage = null;
        }
    }

    public void loadSounds() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "Jump_Sound.wav")));
            jumpClip = AudioSystem.getClip();
            jumpClip.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Could not load jump sound.");
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "PunchKick_Sound.wav")));
            punchClip = AudioSystem.getClip();
            punchClip.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Could not load punch sound.");
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "PunchKick_Sound.wav")));
            kickClip = AudioSystem.getClip();
            kickClip.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Could not load kick sound.");
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "Menu_Theme.wav")));
            menuMusicClip = AudioSystem.getClip();
            menuMusicClip.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Could not load menu music.");
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "Round_Theme.wav")));
            levelMusicClip = AudioSystem.getClip();
            levelMusicClip.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Could not load level music.");
        }
    }
    

    public static void playJumpSound() {
        if (jumpClip != null) {
            jumpClip.setFramePosition(0);
            jumpClip.start();
        }
    }

    public static void playPunchSound() {
        if (punchClip != null) {
            punchClip.setFramePosition(0);
            punchClip.start();
        }
    }

    public static void playKickSound() {
        if (kickClip != null) {
            kickClip.setFramePosition(0);
            kickClip.start();
        }
    }

    public static void playMenuMusic() {
        stopAllMusic();
        if (menuMusicClip != null) {
            menuMusicClip.setFramePosition(0);
            menuMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void playLevelMusic() {
        stopAllMusic();
        if (levelMusicClip != null) {
            levelMusicClip.setFramePosition(0);
            levelMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stopAllMusic() {
        if (menuMusicClip != null && menuMusicClip.isRunning()) {
            menuMusicClip.stop();
        }
        if (levelMusicClip != null && levelMusicClip.isRunning()) {
            levelMusicClip.stop();
        }
    }


    public void startNewMatch() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int groundHeight = screenSize.height / 8;
        int groundY;
        if (levelName.equals("Street Side")) {
            groundY = screenSize.height - groundHeight;
        } else if (levelName.equals("Subway")) {
            groundY = screenSize.height - groundHeight - 100; // Higher ground
        } else { // Roof Top
            groundY = screenSize.height - groundHeight + 50; // Lower ground
        }
        currentLevel = new Level(levelName, groundY);
        powerUps = new ArrayList<>();

        int playerWidth = 100;
        
        // Use the character selections for the players
        player1 = new Player(100, 6, 100, currentLevel.getGroundLevelY() - 200, playerWidth, 200, p1CharacterSelection, true);
        
        int player2StartX = screenSize.width - 100 - playerWidth;
        player2 = new Player(100, 6, player2StartX, currentLevel.getGroundLevelY() - 200, playerWidth, 200, p2CharacterSelection, false);
        
        gameState = "PLAYING";
        restartTimer = 0;
        timer = 60; 
        frameCount = 0; 
    }

    public void startGame() {
        playLevelMusic();
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (isRunning) { // Kills the thread safely when isRunning is false
            update();
            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void handleWin(int winnerId) {
        if (isTournament) {
            playerWins[winnerId - 1]++;
            
            if (playerWins[winnerId - 1] >= 2) {
                gameState = (winnerId == 1) ? "P1_GRAND_WIN" : "P2_GRAND_WIN";
            } else {
                gameState = (winnerId == 1) ? "P1_WINS" : "P2_WINS";
            }
        } else {
            gameState = (winnerId == 1) ? "P1_WINS" : "P2_WINS";
        }
        restartTimer = 0;
    }

    public void update() {
        if (isPaused) {
            return;
        }

        if (!gameState.equals("PLAYING")) {
            restartTimer++;

            if (gameState.equals("P1_GRAND_WIN") || gameState.equals("P2_GRAND_WIN")) {
                if (restartTimer > 240) {
                    isRunning = false; // Kill the background thread
                    frame.dispose(); // Close game window
                    createMenu().setVisible(true); // Return to menu
                }
                return;
            }

            if (restartTimer > 180) {
                if (isTournament && (gameState.equals("P1_WINS") || gameState.equals("P2_WINS"))) {
                    nextStage();
                } else {
                    startNewMatch();
                }
            }
            return;
        }

        frameCount++;
        if (frameCount >= 60) {
            timer--;
            frameCount = 0;
        }

         // Randomly spawn power-ups
        if (Math.random() < 0.008 && powerUps.size() < 4) { // 0.8% chance per frame, max 4 active
            int randomX = (int)(Math.random() * (getWidth() - 100) + 50);
            int randomType = (int)(Math.random() * 3); // 0 = damage, 1 = speed, 2 = health
            powerUps.add(new PowerUp(randomX, getHeight() / 4, randomType));
        }
        
        // Extra healing powerup spawn chance (more frequent heals)
        if (Math.random() < 0.003 && powerUps.size() < 4) { // Additional 0.3% for healing only
            int randomX = (int)(Math.random() * (getWidth() - 100) + 50);
            powerUps.add(new PowerUp(randomX, getHeight() / 4, 2)); // Type 2 = healing
        }

        int floorY = getHeight() - getHeight() / 8;

        // Check collisions and update power-ups
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            
            // ADD THIS LINE: Apply gravity so they fall to the ground
            powerUp.update(floorY, getWidth()); 
            
            // Check collision with player 1
            if (powerUp.checkCollision(player1.getXPosition(), player1.getYPosition(), player1.getWidth(), player1.getHeight())) {
                player1.collectPowerUp(powerUp.getType());
                powerUps.remove(i);
                continue;
            }
            
            // Check collision with player 2
            if (powerUp.checkCollision(player2.getXPosition(), player2.getYPosition(), player2.getWidth(), player2.getHeight())) {
                player2.collectPowerUp(powerUp.getType());
                powerUps.remove(i);
            }
        }

    
        if (timer <= 0) {
            if (player1.getHealth() > player2.getHealth()) {
                handleWin(1);
            } else if (player2.getHealth() > player1.getHealth()) {
                handleWin(2);
            } else {
                gameState = "DRAW"; // Draws don't award points
                restartTimer = 0;
            }
        }

        // --- Player 1 ---
        if (aPressed) player1.move(-player1.getSpeed(), 0);
        else if (dPressed) player1.move(player1.getSpeed(), 0);
        if (wPressed) player1.jump();
        if (sPressed) player1.crouch();
        else player1.stopCrouch();

        // --- Player 2 ---
        if (leftPressed) player2.move(-player2.getSpeed(), 0);
        else if (rightPressed) player2.move(player2.getSpeed(), 0);
        if (upPressed) player2.jump();
        if (downPressed) player2.crouch();
        else player2.stopCrouch();

        player1.updatePhysics(floorY, getWidth());
        player2.updatePhysics(floorY, getWidth());

        if (player1.getXPosition() < player2.getXPosition()) {
            player1.setFacingRight(true);
            player2.setFacingRight(false);
        } else {
            player1.setFacingRight(false);
            player2.setFacingRight(true);
        }

        player1.updateAttackTimer();
        player2.updateAttackTimer();

        checkWinCondition();
    }

    public void checkWinCondition() {
        if (timer <= 0) return; 

        if (!player1.isAlive()) {
            handleWin(2); // Player 2 wins by KO
        } else if (!player2.isAlive()) {
            handleWin(1); // Player 1 wins by KO
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Background
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        }

        // 2. Draw Ground
        g.setColor(new Color(50, 50, 50));
        int groundHeight = height / 8;
        int groundY = height - groundHeight;
        g.fillRect(0, groundY, width, groundHeight);

          // 2.5 Draw Power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g);
        }

        // 3. Fighters
        player1.draw(g);
        player2.draw(g);

        // 4. Draw UI Elements
        int uiPadding = Math.max(20, width / 50);
        int barWidth = width / 3;
        int barHeight = Math.max(24, height / 30);
        int barY = Math.max(30, height / 20);
        int leftBarX = uiPadding;
        int rightBarX = width - uiPadding - barWidth;

        g.setColor(Color.RED);
        g.fillRect(leftBarX, barY, barWidth, barHeight);
        g.fillRect(rightBarX, barY, barWidth, barHeight);

        int player1HealthWidth = Math.max(0, Math.min(barWidth, player1.getHealth() * barWidth / 100));
        int player2HealthWidth = Math.max(0, Math.min(barWidth, player2.getHealth() * barWidth / 100));

        g.setColor(Color.GREEN);
        g.fillRect(leftBarX, barY, player1HealthWidth, barHeight);
        g.fillRect(rightBarX + (barWidth - player2HealthWidth), barY, player2HealthWidth, barHeight);

        g.setFont(new Font("Arial", Font.BOLD, Math.max(30, width / 60)));
        g.setColor(Color.WHITE);
        g.drawString("Player 1", leftBarX, barY - 6);
        g.drawString("Player 2", rightBarX + barWidth - g.getFontMetrics().stringWidth("Player 2"), barY - 6);

        g.setFont(new Font("Arial", Font.BOLD, Math.max(48, width / 20)));
        String timeText = String.format("%02d", Math.max(0, timer)); 
        FontMetrics metrics = g.getFontMetrics();
        int timeX = (width - metrics.stringWidth(timeText)) / 2;
        int timeY = barY + barHeight + Math.max(40, height / 30);

        g.setColor(Color.BLACK);
        g.drawString(timeText, timeX + 3, timeY + 3);
        g.setColor(Color.YELLOW);
        g.drawString(timeText, timeX, timeY);

         // Draw Pause Button (top right corner)
        int pauseButtonWidth = (int) Math.round(65 * 1.75);
        int pauseButtonHeight = (int) Math.round(22 * 1.75);
        int pauseButtonX = width - pauseButtonWidth - 10;
        int pauseButtonY = (int) (height * 0.10);

        g.setColor(Color.YELLOW);
        g.fillRect(pauseButtonX, pauseButtonY, pauseButtonWidth, pauseButtonHeight);
        g.setColor(Color.BLACK);
        g.drawRect(pauseButtonX, pauseButtonY, pauseButtonWidth, pauseButtonHeight);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        String pauseButtonText = "PAUSE (P)";
        int pauseTextX = pauseButtonX + (pauseButtonWidth - g.getFontMetrics().stringWidth(pauseButtonText)) / 2;
        int pauseTextY = pauseButtonY + pauseButtonHeight / 2 + g.getFontMetrics().getAscent() / 2 - 2;
        g.drawString(pauseButtonText, pauseTextX, pauseTextY);

        // Display Pause Menu
        if (isPaused) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(new Color(0, 0, 0, 200)); // Semi-transparent black
            g.fillRect(0, 0, width, height);
            
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(Color.WHITE);
            String pauseMsg = "PAUSED";
            int pauseX = (width - g.getFontMetrics().stringWidth(pauseMsg)) / 2;
            g.drawString(pauseMsg, pauseX, height / 2 - 50);

            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.YELLOW);
            String resumeMsg = "Press P to Resume";
            int resumeX = (width - g.getFontMetrics().stringWidth(resumeMsg)) / 2;
            g.drawString(resumeMsg, resumeX, height / 2 + 20);

            String resetMsg = "Press R to Reset";
            int resetX = (width - g.getFontMetrics().stringWidth(resetMsg)) / 2;
            g.drawString(resetMsg, resetX, height / 2 + 60);

            String menuMsg = "Press B to Main Menu";
            int menuX = (width - g.getFontMetrics().stringWidth(menuMsg)) / 2;
            g.drawString(menuMsg, menuX, height / 2 + 100);
            return;
        }

        if (!gameState.equals("PLAYING")) {
            g.setFont(new Font("Arial", Font.BOLD, Math.max(64, width / 25)));
            g.setColor(Color.YELLOW);
            metrics = g.getFontMetrics();
            
            String msg = "DRAW";
            if (gameState.equals("P1_WINS")) msg = "Player 1 Wins!";
            if (gameState.equals("P2_WINS")) msg = "Player 2 Wins!";
            if (gameState.equals("P1_GRAND_WIN")) msg = "Player 1 is the Grand Winner!";
            if (gameState.equals("P2_GRAND_WIN")) msg = "Player 2 is the Grand Winner!";
            
            int x = (width - metrics.stringWidth(msg)) / 2;
            int y = height / 2;
            
            g.setColor(Color.BLACK);
            g.drawString(msg, x + 3, y + 3);
            
            g.setColor(Color.YELLOW);
            g.drawString(msg, x, y);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) aPressed = true;
        if (key == KeyEvent.VK_D) dPressed = true;
        if (key == KeyEvent.VK_W) wPressed = true;
        if (key == KeyEvent.VK_S) sPressed = true;
        if (key == KeyEvent.VK_F) player1.attack(player2, "Punch");
        if (key == KeyEvent.VK_G) player1.attack(player2, "Kick");

        if (key == KeyEvent.VK_P && gameState.equals("PLAYING")) {
            isPaused = !isPaused;
            return;
        }
        if (key == KeyEvent.VK_R && isPaused) {
            startNewMatch();
            isPaused = false;
            return;
        }
        if (key == KeyEvent.VK_B && isPaused) {
            isRunning = false; // Kill background thread safely from Pause screen
            frame.dispose();
            createMenu().setVisible(true);
            return;
        }

        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        if (key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_DOWN) downPressed = true;
        if (key == KeyEvent.VK_ENTER) player2.attack(player1, "Punch");
        if (key == KeyEvent.VK_K) player2.attack(player1, "Kick");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) aPressed = false;
        if (key == KeyEvent.VK_D) dPressed = false;
        if (key == KeyEvent.VK_W) wPressed = false;
        if (key == KeyEvent.VK_S) sPressed = false;

        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_DOWN) downPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static JFrame createMenu() {
        loadMenuSounds();
        playMenuMusic();
        JFrame menuFrame = new JFrame("Runtime Rumble - Main Menu");

        // 1. Create a custom background panel
        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            private Image menuBg;
            {
                try {
                    menuBg = ImageIO.read(new File(getResourcePath("Image Files" + File.separator + "Main Menu Screen.png")));
                } catch (IOException e) {
                    System.err.println("Could not load menu background image. Defaulting to plain background.");
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (menuBg != null) {
                    g.drawImage(menuBg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        menuFrame.setContentPane(backgroundPanel);

        // 2. Center Panel for buttons (Must be transparent)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false); 
        centerPanel.add(Box.createVerticalStrut(200)); 

        JLabel selectLabel = new JLabel("Select Stage:");
        selectLabel.setFont(new Font("Arial", Font.BOLD, 40));
        selectLabel.setForeground(Color.WHITE); 
        selectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(selectLabel);
        
        centerPanel.add(Box.createVerticalStrut(30)); 

        // 3. Stage Radio Buttons 
        ButtonGroup group = new ButtonGroup();
        JRadioButton dojoBtn = new JRadioButton("Street Side", true);
        JRadioButton redBtn = new JRadioButton("Subway");
        JRadioButton blueBtn = new JRadioButton("Roof Top");
        JRadioButton bestOfThreeBtn = new JRadioButton("Best of Three");

        JRadioButton[] buttons = {dojoBtn, redBtn, blueBtn, bestOfThreeBtn};
        for (JRadioButton btn : buttons) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setOpaque(false); 
            btn.setForeground(Color.WHITE); 
            btn.setFont(new Font("Arial", Font.BOLD, 30));
            group.add(btn);
            centerPanel.add(btn);
        }

        centerPanel.add(Box.createVerticalStrut(30)); 

        // 4. Character Selection UI
        JPanel charSelectPanel = new JPanel(new FlowLayout());
        charSelectPanel.setOpaque(false);
        
        String[] characters = {"Blue", "Red", "Pink", "Purple"};
        JComboBox<String> p1Dropdown = new JComboBox<>(characters);
        JComboBox<String> p2Dropdown = new JComboBox<>(characters);
        p1Dropdown.setSelectedIndex(0); // Default Blue
        p2Dropdown.setSelectedIndex(1); // Default Red

        JLabel p1Label = new JLabel("P1 Fighter: ");
        p1Label.setForeground(Color.WHITE);
        p1Label.setFont(new Font("Arial", Font.BOLD, 20));
        JLabel p2Label = new JLabel("  P2 Fighter: ");
        p2Label.setForeground(Color.WHITE);
        p2Label.setFont(new Font("Arial", Font.BOLD, 20));

        charSelectPanel.add(p1Label);
        charSelectPanel.add(p1Dropdown);
        charSelectPanel.add(p2Label);
        charSelectPanel.add(p2Dropdown);
        centerPanel.add(charSelectPanel);
        
        centerPanel.add(Box.createVerticalStrut(30)); 

        // 5. Start Button
        JButton startButton = new JButton("Start Match");
        startButton.setFont(new Font("Arial", Font.BOLD, 35));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> {
            String selectedLevel;
            if (dojoBtn.isSelected()) selectedLevel = "Street Side";
            else if (redBtn.isSelected()) selectedLevel = "Subway";
            else if (blueBtn.isSelected()) selectedLevel = "Roof Top";
            else selectedLevel = "Best of Three";
            
            String p1Char = (String) p1Dropdown.getSelectedItem();
            String p2Char = (String) p2Dropdown.getSelectedItem();
            
            stopAllMusic(); // Stop menu music before starting game
            menuFrame.dispose();
            
            // Start the game
            JFrame frame = new JFrame("Java Street Fighter");
            Game game = new Game(selectedLevel, p1Char, p2Char);
            game.setFrame(frame);
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.startGame();
        });
        centerPanel.add(startButton);

        backgroundPanel.add(centerPanel, BorderLayout.CENTER);

        menuFrame.setSize(600, 600); 
        menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menuFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        menuFrame.setLocationRelativeTo(null);
        return menuFrame;
    }

     public static void loadMenuSounds() {
        if (menuMusicClip == null) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(getResourcePath("Sound Files" + File.separator + "Menu_Theme.wav")));
                menuMusicClip = AudioSystem.getClip();
                menuMusicClip.open(audioInputStream);
            } catch (Exception e) {
                System.err.println("Could not load menu music.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createMenu().setVisible(true);
        });
    }
}

// ==========================================
// REQUIRED HELPER CLASSES
// ==========================================

class Player {
    private int health;
    private int speed;
    private int xPosition;
    private int yPosition;
    private int width;
    private int height;
    private String characterName; 
    private boolean isFacingRight; 
    private boolean isBlocking;

    private int velocityY = 0;
    private boolean isJumping = false;
    private boolean isAttacking = false;
    private String currentAttack = ""; // Tracks if we are punching or kicking
    private int attackTimer = 0;
    private int kickCooldown = 0;
    private final int JUMP_STRENGTH = -16;
    private final int GRAVITY = 1;

    // Power-up effects
    private double damageMultiplier = 1.0;
    private double speedMultiplier = 1.0;
    private int damageBoostTimer = 0;
    private int speedBoostTimer = 0;
    private final int POWERUP_DURATION = 300; // ~5 seconds at 60 FPS

    // Sprite Images
    private Image standImg, punchImg, kickImg, crouchImg;

    public Player(int health, int speed, int x, int y, int w, int h, String characterName, boolean facingRight) {
        this.health = health;
        this.speed = speed;
        this.xPosition = x;
        this.yPosition = y;
        this.width = w;
        this.height = h;
        this.characterName = characterName;
        this.isFacingRight = facingRight;
        loadSprites();
    }

    private void loadSprites() {
        try {
            String basePath = Game.getResourcePath("Image Files" + File.separator + characterName);
            standImg = ImageIO.read(new File(basePath + "_Stand.png"));
            punchImg = ImageIO.read(new File(basePath + "_Punch.png"));
            kickImg = ImageIO.read(new File(basePath + "_Kick.png"));
            crouchImg = ImageIO.read(new File(basePath + "_Crouch.png"));
        } catch (IOException e) {
            System.err.println("Could not load sprites for " + characterName + ". Make sure the files are named correctly in the Sprites folder.");
        }
    }

    public void move(int deltaX, int deltaY) {
        if (!isBlocking) {
            xPosition += deltaX;
            yPosition += deltaY;
        }
    }

    public void jump() {
        if (!isJumping && !isBlocking) {
            velocityY = JUMP_STRENGTH;
            isJumping = true;
            Game.playJumpSound();
        }
    }

    public void crouch() {
        isBlocking = true;
    }

    public void stopCrouch() {
        isBlocking = false;
    }

    public void attack(Player opponent, String attackType) {
        if (isAttacking || isBlocking || !isAlive()) return;
        if (attackType.equals("Kick") && kickCooldown > 0) return;
        
        isAttacking = true;
        currentAttack = attackType; // Store the attack type for drawing the right sprite
        attackTimer = 12;

        if (attackType.equals("Punch")) Game.playPunchSound();
        else if (attackType.equals("Kick")) Game.playKickSound();

        int attackRange = attackType.equals("Kick") ? 140 : 70;
        int verticalRange = 80;

        if (attackType.equals("Kick")) {
            kickCooldown = 120; // two seconds at ~60 FPS
        }

        if (Math.abs(this.xPosition - opponent.getXPosition()) < attackRange && 
            Math.abs(this.yPosition - opponent.getYPosition()) < verticalRange) {
            
            int damage;
            if (attackType.equals("Punch")) damage = 10;
            else if (attackType.equals("Kick")) damage = 15;
            else damage = 0;

            // Apply damage multiplier
            damage = (int)(damage * damageMultiplier);

            opponent.takeDamage(damage);
            opponent.move(this.isFacingRight ? 30 : -30, 0);
        }
    }

    public void takeDamage(int damageAmount) {
        if (isBlocking) {
            health -= (damageAmount / 2); 
        } else {
            health -= damageAmount;
        }
    }

    public boolean isAlive() { 
        return health > 0; 
    }

    public void updatePhysics(int floorY, int screenWidth) {
        yPosition += velocityY;
        if (yPosition + height < floorY) {
            velocityY += GRAVITY;
        } else {
            yPosition = floorY - height;
            velocityY = 0;
            isJumping = false;
        }

        if (xPosition < 0) xPosition = 0;
        if (xPosition > screenWidth - width) xPosition = screenWidth - width;
    }

    public void updateAttackTimer() {
        if (attackTimer > 0) {
            attackTimer--;
        } else {
            isAttacking = false;
            currentAttack = ""; // Reset attack state
        }

        if (kickCooldown > 0) kickCooldown--;

        // Update power-up timers
        if (damageBoostTimer > 0) {
            damageBoostTimer--;
        } else {
            damageMultiplier = 1.0;
        }

        if (speedBoostTimer > 0) {
            speedBoostTimer--;
        } else {
            speedMultiplier = 1.0;
        }
    }

    public void draw(Graphics g) {
        Image imgToDraw = standImg;

        // Determine which sprite to use based on the current state
        if (isBlocking) {
            imgToDraw = crouchImg;
        } else if (isAttacking) {
            if (currentAttack.equals("Punch")) imgToDraw = punchImg;
            else if (currentAttack.equals("Kick")) imgToDraw = kickImg;
        }

        if (imgToDraw != null) {
            if (isFacingRight) {
                // Draw normally
                g.drawImage(imgToDraw, xPosition, yPosition, width, height, null);
            } else {
                // Flip horizontally using negative width
                g.drawImage(imgToDraw, xPosition + width, yPosition, -width, height, null);
            }
        } else {
            // Fallback to rectangles if images are missing
            g.setColor(characterName.equals("Red") ? Color.RED : Color.BLUE);
            if (isBlocking) g.fillRect(xPosition, yPosition + 20, width, height - 20);
            else g.fillRect(xPosition, yPosition, width, height);
        }

        // Draw power-up indicators
        if (damageBoostTimer > 0) {
            g.setColor(Color.RED);
            g.fillRect(xPosition + 5, yPosition - 20, 20, 15);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("DMG", xPosition + 6, yPosition - 8);
        }

        if (speedBoostTimer > 0) {
            g.setColor(Color.CYAN);
            g.fillRect(xPosition + width - 25, yPosition - 20, 20, 15);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("SPD", xPosition + width - 24, yPosition - 8);
        }
    }

    public int getHealth() { return health; }
    public int getSpeed() { return (int)(speed * speedMultiplier); }
    public int getXPosition() { return xPosition; }
    public int getYPosition() { return yPosition; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setFacingRight(boolean b) { isFacingRight = b; }

    public void collectPowerUp(int type) {
        if (type == 0) { // Damage boost
            damageMultiplier = 1.4; // 40% damage boost - balanced
            damageBoostTimer = POWERUP_DURATION;
        } else if (type == 1) { // Speed boost
            speedMultiplier = 1.6; // 60% speed boost - balanced
            speedBoostTimer = POWERUP_DURATION;
        } else if (type == 2) { // Healing powerup
            health = Math.min(100, health + 35); // Heal 35 HP (max 100)
        }
    }
}

class Level {
    private String levelName;
    private int groundLevelY;

    public Level(String levelName, int groundLevelY) {
        this.levelName = levelName;
        this.groundLevelY = groundLevelY;
    }

    public int getGroundLevelY() {
        return groundLevelY;
    }
}

class PowerUp {
    private int type; // 0 = damage boost, 1 = speed boost
    private int xPosition;
    private int yPosition;
    private int width = 30;
    private int height = 30;
    private double velocityY = 0;
    private final double GRAVITY = 0.5;
    private boolean isOnGround = false;

    public PowerUp(int x, int y, int type) {
        this.xPosition = x;
        this.yPosition = y;
        this.type = type;
        this.velocityY = 0;
    }

    public void update(int groundLevelY, int screenWidth) {
        // Apply gravity
        if (!isOnGround) {
            velocityY += GRAVITY;
            yPosition += (int)velocityY;
        }

        // Check if hit ground
        if (yPosition + height >= groundLevelY) {
            yPosition = groundLevelY - height;
            velocityY = 0;
            isOnGround = true;
        }

        // Keep within screen bounds horizontally
        if (xPosition < 0) xPosition = 0;
        if (xPosition + width > screenWidth) xPosition = screenWidth - width;
    }

    public boolean checkCollision(int playerX, int playerY, int playerWidth, int playerHeight) {
        return xPosition < playerX + playerWidth &&
               xPosition + width > playerX &&
               yPosition < playerY + playerHeight &&
               yPosition + height > playerY;
    }

    public int getType() {
        return type;
    }

    public int getYPosition() {
        return yPosition;
    }

    public void draw(Graphics g) {
        if (type == 0) { // Damage boost - red
            g.setColor(Color.RED);
            g.fillOval(xPosition, yPosition, width, height);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("1.4X", xPosition + 2, yPosition + 22);
        } else if (type == 1) { // Speed boost - cyan
            g.setColor(Color.CYAN);
            g.fillOval(xPosition, yPosition, width, height);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("1.6X", xPosition + 2, yPosition + 22);
        } else if (type == 2) { // Health boost - green
            g.setColor(Color.GREEN);
            g.fillOval(xPosition, yPosition, width, height);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("+35", xPosition + 2, yPosition + 22);
        }
        
        // Draw border
        g.setColor(Color.WHITE);
        g.drawOval(xPosition, yPosition, width, height);
        g.drawOval(xPosition + 1, yPosition + 1, width - 2, height - 2);
    }
}

class Item {
    private String itemType; 
    private int xPosition;
    private int yPosition;
    private boolean isCollected;

    public void spawn(int spawnX, int spawnY) {
        this.xPosition = spawnX;
        this.yPosition = spawnY;
        this.isCollected = false;
    }
}
