import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.BufferedImage;
import javax.swing.*;

class Game extends JPanel implements Runnable, KeyListener {
    // Window constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;

    // Game state tracking
    private Thread gameThread;
    private String gameState = "PLAYING"; 
    private int selectingPlayer = 1;
    private int p1Selectionn = 0;
    private int p2Selection = 1;
    private int timer = 60; // 60 seconds (requires a separate tick to decrement accurately, simplified here)
    private int maxRounds = 3;
    private int p1RoundsWon = 0;
    private int p2RoundsWon = 0;
    private int restartTimer = 0;

    // Objects
    private Player player1;
    private Player player2;
    private Level currentLevel;
    // Item itemDrop; // Ready for future implementation

    // Keyboard state tracking
    private boolean wPressed, aPressed, sPressed, dPressed;
    private boolean upPressed, leftPressed, downPressed, rightPressed;

    private String[] avatarNames = {"Blue Male", "Red Male", "Purple Female", "Pink Female"};
    private String[] avatarFiles = {"char_blue_m.png", "char_red_m.png", "char_purp_f.png", "char_pink_f.png"};

    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        addKeyListener(this);

        startNewMatch();
    }

    public void startNewMatch(int p1CharIndex, int p2CharIndex) {
        // Initialize level
        currentLevel = new Level("Dojo", 250);

        // Initialize players (health, speed, x, y, width, height, sprite(color), facingRight)
        player1 = new Player(100, 6, 100, currentLevel.getGroundLevelY(), 50, 100, avatarFiles[p1CharIndex], true);
        player2 = new Player(100, 6, 650, currentLevel.getGroundLevelY(), 50, 100, avatarFiles[p2CharIndex], false);
        
        gameState = "PLAYING";
        setBackground(Color.DARK_GRAY);
        restartTimer = 0;
    }

    public void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        // Main game loop (runs at roughly 60 Frames Per Second)
        while (true) {
            update();
            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (!gameState.equals("PLAYING")) {
            restartTimer++;
            if (restartTimer > 180) {
                startNewMatch();
            }
            return;
        }

        // Player 1 movement
        if (aPressed) player1.move(-player1.getSpeed(), 0);
        else if (dPressed) player1.move(player1.getSpeed(), 0);
        if (wPressed) player1.jump();
        if (sPressed) player1.crouch();
        else player1.stopCrouch();

        // Player 2 movement
        if (leftPressed) player2.move(-player2.getSpeed(), 0);
        else if (rightPressed) player2.move(player2.getSpeed(), 0);
        if (upPressed) player2.jump();
        if (downPressed) player2.crouch();
        else player2.stopCrouch();

        // Apply physics
        player1.updatePhysics(currentLevel.getGroundLevelY(), WIDTH);
        player2.updatePhysics(currentLevel.getGroundLevelY(), WIDTH);

        // Auto-face opponents
        if (player1.getXPosition() < player2.getXPosition()) {
            player1.setFacingRight(true);
            player2.setFacingRight(false);
        } else {
            player1.setFacingRight(false);
            player2.setFacingRight(true);
        }

        // Update attack animation cooldowns
        player1.updateAttackTimer();
        player2.updateAttackTimer();

        checkWinCondition();
    }

    public void checkWinCondition() {
        if (!player1.isAlive()) {
            gameState = "P2_WINS";
            restartTimer = 0;
        }
        if (!player2.isAlive()) {
            gameState = "P1_WINS";
            restartTimer = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (gameState.equals("PLAYING") || gameState.equals("CHAR_SELECT")) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Fonr.Bold, 36));
            FontMetrics fm = g2.getFontMetrics();

            String title = "CHARACTER SELECT";
            g2.drawString(title, WIDTH - fm.stringWidth(title))/2, 50);

            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            String prompt = (selectingPlayer == 1) ? "Player 1: Choose Avatar (A/D, F)" : "Player2: Choose Avatar (Left/Right, Enter)";
            g2.drawString(prompt, WIDTH - g2.getFontMetric().stringWidth(prompt))/ 2, 90);

            int boxWidth = 150;
            int boxHeight = 200;
            int gap = 30;
            int startX = (WIDTH - (4* boxWidth + 3 * gap))/2;
            int y = 120;

            for (int i = 0; i < 4; i++) {
                int x = startX + (i * (boxWidth + gap));

                if (selectingPlayer == 1 && i == p1Selection) g2.setColor(Color.RED);
                else if (selectingPlayer == 2 && p2Selection) g2.setColor(Color.Blue);
                else g2.setColor(Color.GRAY);

                g2.fillRect(x, y, boxWidth, boxHeight);
                g2.SetColor(Color.BLACK);
                g2.drawRect(x, y, boxWidth, boxHeight);

                g2.setColor(Color.WHITE);
                G2.setFont(new Font("Arial", Font.BOLD, 14));
                String[] desc = getAvatarDescription(i);
                for (int j = 0; j < desc.length; j++) {
                    g2.drawString(desc[j], x + 10, y + 30 + (j* 20));
                }
            }
            return;
        }
                

        // Draw the ground from the Level class
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, currentLevel.getGroundLevelY() + 100, WIDTH, HEIGHT - currentLevel.getGroundLevelY());

        // Draw the fighters
        player1.draw(g);
        player2.draw(g);

        // Draw Health Bar Backgrounds (Red)
        g.setColor(Color.RED);
        g.fillRect(50, 20, 300, 20);
        g.fillRect(450, 20, 300, 20);

        // Draw Health Bar Foreground (Green)
        g.setColor(Color.GREEN);
        g.fillRect(50, 20, Math.max(0, player1.getHealth()) * 3, 20);
        g.fillRect(450 + (100 - Math.max(0, player2.getHealth())) * 3, 20, Math.max(0, player2.getHealth()) * 3, 20);

        // Draw Player Labels
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("Player 1", 50, 15);
        g.drawString("Player 2", 700, 15);

        // Display Game Over Text
        if (!gameState.equals("PLAYING")) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(Color.YELLOW);
            FontMetrics metrics = g.getFontMetrics();
            String msg = gameState.equals("P1_WINS") ? "Player 1 Wins!" : "Player 2 Wins!";
            int x = (WIDTH - metrics.stringWidth(msg)) / 2;
            g.drawString(msg, x, HEIGHT / 2);
        }
    }
// Helper for descriptions
    private String[] getAvatarDescription(int index) {
        swithc (index) {
            case 0: return new String[]{"Male", "Dark Hair", "Blue Shorts", "Blue Gloves"};
            case 1: return new String[]{"Male", "Blond Hair", "Red Shorts", "Red Gloves"};
            case 2: return new String[]{"Female", "Dark Hair", "Purple Shirt/Shorts", "Purple Gloves"};
            case 3: return new String[]{"Female", "Blond Hair", "Pink Shirt/Shorts", "Pink Gloves"};
            default: return new String[]{"Unkown"};
        }

            
    // --- Keyboard Controls ---
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        //character selection
        if (gameState.equals("PLAYING") || gameState.equals("CHAR_SELECT")) {
            if (selectingPlayer == 1) {
                if (key == KeyEvent.VK_A) p1Selection = (p1Selection > 0) ? p1Selection -1 : 3;
                if (key == KeyEvent.VK_D) p1Selection = (p1Selection < 3) ? p1Selection +1 : 0;
                if (key == KeyEvent.VK_F) {
                    selectingPlayer = 2;
                }
            }
            else if (selectingPlayer == 2) {
                if(key == KeyEvent.VK_LEFT) p2Selection = (p2Selection > 0) ? p2Selection - 1 : 3;
                if (key == KeyEvent.VK_RIGHT) p2Selection = (p2Selection < 3) ? p2Selection + 1 : 0;
                if (key == KeyEvent.VK_ENTER) {
                    startNewMatch(p1Selection, p2Selection);
                }
            }
            return;
        }

        // P1 Controls
        if (key == KeyEvent.VK_A) aPressed = true;
        if (key == KeyEvent.VK_D) dPressed = true;
        if (key == KeyEvent.VK_W) wPressed = true;
        if (key == KeyEvent.VK_S) sPressed = true;
        if (key == KeyEvent.VK_F) player1.attack(player2, "Punch");

        // P2 Controls
        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        if (key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_DOWN) downPressed = true;
        if (key == KeyEvent.VK_ENTER) player2.attack(player1, "Punch");
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Street Fighter");
        Game game = new Game();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
        game.startGame();
    }
}

// ==========================================
// NEW CLASSES MAPPED TO YOUR SPECIFICATIONS
// ==========================================

class Player {
    private int health;
    private int speed;
    private int xPosition;
    private int yPosition;
    private int width;
    private int height;
    private String spritePath; 
    private boolean isFacingRight; 
    private boolean isBlocking;

    // Physics specific fields
    private int velocityY = 0;
    private boolean isJumping = false;
    private boolean isAttacking = false;
    private int attackTimer = 0;
    private final int JUMP_STRENGTH = -16;
    private final int GRAVITY = 1;

    public Player(int health, int speed, int x, int y, int w, int h, String spritePath, boolean facingRight) {
        this.health = health;
        this.speed = speed;
        this.xPosition = x;
        this.yPosition = y;
        this.width = w;
        this.height = h;
        this.spritePath = spritePath;
        this.isFacingRight = facingRight;
    }

    public void move(int deltaX, int deltaY) {
        if (!isBlocking) { // Cannot move while blocking
            xPosition += deltaX;
            yPosition += deltaY;
        }
    }

    public void jump() {
        if (!isJumping && !isBlocking) {
            velocityY = JUMP_STRENGTH;
            isJumping = true;
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
        
        isAttacking = true;
        attackTimer = 12;

        int attackRange = 70;
        int verticalRange = 80;

        if (Math.abs(this.xPosition - opponent.getXPosition()) < attackRange && 
            Math.abs(this.yPosition - opponent.getYPosition()) < verticalRange) {
            
            // Apply damage based on attack type
            int damage = attackType.equals("Punch") ? 10 : 5;
            opponent.takeDamage(damage);

            // Knockback
            opponent.move(this.isFacingRight ? 30 : -30, 0);
        }
    }

    public void takeDamage(int damageAmount) {
        if (isBlocking) {
            health -= (damageAmount / 2); // Block mitigates half damage
        } else {
            health -= damageAmount;
        }
    }

    public void collectItem(Item item) {
        item.applyEffect(this);
    }

    public boolean isAlive() { 
        return health > 0; 
    }

    public void updatePhysics(int floorY, int screenWidth) {
        yPosition += velocityY;
        if (yPosition < floorY) {
            velocityY += GRAVITY;
        } else {
            yPosition = floorY;
            velocityY = 0;
            isJumping = false;
        }

        if (xPosition < 0) xPosition = 0;
        if (xPosition > screenWidth - width) xPosition = screenWidth - width;
    }

    public void updateAttackTimer() {
        if (attackTimer > 0) attackTimer--;
        else isAttacking = false;
    }

    public void draw(Graphics g) {
        // Parse sprite string to color for now
        g.setColor(spritePath.equals("RED") ? Color.RED : Color.BLUE);
        
        // Visual indicator for blocking (shrinks the player slightly)
        if (isBlocking) {
            g.fillRect(xPosition, yPosition + 20, width, height - 20);
        } else {
            g.fillRect(xPosition, yPosition, width, height);
        }

        // Draw attack
        if (isAttacking) {
            g.setColor(Color.YELLOW);
            if (isFacingRight) {
                g.fillRect(xPosition + width, yPosition + 20, 35, 15);
            } else {
                g.fillRect(xPosition - 35, yPosition + 20, 35, 15);
            }
        }
    }

    // Getters and Setters
    public int getHealth() { return health; }
    public int getSpeed() { return speed; }
    public int getXPosition() { return xPosition; }
    public int getYPosition() { return yPosition; }
    public void setFacingRight(boolean b) { isFacingRight = b; }
}

class Level {
    private String levelName;
    private String backgroundMusicPath;
    private String backgroundImagePath;
    private int groundLevelY;
    private int[] platformXPositions; 
    private int[] platformYPositions;

    public Level(String levelName, int groundLevelY) {
        this.levelName = levelName;
        this.groundLevelY = groundLevelY;
    }

    public int getGroundLevelY() {
        return groundLevelY;
    }

    public void loadLevelAssets() { /* Future Implementation */ }
    public void playBackgroundMusic() { /* Future Implementation */ }
    public boolean isPlayerOnPlatform(Player player) { return false; }
    public void triggerHazard(Player player) { /* Future Implementation */ }
}

class Item {
    private String itemType; 
    private int xPosition;
    private int yPosition;
    private boolean isCollected;
    private int durationTimer; 

    public void spawn(int spawnX, int spawnY) {
        this.xPosition = spawnX;
        this.yPosition = spawnY;
        this.isCollected = false;
    }

    public void applyEffect(Player player) {
        isCollected = true;
        // Example logic: if itemType.equals("Speed"), boost player.speed
    }

    public void removeEffect(Player player) { 
        // Revert stats
    }
    
    public boolean checkCollision(Player player) { 
        return false; // Future AABB collision logic here
    }
}

    public void removeEffect(Player player) { 
        // Revert stats
    }
    
    public boolean checkCollision(Player player) { 
        return false; // Future AABB collision logic here
    }
}
