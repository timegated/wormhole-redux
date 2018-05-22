import java.util.List;
import java.util.Vector;

public class ServerUserManager {
	List<ServerUser> users = new Vector<ServerUser>();
	
	public ServerUserManager(){}
	
	public List<ServerUser> users(){
		return this.users;
	}
	
	public synchronized void addUser(ServerUser user){
		// Loop over to find open slot, or add to end.
		// The id of the table is set to index of this list.
		short idx = 0;
		while (idx < users.size() && users.get(idx) != null){
			idx ++;
		}
		user.setUserId(idx);
		if (idx == users.size()){
			users.add(user);
		}
		else{
			users.set(idx, user);
		}
	}
}