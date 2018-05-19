public class ServerTable{
	private short 	m_id = -1;
	private boolean m_isRanked;
	private boolean m_hasPassword;
	private boolean m_isBigTable;
	private boolean m_isTeamTable;
	private boolean m_isBalancedTable;
	private byte 	m_teamSize;
	private String 	m_password;
	
	public ServerTable(boolean isRanked, String password, boolean isBigTable, boolean isTeamTable, byte teamSize, boolean isBalancedTable){
		m_isRanked = isRanked;
		m_password = password;
		m_isBigTable = isBigTable;
		m_isTeamTable = isTeamTable;
		m_teamSize = teamSize;
		m_isBalancedTable = isBalancedTable;
	}
	
	public short id(){
		return m_id;
	}
	public void setId(short id){
		m_id = id;
	}
}