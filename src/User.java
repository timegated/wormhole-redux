import java.util.LinkedList;
import java.util.List;


public class User {
	private String m_username;
	private int m_userID;
	private int m_totalCredits;
	private byte m_subscriptionLevel;
	private short m_rank;
	private String m_clan;
	private ServerTable m_table; 
	private List<String> m_icons;
	
	public User(String username){
		m_username = username;
		m_icons = new LinkedList<String>();
		m_table = null;
		
		// Set some placeholder values that we aren't really using yet.
		m_rank = 0;
		m_clan = "clan";
		m_icons.add("small-platinumWeapons.gif");
		
	}
	
	public void setTable(ServerTable table){
		m_table = table;
	}
	
	public ServerTable table(){
		return m_table;
	}
	public String username(){
		return m_username;
	}
	public int userID(){
		return m_userID;
	}
	public int totalCredits(){
		return m_totalCredits;
	}
	public byte subscriptionLevel(){
		return m_subscriptionLevel;
	}
	public short rank(){
		return m_rank;
	}
	public String clan(){
		return m_clan;
	}
	public List<String> icons(){
		return m_icons;
	}
	public short numIcons(){
		return (short) m_icons.size();
	}

}
