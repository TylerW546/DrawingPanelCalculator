import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MouseInputListener;

import java.util.ArrayList;
import java.util.List;

public class Controller extends JFrame {
    public int screenWidth = 225;
    public int screenHeight = 335;

    int mouseX;
    int mouseY;

    int frameInWait = 0;
    int totalFrameWait = 20;

    HashSet<Character> keys;
    HashSet<Integer> mouseButtons;

    Button[] buttons = new Button[19];
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
        
        String[] strings = {
            "C", "+-", "%", "/", "7", "8", "9", "X", "4", "5", "6", "-", "1", "2", "3", "+", "0", ".", "="
        };

        for (int i = 0; i < 19; i++) {
            Button b = new Button(5+i%4*55, i/4*55+60, strings[i]);
            buttons[i] = b;
        }
    }

    public void update() {
        inputs();
        values();

        Calculator.c.render(this);

        g.setFont(new Font("Purisa", Font.PLAIN, 25));
        for (int i = 0; i < 19; i++) {
            buttons[i].update(this);
            buttons[i].render(this);
        }
    }

    public void inputs() {
        /*************************
         * \
         * | MOUSE |
         * \
         *************************/
        mouseX = mouse.getX();
        mouseY = mouse.getY();
        int s = mouse.wheel.getAndResetScroll();

        mouseButtons = mouse.getButtons();
        if (Button.clickTimer_ > 0) {
            Button.clickTimer_--;
        }

        // System.out.println(currentZoomModifier);

        /*************************
         * \
         * | KEYS |
         * \
         *************************/
        keys = keyboard.getKeysDown();
    }

    public void values() {
        
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
        while (endDelay - startDelay < n)
            endDelay = System.currentTimeMillis();
    }
}

class Calculator {
    static Calculator c = new Calculator();
    
    String input = "";
    float typedInput = 0;

    List<Float> queue = new ArrayList<>();
    List<String> opQueue = new ArrayList<>();

    String lastOp = "+";
    float lastOpVal = 0;

    int[] color_ = new int[]{125, 125, 0};

    public Calculator() {
        queue.add(0f);
    }

    public void addChar(String s) {
        if ("0123456789".contains(s)) {
            input += s;
            typedInput = Float.parseFloat(input);
            queue.set(queue.size()-1, typedInput);
        } else if (".".contains(s) && !input.contains(".")) {
            input = Float.toString(queue.get(queue.size()-1)/1) + ".";
            typedInput = Float.parseFloat(input);
            queue.set(queue.size()-1, typedInput);
        } else if ("/X-+".contains(s)) {
            if ("-+".contains(s)) {
                calculate(false);
            }
            input = "";
            queue.add(0f);
            opQueue.add(s);
        } else if ("%".contains(s)) {
            queue.set(queue.size()-1, queue.get(queue.size()-1)/100);
        } else if ("+-".contains(s)) {
            queue.set(queue.size()-1, queue.get(queue.size()-1)*-1);
        } else if ("C".contains(s)) {
            input = "";
            queue.clear();
            queue.add(0f);
            opQueue.clear();

            lastOp = "+";
            lastOpVal = 0;
        } else if ("=".contains(s)) {
            input = "";
            calculate(true);
        }
    }

    public float calculate(boolean equalsButtonUsed) {
        Calculator.c.printQueue();

        if (queue.size() == 1 && equalsButtonUsed) {
            queue.add(lastOpVal);
            opQueue.add(lastOp);
            calculate(false);
        }
        
        while (queue.size() > 1) {
            float result = 0;
            String operator = "";
            float v1 = 0;
            float v2 = queue.get(queue.size()-1);

            if (opQueue.size() > 0) {
                operator = opQueue.get(opQueue.size()-1);
                v1 = queue.get(queue.size()-2);
                
                lastOp = operator;
                lastOpVal = v2;
            }

            if (operator == "+") {
                result = v1 + v2;
            } else if (operator == "-") {
                result = v1 - v2;
            } else if (operator == "X") {
                result = v1 * v2;
            } else if (operator == "/") {
                result = v1 / v2;
            }

            queue.remove(queue.size()-1);
            opQueue.remove(opQueue.size()-1);
            queue.set(queue.size()-1, result);
        }
        return queue.get(0);
    }

    public void render(Controller controller) {
        controller.g.setColor(new Color(color_[0], color_[1], color_[2]));
        controller.g.fillRect(5, 5, 215, 50);
        
        controller.g.setColor(new Color(125,255,255));
        
        String outStr = Float.toString(queue.get(queue.size()-1));
        controller.g.drawString(outStr, controller.screenWidth/2-6*outStr.length(),40);
    }

    public void printQueue() {
        for (int i = 0; i < queue.size()-1; i++) {
            System.out.print(Float.toString(queue.get(i)) + " " + opQueue.get(i) + " ");
        }
        System.out.println(Float.toString(queue.get(queue.size()-1)));
    }
}

class Button {
    static int[] defaultColor = new int[] { 0, 0, 0 };
    static int[] clickedColor = new int[] { 255, 0, 0 };

    int[] color_ = new int[3];

    int x_;
    int y_;
    String str_;

    int width_ = 50;
    int height_ = 50;

    boolean clicked_ = false;
    static int clickTimer_ = 0;
    static int clickMax_ = 5;

    public Button(int x, int y, String str) {
        x_ = x;
        y_ = y;
        str_ = str;
        color_ = Button.defaultColor;

        if (str_.equals("=")) {
            width_ = 105;
            color_ = new int[] { 0, 255, 0 };
        }
    }

    public void update(Controller controller) {
        if (controller.mouse.getButtons().contains(1) && Button.clickTimer_ == 0) {
            if (controller.mouseX > x_ && controller.mouseX < x_ + width_ && controller.mouseY > y_ && controller.mouseY < y_ + height_) {
                System.out.println(str_);
                Calculator.c.addChar(str_);
                Button.clickTimer_ = Button.clickMax_;
                clicked_ = true;
            }
        }

        if (Button.clickTimer_ == 0) {
            clicked_ = false;
        }
    }

    public void render(Controller controller) {
        controller.g.setColor(new Color(color_[0], color_[1], color_[2]));
        controller.g.fillRect(x_, y_, width_, height_);
        
        controller.g.setColor(new Color(255,255,255));
        controller.g.drawString(str_, x_ + (width_/2-(8*str_.length())), y_ + (height_/2+(8)));
    }
}



class MousePresence implements MouseInputListener {
    int x;
    int y;
    int scroll;

    HashSet<Integer> buttonsDown = new HashSet<Integer>();

    public MouseWheelPresence wheel = new MouseWheelPresence();

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void mousePressed(MouseEvent e) {
        buttonsDown.add(e.getButton());
    }

    public void mouseReleased(MouseEvent e) {
        buttonsDown.remove(e.getButton());
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }

    public HashSet<Integer> getButtons() {
        return buttonsDown;
    }

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
    HashSet<Character> keysDown = new HashSet<Character>();

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
        return keysDown;
    }

    void saySomething(String eventDescription, KeyEvent e) {
        System.out.println(eventDescription + " detected on " + e.getComponent().getClass().getName() + ".");
    }
}
