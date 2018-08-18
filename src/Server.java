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
	
	void broadcastJoinTable(ServerTable table, String username, byte slot, byte teamId) throws IOException {
		for (ServerThread client : this.clients){
			client.sendJoinTable(table, username, slot, teamId);
		}
	}
	
	void broadcastLeaveTable(short tableId, String username) throws IOException {
		for (ServerThread client : this.clients){
			client.sendLeaveTable(tableId, username);
		}
	}
	
	void broadcastTableStatusChange(short tableId, byte status, short countdown) throws IOException {
		for (ServerThread client : this.clients){
			client.sendTableStatusChange(tableId, status, countdown);
		}
	}
	
	void broadcastTeamChange(ServerTable table, byte slot, byte teamId) throws IOException {
		for (ServerThread client : this.clients){
			if (table.hasPlayer(client.user().username())) {
				client.sendTeamChange(slot, teamId);
			}
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
	
	void broadcastPlayerState(ServerTable table, short gameSession, byte slot, short healthPerc, Byte[] powerups, byte shipType) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendPlayerState(gameSession, slot, healthPerc, powerups, shipType);
			}
		}
	}
	
	void broadcastPlayerEvent(ServerTable table, short gameSession, String eventString) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendPlayerEvent(gameSession, eventString);
			}
		}
	}
	
	void broadcastGameOver(ServerTable table, short gameSession, byte deceasedSlot, byte killerSlot) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendGameOver(gameSession, deceasedSlot, killerSlot);
			}
		}
	}
	
	void broadcastGameEnd(ServerTable table) throws IOException {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendGameEnd(table);
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
		
		public void receivePlayerState() throws IOException {
			final DataInputStream stream = this.pr.getStream();

			short	gameSession		= stream.readShort();
			short	healthPerc		= stream.readShort();
			byte	numPowerups		= stream.readByte();
			
			Byte[] powerups = new Byte[numPowerups];
	        for (int i = 0; i < numPowerups; ++i) {
	        	powerups[i] = stream.readByte();
	        }
	        
	        byte 		shipType 		= stream.readByte();
	        boolean		damagedByPlayer	= stream.readByte() == 1;
	        if (damagedByPlayer) {
		        String 	damagingPlayer 	= stream.readUTF();
		        byte	damagingPowerup = stream.readByte();
		        byte	lostHealth		= stream.readByte();
	        }
	        
	        broadcastPlayerState(user().table(), gameSession, user().slot(), healthPerc, powerups, shipType);
		}
		
		public void receivePlayerDeath() throws IOException {
			final DataInputStream stream = this.pr.getStream();

			short	gameSession		= stream.readShort();
			byte	killedBy		= stream.readByte();
			
			ServerTable table = user().table();			
	        broadcastGameOver(table, gameSession, user().slot(), killedBy);
				        
			user().setAlive(false);
			if (table.gameOver()) {
				table.increaseWinCounts();
				broadcastGameEnd(table);
				table.setStatus(TableStatus.GAMEOVER);
				broadcastTableStatusChange(table.id(), table.status(), (short)-1);
				new TableTransitionThread(table, table.status());
			}
		}
		
		public void receivePlayerEvent() throws IOException {
			final DataInputStream stream = this.pr.getStream();

			short	gameSession		= stream.readShort();
			String	eventString		= stream.readUTF();
	        
	        broadcastPlayerEvent(user().table(), gameSession, eventString);
		}
		
		/*
		 * Do nothing here, not sure what receiveCredits is for. We will probably never use.
		 */
		public void receiveCredits() throws IOException {
			final DataInputStream stream = this.pr.getStream();

			short	credits			= stream.readShort();
			int		timeElapsed		= stream.readInt();
		}
		
		public void receiveTeamChange() throws IOException {
			final DataInputStream stream = this.pr.getStream();

			byte teamId = (byte)stream.readShort();	// sendGeneric sends as a short... not sure why use sendGeneric
			user().setTeamId(teamId);
			broadcastTeamChange(user().table(), user.slot(), teamId);
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
			
			byte boardSize = 0, numStringPairs;
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
				boardSize = stream.readByte();
				isBalancedTable	= (stream.readByte()==1);
			}
			
			numStringPairs = stream.readByte();
			if (numStringPairs > 0){	// pretty sure this is always 0
				for (int i = 0; i<numStringPairs; i++){
					String s1 = stream.readUTF();
					String s2 = stream.readUTF();
				}
			}
			
			ServerTable table = new ServerTable(isRanked, password, isBigTable, isTeamTable, boardSize, isBalancedTable);
			byte slot = table.addUser(user().username());
			table.addUser(user());
			user().setSlot(slot);
			user().setTable(table);
			user().setTeamId(table.isTeamTable() ? Team.GOLDTEAM : Team.NOTEAM);
			addTable(table);
			broadcastCreateTable(table);
		}
		
		public void receiveJoinTable() throws IOException {			
			final DataInputStream stream = this.pr.getStream();
			
			short 		tableId		= stream.readShort();
			String 		password	= stream.readUTF();
			ServerTable table 		= tableManager.getTable(tableId);
			byte 		teamId		= table.isTeamTable() ? Team.GOLDTEAM : Team.NOTEAM;
			
			if (!table.isPrivate() || password.equals(table.password())) {
				byte slot = table.addUser(user().username());
				table.addUser(user());
				user().setSlot(slot);
				user().setTable(table);
				user().setTeamId(teamId);
				broadcastJoinTable(table, user().username(), slot, teamId);
				sendTableWins(table);
			}
			else {	// user entered the wrong password to join the table
				sendIncorrectTablePassword();
			}
		}
		
		public void receiveLeaveTable() throws IOException {	
			ServerTable table = user().table();
			table.removeUser(user());
			broadcastLeaveTable(table.id(), user().username());
			user().setTable(null);
			if (table.numPlayers() <= 0) {
				broadcastTableStatusChange(table.id(), (byte)1, (short)-1);	
				tableManager.removeTable(table);
			}
		}
		
		public void receiveStartGame() throws IOException, InterruptedException {
			final DataInputStream stream = this.pr.getStream();
			
			short tableId = stream.readShort();
			
			ServerTable table = tableManager.getTable(tableId);
			
			if (table.status() != TableStatus.COUNTDOWN && table.numPlayers() > 1) {
				table.setStatus(TableStatus.COUNTDOWN);
				new TableTransitionThread(table, table.status());
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
		
		public void sendTeamChange(byte slot, byte teamId) throws IOException {
			byte opcode = 80;
			byte opcode2 = 121;

			marshall( opcode );
			marshall( opcode2 );
			marshall( slot );
			marshall( teamId );
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
			for (ServerUser user : table.users()) {
				if (user != null) {
					marshall( user.username() );
					marshall( user.slot() );
					marshall( (byte)1 );	// gameOver, 1 is not gameOver because checks byte==0 in WormholeModel:setPlayers
					marshall( user.teamId() );	// teamId
				}
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
				marshall( table.boardSize() );
				marshall( table.isBalancedTable() );				
			}
			marshall( (byte)0 );	// number of table options
			sendPacket();
		}
		
		public void sendJoinTable(ServerTable table, String username, byte slot, byte teamId) throws IOException {	
			byte opcode = 102;
			marshall( opcode );
			marshall( table.id() );
			marshall( username );
			marshall( slot );
			marshall( teamId );
			// if team table, then we need to send to the new player the teamIds of the other players
			if (table.isTeamTable() && user.username() == username) {
				for (ServerUser user : table.users()) {
					if (user != null) {
						marshall(user.teamId());
					}
				}
			}
			sendPacket();
		}
		
		public void sendIncorrectTablePassword() throws IOException {	
			byte opcode = 103;
			marshall( opcode );
			sendPacket();
		}
		
		public void sendLeaveTable(short tableId, String username) throws IOException {	
			byte opcode = 65;
			marshall( opcode );
			marshall( tableId );
			marshall( username );
			sendPacket();
		}
		
		public void sendTableWins(ServerTable table) throws IOException {	
			byte opcode = 80;
			byte opcode2 = 120;
			marshall( opcode );
			marshall( opcode2 );
			marshall( table.numPlayers() );
			
			for (ServerUser user : table.users()) {
				if (user != null) {
					marshall( user.slot() );
					marshall( table.winCountOf(user.slot()) );
				}
			}
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
				marshall( table.boardSize() );
				marshall( table.isBalancedTable() );				
			}
			for (ServerUser user : table.users()) {
				marshall( user != null ? user.username() : "" );
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
		
		public void sendPlayerState(short gameSession, byte slot, short healthPerc, Byte[] powerups, byte shipType) throws IOException {
			byte opcode1 = 80;
			byte opcode2 = 106;
			byte numPowerups = (byte)powerups.length;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( gameSession );
			marshall( slot );
			marshall( healthPerc );
			marshall( numPowerups );
			for (byte i=0; i<numPowerups; i++) {
				marshall( powerups[i] );
			}
			marshall( shipType );
			sendPacket();
		}
		
		public void sendPlayerEvent(short gameSession, String eventString) throws IOException {
			byte opcode1 = 80;
			byte opcode2 = 109;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( eventString );
			sendPacket();
		}
		
		public void sendGameOver(short gameSession, byte deceasedSlot, byte killerSlot) throws IOException {
			byte opcode1 = 80;
			byte opcode2 = 110;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( deceasedSlot );
			marshall( killerSlot );
			sendPacket();
		}
		
		public void sendGameEnd(ServerTable table) throws IOException {
			byte opcode1 = 80;
			byte opcode2 = table.isTeamTable() ? (byte)112 : (byte)111;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( table.winnerSlot() );
			sendPacket();
		}
		
		public void processPackets(final DataInputStream stream) {
			try {
				final byte opcode = stream.readByte();
				switch(opcode){
				case 106:
					receivePlayerState();
					break;
				case 107:
					receivePowerup();
					break;
				case 109:
					receivePlayerEvent();
					break;
				case 110:
					receivePlayerDeath();
					break;
				case 20:
					receiveCreateTable();
					break;
				case 21:
					receiveJoinTable();
					break;
				case 22:
					receiveLeaveTable();
					break;
				case 27:
					receiveCredits();
					break;
				case 30:
					receiveStartGame();
					break;
				case 40:
					receiveTeamChange();
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
					if (table != null) {
						sendFullTable(table);
					}
				}
				
				// Add user and broadcast to all clients
				addUser(this.user);
				broadcastUser(this.user);
				
				// Start processing packets from client
				final DataInputStream stream = pr.getStream();
				while (true) {
					short numBytes = stream.readShort();	// packetStreamWriter/Reader let first short be size, we do not use that here
					if (numBytes > 0) {
						processPackets(stream);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
	

	
	private class TableTransitionThread extends Thread {
		ServerTable table;
		short countdown; 
		byte startStatus;
		
		public TableTransitionThread(ServerTable table, byte startStatus) {
			this.table = table;
			this.countdown = TABLE_COUNTDOWN;
			this.startStatus = startStatus;
			start();
		}
		
		
		public void run() {
			if (startStatus == 3) {
				countDownTransition();
			}
			else if (startStatus == 5) {
				endGameTransition();
			}
		}
		
		public void endGameTransition() {
			try {
				Thread.sleep(3000);
				table.setStatus(TableStatus.IDLE);
				broadcastTableStatusChange(table.id(), table.status(), (short)-1);		
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void countDownTransition() {
			try {
				for (int i=0; i<TABLE_COUNTDOWN; i++) {
					broadcastTableStatusChange(table.id(), table.status(), countdown);
					countdown --;
					Thread.sleep(1000);
					if (table.numPlayers() < 2) {	// People left below the limit, we need to stop counting down
						table.setStatus(TableStatus.IDLE);
						broadcastTableStatusChange(table.id(), table.status(), TABLE_COUNTDOWN);
						return;
					}
				}
				// Game is ready to start
				table.setStatus(TableStatus.PLAYING);
				table.setPlayersAlive();
				broadcastTableStatusChange(table.id(), table.status(), (short)-1);
				broadcastGameStart(table);
				
				// artificial game end
//				Thread.sleep(4000);
//				broadcastGameOver(table, (short)0, (byte)0, (byte)1);
//				broadcastGameOver(table, (short)0, (byte)1, (byte)2);
//				table.setStatus(TableStatus.GAMEOVER);
//				broadcastGameEnd(table, (byte)2);
//				broadcastTableStatusChange(table.id(), table.status(), (short)-1);
//				new TableTransitionThread(table, table.status());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
