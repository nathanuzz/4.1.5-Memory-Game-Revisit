/*
* Project 4.1.5: Escape Room Revisited
* 
* V1.0
* Copyright(c) 2024 PLTW to present. All rights reserved
*/
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A game where a player maneuvers around a gameboard to answer
 * riddles or questions, collecing prizes with correct answers.
 */
public class GameGUI extends JComponent implements KeyListener
{
  static final long serialVersionUID = 415L;

  // constants for gameboard confg
  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int MOVE = 60;

  // frame and images for gameboard
  private JFrame frame;
  private Image bgImage;
  private Image prizeImage;
  private Image player;
  private Image playerQ;

  // player config
  private int currX = 15; 
  private int currY = 15;
  private boolean atPrize;
  private Point playerLoc;
  private int playerSteps;

  // walls, player level, and prizes
  private int numWalls = 8;
  private int playerLevel = 1;
  private Rectangle[] walls; 
  private Rectangle[] prizes;

  private String[][] quiz;

  // scores, sometimes awarded as (negative) penalties
  private int goodMove = 1;
  private int offGridVal = 5; // penalty 
  private int hitWallVal = 5;  // penalty 
  private int correctAns = 10;
  private int wrongAns = 7; // penalty 
  private int score = 0; 

  /**
   * Constructor for the GameGUI class.
   * 
   * Gets the player level and the questions/answers for the game 
   * from two files on disk. Creates th gameboard with a background image,
   *  walls, prizes, and a player.
   */
  public GameGUI() throws IOException
  {
    newPlayerLevel();
    createQuiz();
    createBoard();
  }

   /**
   * Create array of questions and answers from the quiz.txt file.
   * 
   * @preconditon: The CSV file contains at least playerLevel number of questions.
   * (It may contain more unused questions.)
   * 
   * @postconditon: A 2D array is populated with one question and one answer per row.
   * 
   * @throws IOException
   */
  private void createQuiz() throws IOException {
    BufferedReader counter = new BufferedReader(new FileReader("quiz.txt"));
    int lineCount = 0;
    while (counter.readLine() != null) {
        lineCount++;
    }
    counter.close();

    quiz = new String[lineCount][2];

    BufferedReader reader = new BufferedReader(new FileReader("quiz.txt"));
    for (int i = 0; i < quiz.length; i++) {
        String line = reader.readLine();
        String[] parts = line.split(",");
        quiz[i][0] = parts[0];
        quiz[i][1] = parts[1];
    }
    reader.close();
  }

  /**
   * Update the instance variables playerLevel and numWalls
   * based on user level stored in the level.txt file.
   * 
   * @preconditon: The CSV file must contain a level of at least 1.
   * @throws IOException
   */
  private void newPlayerLevel() 
  {
    try {
      BufferedReader reader = new BufferedReader(new FileReader("level.txt"));
      String line = reader.readLine();
      playerLevel = Integer.parseInt(line.trim());
      reader.close();
    } catch (IOException e) {
      playerLevel = 1;
    }

    numWalls = 8 + (playerLevel * 2);
  }

  /**
   * Manage the input from the keybard: arrow keys, wasd keys, p, q, and h keys.
   * Key input is not case sensivite.
   * 
   * @param the key that was pressed
   */
  @Override
  public void keyPressed(KeyEvent e)
  {
    // P Key: If player is at a prize, ask a questiona and check for correct answer.
    // If correct, pickup prize and add correctAns to score, otherwse deduct from score.
    if (e.getKeyCode() == KeyEvent.VK_P)
    {
      if (atPrize == true)
      {
        for (int i = 0; i < prizes.length; i++)
        {
          if (prizes[i].getWidth() > 0 && prizes[i].contains(playerLoc.getX(), playerLoc.getY()))
          {
            String userAnswer = askQuestion(quiz[i][0]);

            if (userAnswer != null && userAnswer.equalsIgnoreCase(quiz[i][1].trim()))
            {
              pickupPrize();
              score = score + correctAns;
              showMessage("Correct. You earned " + correctAns + " points. Score: " + score);
            }
            else
            {
              score = score - wrongAns;
              showMessage("Wrong answer. You lost " + wrongAns + " points. Score: " + score);
            }

            break;
          }
        }
      }
      else
      {
        showMessage("Not on a prize. Move to a coin first.");
      }
    }

    // Q key: quit game only if all prizes have been collected
    if (e.getKeyCode() == KeyEvent.VK_Q)
    {
      boolean allCollected = true;
      for (Rectangle p : prizes)
      {
        if (p.getWidth() > 0)
        {
          allCollected = false;
          break;
        }
      }

      if (allCollected == false)
      {
        showMessage("You need to collect all coins before quitting.");
      }
      else
      {
        score = score - playerSteps;

        if (score > 0)
        {
          playerLevel = playerLevel + 1;
          showMessage("Final score: " + score + ". You leveled up to level " + playerLevel);
        }
        else
        {
          showMessage("Game over. Final score: " + score + ". Practise some more.");
        }

        endGame();
      }
    }

    // H key: help
    if (e.getKeyCode() == KeyEvent.VK_H)
    {
      String msg = "Move player: arrows or WASD keys\n" + 
      "Pickup prize: p\n" +
      "Quit: q\n" +
      "Help: h\n";
      showMessage(msg);
    }
    
    // Arrow and WASD keys: moved down, up, left or right
    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S )
    {
      score += movePlayer(0, MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
    {
      score += movePlayer(0, -MOVE);
    }
    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
    {
      score += movePlayer(-MOVE, 0);
    }
    if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
    {
      score += movePlayer(MOVE, 0);
    }
  } 

  /**
   * Manage the key release, checking if the player is at a prize.
   * 
   * @param the key that was pressed
   */
  @Override
  public void keyReleased(KeyEvent e) 
  { 
    checkForPrize();
  }

  /* override necessary but no action */
  @Override
  public void keyTyped(KeyEvent e) { }

  /**
  * Add player, prizes, and walls to the gameboard.
  */
  private void createBoard() throws IOException
  {    
    prizes = new Rectangle[playerLevel];
    createPrizes();

    walls = new Rectangle[numWalls];
    createWalls();

    bgImage = ImageIO.read(new File("grid.png"));
    prizeImage = ImageIO.read(new File("coin.png"));
    player = ImageIO.read(new File("player.png")); 
    playerQ = ImageIO.read(new File("playerQ.png")); 
    
    // save player location
    playerLoc = new Point(currX, currY);

    // create the game frame
    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
    frame.setResizable(false); 
    frame.addKeyListener(this);

    checkForPrize();

    showMessage("Welcome to Level " + playerLevel + "! Collect all " + playerLevel + " coin(s) and press Q to finish. Press H for help.");
  }

  /**
   * Increment/decrement the player location by the amount designated.
   * This method checks for bumping into walls and going off the grid,
   * both of which result in a penalty.
   * 
   * @param incrx amount to move player in x direction
   * @param incry amount to move player in y direciton
   * 
   * @return penaly for hitting a wall or trying to go off the grid, goodMove otherwise
   */
  private int movePlayer(int incrx, int incry)
  {
      int newX = currX + incrx;
      int newY = currY + incry;

      // check if off grid horizontally and vertically
      if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
      {
        showMessage("You have tried to go off the grid!");
        return -offGridVal;
      }

      // determine if a wall is in the way
      for (Rectangle r: walls)
      {
        // this rect. location
        int startX =  (int)r.getX();
        int endX  =  (int)r.getX() + (int)r.getWidth();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        // (Note: the following if stmts could be written as huge conditional but who wants to look at that!?)
        // moving RIGHT, check to the right
        if ((incrx > 0) && (currX <= startX) && (startX <= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving LEFT, check to the left
        else if ((incrx < 0) && (currX >= startX) && (startX >= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving DOWN check below
        else if ((incry > 0) && (currY <= startY && startY <= newY && currX >= startX && currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        // moving UP check above
        else if ((incry < 0) && (currY >= startY) && (startY >= newY) && (currX >= startX) && (currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }     
      }

      // all is well, move player
      playerSteps++;
      currX += incrx;
      currY += incry;
      repaint();   
      return goodMove;
  }

  /**
   * Displays a dialog with a simple message and an OK button
   * 
   * @param str the message to show
   */
  private void showMessage(String str)
  {
    JOptionPane.showMessageDialog(frame,str );
  }

  /**
   * Display a dialog that asks a question and waits for an answer
   *
   * @param the question to display
   *
   * @return the text the user entered, null otherwise
   */
  private String askQuestion(String q)
  {
    // \n was parsed as a literal by the split method, replace with escape sequence
    return JOptionPane.showInputDialog(q.replace("\\n","\n") , JOptionPane.OK_OPTION);
  }

  /**
   * If there's a prize at the location, set atPrize to true and change player image
   *
   * @param w number of walls to create
   */
  private void checkForPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle r: prizes)
    {
      if (r.contains(px, py))
      {
        atPrize = true;
        repaint();
        return;
      }
    }
    atPrize = false;
  }

  /**
   * Pickup a prize and score points. If no prize is in that location, it results in a penalty.
   */
  private void pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p: prizes)
    {
      // if location has a prize, pick it up
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        p.setSize(0,0);
        atPrize = false;
        repaint();
      }
    }
  }

 /**
  * End the game, update and save the player level.
  */
  private void endGame() 
  {
    try {
      FileWriter fw = new FileWriter("level.txt");
      String s = playerLevel + "\n";
      fw.write(s);
      fw.close();
    } catch (IOException e)  { System.err.println("Could not level up."); }
  
    setVisible(false);
    frame.dispose();
  }

  /**
   * Add randomly placed prizes to be picked up.
   */
  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
    for (int numPrizes = 0; numPrizes < playerLevel; numPrizes++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);
      Rectangle r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);

       // get a rect. without a prize already there
       for (Rectangle p : prizes) {
        while (p != null && p.equals(r)) {
          h = rand.nextInt(GRID_H);
          w = rand.nextInt(GRID_W);
          r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
        }
      }
      prizes[numPrizes] = r;
    }
  }

  /**
   * Add walls to the board in random locations. Multiple walls may
   * be in the same locaiton.
   */
  private void createWalls()
  {
     int s = SPACE_SIZE; 
     Random rand = new Random();

     for (int n = 0; n < numWalls; n++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      if (rand.nextInt(2) == 0) 
      {
        // vertical
        r = new Rectangle((w*s + s - 5),h*s, 8,s);
      }
      else
      {
        /// horizontal
        r = new Rectangle(w*s,(h*s + s - 5), s, 8);
      }

      walls[n] = r;
    }
  }

  /* 
   * Manage board elements with graphics buffer g.
   * For internal use - do not call directly, use repaint instead.
   */
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;

    // draw grid
    g.drawImage(bgImage, 0, 0, null);

    // add prizes
    for (Rectangle p : prizes)
    {
      // pickedup prizes are 0 size so don't render
      if (p.getWidth() > 0) 
      {
      int px = (int)p.getX();
      int py = (int)p.getY();
      g.drawImage(prizeImage, px, py, null);
      }
    }

    // add walls
    for (Rectangle r : walls) 
    {
      g2.setPaint(Color.BLACK);
      g2.fill(r);
    }
   
    // draw player, saving its location
    if(atPrize)
    {
      g.drawImage(playerQ, currX, currY, 40,40, null);
    }
    else
    {
      g.drawImage(player, currX, currY, 40,40, null);
    }
    playerLoc.setLocation(currX, currY);
  }

}
