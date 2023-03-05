import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MouseInputListener;

public class Controller extends Frame {
   public int screenWidth = 1080;
   public int screenHeight = 720;
 
   int initialPixX = 0;
   int initialPixY = 0;
   double initialZoom = 1;
 
   int initialMoveSpeed = 16;
   int moveSpeed = initialMoveSpeed;
 
   int currentPixX = initialPixX;
   int currentPixY = initialPixY;
   double currentZoomModifier = initialZoom;
   
   int screenCornerGlobalX;
   int screenCornerGlobalY;
   
   boolean pause = false;
   
   //every screen pixel = pixelWorth of global pixels
   double pixelWorth;
 
   int mouseX;
   int mouseY;
   
   int frameInWait=0;
   int totalFrameWait=20;
   
   int clickDelay = 0;
   int maxClickDelay = 5;
   
   HashSet<Character> keys;
   HashSet<Integer> mouseButtons;
 
   Grid grid;
   MousePresence mouse;
   KeyboardPresence keyboard;
 
   public Graphics g;
   public DrawingPanel p;
 
   public Controller() {
      p = new DrawingPanel(screenWidth, screenHeight);
      g = p.getGraphics();
     
      mouse = new MousePresence();
      keyboard = new KeyboardPresence();
     
      p.addMouseListener(mouse);
      p.addMouseWheelListener(mouse.wheel);
      p.addKeyListener(keyboard);
     
      grid = new Grid(100, 100, 2);
   }

 
   public void update() {
      inputs();
      values();
      
      if (!pause) {
         if (frameInWait >= totalFrameWait-1)
            grid.logicUpdate(this);
         frameInWait++;
         frameInWait %= totalFrameWait;
      }
         
      grid.updateForRender(this);
      grid.renderLines(this);
      grid.renderSquares(this);
      
      g.setColor(new Color(255,255,255));
      if (pause) {
         g.fillRect(screenWidth-40, 20, 20, 80);
         g.fillRect(screenWidth-80, 20, 20, 80);  
      } else {
         g.fillPolygon(new int[]{screenWidth-80, screenWidth-80, screenWidth-20}, new int[]{20,100,60}, 3);
      }
   }
 
   public void inputs() {
      /*************************\
      |          MOUSE          |
      \*************************/
      mouseX = mouse.getX();
      mouseY = mouse.getY();
      int s = mouse.wheel.getAndResetScroll();
      if (s != 0) {
         currentZoomModifier /= Math.pow(2,s/Math.abs(s));
      }
      if (currentZoomModifier < .0078125) {currentZoomModifier = .0078125;}
      if (currentZoomModifier > 128) {currentZoomModifier = 128;}
     
      pixelWorth = 1/currentZoomModifier;
     
      moveSpeed = (int) (initialMoveSpeed/currentZoomModifier);
      if (moveSpeed < 1) {moveSpeed=1;}
     
     
      mouseButtons = mouse.getButtons();
      if (mouseButtons.contains(1) && clickDelay == 0) {
         grid.invertOffClick(this, mouseX, mouseY);
         clickDelay = maxClickDelay;
      }
      if (clickDelay > 0) {
         clickDelay--;
      }
      
     
      //System.out.println(currentZoomModifier);
     
      /*************************\
      |           KEYS          |
      \*************************/
      keys = keyboard.getKeysDown();
      if (keys.contains('w')) {
         currentPixY -= moveSpeed;
      }
      if (keys.contains('a')) {
         currentPixX -= moveSpeed;
      }
      if (keys.contains('s')) {
         currentPixY += moveSpeed;
      }
      if (keys.contains('d')) {
         currentPixX += moveSpeed;
      }
      if (keys.contains('c')) {
         grid = new Grid(64,64,2);
      }
      if (keys.contains('1')) {
         grid.fillOffClick(this, mouseX, mouseY);
      }
      if (keys.contains('2')) {
         grid.killOffClick(this, mouseX, mouseY);
      }
      if (keys.contains('p')) {
         pause = true;
      }
      if (keys.contains('u')) {
         pause = false;
      }
   }
   
   public void values() {
      screenCornerGlobalX = (int)(currentPixX-(screenWidth/2*pixelWorth));
      screenCornerGlobalY = (int)(currentPixY-(screenHeight/2*pixelWorth));
   }
 
   public static void main(String[] args) {
      Controller controller = new Controller();
      while (true) {
         controller.update();
         Controller.delay(50);
      }
   }
 
   public static void delay(int n) {
      long startDelay = System.currentTimeMillis();
      long endDelay = 0;
      while(endDelay - startDelay < n)
         endDelay = System.currentTimeMillis();
   }
}

class Grid {
   int width;
   int height;
   int pixSpacing;
 
   Square[][] squares;
   int[] lineColor = new int[]{100,100,200};
 
   int pixSquareSize = 64;
   
   int startX;
   int startY;
 
   public Grid(int w, int h, int s) {
      width = w;
      height = h;
      pixSpacing = s;
     
      squares = new Square[height][width];
      for (int i = 0; i < squares.length; i++) {
         for (int j = 0; j < squares[0].length; j++) {
            squares[i][j] = new Square(i*(pixSquareSize+pixSpacing), j*(pixSquareSize+pixSpacing));
         }
      }
   }
 
   public void logicUpdate(Controller c) {
      Boolean[][] gridCopy = new Boolean[height][width];
      for (int i = 0; i < squares.length; i++) {
         for (int j = 0; j< squares[0].length; j++) {
            gridCopy[i][j] = squares[i][j].filled;
         }  
      }
      
      for (int i = 0; i < squares.length; i++) {
         for (int j = 0; j< squares[0].length; j++) {
            int total = 0;
            for (int rowShift = -1; rowShift < 2; rowShift++) {
               for (int colShift = -1; colShift < 2; colShift++) {
                  if (gridCopy[((i+rowShift)%height+height)%height][((j+colShift)%width+width)%width] == true) {
                     total++;
                  }
               }
            }
            if (gridCopy[i][j])
               total--;
            if (gridCopy[i][j]) {
               if (total < 2 || total > 3) {
                  squares[i][j].kill();
               }
            } 
            else {
               if (total == 3) {
                  squares[i][j].fill();
               }
            }
         }
      }
   }
   
   public void updateForRender(Controller c) {
      Canvas sC = new Canvas();
      sC.setSize(c.screenWidth, c.screenHeight);
      sC.paint(c.g);
     
      c.g.setColor(new Color(0,0,0));
      c.g.fillRect(0,0,1080,720);
     
      startX = -c.screenCornerGlobalX%(pixSquareSize+pixSpacing);
      startY = -c.screenCornerGlobalY%(pixSquareSize+pixSpacing);
     
      if (c.screenCornerGlobalX<0)
         startX -= (pixSquareSize+pixSpacing);
      if (c.screenCornerGlobalY<0)
         startY -= (pixSquareSize+pixSpacing);
         
      //System.out.println(c.screenCornerGlobalX + " " + c.screenCornerGlobalY + " " + c.currentZoomModifier + " " + (startX+c.screenCornerGlobalX));
   }
 
   public void renderLines(Controller c) {
      c.g.setColor(new Color(lineColor[0], lineColor[1], lineColor[2]));
      int lineThickness = (int) (pixSpacing * c.currentZoomModifier);
      for (int i = startY; i < c.screenHeight*c.pixelWorth; i+=(pixSquareSize+pixSpacing)) {
         c.g.fillRect(0, (int) (i/c.pixelWorth+pixSquareSize*c.currentZoomModifier), c.screenWidth, lineThickness);
      }
      for (int j = startX; j < c.screenWidth*c.pixelWorth; j+=(pixSquareSize+pixSpacing)) {
         c.g.fillRect((int) (j/c.pixelWorth+pixSquareSize*c.currentZoomModifier), 0, lineThickness, c.screenHeight);
      }
   }
 
   public void renderSquares(Controller c) {
      for (int i = startY; i < c.screenHeight*c.pixelWorth; i+=(pixSquareSize+pixSpacing)) {
         for (int j = startX; j < c.screenWidth*c.pixelWorth; j+=(pixSquareSize+pixSpacing)) {
            int row = ((i+c.screenCornerGlobalY)/(pixSquareSize+pixSpacing) % height + height) % height;
            int col = ((j+c.screenCornerGlobalX)/(pixSquareSize+pixSpacing) % width + width) % width;
         
            Square square = squares[row][col];
            if (square.filled) {
               c.g.setColor(new Color(square.color[0], square.color[1], square.color[2]));
               c.g.fillRect((int) (j/c.pixelWorth), (int) (i/c.pixelWorth), (int) (pixSquareSize*c.currentZoomModifier), (int) (pixSquareSize*c.currentZoomModifier));
            }
         }
      }
      c.g.setColor(new Color(255,255,255));
      c.g.fillRect(535,345,10,10);  
   }
   
   public int[] getRoundedGlobalClicks(Controller c, int x, int y) {
      int globalXClick = (int)(c.screenCornerGlobalX + x*c.pixelWorth);
      int globalYClick = (int)(c.screenCornerGlobalY + y*c.pixelWorth);
      if (globalXClick < 0) {
         globalXClick -= (pixSquareSize+pixSpacing);
      }
      if (globalYClick < 0) {
         globalYClick -= (pixSquareSize+pixSpacing);
      }
      return new int[]{globalXClick, globalYClick};
   }
   
   public void invertOffClick(Controller c, int x, int y) {
      int[] clicks = getRoundedGlobalClicks(c, x, y);
      int globalXClick = clicks[0];
      int globalYClick = clicks[1];
      int row = (globalYClick / (pixSquareSize+pixSpacing) % height + height) % height;
      int col = (globalXClick / (pixSquareSize+pixSpacing) % width + width) % width;
      squares[row][col].invert();
   }
   
   public void killOffClick(Controller c, int x, int y) {
      int[] clicks = getRoundedGlobalClicks(c, x, y);
      int globalXClick = clicks[0];
      int globalYClick = clicks[1];
      int row = (globalYClick / (pixSquareSize+pixSpacing) % height + height) % height;
      int col = (globalXClick / (pixSquareSize+pixSpacing) % width + width) % width;
      squares[row][col].kill();
   }
   
   public void fillOffClick(Controller c, int x, int y) {
      int[] clicks = getRoundedGlobalClicks(c, x, y);
      int globalXClick = clicks[0];
      int globalYClick = clicks[1];
      int row = (globalYClick / (pixSquareSize+pixSpacing) % height + height) % height;
      int col = (globalXClick / (pixSquareSize+pixSpacing) % width + width) % width;
      squares[row][col].fill();
   }
}

class Square {
   static int[] defaultColor = new int[]{0, 0, 0};
   static int[] filledColor = new int[]{255,0,0};
 
   int[] color = new int[3];
 
   int globalX;
   int globalY;
   
   boolean filled = false;
 
   public Square(int x, int y, int[] c) {
      globalX = x;
      globalY = y;
      color = c;
   }
 
   public Square(int x, int y) {
      globalX = x;
      globalY = y;
   }
   
   public void invert() {
      filled = !filled;
     
      if (filled) {
         color = filledColor;
      } 
      else {
         color = defaultColor;
      }
   }
   
   public void fill() {
      filled = true;
      color = filledColor;
   }
   
   public void kill() {
      filled = false;
      color = defaultColor;
   }
}

class MousePresence implements MouseInputListener {
   int x;
   int y;
   int scroll;
 
   HashSet<Integer> buttonsDown = new HashSet();
 
   public MouseWheelPresence wheel = new MouseWheelPresence();
 
   public int getX() {
      return x;}
   public int getY() {
      return y;}
 
   public void mousePressed(MouseEvent e) {
      buttonsDown.add(e.getButton());
   }

   public void mouseReleased(MouseEvent e) {
      buttonsDown.remove(e.getButton());
   }
     
   public void mouseClicked(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}
   public void mouseDragged(MouseEvent e) {}
 
   public void mouseMoved(MouseEvent e) {
      x = e.getX();
      y = e.getY();
   }
   
   public HashSet<Integer> getButtons() {
      return buttonsDown;}


}

class MouseWheelPresence implements MouseWheelListener {
   private int scroll;
   public void mouseWheelMoved(MouseWheelEvent e) {
      scroll = e.getUnitsToScroll();
   }
   public int getAndResetScroll() {
      int temp = scroll;
      scroll = 0;
      return temp;
   }
}

class KeyboardPresence implements KeyListener {
   HashSet<Character> keysDown = new HashSet();
 
   public void keyPressed(KeyEvent e) {
      keysDown.add(e.getKeyChar());
   }
 
   public void keyReleased(KeyEvent e) {
      keysDown.remove(e.getKeyChar());
   }
 
   public void keyTyped(KeyEvent e) {
      keysDown.add(e.getKeyChar());
   }
 
   public HashSet<Character> getKeysDown() {
      return keysDown;}
 
   void saySomething(String eventDescription, KeyEvent e) {
      System.out.println(eventDescription + " detected on " + e.getComponent().getClass().getName() + ".");
   }
}
