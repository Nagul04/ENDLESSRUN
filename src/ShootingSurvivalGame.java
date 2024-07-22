import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ShootingSurvivalGame extends JFrame {

    public static final String HIGH_SCORE_FILE = "highscore.dat";

    public ShootingSurvivalGame() {
        setTitle("Shooting Survival Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        add(new GamePanel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ShootingSurvivalGame window = new ShootingSurvivalGame();
            window.setVisible(true);
        });
    }
}

class GamePanel extends JPanel implements ActionListener {

    private Timer timer;
    private Player player;
    private List<Bullet> bullets;
    private List<Enemy> enemies;
    private List<Medikit> medikits;
    private int score;
    private boolean gameOver;
    private Random rand;
    private int wave;
    private int enemySpawnDelay;
    private int enemySpawnCounter;
    private int medikitSpawnDelay;
    private int medikitSpawnCounter;
    private JLabel highScoreLabel;

    public GamePanel() {
        setFocusable(true);
        setBackground(Color.DARK_GRAY);
        setDoubleBuffered(true);

        rand = new Random();
        player = new Player();
        bullets = new ArrayList<>();
        enemies = new ArrayList<>();
        medikits = new ArrayList<>();
        score = 0;
        gameOver = false;
        wave = 1;
        enemySpawnDelay = 100;
        enemySpawnCounter = 0;
        medikitSpawnDelay = 1000;
        medikitSpawnCounter = 0;

        timer = new Timer(10, this);
        timer.start();

        highScoreLabel = new JLabel("High Score: " + loadHighScore());
        highScoreLabel.setForeground(Color.WHITE);
        highScoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        highScoreLabel.setBounds(10, 80, 200, 20);
        add(highScoreLabel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
                    resetGame();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (!gameOver) {
                        Enemy nearestEnemy = findNearestEnemy();
                        if (nearestEnemy != null) {
                            shootBulletsTowardsEnemy(nearestEnemy);
                        }
                    }
                } else {
                    player.keyPressed(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.keyReleased(e);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            player.move();
            moveBullets();
            moveEnemies();
            checkCollisions();
            spawnEnemies();
            spawnMedikit();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        player.draw(g2d);

        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }

        for (Enemy enemy : enemies) {
            enemy.draw(g2d);
        }

        for (Medikit medikit : medikits) {
            medikit.draw(g2d);
        }

        drawScore(g2d);

        if (gameOver) {
            drawGameOver(g2d);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void moveBullets() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            if (bullet.isOutOfBounds()) {
                bullets.remove(i);
                i--;
            } else {
                bullet.move();
            }
        }
    }

    private void moveEnemies() {
        for (Enemy enemy : enemies) {
            enemy.moveTowards(player);
        }
    }

    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();

        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (playerBounds.intersects(enemy.getBounds())) {
                player.takeDamage(10);
                enemies.remove(i);
                i--;
                if (player.getHealth() <= 0) {
                    gameOver = true;
                    updateHighScore();
                    timer.stop();
                }
            }
        }

        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            Rectangle bulletBounds = bullet.getBounds();
            for (int j = 0; j < enemies.size(); j++) {
                Enemy enemy = enemies.get(j);
                if (bulletBounds.intersects(enemy.getBounds())) {
                    bullets.remove(i);
                    i--;
                    enemies.remove(j);
                    j--;
                    score += 10;
                    break;
                }
            }
        }

        for (int i = 0; i < medikits.size(); i++) {
            Medikit medikit = medikits.get(i);
            if (playerBounds.intersects(medikit.getBounds())) {
                player.heal(20);
                medikits.remove(i);
                i--;
            }
        }
    }

    private void drawScore(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Health: " + player.getHealth(), 10, 40);
        g.drawString("Wave: " + wave, 10, 60);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Over!", 300, 250);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Press 'R' to Restart", 320, 300);
    }

    private void resetGame() {
        player = new Player();
        bullets.clear();
        enemies.clear();
        medikits.clear();
        score = 0;
        wave = 1;
        enemySpawnCounter = 0;
        gameOver = false;
        timer.start();
    }

    private void spawnEnemies() {
    enemySpawnCounter++;
    
    // Spawn new enemy if enough time has passed
    if (enemySpawnCounter >= enemySpawnDelay) {
        enemySpawnCounter = 0;
        int type = rand.nextInt(2);
        if (type == 0) {
            enemies.add(new EnemyType1(rand.nextInt(800), rand.nextInt(600)));
        } else {
            enemies.add(new EnemyType2(rand.nextInt(800), rand.nextInt(600)));
        }
    }

    // Check if all enemies are defeated to increase wave
    if (enemies.isEmpty() && wave > 1) {
        wave++;
        enemySpawnDelay = Math.max(10, enemySpawnDelay - 10);
        int enemiesToSpawn = wave * 2; // Spawn more enemies for the next wave
        for (int i = 0; i < enemiesToSpawn; i++) {
            int type = rand.nextInt(2);
            if (type == 0) {
                enemies.add(new EnemyType1(rand.nextInt(800), rand.nextInt(600)));
            } else {
                enemies.add(new EnemyType2(rand.nextInt(800), rand.nextInt(600)));
            }
        }
    }
}

    private void spawnMedikit() {
        medikitSpawnCounter++;
        if (medikitSpawnCounter >= medikitSpawnDelay) {
            medikitSpawnCounter = 0;
            medikits.add(new Medikit(rand.nextInt(800), rand.nextInt(600)));
        }
    }

    private Enemy findNearestEnemy() {
        if (enemies.isEmpty()) {
            return null;
        }

        return enemies.stream()
                .min(Comparator.comparingDouble(enemy ->
                        Math.sqrt(Math.pow(player.getX() - enemy.getX(), 2) + Math.pow(player.getY() - enemy.getY(), 2))))
                .orElse(null);
    }

    private void shootBulletsTowardsEnemy(Enemy enemy) {
        bullets.add(new Bullet(player.getX() + player.getWidth() / 2 - 2, player.getY() + player.getHeight() / 2 - 2,
                enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight() / 2));
    }

    private int loadHighScore() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ShootingSurvivalGame.HIGH_SCORE_FILE))) {
            return ois.readInt();
        } catch (IOException e) {
            // If file doesn't exist or there's an I/O error, return 0 as default high score
            return 0;
        }
    }

    private void updateHighScore() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ShootingSurvivalGame.HIGH_SCORE_FILE))) {
            oos.writeInt(score);
            highScoreLabel.setText("High Score: " + score);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Player {

        private int x, y;
        private int width, height;
        private int dx, dy;
        private int health;
        private Image image;

        public Player() {
            x = 400;
            y = 300;
            width = 50;
            height = 50;
            dx = 0;
            dy = 0;
            health = 100;
            loadImage();
        }

        private void loadImage() {
            ImageIcon ii = new ImageIcon("C:\\Users\\nagul\\OneDrive\\Pictures\\img4.png"); // Replace with your image path
            image = ii.getImage();
        }

        public void move() {
            x += dx;
            y += dy;

            if (x < 0) x = 0;
            if (x > 750) x = 750;
            if (y < 0) y = 0;
            if (y > 550) y = 550;
        }

        public void draw(Graphics2D g) {
            g.drawImage(image, x, y, width, height, null);
        }

        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                dx = -5;
            }
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                dx = 5;
            }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
                dy = -5;
            }
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                dy = 5;
            }
        }

        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                dx = 0;
            }
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                dx = 0;
            }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
                dy = 0;
            }
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                dy = 0;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getHealth() {
            return health;
        }

        public void takeDamage(int damage) {
            health -= damage;
        }

        public void heal(int amount) {
            health = Math.min(100, health + amount);
        }
    }

    abstract class Enemy {

        protected int x, y;
        protected int width, height;
        protected int speed;
        protected Image image;

        public Enemy(int x, int y) {
            this.x = x;
            this.y = y;
            this.width = 50;
            this.height = 50;
            loadImage();
        }

        protected abstract void loadImage();

        public void moveTowards(Player player) {
            int diffX = player.getX() - x;
            int diffY = player.getY() - y;
            double angle = Math.atan2(diffY, diffX);
            x += (int) (speed * Math.cos(angle));
            y += (int) (speed * Math.sin(angle));
        }

        public void draw(Graphics2D g) {
            g.drawImage(image, x, y, width, height, null);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    class EnemyType1 extends Enemy {

        public EnemyType1(int x, int y) {
            super(x, y);
            this.speed = 2;
        }

        @Override
        protected void loadImage() {
            ImageIcon ii = new ImageIcon("C:/Users/nagul/OneDrive/Pictures/Screenshot (41).png"); // Replace with your image path
            image = ii.getImage();
        }
    }

    class EnemyType2 extends Enemy {

        public EnemyType2(int x, int y) {
            super(x, y);
            this.speed = 3;
        }

        @Override
        protected void loadImage() {
            ImageIcon ii = new ImageIcon("C:\\Users\\nagul\\OneDrive\\Pictures\\Screenshot (42).png"); // Replace with your image path
            image = ii.getImage();
        }
    }

    class Bullet {

        private int x, y;
        private double dx, dy;
        private int speed;
        private int width, height;

        public Bullet(int startX, int startY, int targetX, int targetY) {
            this.x = startX;
            this.y = startY;
            this.width = 5;
            this.height = 5;
            this.speed = 10;

            double angle = Math.atan2(targetY - startY, targetX - startX);
            this.dx = speed * Math.cos(angle);
            this.dy = speed * Math.sin(angle);
        }

        public void move() {
            x += dx;
            y += dy;
        }

        public boolean isOutOfBounds() {
            return x < 0 || x > 800 || y < 0 || y > 600;
        }

        public void draw(Graphics2D g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Medikit {

        private int x, y;
        private int width, height;
        private Image image;

        public Medikit(int x, int y) {
            this.x = x;
            this.y = y;
            this.width = 30;
            this.height = 30;
            loadImage();
        }

        private void loadImage() {
            ImageIcon ii = new ImageIcon("C:/Users/nagul/OneDrive/Pictures/Screenshots/Screenshot (71).png"); // Replace with your medikit image path
            image = ii.getImage();
        }

        public void draw(Graphics2D g) {
            g.drawImage(image, x, y, width, height, null);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }
}