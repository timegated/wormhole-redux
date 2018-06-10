import java.net.*;
import java.io.*;
import java.util.*;

public class Server{
	public static final short TABLE_COUNTDOWN = 5;
	
	static final int PORT = 4444;
	List<ServerThread> clients = new Vector<ServerThread>();
	ServerUserManager userManager = new ServerUserManager();
	ServerTableManager tableManager = new ServerTableManager();
	
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
	
	void broadcastUser(ServerUser user) throws IOException{
		for (ServerThread client : this.clients){
			client.sendUser(user);
		}
	}
	
	void broadcastCreateTable(ServerTable table) throws IOException {
		for (ServerThread client : this.clients){
			client.sendFullTable(table);
		}
	}
	
	void broadcastJoinTable(short tableId, String username, byte slot, byte teamId) throws IOException {
		for (ServerThread client : this.clients){
			client.sendJoinTable(tableId, username, slot, teamId);
		}
	}
	
	void broadcastTableStatusChange(short tableId, byte status, short countdown) throws IOException {
		for (ServerThread client : this.clients){
			client.sendTableStatusChange(tableId, status, countdown);
		}
	}
	
	void broadcastGameStart(ServerTable table) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendGameStart(table);
			}
		}
	}
	
	void broadcastPowerup(ServerTable table, byte powerupType, byte toSlot, byte b1, short gameSession, byte b2) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendPowerup(powerupType, toSlot, b1, gameSession, b2);
			}
		}
	}

	void addUser(ServerUser user){
		this.userManager.addUser(user);
	}
	
	void addTable(ServerTable table){
		this.tableManager.addTable(table);
	}
	
	
	private class ServerThread extends Thread {
		private ServerUser user;
		private PacketStreamWriter pw;
		private PacketStreamReader pr;
		
	    public void marshall(boolean b) throws IOException {
	    	this.pw.getStream().writeByte((byte)(b ? 1 : 0));
	    }
	    public void marshall(byte b) throws IOException {
	    	this.pw.getStream().writeByte(b);
	    }
	    public void marshall(short s) throws IOException {
	    	this.pw.getStream().writeShort(s);
	    }
	    public void marshall(int i) throws IOException {
	    	this.pw.getStream().writeInt(i);
	    }
	    public void marshall(String s) throws IOException {
	    	this.pw.getStream().writeUTF(s);
	    }
	    public void sendPacket() throws IOException {
	    	this.pw.sendPacket();
	    }
				
		public ServerUser user() {
			return user;
		}
		
		public ServerThread(Socket socket) throws IOException {
			this.pw = new PacketStreamWriter(socket.getOutputStream());
			this.pr = new PacketStreamReader(socket.getInputStream());
			start();
		}
		
		public void receiveLogin() throws IOException {			
			final DataInputStream stream = this.pr.getStream();

			short 	numBytes		= stream.readShort();
			boolean isGuestAccount 	= (stream.readByte()==1);
			String 	username 		= stream.readUTF();
			String	password 		= stream.readUTF();
			int 	gameId 			= stream.readInt();
			short 	majorVersion 	= stream.readShort();
			short 	minorVersion 	= stream.readShort();
			
			this.user = new ServerUser(username);
		}
		
		public void receivePowerup() throws IOException {			
			final DataInputStream stream = this.pr.getStream();

			short	gameSession		= stream.readShort();
			byte	powerupType		= stream.readByte();
			byte	toSlot			= stream.readByte();
			byte 	upgradeLevel	= stream.readByte();
			
			broadcastPowerup(user().table(), powerupType, user().slot(), toSlot, gameSession, (byte)0);
		}
		
		public void receiveCreateTable() throws IOException {			
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
			byte slot = table.addUser(user().username());
			user().setSlot(slot);
			user().setTable(table);
			addTable(table);
			broadcastCreateTable(table);
		}
		
		public void receiveJoinTable() throws IOException {			
			final DataInputStream stream = this.pr.getStream();
			
			short 	tableId  = stream.readShort();
			String 	username = stream.readUTF();	// don't think I need this... must be user connected to this socket
			byte	teamId	 = 0;
			
			ServerTable table = tableManager.getTable(tableId);
			byte slot = table.addUser(user().username());		// see, I am not using username variable here.
			user().setTable(table);
			broadcastJoinTable(tableId, user().username(), slot, teamId);
		}
		
		public void receiveStartGame() throws IOException, InterruptedException {
			final DataInputStream stream = this.pr.getStream();
			
			short tableId = stream.readShort();
			
			ServerTable table = tableManager.getTable(tableId);
			
			if (table.status() != 3) {
				table.setStatus((byte)3);
				new TableCountdownThread(table);
			}
		}
		
		public void sendTableStatusChange(short tableId, byte status, short countdown) throws IOException {
			byte opcode = 66;
			marshall( opcode );
			marshall( tableId );
			marshall( status );
			if (status == 3) {	// countdown phase
				marshall( countdown );
			}
			sendPacket();
		}		

		public void sendLoginResponse() throws IOException {
			byte opcode = 1;
			marshall( opcode );
			marshall( user().userId() );
			marshall( user().totalCredits() );
			marshall( user().subscriptionLevel() );
			marshall( user().username() );
			sendPacket();
		}		
		
		public void sendUser(ServerUser user) throws IOException {	
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
		
		public void sendGameStart(ServerTable table) throws IOException {	
			byte opcode = 80;
			byte opcode2 = 100;
			short gameId = 0;
			short sessionId = 0;
			marshall( opcode );
			marshall( opcode2 );
			marshall( gameId );
			marshall( sessionId );
			
			short nPlayers = table.numPlayers();
			marshall( nPlayers );
			for (int i=0; i<nPlayers; i++) {
				marshall( table.player(i) );
				marshall( (byte)i );
				marshall( (byte)0 );	// gameOver
				marshall( (byte)0 );	// teamId
			}
			
			sendPacket();
		}
		
		public void sendCreateTable(ServerTable table) throws IOException {	
			byte opcode = 60;
			marshall( opcode );
			marshall( table.id() );
			marshall( table.status() );
			marshall( table.player(0) );	// should have just been created, so only one player, default slot 0
			marshall( table.isRanked() );
			marshall( table.isPrivate() );
			marshall( table.isBigTable() );
			marshall( table.isTeamTable() );
			if (table.isTeamTable()){
				marshall( table.teamSize() );
				marshall( table.isBalancedTable() );				
			}
			marshall( (byte)0 );	// number of table options
			sendPacket();
		}
		
		public void sendJoinTable(short tableId, String username, byte slot, byte teamId) throws IOException {	
			byte opcode = 102;
			marshall( opcode );
			marshall( tableId );
			marshall( username );
			marshall( slot );	// should have just been created, so only one player, default slot 0
			marshall( teamId );
			sendPacket();
		}
		
		public void sendFullTable(ServerTable table) throws IOException {	
			byte opcode = 101;
			marshall( opcode );
			marshall( table.id() );
			marshall( table.status() );
			marshall( table.isRanked() );
			marshall( table.isPrivate() );
			marshall( table.isBigTable() );
			marshall( table.isTeamTable() );
			if (table.isTeamTable()) {
				marshall( table.teamSize() );
				marshall( table.isBalancedTable() );				
			}
			for (int i=0; i<table.numPlayerSlots(); i++) {
				marshall( table.player(i) );
			}
			marshall( (byte)0 );	// number of table options
			sendPacket();
		}
		
		public void sendPowerup(byte powerupType, byte toSlot, byte b1, short gameSession, byte b2) throws IOException {
			byte opcode1 = 80;
			byte opcode2 = 107;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( powerupType );
			marshall( toSlot );
			marshall( b1 );	// should have just been created, so only one player, default slot 0
			marshall( gameSession );
			marshall( b2 );
			sendPacket();
		}
		
		public void processPackets(final DataInputStream stream) {
			try {
				final byte opcode = stream.readByte();
				switch(opcode){
				case 107:
					receivePowerup();
					break;
				case 20:
					receiveCreateTable();
					break;
				case 21:
					receiveJoinTable();
					break;
				case 30:
					receiveStartGame();
					break;
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
				for (ServerUser user : userManager.users()) {
					sendUser(user);
				}
				
				// Send current table list to client
				for (ServerTable table : tableManager.tables()) {
					sendFullTable(table);
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
	

	
	private class TableCountdownThread extends Thread {
		ServerTable table;
		short countdown; 
		
		public TableCountdownThread(ServerTable table) {
			this.table = table;
			this.countdown = TABLE_COUNTDOWN;
			start();
		}
		
		public void run() {
			try {
				for (int i=0; i<TABLE_COUNTDOWN; i++) {
					broadcastTableStatusChange(table.id(), table.status(), countdown);
					countdown --;
					Thread.sleep(1000);
					if (table.numPlayers() < 2) {	// People left below the limit, we need to stop counting down
						table.setStatus((byte)0);
						broadcastTableStatusChange(table.id(), table.status(), TABLE_COUNTDOWN);
						return;
					}
				}
				// Game is ready to start
				table.setStatus((byte)4);
				broadcastTableStatusChange(table.id(), table.status(), (short)-1);
				broadcastGameStart(table);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}
