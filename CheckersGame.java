package games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

public class CheckersGame extends JPanel implements ActionListener {

    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 80;
    private static final int WIDTH = BOARD_SIZE * CELL_SIZE;
    private static final int HEIGHT = BOARD_SIZE * CELL_SIZE + 100;

    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private boolean redTurn = true;

    // Drag support
    private Point dragFrom = null;
    private boolean isDragging = false;

    // Keyboard cursor
    private int cursorRow = 0;
    private int cursorCol = 0;
    private Point selected = null;
    private ArrayList<Point> validMoves = new ArrayList<Point>();

    private boolean gameOver = false;
    private String winner = "";
    private int redPieces = 12;
    private int blackPieces = 12;

    private Point powerUp = null;
    private int powerUpType = 0;
    private boolean skipNextTurn = false;

    private javax.swing.Timer timer;
    private boolean soundEnabled = true;

    private Clip moveClip, captureClip, kingClip, powerClip, winClip;

    public CheckersGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 30, 20));
        setFocusable(true);

        initBoard();
        initSounds();
        timer = new javax.swing.Timer(800, this);

        // ================== DRAG & DROP ==================
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (gameOver) return;
                int row = e.getY() / CELL_SIZE;
                int col = e.getX() / CELL_SIZE;
                if (row < 8 && col < 8) {
                    int piece = board[row][col];
                    if ((redTurn && (piece == 1 || piece == 3)) || (!redTurn && (piece == 2 || piece == 4))) {
                        dragFrom = new Point(row, col);
                        isDragging = true;
                        repaint();
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (!isDragging || gameOver) {
                    isDragging = false;
                    repaint();
                    return;
                }
                int row = e.getY() / CELL_SIZE;
                int col = e.getX() / CELL_SIZE;
                if (row < 8 && col < 8 && dragFrom != null) {
                    attemptMove(dragFrom.x, dragFrom.y, row, col);
                }
                dragFrom = null;
                isDragging = false;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (isDragging) repaint();
            }
        });

        // ================== ARROW KEYS ==================
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (gameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_R) restart();
                    return;
                }
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP && cursorRow > 0) cursorRow--;
                else if (key == KeyEvent.VK_DOWN && cursorRow < 7) cursorRow++;
                else if (key == KeyEvent.VK_LEFT && cursorCol > 0) cursorCol--;
                else if (key == KeyEvent.VK_RIGHT && cursorCol < 7) cursorCol++;
                else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    handleKeyboardSelect();
                }
                else if (key == KeyEvent.VK_R) restart();
                else if (key == KeyEvent.VK_M) soundEnabled = !soundEnabled;

                repaint();
            }
        });

        requestFocusInWindow();
    }

    private void handleKeyboardSelect() {
        if (selected == null) {
            int p = board[cursorRow][cursorCol];
            if ((redTurn && (p == 1 || p == 3)) || (!redTurn && (p == 2 || p == 4))) {
                selected = new Point(cursorRow, cursorCol);
                validMoves = getValidMoves(cursorRow, cursorCol);
                play(moveClip);
            }
        } else {
            Point dest = new Point(cursorRow, cursorCol);
            if (validMoves.contains(dest)) {
                attemptMove(selected.x, selected.y, cursorRow, cursorCol);
            }
            selected = null;
            validMoves.clear();
        }
        repaint();
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        ArrayList<Point> moves = getValidMoves(fromRow, fromCol);
        Point target = new Point(toRow, toCol);
        if (moves.contains(target)) {
            performMove(fromRow, fromCol, toRow, toCol);
            redTurn = !redTurn;
            if (skipNextTurn) {
                skipNextTurn = false;
                redTurn = !redTurn;
            }
            checkGameOver();
            repaint();
        }
    }

    private void performMove(int fr, int fc, int tr, int tc) {
        int piece = board[fr][fc];
        board[tr][tc] = piece;
        board[fr][fc] = 0;

        // Capture?
        if (Math.abs(fr - tr) == 2) {
            int mr = (fr + tr) / 2;
            int mc = (fc + tc) / 2;
            board[mr][mc] = 0;
            if (redTurn) blackPieces--;
            else redPieces--;
            play(captureClip);
        } else {
            play(moveClip);
        }

        // King promotion
        if ((piece == 1 && tr == 7) || (piece == 2 && tr == 0)) {
            board[tr][tc] = piece + 2;
            play(kingClip);
        }

        // Power-up
        if (powerUp != null && tr == powerUp.x && tc == powerUp.y) {
            if (powerUpType == 1) {
                board[tr][tc] = (piece % 2 == 1) ? 3 : 4;
                play(kingClip);
            } else {
                skipNextTurn = true;
            }
            play(powerClip);
            powerUp = null;
            spawnPowerUp();
        }
    }

    private ArrayList<Point> getValidMoves(int row, int col) {
        ArrayList<Point> moves = new ArrayList<Point>();
        int piece = board[row][col];
        if (piece == 0) return moves;
        boolean isKing = piece >= 3;
        int dir = (piece == 1 || piece == 3) ? 1 : -1;
        int[] dirs = isKing ? new int[]{-1, 1} : new int[]{dir};

        for (int d : dirs) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int nr = row + d;
                int nc = col + dc;
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && board[nr][nc] == 0) {
                    moves.add(new Point(nr, nc));
                }
                int jr = row + 2 * d;
                int jc = col + 2 * dc;
                if (jr >= 0 && jr < 8 && jc >= 0 && jc < 8 && board[jr][jc] == 0) {
                    int mid = board[row + d][col + dc];
                    if (mid != 0 && (piece % 2 != mid % 2)) {
                        moves.add(new Point(jr, jc));
                    }
                }
            }
        }
        return moves;
    }

    private void spawnPowerUp() {
        Random r = new Random();
        for (int i = 0; i < 50; i++) {
            int row = r.nextInt(8);
            int col = r.nextInt(8);
            if (board[row][col] == 0 && (row + col) % 2 == 1) {
                powerUp = new Point(row, col);
                powerUpType = r.nextInt(2) + 1;
                return;
            }
        }
        powerUp = null;
    }

    private void checkGameOver() {
        if (redPieces == 0) { gameOver = true; winner = "BLACK WINS!"; play(winClip); }
        if (blackPieces == 0) { gameOver = true; winner = "RED WINS!"; play(winClip); }
    }

    private void initBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) board[r][c] = 0;
        }
        for (int c = 0; c < 8; c += 2) {
            board[0][c+1] = board[1][c] = board[2][c+1] = 2;
            board[5][c] = board[6][c+1] = board[7][c] = 1;
        }
        redPieces = blackPieces = 12;
        cursorRow = 0; cursorCol = 0;
        spawnPowerUp();
    }

    private void restart() {
        initBoard();
        redTurn = true;
        selected = null;
        validMoves.clear();
        gameOver = false;
        skipNextTurn = false;
        repaint();
    }

    private void initSounds() {
        AudioFormat fmt = new AudioFormat(8000f, 16, 1, true, false);
        moveClip    = makeClip(440, 0.1);
        captureClip = makeClip(550, 0.18);
        kingClip    = makeClip(800, 0.3);
        powerClip   = makeClip(900, 0.25);
        winClip     = makeClip(660, 1.0);
    }

    private Clip makeClip(double freq, double dur) {
        int rate = 8000;
        int samples = (int)(dur * rate);
        byte[] data = new byte[2 * samples];
        double inc = 2 * Math.PI * freq / rate;
        double angle = 0;
        for (int i = 0; i < samples; i++) {
            short s = (short)(Math.sin(angle) * 32767 * 0.4);
            data[2*i]   = (byte)(s & 0xFF);
            data[2*i+1] = (byte)((s >> 8) & 0xFF);
            angle += inc;
        }
        try {
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), 
                new AudioFormat(8000f,16,1,true,false), samples);
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception e) { return null; }
    }

    private void play(Clip c) {
        if (soundEnabled && c != null) {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                g.setColor((r + c) % 2 == 0 ? new Color(240,217,181) : new Color(181,136,99));
                g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // Power-up
        if (powerUp != null) {
            int x = powerUp.y * CELL_SIZE + 20;
            int y = powerUp.x * CELL_SIZE + 20;
            g.setColor(Color.YELLOW);
            g.fillPolygon(new int[]{x+20,x+26,x+40,x+32,x+35,x+20,x+5,x+8,x+0,x+14},
                          new int[]{y,y+14,y+14,y+25,y+40,y+30,y+40,y+25,y+14,y+14},10);
            g.setColor(Color.ORANGE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString(powerUpType==1?"KING":"SKIP", x+8, y+35);
        }

        // Draw pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p == 0) continue;
                int px = c * CELL_SIZE + 10;
                int py = r * CELL_SIZE + 10;
                g.setColor(p == 1 || p == 3 ? new Color(200,30,30) : Color.DARK_GRAY);
                g.fillOval(px, py, CELL_SIZE-20, CELL_SIZE-20);
                g.setColor(Color.WHITE);
                g.drawOval(px, py, CELL_SIZE-20, CELL_SIZE-20);
                if (p >= 3) {
                    g.setColor(Color.YELLOW);
                    g.setFont(new Font("Arial", Font.BOLD, 32));
                    g.drawString("K", px+20, py+50);
                }
            }
        }

        // Dragging ghost piece
        if (isDragging && dragFrom != null) {
            int p = board[dragFrom.x][dragFrom.y];
            if (p != 0) {
                Point mouse = getMousePosition();
                if (mouse != null) {
                    int mx = mouse.x - 30;
                    int my = mouse.y - 30;
                    g.setColor(p == 1 || p == 3 ? new Color(200,30,30,180) : new Color(30,30,30,180));
                    g.fillOval(mx, my, 60, 60);
                    g.setColor(Color.WHITE);
                    g.drawOval(mx, my, 60, 60);
                    if (p >= 3) {
                        g.setColor(new Color(255,255,0,200));
                        g.setFont(new Font("Arial", Font.BOLD, 30));
                        g.drawString("K", mx+18, my+38);
                    }
                }
            }
        }

        // Keyboard cursor
        g.setColor(new Color(255,255,0,100));
        g.fillRect(cursorCol * CELL_SIZE + 4, cursorRow * CELL_SIZE + 4, CELL_SIZE - 8, CELL_SIZE - 8);
        g.setColor(Color.YELLOW);
        g.drawRect(cursorCol * CELL_SIZE, cursorRow * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);

        // Valid moves (keyboard mode)
        if (selected != null) {
            g.setColor(new Color(0,255,0,80));
            for (Point m : validMoves) {
                g.fillOval(m.y * CELL_SIZE + 15, m.x * CELL_SIZE + 15, CELL_SIZE - 30, CELL_SIZE - 30);
            }
        }

        // Status bar
        g.setColor(new Color(0,0,0,220));
        g.fillRect(0, HEIGHT-100, WIDTH, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Turn: " + (redTurn ? "RED" : "BLACK"), 20, HEIGHT-60);
        g.drawString("Drag pieces  •  Arrows + Enter", 20, HEIGHT-30);

        if (gameOver) {
            g.setColor(new Color(0,0,0,200));
            g.fillRect(0, 0, WIDTH, HEIGHT-100);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 80));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(winner, (WIDTH - fm.stringWidth(winner))/2, HEIGHT/2 - 40);
        }
    }

    public void actionPerformed(ActionEvent e) {
        timer.stop();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Checkers - Drag & Drop + Arrow Keys (Java 1.7)");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new CheckersGame());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);
            }
        });
    }
}
