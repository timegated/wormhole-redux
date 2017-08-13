import java.util.*;
import java.awt.*;
import java.io.*;

public class GameNetLogic implements Runnable, IListener
{
    private int m_userID;
    private int m_tableID;
    private int m_loginPort;
    private int m_loginPort2;
    private Network m_network;
    private boolean m_bInATable;
    private boolean m_bGuestAccount;
    private GamePanel m_pnlGame;
    private Thread m_threadNetwork;
    private String m_username;
    private byte m_subscriptionLevel;
    private String m_host;
    private String m_host2;
    private long nextTime;
    private static long NOOP_DURATION;
    private MediaTracker m_mtIcons;
    private Hashtable m_htLoadedIcons;
    private Hashtable m_htUnloadedIcons;
    private static final String[] g_commands;
    private String m_lastWhisperer;
    
    public void whisper(final String s, final String s2) {
        if (s2.length() > 0) {
            String s3 = null;
            if (s == null) {
                s3 = "User (" + s2 + ") not found";
            }
            else if (s.equalsIgnoreCase(this.m_username)) {
                s3 = "Cannot target yourself for chat command";
            }
            else if (this.getPlayer(s) == null) {
                s3 = "User (" + s + ") not found";
            }
            if (s3 == null) {
                this.m_network.whisper(s, s2);
                return;
            }
            this.addLine(s3);
        }
    }
    
    private CFPlayerDialog findPlayerDialog(final String s) {
        synchronized (GamePanel.g_vDialogs) {
            for (int i = 0; i < GamePanel.g_vDialogs.size(); ++i) {
                final Object element = GamePanel.g_vDialogs.elementAt(i);
                if (element instanceof CFPlayerDialog) {
                    final CFPlayerDialog cfPlayerDialog = (CFPlayerDialog)element;
                    if (cfPlayerDialog.getCFPlayerElement().getName().equals(s)) {
                        // monitorexit(GamePanel.g_vDialogs)
                        return cfPlayerDialog;
                    }
                }
            }
        }
        // monitorexit(GamePanel.g_vDialogs)
        return null;
    }
    
    public boolean isGuest() {
        return this.m_bGuestAccount;
    }
    
    public void fireEvent(final Object o, final Object o2) {
        final LoginPanel loginPanel = this.m_pnlGame.getLoginPanel();
        final LobbyPanel lobbyPanel = this.m_pnlGame.getLobbyPanel();
        final PlayingPanel playingPanel = this.m_pnlGame.getPlayingPanel();
        if (o == loginPanel) {
            if (loginPanel.isEnabled() && loginPanel.isVisible()) {
                this.processLogin();
            }
        }
        else if (o == lobbyPanel.getChatPanel()) {
            if (((String)o2).length() > 0) {
                ((CFChatPanel)o).resetChatLine();
                this.smartSay((String)o2, (CFChatPanel)o);
            }
        }
        else {
            if (o == lobbyPanel.getLogoutButton()) {
                this.disconnect("Logged out");
                return;
            }
            if (o == lobbyPanel.getCreateTableButton()) {
                this.m_network.createTable("", false);
                return;
            }
            if (o == lobbyPanel.getCreateTableOptionsButton()) {
                this.m_pnlGame.setEnabled(false);
                final CFTableDialog generateCFTableDialog = CFSkin.getSkin().generateCFTableDialog(Util.getParentFrame(this.m_pnlGame), this.m_pnlGame.getLocationOnScreen());
                if (this.m_bGuestAccount) {
                    generateCFTableDialog.m_cbRanking.setState(false);
                    generateCFTableDialog.m_cbRanking.setEnabled(false);
                }
                generateCFTableDialog.show();
                if (generateCFTableDialog.ok()) {
                    this.m_network.createTable(generateCFTableDialog.getPassword(), !this.m_bGuestAccount && generateCFTableDialog.isRanked(), generateCFTableDialog.isBigTable(), generateCFTableDialog.isTeamTable(), generateCFTableDialog.getTeamSize(), generateCFTableDialog.isBalancedTable(), CFSkin.getSkin().getTableOptions(generateCFTableDialog));
                }
                this.m_pnlGame.setEnabled(true);
                return;
            }
            if (o == playingPanel.getStartGameButton()) {
                this.m_network.startGame(this.m_tableID);
                return;
            }
            if (o == playingPanel.getSoundButton()) {
                final CFButton cfButton = (CFButton)o;
                if (cfButton.getText().equals("Sound on")) {
                    cfButton.setText("Sound off");
                    GameBoard.setSound(false);
                    return;
                }
                cfButton.setText("Sound on");
                GameBoard.setSound(true);
            }
            else {
                if (o == playingPanel.getLeaveTableButton()) {
                    this.m_network.leaveTable();
                    this.m_bInATable = false;
                    this.m_tableID = -1;
                    this.m_pnlGame.showLobby();
                    return;
                }
                if (o == playingPanel.getChatPanel()) {
                    ((CFChatPanel)o).resetChatLine();
                    this.smartSay((String)o2, (CFChatPanel)o);
                    return;
                }
                if (o instanceof CFTableElement) {
                    if (o2 != null) {
                        this.openDialogForPlayer((String)o2);
                        return;
                    }
                    final CFTableElement cfTableElement = (CFTableElement)o;
                    if (this.m_bGuestAccount) {
                        if (cfTableElement.isRanked()) {
                            this.addLine("Only registered members can join Ranked tables!!!");
                            return;
                        }
                        if (cfTableElement.isTeamTable()) {
                            this.addLine("Only Team plan members can join Team tables!!!");
                            return;
                        }
                        if (cfTableElement.isBigTable()) {
                            this.addLine("Only Team plan members can join Huge tables!!!");
                            return;
                        }
                    }
                    if (this.m_subscriptionLevel < 2 && this.m_subscriptionLevel != -1) {
                        if (cfTableElement.isTeamTable()) {
                            this.addLine("Only Team plan members can join Team tables!!!");
                        }
                        else if (cfTableElement.isBigTable()) {
                            this.addLine("Only Team plan members can join Huge tables!!!");
                        }
                    }
                    if (!cfTableElement.isPrivate()) {
                        this.m_network.joinTable(cfTableElement.getTableID(), null);
                        return;
                    }
                    if (this.findPrivateTableDialog() == null) {
                        CFSkin.getSkin().generateCFPrivateTableDialog(Util.getParentFrame(this.m_pnlGame), this, cfTableElement, this.m_pnlGame.getLocationOnScreen()).show();
                    }
                }
                else {
                    if (o == lobbyPanel.getPlayerPanel()) {
                        this.openDialogForPlayer(((CFPlayerElement)o2).getName());
                        return;
                    }
                    if (o instanceof CFChatElement) {
                        if (!((String)o2).equals(this.m_username)) {
                            this.openDialogForPlayer((String)o2);
                        }
                    }
                    else if (o instanceof CFPrivateTableDialog) {
                        final CFPrivateTableDialog cfPrivateTableDialog = (CFPrivateTableDialog)o;
                        cfPrivateTableDialog.setStatus("Joining table...");
                        this.m_network.joinTable(cfPrivateTableDialog.getTableID(), (String)o2);
                    }
                }
            }
        }
    }
    
    public String login(final String s, final int n, final String s2, final int n2, final String s3, final String s4, final boolean bGuestAccount) {
        if (this.m_network != null) {
            this.m_network.disconnect();
            this.m_network = null;
        }
        if (this.m_threadNetwork != null && this.m_threadNetwork.isAlive()) {
            try {
                this.m_threadNetwork.stop();
            }
            catch (Exception ex) {}
        }
        (this.m_threadNetwork = new Thread(this)).start();
        this.m_pnlGame.getLobbyPanel().getTablePanel().clearTables();
        this.m_pnlGame.getLobbyPanel().getPlayerPanel().clearPlayers();
        this.m_pnlGame.getLobbyPanel().getChatPanel().clearLines();
        CFSkin.getSkin().addWelcomeMessage(this.m_pnlGame.getLobbyPanel().getChatPanel());
        this.m_bGuestAccount = bGuestAccount;
        this.m_network = new Network();
        if (this.m_network.login(CFSkin.getSkin().getGameID(), CFSkin.getSkin().getMajorVersion(), CFSkin.getSkin().getMinorVersion(), s3, s4, bGuestAccount, this.m_host, this.m_loginPort) != null) {
            return this.m_network.login(CFSkin.getSkin().getGameID(), CFSkin.getSkin().getMajorVersion(), CFSkin.getSkin().getMinorVersion(), s3, s4, bGuestAccount, this.m_host2, this.m_loginPort2);
        }
        return null;
    }
    
    public void doOneCycle() {
        if (this.m_network != null && this.m_network.m_bConnected && this.nextTime < System.currentTimeMillis()) {
            this.nextTime = System.currentTimeMillis() + GameNetLogic.NOOP_DURATION;
            this.m_network.sendNoop();
        }
        if (this.m_pnlGame != null && this.m_htUnloadedIcons.size() > 0 && this.m_mtIcons.checkID(0, true)) {
            final Enumeration<String> keys = this.m_htUnloadedIcons.keys();
            while (keys.hasMoreElements()) {
                final String s = keys.nextElement();
                final Image image = this.m_htUnloadedIcons.get(s);
                if (image != null) {
                    this.m_htLoadedIcons.put(s, image);
                }
            }
            this.m_htUnloadedIcons.clear();
            this.m_pnlGame.repaint();
            this.m_pnlGame.getLobbyPanel().getPlayerPanel().repaint();
        }
    }
    
    private synchronized void myAddPlayer(final DataInputStream dataInputStream) throws IOException {
        final String utf = dataInputStream.readUTF();
        if (utf.length() == 0) {
            return;
        }
        final short short1 = dataInputStream.readShort();
        final String[] array = new String[dataInputStream.readShort()];
        for (int i = 0; i < array.length; ++i) {
            final String utf2 = dataInputStream.readUTF();
            array[i] = utf2;
            if (this.m_htLoadedIcons.get(utf2) == null && this.m_htUnloadedIcons.get(utf2) == null) {
                final Image image = GamePanel.m_applet.getImage(GamePanel.m_applet.getCodeBase(), "images/icons/" + utf2);
                this.m_htUnloadedIcons.put(utf2, image);
                this.m_mtIcons.addImage(image, 0);
            }
        }
        this.m_pnlGame.getLobbyPanel().getPlayerPanel().addPlayer(utf, dataInputStream.readUTF(), short1, array);
    }
    
    private String[][] readTableOptions(final DataInputStream dataInputStream) throws IOException {
        final byte byte1 = dataInputStream.readByte();
        if (byte1 > 0) {
            final String[][] array = new String[byte1][2];
            for (byte b = 0; b < byte1; ++b) {
                array[b][0] = dataInputStream.readUTF();
                array[b][1] = dataInputStream.readUTF();
            }
            return array;
        }
        return null;
    }
    
    public void disconnect(final String connectionStatus) {
        this.m_pnlGame.getLoginPanel().setLoginEnabled(true);
        this.m_pnlGame.getLoginPanel().setConnectionStatus(connectionStatus);
        this.m_pnlGame.showLogin();
        if (this.m_network != null) {
            this.m_network.disconnect();
            this.m_network = null;
        }
        if (this.m_threadNetwork != null && this.m_threadNetwork.isAlive()) {
            try {
                this.m_threadNetwork.stop();
            }
            catch (Exception ex) {}
        }
    }
    
    public void addLine(final String s) {
        if (this.m_bInATable) {
            this.m_pnlGame.getPlayingPanel().getChatPanel().addLine(s);
            return;
        }
        this.m_pnlGame.getLobbyPanel().getChatPanel().addLine(s);
    }
    
    private void setInTable(final short tableID, final int slot, final String s) {
        final CFTableElement table = this.m_pnlGame.getLobbyPanel().getTablePanel().findTable(tableID);
        final PlayingPanel playingPanel = this.m_pnlGame.getPlayingPanel();
        playingPanel.getGameBoard().getModel().reset();
        playingPanel.getGameBoard().getModel().setSlot(slot);
        playingPanel.setTableInfo(this.m_username, s);
        playingPanel.setTable(table);
        this.m_tableID = tableID;
        this.m_bInATable = true;
        playingPanel.getChatPanel().clearLines();
        CFSkin.getSkin().addTableInstructions(playingPanel.getChatPanel());
        this.m_pnlGame.showGame();
    }
    
    public void gameOver() {
        this.m_network.submitCredits(this.m_pnlGame.getPlayingPanel().getCreditsPanel().getCredits(), (int)this.m_pnlGame.getPlayingPanel().getGameBoard().getModel().getTimeElapsed());
    }
    
    public Network getNetwork() {
        return this.m_network;
    }
    
    public Dimension getSize() {
        return this.m_pnlGame.getSize();
    }
    
    public int getTableForPlayer(final String s) {
        final CFPlayerElement player = this.getPlayer(s);
        if (player != null) {
            return player.getTableID();
        }
        return -1;
    }
    
    private void setTableForPlayer(final String s, final int tableID) {
        final CFPlayerElement player = this.getPlayer(s);
        if (player != null) {
            player.setTableID(tableID);
        }
    }
    
    private void handleSubscriptionButtons(final Vector vector) {
        for (int i = 0; i < vector.size(); ++i) {
            final CFButton cfButton = vector.elementAt(i);
            if (cfButton != null) {
                cfButton.setVisible(cfButton.shouldShowURLButton(this.m_subscriptionLevel));
            }
        }
    }
    
    public void resetCredits() {
        this.m_pnlGame.getPlayingPanel().getCreditsPanel().setCredits(0);
    }
    
    public void finishLogin(final int userID, final int totalCredits, final byte subscriptionLevel, final String username) {
        this.m_username = username;
        this.m_userID = userID;
        this.m_tableID = -1;
        this.m_subscriptionLevel = subscriptionLevel;
        this.handleSubscriptionButtons(this.m_pnlGame.getLobbyPanel().getURLButtons());
        this.handleSubscriptionButtons(this.m_pnlGame.getPlayingPanel().getURLButtons());
        this.m_pnlGame.getLobbyPanel().setUsername(this.m_username);
        this.m_pnlGame.getPlayingPanel().getCreditsPanel().setRegistered(!this.m_bGuestAccount);
        this.m_pnlGame.getPlayingPanel().getCreditsPanel().setTotalCredits(totalCredits);
        this.m_pnlGame.getLoginPanel().setLoginEnabled(true);
        this.m_pnlGame.showLobby();
        this.m_network.listUserNames();
        this.m_network.listTables();
        this.nextTime = 0L;
    }
    
    private void handleTableStatusChange(final int n, final byte b, final short n2) {
        final CFTablePanel tablePanel = this.m_pnlGame.getLobbyPanel().getTablePanel();
        tablePanel.setTableStatus(n, b, n2);
        switch (b) {
            case 1: {
                final CFPrivateTableDialog privateTableDialog = this.findPrivateTableDialog();
                if (privateTableDialog != null) {
                    privateTableDialog.setTableRemoved();
                }
                tablePanel.removeTable(n);
            }
            case 4: {
                if (this.m_tableID == n) {
                    this.m_pnlGame.getPlayingPanel().setInCountdown(false, n2);
                    return;
                }
                break;
            }
            case 3: {
                if (this.m_tableID == n) {
                    this.m_pnlGame.getPlayingPanel().setInCountdown(true, n2);
                    return;
                }
                break;
            }
        }
    }
    
    public void openDialogForPlayer(final String s) {
        final CFPlayerDialog playerDialog = this.findPlayerDialog(s);
        if (playerDialog == null) {
            final CFPlayerElement player = this.getPlayer(s);
            if (player != null) {
                final CFPlayerDialog generateCFPlayerDialog = CFSkin.getSkin().generateCFPlayerDialog(Util.getParentFrame(this.m_pnlGame), player, this, this, this.m_pnlGame.getLocationOnScreen());
                if (s.equals(this.m_username)) {
                    generateCFPlayerDialog.setSelfDialog();
                }
                generateCFPlayerDialog.show();
            }
        }
        else {
            playerDialog.requestFocus();
        }
    }
    
    private void smartSay(String s, final CFChatPanel cfChatPanel) {
        if (s == null || s.length() < 1) {
            return;
        }
        String s2 = null;
        int i = 0;
        while (i < GameNetLogic.g_commands.length) {
            if (s.toLowerCase().startsWith(GameNetLogic.g_commands[i])) {
                final boolean b = i == 2 || i == 3;
                final boolean b2 = i == 4 || i == 5;
                s = s.substring(GameNetLogic.g_commands[i].length()).trim();
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5: {
                        final int index = s.indexOf(32);
                        if (index > 0) {
                            s2 = s.substring(0, index);
                            s = s.substring(index + 1);
                        }
                        if (b || b2) {
                            s2 = s.toLowerCase();
                            break;
                        }
                        break;
                    }
                    case 6:
                    case 7: {
                        s2 = this.m_lastWhisperer;
                        break;
                    }
                    default: {
                        if (cfChatPanel == this.m_pnlGame.getLobbyPanel().getChatPanel()) {
                            CFSkin.getSkin().addInstructions(cfChatPanel);
                            return;
                        }
                        CFSkin.getSkin().addTableInstructions(cfChatPanel);
                        CFSkin.getSkin().addInstructions(cfChatPanel);
                        return;
                    }
                }
                String s3 = null;
                if (s2 == null) {
                    s3 = "User (" + s + ") not found";
                }
                else if (s2.equalsIgnoreCase(this.m_username)) {
                    s3 = "Cannot target yourself for chat command";
                }
                else if (this.getPlayer(s2) == null) {
                    s3 = "User (" + s2 + ") not found";
                }
                if (s3 != null) {
                    this.addLine("[Error] " + s3);
                }
                if (b) {
                    this.setIgnoreUser(s2, true);
                    return;
                }
                if (b2) {
                    this.setIgnoreUser(s2, false);
                    return;
                }
                this.m_network.whisper(s2, s);
                return;
            }
            else {
                ++i;
            }
        }
        if (this.m_bInATable) {
            this.m_network.tableSay(s);
            return;
        }
        this.m_network.say(s);
    }
    
    private CFPrivateTableDialog findPrivateTableDialog() {
        synchronized (GamePanel.g_vDialogs) {
            for (int i = 0; i < GamePanel.g_vDialogs.size(); ++i) {
                final Object element = GamePanel.g_vDialogs.elementAt(i);
                if (element instanceof CFPrivateTableDialog) {
                    // monitorexit(GamePanel.g_vDialogs)
                    return (CFPrivateTableDialog)element;
                }
            }
        }
        // monitorexit(GamePanel.g_vDialogs)
        return null;
    }
    
    public byte getSubscriptionLevel() {
        return this.m_subscriptionLevel;
    }
    
    public CFPlayerElement getPlayer(final String s) {
        return this.m_pnlGame.getLobbyPanel().getPlayerPanel().getPlayer(s);
    }
    
    public GameNetLogic(final GamePanel pnlGame, final CFProps cfProps) {
        this.m_htLoadedIcons = new Hashtable();
        this.m_htUnloadedIcons = new Hashtable();
        this.m_pnlGame = pnlGame;
        this.m_mtIcons = new MediaTracker(GamePanel.m_applet);
        this.m_loginPort = cfProps.getInt("loginserviceport", 6041);
        this.m_loginPort2 = cfProps.getInt("loginserviceport2", 7042);
        final String host = GamePanel.m_applet.getCodeBase().getHost();
        this.m_host2 = host;
        this.m_host = host;
        this.nextTime = System.currentTimeMillis() + 10000000L;
    }
    
    public void addCredits(final int n) {
        final CreditsPanel creditsPanel = this.m_pnlGame.getPlayingPanel().getCreditsPanel();
        creditsPanel.setCredits(creditsPanel.getCredits() + n);
    }
    
    public Image getIcon(final String s) {
        return this.m_htLoadedIcons.get(s);
    }
    
    public String getUsername() {
        return this.m_username;
    }
    
    private void processLogin() {
        final LoginPanel loginPanel = this.m_pnlGame.getLoginPanel();
        loginPanel.setLoginEnabled(false);
        loginPanel.setConnectionStatus("Logging in");
        final boolean b = loginPanel.getPassword().length() == 0;
        String s = loginPanel.getUsername();
        loginPanel.getPassword();
        if (s.length() == 0) {
            this.disconnect("Please enter username");
            return;
        }
        if (b) {
            if (s.length() > 5) {
                this.disconnect("Guest usernames cannot exceed 5 characters");
                return;
            }
            s = CFPlayerElement.GUEST_STRING + s;
        }
        else if (s.length() > 20) {
            this.disconnect("Usernames cannot exceed 20 characters");
            return;
        }
        final String login = this.login(this.m_host, this.m_loginPort, this.m_host2, this.m_loginPort2, s, loginPanel.getPassword(), b);
        if (login != null) {
            this.disconnect(login);
        }
    }
    
    static {
        GameNetLogic.NOOP_DURATION = 20000L;
        g_commands = new String[] { "/whisper ", "/w ", "/ignore ", "/i ", "/unignore ", "/u ", "/reply ", "/r ", "/help", "/h", "/?" };
    }
    
    public void processPackets(final DataInputStream dataInputStream, final short n) {
        try {
            final byte byte1 = dataInputStream.readByte();
            switch (byte1) {
                case 0: {
                    this.disconnect(dataInputStream.readUTF());
                }
                case 1: {
                    this.finishLogin(dataInputStream.readInt(), dataInputStream.readShort(), dataInputStream.readByte(), dataInputStream.readUTF());
                }
                case 4: {
                    String utf = null;
                    try {
                        utf = dataInputStream.readUTF();
                    }
                    catch (Exception ex2) {}
                    this.disconnect((utf == null) ? "Logged out" : utf);
                }
                case 6: {
                    final String utf2 = dataInputStream.readUTF();
                    final String utf3 = dataInputStream.readUTF();
                    final String utf4 = dataInputStream.readUTF();
                    final CFPlayerElement player = this.getPlayer(utf2);
                    if (player != null && player.getIgnored()) {
                        return;
                    }
                    String s;
                    String s2;
                    if (utf2.equalsIgnoreCase(this.m_username)) {
                        s = "[to " + utf3;
                        s2 = utf3;
                    }
                    else {
                        if (!utf3.equalsIgnoreCase(this.m_username)) {
                            return;
                        }
                        s = "[from " + utf2;
                        this.m_lastWhisperer = utf2;
                        s2 = utf2;
                    }
                    final String string = s + "]";
                    this.m_pnlGame.getPlayingPanel().getChatPanel().addLine(s2, string, utf4, null);
                    this.m_pnlGame.getLobbyPanel().getChatPanel().addLine(s2, string, utf4, null);
                }
                case 5:
                case 18: {
                    final String utf5 = dataInputStream.readUTF();
                    final String utf6 = dataInputStream.readUTF();
                    final CFPlayerElement player2 = this.getPlayer(utf5);
                    if (player2 != null && player2.getIgnored()) {
                        return;
                    }
                    ((byte1 == 5) ? this.m_pnlGame.getLobbyPanel().getChatPanel() : this.m_pnlGame.getPlayingPanel().getChatPanel()).addLine(utf5, utf6);
                }
                case 13: {
                    this.myAddPlayer(dataInputStream);
                }
                case 14: {
                    final String utf7 = dataInputStream.readUTF();
                    this.m_pnlGame.getLobbyPanel().getPlayerPanel().removePlayer(utf7);
                    final CFPlayerDialog playerDialog = this.findPlayerDialog(utf7);
                    if (playerDialog != null) {
                        playerDialog.setUserLoggedOff();
                        return;
                    }
                    break;
                }
                case 9: {
                    for (short short1 = dataInputStream.readShort(), n2 = 0; n2 < short1; ++n2) {
                        this.myAddPlayer(dataInputStream);
                    }
                }
                case 41: {
                    final short short2 = dataInputStream.readShort();
                    final String utf8 = dataInputStream.readUTF();
                    final byte byte2 = dataInputStream.readByte();
                    if (short2 == this.m_tableID) {
                        this.m_pnlGame.getPlayingPanel().getGameBoard().getModel().setTeam(utf8, byte2);
                        return;
                    }
                    break;
                }
                case 50: {
                    final CFTablePanel tablePanel = this.m_pnlGame.getLobbyPanel().getTablePanel();
                    for (short short3 = dataInputStream.readShort(), n3 = 0; n3 < short3; ++n3) {
                        final short short4 = dataInputStream.readShort();
                        final byte byte3 = dataInputStream.readByte();
                        final boolean b = dataInputStream.readByte() == 1;
                        final boolean b2 = dataInputStream.readByte() == 1;
                        final boolean b3 = dataInputStream.readByte() == 1;
                        final boolean b4 = dataInputStream.readByte() == 1;
                        byte byte4 = -1;
                        boolean b5 = false;
                        if (b4) {
                            byte4 = dataInputStream.readByte();
                            b5 = (dataInputStream.readByte() == 1);
                        }
                        final String[][] tableOptions = this.readTableOptions(dataInputStream);
                        if (tablePanel.findTable(short4) == null) {
                            tablePanel.addTable(short4, b3 ? 8 : 4);
                        }
                        tablePanel.setTableStatus(short4, byte3, 0);
                        tablePanel.findTable(short4).setOptions(b, b2, b4, byte4, b5, tableOptions);
                        for (short short5 = dataInputStream.readShort(), n4 = 0; n4 < short5; ++n4) {
                            final byte byte5 = dataInputStream.readByte();
                            final String utf9 = dataInputStream.readUTF();
                            tablePanel.addPlayerToTable(short4, utf9, byte5);
                            this.setTableForPlayer(utf9, short4);
                        }
                    }
                }
                case 20: {
                    if (dataInputStream.readByte() == 1) {
                        final short short6 = dataInputStream.readShort();
                        final String utf10 = dataInputStream.readUTF();
                        final boolean b6 = dataInputStream.readByte() == 1;
                        final boolean b7 = dataInputStream.readByte() == 1;
                        byte byte6 = -1;
                        boolean b8 = false;
                        if (b7) {
                            byte6 = dataInputStream.readByte();
                            b8 = (dataInputStream.readByte() == 1);
                        }
                        final String[][] tableOptions2 = this.readTableOptions(dataInputStream);
                        final CFTablePanel tablePanel2 = this.m_pnlGame.getLobbyPanel().getTablePanel();
                        if (tablePanel2.findTable(short6) == null) {
                            tablePanel2.addTable(short6, b6 ? 8 : 4);
                        }
                        tablePanel2.findTable(short6).setOptions(false, utf10.length() > 0, b7, byte6, b8, tableOptions2);
                        this.setInTable(short6, 0, (utf10.length() == 0) ? null : utf10);
                        return;
                    }
                    break;
                }
                case 60: {
                    final CFTablePanel tablePanel3 = this.m_pnlGame.getLobbyPanel().getTablePanel();
                    final short short7 = dataInputStream.readShort();
                    final byte byte7 = dataInputStream.readByte();
                    final String utf11 = dataInputStream.readUTF();
                    final boolean b9 = dataInputStream.readByte() == 1;
                    final boolean b10 = dataInputStream.readByte() == 1;
                    final int n5 = (dataInputStream.readByte() == 1) ? 8 : 4;
                    final boolean b11 = dataInputStream.readByte() == 1;
                    byte byte8 = -1;
                    boolean b12 = false;
                    if (b11) {
                        byte8 = dataInputStream.readByte();
                        b12 = (dataInputStream.readByte() == 1);
                    }
                    final String[][] tableOptions3 = this.readTableOptions(dataInputStream);
                    if (tablePanel3.findTable(short7) == null) {
                        tablePanel3.addTable(short7, n5);
                    }
                    tablePanel3.setTableStatus(short7, byte7, 0);
                    tablePanel3.addPlayerToTable(short7, utf11, (byte)0);
                    this.setTableForPlayer(utf11, short7);
                    tablePanel3.findTable(short7).setOptions(b9, b10, b11, byte8, b12, tableOptions3);
                    if (short7 == this.m_tableID) {
                        this.m_pnlGame.getPlayingPanel().repaint();
                        return;
                    }
                    break;
                }
                case 21: {
                    if (dataInputStream.readByte() == 1) {
                        String utf12 = null;
                        final CFPrivateTableDialog privateTableDialog = this.findPrivateTableDialog();
                        if (privateTableDialog != null) {
                            privateTableDialog.handleClosing();
                        }
                        final short short8 = dataInputStream.readShort();
                        final byte byte9 = dataInputStream.readByte();
                        if (dataInputStream.readByte() == 1) {
                            utf12 = dataInputStream.readUTF();
                        }
                        this.setInTable(short8, byte9, utf12);
                        this.m_pnlGame.getPlayingPanel().getGameBoard().readJoin(dataInputStream);
                        return;
                    }
                    final CFPrivateTableDialog privateTableDialog2 = this.findPrivateTableDialog();
                    if (privateTableDialog2 != null) {
                        privateTableDialog2.setStatus(dataInputStream.readUTF());
                        return;
                    }
                    break;
                }
                case 64: {
                    final short short9 = dataInputStream.readShort();
                    final String utf13 = dataInputStream.readUTF();
                    final byte byte10 = dataInputStream.readByte();
                    final byte byte11 = dataInputStream.readByte();
                    this.m_pnlGame.getLobbyPanel().getTablePanel().addPlayerToTable(short9, utf13, byte10);
                    this.setTableForPlayer(utf13, short9);
                    if (this.m_tableID == short9 && this.m_bInATable) {
                        final CFPlayerElement player3 = this.getPlayer(utf13);
                        this.m_pnlGame.getPlayingPanel().getGameBoard().addPlayer(utf13, player3.getRank(), byte11, player3.getIcons(), byte10);
                        return;
                    }
                    break;
                }
                case 65: {
                    final CFTablePanel tablePanel4 = this.m_pnlGame.getLobbyPanel().getTablePanel();
                    final short short10 = dataInputStream.readShort();
                    final String utf14 = dataInputStream.readUTF();
                    tablePanel4.removePlayerFromTable(short10, utf14);
                    this.setTableForPlayer(utf14, -1);
                    if (this.m_bInATable && this.m_tableID == short10) {
                        this.m_pnlGame.getPlayingPanel().getGameBoard().removePlayer(utf14);
                        return;
                    }
                    break;
                }
                case 66: {
                    final short short11 = dataInputStream.readShort();
                    final byte byte12 = dataInputStream.readByte();
                    this.handleTableStatusChange(short11, byte12, (short)((byte12 == 3) ? dataInputStream.readShort() : -1));
                }
                case 27: {
                    final CreditsPanel creditsPanel = this.m_pnlGame.getPlayingPanel().getCreditsPanel();
                    creditsPanel.setTotalCredits(dataInputStream.readInt());
                    int credits = creditsPanel.getCredits() - dataInputStream.readShort();
                    if (credits < 0) {
                        credits = 0;
                    }
                    creditsPanel.setCredits(credits);
                }
                case 75: {
                    final short short12 = dataInputStream.readShort();
                    for (byte byte13 = dataInputStream.readByte(), b13 = 0; b13 < byte13; ++b13) {
                        final String utf15 = dataInputStream.readUTF();
                        final short short13 = dataInputStream.readShort();
                        final short short14 = dataInputStream.readShort();
                        if (short12 == this.m_tableID) {
                            this.m_pnlGame.getPlayingPanel().getGameBoard().getModel().updatePlayerRank(utf15, short13, short14);
                            if (utf15.equals(this.m_username)) {
                                this.m_pnlGame.getPlayingPanel().repaint();
                            }
                        }
                        final CFPlayerElement player4 = this.getPlayer(utf15);
                        if (player4 != null) {
                            player4.setRank(short14);
                            final CFTableElement table = this.m_pnlGame.getLobbyPanel().getTablePanel().findTable(player4.getTableID());
                            if (table != null) {
                                table.repaint();
                            }
                            final CFPlayerDialog playerDialog2 = this.findPlayerDialog(utf15);
                            if (playerDialog2 != null) {
                                playerDialog2.repaint();
                            }
                        }
                    }
                }
                case 80: {
                    this.m_pnlGame.getPlayingPanel().getGameBoard().getModel().handleGamePacket(dataInputStream);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void setIgnoreUser(final String s, final boolean ignored) {
        final CFPlayerElement player = this.getPlayer(s);
        if (player != null && player.getIgnored() != ignored) {
            player.setIgnored(ignored);
            if (ignored) {
                this.addLine("[Ignored] (" + s + ")");
                return;
            }
            this.addLine("[Unignored] " + s);
        }
    }
    
    public void run() {
        while (true) {
            try {
                while (true) {
                    if (this.m_network != null && this.m_network.m_bConnected) {
                        try {
                            final DataInputStream packet = this.m_network.readPacket();
                            this.processPackets(packet, packet.readShort());
                            continue;
                        }
                        catch (IOException ex) {
                            return;
                        }
                    }
                    Thread.sleep(500L);
                }
            }
            catch (Exception ex2) {
                if (this.m_network != null) {
                    this.disconnect("Connection Lost");
                    return;
                }
                continue;
            }
            break;
        }
    }
    
    public int getPlayerRank(final String s) {
        final CFPlayerElement player = this.getPlayer(s);
        if (player != null) {
            return player.getRank();
        }
        return -1;
    }
}
