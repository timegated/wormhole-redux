import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	void broadcastUser(ServerUser user) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendUser(user));
		}
	}
	
	void broadcastUserLogout(ServerUser user) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendUserLogout(user));
		}
	}
	
	void broadcastCreateTable(ServerTable table) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendFullTable(table));
		}
	}
	
	void broadcastJoinTable(ServerTable table, String username, byte slot, byte teamId) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendJoinTable(table, username, slot, teamId));
		}
	}
	
	void broadcastLeaveTable(short tableId, String username) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendLeaveTable(tableId, username));
		}
	}
	
	void broadcastTableStatusChange(short tableId, byte status, short countdown) {
		for (ServerThread client : this.clients){
			client.sendMessages.add(()->client.sendTableStatusChange(tableId, status, countdown));
		}
	}
	
	void broadcastTeamChange(ServerTable table, byte slot, byte teamId) {
		for (ServerThread client : this.clients){
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendTeamChange(slot, teamId));
			}
		}
	}
	
	void broadcastGameStart(ServerTable table) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendGameStart(table));
			}
		}
	}
	
	void broadcastPowerup(ServerTable table, byte powerupType, byte toSlot, byte b1, short gameSession, byte b2) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendPowerup(powerupType, toSlot, b1, gameSession, b2));
			}
		}
	}
	
	void broadcastPlayerState(ServerTable table, short gameSession, byte slot, short healthPerc, Byte[] powerups, byte shipType) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendPlayerState(gameSession, slot, healthPerc, powerups, shipType));
			}
		}
	}
	
	void broadcastPlayerEvent(ServerTable table, short gameSession, String eventString) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendPlayerEvent(gameSession, eventString));
			}
		}
	}
	
	void broadcastGameOver(ServerTable table, short gameSession, byte deceasedSlot, byte killerSlot) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendGameOver(gameSession, deceasedSlot, killerSlot));
			}
		}
	}
	
	void broadcastGameEnd(ServerTable table) {
		for (ServerThread client : this.clients) {
			if (table.hasPlayer(client.user().username())) {
				client.sendMessages.add(()->client.sendGameEnd(table));
			}
		}
	}
	
	void broadcastGlobalMessage(String username, String message) {
		boolean isForLobby = true;
		for (ServerThread client : this.clients) {
			client.sendMessages.add(()->client.sendGlobalMessage(username, message, isForLobby));
		}
	}
	
	void broadcastGlobalMessage(ServerTable table, String username, String message) {
		boolean isForLobby = false;
		for (ServerThread client : this.clients) {
			if (client.user().table() == table) {
				client.sendMessages.add(()->client.sendGlobalMessage(username, message, isForLobby));
			}
		}
	}
	
	void broadcastPrivateMessage(String fromUser, String toUser, String message) {
		for (ServerThread client : this.clients) {
			if (client.user().username().equals(toUser)) {
				client.sendMessages.add(()->client.sendPrivateMessage(fromUser, toUser, message));
			}
			if (client.user().username().equals(fromUser)) {
				client.sendMessages.add(()->client.sendPrivateMessage(fromUser, toUser, message));
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
		private long nextTime;
		public Queue<Runnable> sendMessages = new ConcurrentLinkedQueue<>();
		
	    public void marshall(boolean b) {
	    	try {
				this.pw.getStream().writeByte((byte)(b ? 1 : 0));
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void marshall(byte b) {
	    	try {
				this.pw.getStream().writeByte(b);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void marshall(short s) {
	    	try {
				this.pw.getStream().writeShort(s);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void marshall(int i) {
	    	try {
				this.pw.getStream().writeInt(i);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void marshall(String s) {
	    	try {
				this.pw.getStream().writeUTF(s);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void sendPacket() {
	    	try {
	    		this.pw.sendPacket();
	    	}
	    	catch (IOException e) {
	    		// Do not need to do anything here?
	    		// We need this because client could be closed, but not yet removed from server's list.
	    	}
	    }
				
		public ServerUser user() {
			return user;
		}
		
		public void handleGameEnd(ServerTable table) throws IOException {
			table.increaseWinCounts();
			broadcastGameEnd(table);
			table.setStatus(TableStatus.GAMEOVER);
			broadcastTableStatusChange(table.id(), table.status(), (short)-1);
			new TableTransitionThread(table, table.status());
		}
		
		public void handleUserLogout() throws IOException {
			userManager.removeUser(user());
			clients.remove(this);
			if (user().table() != null) {
				receiveLeaveTable();
			}
			broadcastUserLogout(user());
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
				handleGameEnd(table);
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
				broadcastTableStatusChange(table.id(), TableStatus.DELETE, (short)-1);	
				tableManager.removeTable(table);
			}
			if (table.status() == TableStatus.PLAYING && table.gameOver()) {
				handleGameEnd(table);
			}
		}
		
		public void receiveSay() throws IOException {	
			final DataInputStream stream = this.pr.getStream();
			String text = stream.readUTF();
			broadcastGlobalMessage(user().username(), text);
		}
		
		public void receiveTableSay() throws IOException {	
			final DataInputStream stream = this.pr.getStream();
			String text = stream.readUTF();
			if (user().table() != null) {
				broadcastGlobalMessage(user().table(), user().username(), text);
			}
		}
		
		public void receiveWhisper() throws IOException {	
			final DataInputStream stream = this.pr.getStream();
			
			String user = stream.readUTF();
			String text = stream.readUTF();
			
			broadcastPrivateMessage(user().username(), user, text);
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
		
		public void sendGlobalMessage(String username, String message, boolean isForLobby) {
			byte opcode = isForLobby ? (byte)5 : (byte)18;
			marshall( opcode );
			marshall( username );
			marshall( message );
			sendPacket();
		}	
		
		public void sendPrivateMessage(String fromUser, String toUser, String message) {
			byte opcode = 6;
			marshall( opcode );
			marshall( fromUser );
			marshall( toUser );
			marshall( message );
			sendPacket();
		}
		
		public void sendTableStatusChange(short tableId, byte status, short countdown) {
			byte opcode = 66;
			marshall( opcode );
			marshall( tableId );
			marshall( status );
			if (status == 3) {	// countdown phase
				marshall( countdown );
			}
			sendPacket();
		}	
		
		public void sendTeamChange(byte slot, byte teamId)  {
			byte opcode = 80;
			byte opcode2 = 121;

			marshall( opcode );
			marshall( opcode2 );
			marshall( slot );
			marshall( teamId );
			sendPacket();
		}	
		
		public void sendLoginSucceed() {
			byte opcode = 1;
			marshall( opcode );
			marshall( user().userId() );
			marshall( user().totalCredits() );
			marshall( user().subscriptionLevel() );
			marshall( user().username() );
			sendPacket();
		}
		
		public void sendLoginFailed() {
			byte opcode = 0;
			marshall( opcode );
			marshall( "Username already taken" );
			sendPacket();
		}	
		
		public void sendUser(ServerUser user) {	
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
		
		public void sendUserLogout(ServerUser user) {	
			byte opcode = 14;
			marshall( opcode );
			marshall( user.username() );
			sendPacket();
		}
		
		public void sendGameStart(ServerTable table) {	
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
		
		public void sendJoinTable(ServerTable table, String username, byte slot, byte teamId) {	
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
		
		public void sendIncorrectTablePassword() {	
			byte opcode = 103;
			marshall( opcode );
			sendPacket();
		}
		
		public void sendLeaveTable(short tableId, String username) {	
			byte opcode = 65;
			marshall( opcode );
			marshall( tableId );
			marshall( username );
			sendPacket();
		}
		
		public void sendTableWins(ServerTable table) {	
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
		
		public void sendFullTable(ServerTable table) {	
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
		
		public void sendPowerup(byte powerupType, byte toSlot, byte b1, short gameSession, byte b2) {
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
		
		public void sendPlayerState(short gameSession, byte slot, short healthPerc, Byte[] powerups, byte shipType) {
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
		
		public void sendPlayerEvent(short gameSession, String eventString) {
			byte opcode1 = 80;
			byte opcode2 = 109;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( eventString );
			sendPacket();
		}
		
		public void sendGameOver(short gameSession, byte deceasedSlot, byte killerSlot) {
			byte opcode1 = 80;
			byte opcode2 = 110;
			marshall( opcode1 );
			marshall( opcode2 );
			marshall( deceasedSlot );
			marshall( killerSlot );
			sendPacket();
		}
		
		public void sendGameEnd(ServerTable table) {
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
				case 0:
					// NOOP, heartbeat
					break;
				case 1:
					handleUserLogout();
					break;
				case 5:
					receiveSay();
					break;
				case 6:
					receiveWhisper();
					break;
				case 18:
					receiveTableSay();
					break;
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
				
				if (userManager.usernameTaken(user().username())){
					sendLoginFailed();
					clients.remove(this);
					return;
				}
				
				sendLoginSucceed();
				
				// Send current user list to client
				for (ServerUser user : userManager.users()) {
					if (user != null) {
						sendUser(user);
					}
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
				this.nextTime = System.currentTimeMillis() + GameNetLogic.NOOP_DURATION*2;
				while (true) {
					try {
						while (!sendMessages.isEmpty()) {
							sendMessages.poll().run();
						}
						if (stream.available() > 0) {
							short numBytes = stream.readShort();	// packetStreamWriter/Reader let first short be size, we do not use that here
							if (numBytes > 0) {
								processPackets(stream);
							}
							this.nextTime = System.currentTimeMillis() + GameNetLogic.NOOP_DURATION*2;
						}
						else {
							if (System.currentTimeMillis() > this.nextTime) {	// no communication from client
								handleUserLogout();
								break;
							}
						}
					} catch (EOFException e) {
						// No more communication to client
						handleUserLogout();
						break;
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
			}
		}
	}
}
