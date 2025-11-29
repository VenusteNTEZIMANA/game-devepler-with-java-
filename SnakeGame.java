package com.fams;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

public class SnakeGame extends JPanel implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int UNIT = 25;

    private static final String[] DIFFICULTIES = {"Easy", "Normal", "Fast"};
    private static final int[] INITIAL_DELAYS = {220, 160, 110};
    private static final int[] SPEED_STEPS = {2, 3, 5};
    private static final int[] MIN_DELAYS = {120, 90, 60};
    private static int[] highScores = {0, 0, 0};

    private java.util.List<Point> snake;
    private int dx = UNIT;
    private int dy = 0;
    private Point food;
    private javax.swing.Timer timer;
    private int score = 0;
    private boolean gameOver = false;
    private int delay = 150;
    private int difficulty;
    private int minDelay;
    private int speedStep;

    // Sound effects
    private boolean soundEnabled = true;
    private Clip moveClip, eatClip, overClip;
    private AudioFormat format;

    public SnakeGame(int difficulty) {
        this.difficulty = difficulty;
        minDelay = MIN_DELAYS[difficulty];
        speedStep = SPEED_STEPS[difficulty];
        delay = INITIAL_DELAYS[difficulty];

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        snake = new ArrayList<Point>();
        initSounds();
        startGame();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_M) {
                    soundEnabled = !soundEnabled;
                    repaint();
                } else if (key == KeyEvent.VK_R) {
                    startGame();
                } else if (!gameOver) {
                    if (key == KeyEvent.VK_LEFT && dx != UNIT) {
                        dx = -UNIT; dy = 0; play(moveClip);
                    } else if (key == KeyEvent.VK_RIGHT && dx != -UNIT) {
                        dx = UNIT; dy = 0; play(moveClip);
                    } else if (key == KeyEvent.VK_UP && dy != UNIT) {
                        dx = 0; dy = -UNIT; play(moveClip);
                    } else if (key == KeyEvent.VK_DOWN && dy != -UNIT) {
                        dx = 0; dy = UNIT; play(moveClip);
                    }
                }
            }
        });
    }

    private static int chooseDifficulty() {
        Object[] options = {"Easy (Slow Snake)", "Normal", "Fast (Quick Snake)"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Choose your difficulty level! 🐍",
            "Snake Game Settings",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[1]
        );
        return (choice >= 0) ? choice : 1;  // Default to Normal
    }

    private void startGame() {
        snake.clear();
        snake.add(new Point(UNIT * 8, UNIT * 10));
        snake.add(new Point(UNIT * 9, UNIT * 10));
        snake.add(new Point(UNIT * 10, UNIT * 10));
        dx = UNIT; dy = 0;
        newFood();
        score = 0;
        delay = INITIAL_DELAYS[difficulty];
        gameOver = false;
        if (timer != null) timer.stop();
        timer = new javax.swing.Timer(delay, this);
        timer.start();
    }

    private void newFood() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(WIDTH / UNIT) * UNIT;
            y = rand.nextInt(HEIGHT / UNIT) * UNIT;
        } while (snake.contains(new Point(x, y)));
        food = new Point(x, y);
    }

    private void initSounds() {
        format = new AudioFormat(8000f, 16, 1, true, false);
        moveClip = createClip(generateTone(440, 0.05));
        eatClip = createClip(generateTone(659, 0.20));
        overClip = createClip(generateTone(220, 0.80));
    }

    private byte[] generateTone(double freq, double duration) {
        int sampleRate = 8000;
        int samples = (int)(duration * sampleRate);
        byte[] buffer = new byte[2 * samples];
        double angleInc = 2 * Math.PI * freq / sampleRate;
        double angle = 0;
        for (int i = 0; i < samples; i++) {
            double sample = Math.sin(angle) * 0.4;
            short val = (short)(sample * 32767);
            buffer[2*i] = (byte)(val & 0xFF);
            buffer[2*i+1] = (byte)((val >> 8) & 0xFF);
            angle += angleInc;
        }
        return buffer;
    }

    private Clip createClip(byte[] data) {
        try {
            AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(data), format, data.length / 2);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void play(Clip clip) {
        if (soundEnabled && clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Grid
        g.setColor(new Color(40,40,40));
        for (int i = 0; i <= WIDTH/UNIT; i++) g.drawLine(i*UNIT,0,i*UNIT,HEIGHT);
        for (int i = 0; i <= HEIGHT/UNIT; i++) g.drawLine(0,i*UNIT,WIDTH,i*UNIT);

        if (!gameOver) {
            // Food
            g.setColor(Color.RED);
            g.fillOval(food.x + 4, food.y + 4, UNIT-8, UNIT-8);

            // Snake body
            g.setColor(Color.GREEN);
            for (int i = 0; i < snake.size()-1; i++) {
                Point p = snake.get(i);
                g.fillRect(p.x, p.y, UNIT, UNIT);
            }
            // Snake head (brighter)
            g.setColor(new Color(0, 255, 0));
            Point head = snake.get(snake.size()-1);
            g.fillRect(head.x, head.y, UNIT, UNIT);
        }

        // Score & High Score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        String s = "Score: " + score;
        g.drawString(s, (WIDTH - fm.stringWidth(s))/2, 50);
        s = "High (" + DIFFICULTIES[difficulty] + "): " + highScores[difficulty];
        g.drawString(s, (WIDTH - fm.stringWidth(s))/2, 95);

        // Difficulty info
        g.setFont(new Font("Arial", Font.BOLD, 24));
        s = "Difficulty: " + DIFFICULTIES[difficulty];
        g.drawString(s, (WIDTH - fm.stringWidth(s))/2, 130);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Arrows: Move | M: Sound | R: Restart", 20, HEIGHT-30);
        g.drawString("Sound: " + (soundEnabled?"ON":"OFF"), 20, HEIGHT-10);

        // Game Over
        if (gameOver) {
            g.setColor(new Color(0,0,0,200));
            g.fillRect(0,0,WIDTH,HEIGHT);

            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 80));
            String msg = "GAME OVER";
            FontMetrics bigFm = g.getFontMetrics();
            g.drawString(msg, (WIDTH - bigFm.stringWidth(msg))/2, HEIGHT/2 - 40);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm2 = g.getFontMetrics();

            String scoreMsg = "Your Score: " + score;
            int scoreW = fm2.stringWidth(scoreMsg);
            g.drawString(scoreMsg, (WIDTH - scoreW)/2, HEIGHT/2 + 20);

            String highMsg = "High (" + DIFFICULTIES[difficulty] + "): " + highScores[difficulty];
            int highW = fm2.stringWidth(highMsg);
            g.drawString(highMsg, (WIDTH - highW)/2, HEIGHT/2 + 70);

            String restartMsg = "Press R to Restart";
            int restartW = fm2.stringWidth(restartMsg);
            g.drawString(restartMsg, (WIDTH - restartW)/2, HEIGHT/2 + 120);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) { repaint(); return; }

        Point head = snake.get(snake.size()-1);
        Point newHead = new Point(head.x + dx, head.y + dy);

        // Collision
        if (newHead.x < 0 || newHead.x >= WIDTH || newHead.y < 0 || newHead.y >= HEIGHT
            || snake.contains(newHead)) {
            gameOver = true;
            if (score > highScores[difficulty]) {
                highScores[difficulty] = score;
            }
            play(overClip);
            return;
        }

        snake.add(newHead);

        if (newHead.equals(food)) {
            score += 10;
            newFood();
            delay = Math.max(minDelay, delay - speedStep);
            timer.setDelay(delay);
            play(eatClip);
        } else {
            snake.remove(0);
        }
        repaint();
    }

    public static void main(String[] args) {
        final int difficulty = chooseDifficulty();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Snake Game - " + DIFFICULTIES[difficulty] + " Mode - Java 1.7");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new SnakeGame(difficulty));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
