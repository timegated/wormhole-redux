public class Table {
	private boolean m_isRanked;
	private boolean m_hasPassword;
	private boolean m_isBigTable;
	private boolean m_isTeamTable;
	private boolean m_isBalancedTable;
	private byte 	m_teamSize;
	private String 	m_password;
	
	public Table(boolean isRanked, String password, boolean isBigTable, boolean isTeamTable, byte teamSize, boolean isBalancedTable){
		m_isRanked = isRanked;
		m_password = password;
		m_isBigTable = isBigTable;
		m_isTeamTable = isTeamTable;
		m_teamSize = teamSize;
		m_isBalancedTable = isBalancedTable;
	}
}