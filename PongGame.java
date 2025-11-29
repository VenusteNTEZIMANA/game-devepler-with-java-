package games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.sound.sampled.*;
import java.io.*;

public class PongGame extends JPanel implements ActionListener {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 20;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;

    // Difficulty levels
    private static final String[] DIFFICULTIES = {"Easy", "Normal", "Hard", "Insane"};
    private static final int[] BALL_SPEEDS = {4, 6, 9, 13};
    private static final int[] PADDLE_SPEEDS = {10, 8, 6, 4};
    private static final int[] WIN_SCORES = {10, 15, 20, 25};

    private int difficulty;
    private int ballSpeed;
    private int paddleSpeed;
    private int winScore;

    private int paddle1Y = (HEIGHT - PADDLE_HEIGHT) / 2;
    private int paddle2Y = (HEIGHT - PADDLE_HEIGHT) / 2;
    private int ballX = WIDTH / 2;
    private int ballY = HEIGHT / 2;
    private int ballDX;
    private int ballDY;
    private int score1 = 0;
    private int score2 = 0;
    private boolean gameOver = false;

    private String player1Name = "Player 1";
    private String player2Name = "Player 2";

    private Timer timer;
    private boolean up1Pressed = false;
    private boolean down1Pressed = false;
    private boolean up2Pressed = false;
    private boolean down2Pressed = false;

    // Sound effects
    private boolean soundEnabled = true;
    private Clip moveClip, hitClip, wallClip, scoreClip;
    private AudioFormat format;

    public PongGame(int selectedDifficulty) {
        this.difficulty = selectedDifficulty;
        this.ballSpeed = BALL_SPEEDS[difficulty];
        this.paddleSpeed = PADDLE_SPEEDS[difficulty];
        this.winScore = WIN_SCORES[difficulty];

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        initSounds();
        initPlayerNames();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_R && gameOver) {
                    restart();
                } else if (key == KeyEvent.VK_M) {
                    soundEnabled = !soundEnabled;
                    repaint();
                } else if (!gameOver) {
                    if (key == KeyEvent.VK_W) { up1Pressed = true; play(moveClip); }
                    else if (key == KeyEvent.VK_S) { down1Pressed = true; play(moveClip); }
                    else if (key == KeyEvent.VK_UP) { up2Pressed = true; play(moveClip); }
                    else if (key == KeyEvent.VK_DOWN) { down2Pressed = true; play(moveClip); }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_W) up1Pressed = false;
                else if (key == KeyEvent.VK_S) down1Pressed = false;
                else if (key == KeyEvent.VK_UP) up2Pressed = false;
                else if (key == KeyEvent.VK_DOWN) down2Pressed = false;
            }
        });

        timer = new Timer(16, this);
        timer.start();
        resetBall();
    }

    private void initPlayerNames() {
        player1Name = JOptionPane.showInputDialog(null,
                "Enter Player 1 (Left Paddle) Name:", "Player 1");
        if (player1Name == null || player1Name.trim().isEmpty())
            player1Name = "Player 1";
        else
            player1Name = player1Name.trim();

        player2Name = JOptionPane.showInputDialog(null,
                "Enter Player 2 (Right Paddle) Name:", "Player 2");
        if (player2Name == null || player2Name.trim().isEmpty())
            player2Name = "Player 2";
        else
            player2Name = player2Name.trim();
    }

    private void initSounds() {
        format = new AudioFormat(8000f, 16, 1, true, false);
        moveClip = createClip(generateTone(523, 0.08));
        hitClip = createClip(generateTone(659, 0.12));
        wallClip = createClip(generateTone(784, 0.10));
        scoreClip = createClip(generateTone(392, 0.30));
    }

    private byte[] generateTone(double freq, double duration) {
        int sampleRate = 8000;
        int samples = (int) (duration * sampleRate);
        byte[] buffer = new byte[2 * samples];
        double angleInc = 2 * Math.PI * freq / sampleRate;
        double angle = 0;
        for (int i = 0; i < samples; i++) {
            double sample = Math.sin(angle) * 0.4;
            short val = (short) (sample * 32767);
            buffer[2 * i] = (byte) (val & 0xFF);
            buffer[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
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

    private void restart() {
        score1 = 0;
        score2 = 0;
        paddle1Y = (HEIGHT - PADDLE_HEIGHT) / 2;
        paddle2Y = (HEIGHT - PADDLE_HEIGHT) / 2;
        resetBall();
        gameOver = false;
        repaint();
    }

    private void resetBall() {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballDX = (Math.random() < 0.5 ? ballSpeed : -ballSpeed);
        ballDY = (int) (Math.random() * 6 - 3);
        if (ballDY == 0) ballDY = 3;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Middle line
        g.setColor(Color.WHITE);
        for (int i = 0; i < HEIGHT; i += 20) {
            g.fillRect(WIDTH / 2 - 2, i, 4, 15);
        }

        // Paddles
        g.fillRect(20, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        g.fillRect(WIDTH - 40, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Ball
        g.fillOval(ballX - BALL_SIZE / 2, ballY - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);

        // Scores & Names
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(String.valueOf(score1), (WIDTH / 4) - fm.stringWidth(String.valueOf(score1)) / 2, 60);
        g.drawString(String.valueOf(score2), (3 * WIDTH / 4) - fm.stringWidth(String.valueOf(score2)) / 2, 60);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g.getFontMetrics();
        g.drawString(player1Name, (WIDTH / 4) - fm.stringWidth(player1Name) / 2, 95);
        g.drawString(player2Name, (3 * WIDTH / 4) - fm.stringWidth(player2Name) / 2, 95);

        // Mode & Target
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.drawString("Mode: " + DIFFICULTIES[difficulty], WIDTH / 2 - 100, 40);
        g.drawString("First to " + winScore, WIDTH / 2 - 90, 70);

        // Controls
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("W/S: Left | Up/Down: Right | M: Sound | R: Restart", 10, HEIGHT - 40);
        g.drawString("Sound: " + (soundEnabled ? "ON" : "OFF"), 10, HEIGHT - 20);

        // Game Over
        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 80));
            String winner = (score1 >= winScore ? player1Name : player2Name) + " WINS!";
            g.drawString(winner, (WIDTH - g.getFontMetrics().stringWidth(winner)) / 2, HEIGHT / 2 - 30);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 36));
            g.drawString("Press R to Play Again", (WIDTH - g.getFontMetrics().stringWidth("Press R to Play Again")) / 2, HEIGHT / 2 + 40);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            repaint();
            return;
        }

        // Move paddles
        if (up1Pressed && paddle1Y > 0) paddle1Y -= paddleSpeed;
        if (down1Pressed && paddle1Y < HEIGHT - PADDLE_HEIGHT) paddle1Y += paddleSpeed;
        if (up2Pressed && paddle2Y > 0) paddle2Y -= paddleSpeed;
        if (down2Pressed && paddle2Y < HEIGHT - PADDLE_HEIGHT) paddle2Y += paddleSpeed;

        // Move ball
        ballX += ballDX;
        ballY += ballDY;

        // Wall bounce
        if (ballY < BALL_SIZE / 2 || ballY > HEIGHT - BALL_SIZE / 2) {
            ballDY = -ballDY;
            play(wallClip);
        }

        // Paddle hit
        if (ballX < 50 && ballY >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT && ballDX < 0) {
            ballDX = -ballDX;
            play(hitClip);
        }
        if (ballX > WIDTH - 50 && ballY >= paddle2Y && ballY <= paddle2Y + PADDLE_HEIGHT && ballDX > 0) {
            ballDX = -ballDX;
            play(hitClip);
        }

        // Score
        if (ballX < 0) {
            score2++;
            play(scoreClip);
            if (score2 >= winScore) gameOver = true;
            else resetBall();
        } else if (ballX > WIDTH) {
            score1++;
            play(scoreClip);
            if (score1 >= winScore) gameOver = true;
            else resetBall();
        }

        repaint();
    }

    // Settings Dialog (Java 1.7 style - no lambda)
    private static int showSettingsDialog() {
        Object[] options = {"Easy (Slow Ball)", "Normal", "Hard (Fast Ball)", "Insane (Extreme!)"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "=== PONG GAME SETTINGS ===\nChoose your difficulty!",
                "Pong - Select Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );
        return (choice >= 0) ? choice : 1;
    }

    public static void main(String[] args) {
        final int selectedDifficulty = showSettingsDialog();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Pong - " + DIFFICULTIES[selectedDifficulty] + " Mode");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new PongGame(selectedDifficulty));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);
            }
        });
    }
}
