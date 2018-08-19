import java.awt.*;
import java.awt.event.*;

public class CFTableDialog extends Dialog implements IListener, WindowListener, ActionListener, ItemListener
{
    public Checkbox m_cbRanking;
    public TextField m_tfPassword;
    public CFButton m_cfBtnOK;
    public CFButton m_cfBtnCancel;
    public Choice m_choiceTeamTable;
    public Checkbox m_cbBigTable;
    public Checkbox m_cbBalancedTeams;
    private IListener m_listener;
    private boolean m_bOK;
    
    public void fireEvent(final Object o, final Object o2) {
        if (o == this.m_cfBtnOK) {
            this.m_bOK = true;
        }
        this.handleClosing();
    }
    
    public CFTableDialog(final Frame frame) {
        super(frame, "Create Table", true);
        this.setLayout(null);
        super.setResizable(false);
        GamePanel.g_vDialogs.addElement(this);
        this.m_cbRanking = CFSkin.getSkin().generateCheckBox("Ranked Table", true);
        (this.m_cbBigTable = CFSkin.getSkin().generateCheckBox("Huge Table", false)).addItemListener(this);
        this.m_cbBigTable.setEnabled(false);
        this.add(this.m_cbBigTable);
        (this.m_cbBalancedTeams = CFSkin.getSkin().generateCheckBox("Balanced Teams", false)).setEnabled(false);
        this.add(this.m_cbBalancedTeams);
        (this.m_choiceTeamTable = CFSkin.getSkin().generateChoice()).addItemListener(this);
        this.m_choiceTeamTable.setEnabled(false);
        this.add(this.m_choiceTeamTable);
        this.m_cfBtnOK = CFSkin.getSkin().generateCFButton("Create", this, 8);
        this.m_cfBtnCancel = CFSkin.getSkin().generateCFButton("Cancel", this, 8);
        (this.m_tfPassword = new TextField(15)).addActionListener(this);
        this.add(this.m_tfPassword);
        this.add(this.m_cbRanking);
        this.add(this.m_cfBtnOK);
        this.add(this.m_cfBtnCancel);
        this.addWindowListener(this);
    }
    
    public void windowDeactivated(final WindowEvent windowEvent) {
    }
    
    public void itemStateChanged(final ItemEvent itemEvent) {
        if (itemEvent.getSource() != this.m_choiceTeamTable) {
            final byte subscriptionLevel = CFSkin.getSkin().getLogic().getSubscriptionLevel();
            //if (subscriptionLevel >= 2) {
            if (subscriptionLevel >= -1) {
                this.m_choiceTeamTable.setEnabled(this.m_cbBigTable.getState());
                this.m_cbBalancedTeams.setEnabled(this.m_choiceTeamTable.isEnabled() && !this.m_choiceTeamTable.getSelectedItem().equals(CFSkin.STR_TEAMS[0]));
                if (!this.m_choiceTeamTable.isEnabled()) {
                	this.m_choiceTeamTable.select(0);
                	this.m_cbBalancedTeams.setState(false);
                }
            }
            return;
        }

        final boolean enabled = !this.m_choiceTeamTable.getSelectedItem().equals(CFSkin.STR_TEAMS[0]);
        this.m_cbBalancedTeams.setEnabled(enabled);
        if (!enabled) {
            this.m_cbBalancedTeams.setState(false);
            return;
        }
	    this.m_cbRanking.setState(false);
    }
    
    public void paint(final Graphics graphics) {
        CFSkin.getSkin().paintCFTableDialog(graphics, this);
    }
    
    public void windowClosing(final WindowEvent windowEvent) {
        this.handleClosing();
    }
    
    private void handleClosing() {
        GamePanel.g_vDialogs.removeElement(this);
        this.dispose();
    }
    
    public boolean isBalancedTable() {
        return this.m_cbBalancedTeams.getState();
    }
    
    public String getPassword() {
        return this.m_tfPassword.getText();
    }
    
    public void windowOpened(final WindowEvent windowEvent) {
    }
    
    public void windowClosed(final WindowEvent windowEvent) {
    }
    
    public void windowDeiconified(final WindowEvent windowEvent) {
    }
    
    public void windowActivated(final WindowEvent windowEvent) {
    }
    
    public void actionPerformed(final ActionEvent actionEvent) {
        if (actionEvent.getSource() != null) {
            this.m_bOK = true;
            this.handleClosing();
        }
    }
    
    public boolean isRanked() {
        return this.m_cbRanking.getState();
    }
    
    public boolean isBigTable() {
        return this.m_cbBigTable.getState();
    }
    
    public void setListener(final IListener listener) {
        this.m_listener = listener;
    }
    
    public int getBoardSize() {
        final String selectedItem = this.m_choiceTeamTable.getSelectedItem();
        if (selectedItem == null) {
            return 0;
        }
        for (int i = 0; i < CFSkin.STR_TEAMS.length; ++i) {
            if (CFSkin.STR_TEAMS[i].equals(selectedItem)) {
                return i;
            }
        }
        return 0;
    }
    
    public boolean ok() {
        return this.m_bOK;
    }
    
    public void windowIconified(final WindowEvent windowEvent) {
    }
    
    public boolean isTeamTable() {
        final String selectedItem = this.m_choiceTeamTable.getSelectedItem();
        return selectedItem != null && !selectedItem.equals(CFSkin.STR_TEAMS[0]);
    }
}
