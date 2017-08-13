import java.applet.*;
import java.util.*;
import java.io.*;
import java.awt.*;

public class GameBoard extends Panel
{
    private static boolean m_bPlaySound;
    private Model m_model;
    private CFTableElement m_tableElement;
    
    public static void playSound(final AudioClip audioClip) {
        if (audioClip != null && GameBoard.m_bPlaySound) {
            audioClip.play();
        }
    }
    
    public GameBoard(final CFProps cfProps, final GameNetLogic gameNetLogic, final Hashtable hashtable) {
        this.m_model = CFSkin.getSkin().generateModel(this, gameNetLogic, cfProps, hashtable);
    }
    
    public void readJoin(final DataInput dataInput) throws IOException {
        this.m_model.readJoin(dataInput);
    }
    
    public void paint(final Graphics graphics) {
        this.m_model.paintParent(graphics);
    }
    
    public void doOneCycle() {
        try {
            Thread.sleep(this.m_model.getSleepTime());
            this.m_model.doOneCycle();
            if (this.m_model.needToUpdateParent()) {
                this.repaint();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void removePlayer(final String s) throws IOException {
        this.m_model.removePlayer(s);
    }
    
    public void update(final Graphics graphics) {
        this.paint(graphics);
    }
    
    static {
        GameBoard.m_bPlaySound = true;
    }
    
    public void addPlayer(final String s, final int n, final byte b, final String[] array, final int n2) throws IOException {
        this.m_model.addPlayer(s, n, b, array, n2);
    }
    
    public static void setSound(final boolean bPlaySound) {
        GameBoard.m_bPlaySound = bPlaySound;
    }
    
    public Model getModel() {
        return this.m_model;
    }
    
    public void setTable(final CFTableElement cfTableElement) {
        this.m_model.setTable(cfTableElement);
        this.m_tableElement = cfTableElement;
    }
}
