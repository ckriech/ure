package ure;

import java.awt.event.KeyListener;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Receive input and dispatch game commands or UI controls.
 */


public class URECommander implements KeyListener {

    private HashMap<Character, String> keyBinds;
    private HashSet<UTimeListener> timeListeners;

    private UREActor player;

    public URECommander(UREActor theplayer) {
        timeListeners = new HashSet<UTimeListener>();
        setPlayer(theplayer);
        readKeyBinds();
    }

    public void setPlayer(UREActor theplayer) {
        player = theplayer;
    }

    public void registerTimeListener(UTimeListener listener) {
        timeListeners.add(listener);
    }

    public void unRegisterTimeListener(UTimeListener listener) {
        timeListeners.remove(listener);
    }

    public void readKeyBinds() {
        // TODO: Actually read keybinds.txt
        //
        keyBinds = new HashMap<Character, String>();
        keyBinds.put('w', "MOVE_N");
        keyBinds.put('s', "MOVE_S");
        keyBinds.put('a', "MOVE_W");
        keyBinds.put('d', "MOVE_E");
        keyBinds.put('e', "DEBUG");

    }

    public void keyPressed(KeyEvent e) {
        char c = e.getKeyChar();
        if (keyBinds.containsKey((Character)c)) {
            hearCommand(keyBinds.get((Character)c));
        }
    }

    public void keyReleased(KeyEvent e) {

    }

    public void keyTyped(KeyEvent e) {

    }

    void hearCommand(String command) {
        boolean acted = true;
        switch (command) {
            case "MOVE_N":
                walkPlayer(0, -1);
                break;
            case "MOVE_S":
                walkPlayer(0, 1);
                break;
            case "MOVE_W":
                walkPlayer(-1, 0);
                break;
            case "MOVE_E":
                walkPlayer(1, 0);
                break;
            case "DEBUG":
                debug();
        }
        if (acted) {
            tickTime();
        }
    }

    public void tickTime() {
        Iterator<UTimeListener> timeI = timeListeners.iterator();
        while (timeI.hasNext()) {
            timeI.next().hearTick();
        }
        System.gc();
    }

    void walkPlayer(int xdir, int ydir) {
        player.walkDir(xdir,ydir);
    }

    void debug() {
        player.debug();
    }
}