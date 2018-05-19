import java.net.*;
import java.io.*;
import java.util.*;

public class Server{
	
	static final int PORT = 4444;
	List<ServerThread> clients = new Vector<ServerThread>();
	List<User> users = new Vector<User>();
	ServerTables tables = new ServerTables();
	
	public static void main(String args[]) throws Exception{
		new Server().process();
	}
	
	public void process() throws IOException{
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
			while(true){
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
				Socket socket = serverSocket.accept();
				System.out.println("Just connected to " + socket.getRemoteSocketAddress());
				ServerThread client =  new ServerThread(socket);
				clients.add(client);
			}
		} catch(IOException e){
			
		} finally {
	        if (serverSocket != null) {
	        	serverSocket.close();
	        }
		}
	}
	
	void broadcastUser(User user) throws IOException{
		for (ServerThread client : this.clients){
			client.sendUser(user);
		}
	}

	void addUser(User user){
		this.users.add(user);
	}
	
	void addTable(ServerTable table){
		this.tables.addTable(table);
	}
	
	private class ServerThread extends Thread {
		private User user;
		private PacketStreamWriter pw;
		private PacketStreamReader pr;
		
	    public void marshall(byte b) throws IOException{
	    	this.pw.getStream().writeByte(b);
	    }
	    public void marshall(short s) throws IOException{
	    	this.pw.getStream().writeShort(s);
	    }
	    public void marshall(int i) throws IOException{
	    	this.pw.getStream().writeInt(i);
	    }
	    public void marshall(String s) throws IOException{
	    	this.pw.getStream().writeUTF(s);
	    }
	    public void sendPacket() throws IOException{
	    	this.pw.sendPacket();
	    }
				
		public User user(){
			return user;
		}
		
		public ServerThread(Socket socket) throws IOException{
			this.pw = new PacketStreamWriter(socket.getOutputStream());
			this.pr = new PacketStreamReader(socket.getInputStream());
			start();
		}
		
		public void receiveLogin() throws IOException{			
			final DataInputStream stream = this.pr.getStream();

			short 	numBytes		= stream.readShort();
			boolean isGuestAccount 	= (stream.readByte()==1);
			String 	username 		= stream.readUTF();
			String	password 		= stream.readUTF();
			int 	gameId 			= stream.readInt();
			short 	majorVersion 	= stream.readShort();
			short 	minorVersion 	= stream.readShort();
			
			this.user = new User(username);
		}
		
		public void receiveCreateTable() throws IOException{			
			final DataInputStream stream = this.pr.getStream();
			
			byte teamSize = 0, numStringPairs;
			boolean isRanked, hasPassword, isBigTable, isTeamTable, isBalancedTable = false;
			String password = "";

			isRanked 	= (stream.readByte()==1);
			hasPassword = (stream.readByte()==1);
			
			if (hasPassword){
				password = stream.readUTF();
			}
			
			isBigTable	= (stream.readByte()==1);
			isTeamTable = (stream.readByte()==1);
			if (isTeamTable){
				teamSize = stream.readByte();
				isBalancedTable	= (stream.readByte()==1);
			}
			
			numStringPairs = stream.readByte();
			if (numStringPairs > 0){	// pretty sure this is always 0
				for (int i = 0; i<numStringPairs; i++){
					String s1 = stream.readUTF();
					String s2 = stream.readUTF();
				}
			}
			
			ServerTable table = new ServerTable(isRanked, password, isBigTable, isTeamTable, teamSize, isBalancedTable);
			user().setTable(table);
			addTable(table);
			//broadcastTable(table);
		}

		public void sendLoginResponse() throws IOException{
			byte opcode = 1;
			marshall( opcode );
			marshall( user().userID() );
			marshall( user().totalCredits() );
			marshall( user().subscriptionLevel() );
			marshall( user().username() );
			sendPacket();
		}		
		
		public void sendUser(User user) throws IOException{	
			byte opcode = 13;
			marshall( opcode );
			marshall( user.username() );
			marshall( user.rank() );
			marshall( user.numIcons() );
			for (String iconName : user.icons())
				marshall( iconName );
			marshall( user.clan() );
			sendPacket();
		}
		
		public void sendTable(ServerTable table) throws IOException{	
			byte opcode = 60;

			sendPacket();
		}
		
		public void processPackets(final DataInputStream stream) {
			try {
				final byte opcode = stream.readByte();
				switch(opcode){
				case 20:
					receiveCreateTable();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		public void run() {
			try {
				receiveLogin();
				sendLoginResponse();
				
				// Send current user list to client
				for (User u : users){
					sendUser(u);
				}
				
				// Add user and broadcast to all clients
				addUser(this.user);
				broadcastUser(this.user);
				
				// Start processing packets from client
				final DataInputStream stream = pr.getStream();
				while (true) {
					processPackets(stream);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
