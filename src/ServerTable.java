public class ServerTable{
	private short 		m_id = -1;
	private short 		m_numPlayers = 0;
	private boolean 	m_isRanked;
	private boolean 	m_hasPassword;
	private boolean 	m_isBigTable;
	private boolean 	m_isTeamTable;
	private boolean 	m_isBalancedTable;
	private boolean 	m_isPrivate;
	private byte 		m_teamSize;
	private byte		m_status;
	private String 		m_password;
	private String[]	m_names;
	
	public ServerTable(boolean isRanked, String password, boolean isBigTable, boolean isTeamTable, byte teamSize, boolean isBalancedTable){
		m_isRanked = isRanked;
		m_password = password;
		m_isBigTable = isBigTable;
		m_isTeamTable = isTeamTable;
		m_teamSize = teamSize;
		m_isBalancedTable = isBalancedTable;
		m_isPrivate = false;
		m_status = 0;
		m_names = new String[isBigTable ? 8 : 4];
			
		if (password.length() > 0) {
			m_isPrivate = true;
		}
	}
	
	public void setId(short id) {
		m_id = id;
	}
	public byte addUser(String username) {
		for (int i=0; i<m_names.length; i++) {
			if (m_names[i] == null) {
				m_names[i] = username;
				m_numPlayers ++;
				return (byte)i;
			}
		}
		return (byte)-1;
	}
	public String player(int i) {
		return m_names[i] != null ? m_names[i] : "";
	}
	public short id() {
		return m_id;
	}
	public boolean isRanked() {
		return m_isRanked;
	}
	public boolean isBigTable() {
		return m_isBigTable;
	}
	public boolean isTeamTable() {
		return m_isTeamTable;
	}
	public boolean isBalancedTable() {
		return m_isBalancedTable;
	}
	public boolean isPrivate() {
		return m_isPrivate;
	}
	public byte teamSize() {
		return m_teamSize;
	}
	public byte status() {
		return m_status;
	}
	public int numPlayerSlots() {
		return m_names.length;
	}
	public short numPlayers() {
		return m_numPlayers;
	}



}