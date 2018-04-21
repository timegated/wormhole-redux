
public class User {
	private String m_username;
	private int m_userID;
	private int m_totalCredits;
	private byte m_subscriptionLevel;
	
	public User(String username){
		m_username = username;
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

}
