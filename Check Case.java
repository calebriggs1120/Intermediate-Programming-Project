import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

class Game extends JPanel implements Runnable, KeyListener {
    // Window constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;

    // Game state tracking
    private Thread gameThread;
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

    // Keyboard state tracking
    private boolean wPressed, aPressed, sPressed, dPressed;
    private boolean upPressed, leftPressed, downPressed, rightPressed;

    public Game() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(screenSize);
        setBackground(new Color(173, 216, 230)); 
        setFocusable(true);
        addKeyListener(this);

        setBackgroundImage("C:\\Users\\caleb\\OneDrive\\Desktop\\Video Game Test Java\\Game background.jpg");

        startNewMatch();
    }

    public void setBackgroundImage(String imagePath) {
        try {
            backgroundImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Could not load background image at " + imagePath + ". Defaulting to light blue.");
            backgroundImage = null;
        }
    }

    public void startNewMatch() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int groundHeight = screenSize.height / 4;
        currentLevel = new Level("Dojo", screenSize.height - groundHeight);

        player1 = new Player(100, 6, 100, currentLevel.getGroundLevelY() - 200, 100, 200, "RED", true);
        player2 = new Player(100, 6, 650, currentLevel.getGroundLevelY() - 200, 100, 200, "BLUE", false);
        
        gameState = "PLAYING";
        restartTimer = 0;
        timer = 60; 
        frameCount = 0; 
    }

    public void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
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

        frameCount++;
        if (frameCount >= 60) {
            timer--;
            frameCount = 0;
        }
        
        if (timer <= 0) {
            if (player1.getHealth() > player2.getHealth()) {
                gameState = "P1_WINS";
            } else if (player2.getHealth() > player1.getHealth()) {
                gameState = "P2_WINS";
            } else {
                gameState = "DRAW";
            }
            restartTimer = 0;
        }

        if (aPressed) player1.move(-player1.getSpeed(), 0);
        else if (dPressed) player1.move(player1.getSpeed(), 0);
        if (wPressed) player1.jump();
        if (sPressed) player1.crouch();
        else player1.stopCrouch();

        if (leftPressed) player2.move(-player2.getSpeed(), 0);
        else if (rightPressed) player2.move(player2.getSpeed(), 0);
        if (upPressed) player2.jump();
        if (downPressed) player2.crouch();
        else player2.stopCrouch();

        int floorY = getHeight() - getHeight() / 8;
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
            gameState = "P2_WINS";
            restartTimer = 0;
        } else if (!player2.isAlive()) {
            gameState = "P1_WINS";
            restartTimer = 0;
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

        // 3. Draw Fighters
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

        if (!gameState.equals("PLAYING")) {
            g.setFont(new Font("Arial", Font.BOLD, Math.max(64, width / 25)));
            g.setColor(Color.YELLOW);
            metrics = g.getFontMetrics();
            
            String msg = "DRAW";
            if (gameState.equals("P1_WINS")) msg = "Player 1 Wins!";
            if (gameState.equals("P2_WINS")) msg = "Player 2 Wins!";
            
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
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
        game.startGame();
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
    private String spritePath; 
    private boolean isFacingRight; 
    private boolean isBlocking;

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
        if (!isBlocking) {
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
            
            int damage = attackType.equals("Punch") ? 10 : 5;
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
        if (attackTimer > 0) attackTimer--;
        else isAttacking = false;
    }

    public void draw(Graphics g) {
        g.setColor(spritePath.equals("RED") ? Color.RED : Color.BLUE);
        
        if (isBlocking) {
            g.fillRect(xPosition, yPosition + 20, width, height - 20);
        } else {
            g.fillRect(xPosition, yPosition, width, height);
        }

        if (isAttacking) {
            g.setColor(Color.YELLOW);
            if (isFacingRight) {
                g.fillRect(xPosition + width, yPosition + 20, 35, 15);
            } else {
                g.fillRect(xPosition - 35, yPosition + 20, 35, 15);
            }
        }
    }

    public int getHealth() { return health; }
    public int getSpeed() { return speed; }
    public int getXPosition() { return xPosition; }
    public int getYPosition() { return yPosition; }
    public void setFacingRight(boolean b) { isFacingRight = b; }
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