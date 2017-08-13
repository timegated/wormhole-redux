import java.net.*;
import java.io.*;

public class Network extends ByteArrayInputStream
{
    public static final byte LOGIN_TYPE_REGISTERED = 0;
    public static final byte LOGIN_TYPE_GUEST = 1;
    public static final byte LOGIN_TYPE_AUTO_GUEST = 3;
    private Socket m_socket;
    private PacketStreamWriter m_writer;
    private PacketStreamReader m_reader;
    private DataInputStream m_dataStream;
    public boolean m_bConnected;
    
    synchronized void joinTable(final int n, final String s) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(21);
            stream.writeShort((short)n);
            stream.writeUTF((s == null) ? "" : s);
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    synchronized void whisper(final String s, final String s2) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(6);
            stream.writeUTF(s);
            stream.writeUTF(s2);
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    public void sendPacket() throws Exception {
        this.m_writer.sendPacket();
    }
    
    private void sendGeneric(final byte b, final short n) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(b);
            if (n != -2) {
                stream.writeShort(n);
            }
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    public Network() {
        super(new byte[9064]);
        this.m_dataStream = new DataInputStream(this);
    }
    
    public String login(final int n, final short n2, final short n3, final String s, final String s2, final boolean b, final String s3, final int n4) {
        try {
            (this.m_socket = new Socket(InetAddress.getByName(s3), n4)).setSoTimeout(10000);
        }
        catch (Exception ex) {
            System.out.println(ex);
            return "Could not connect to server";
        }
        try {
            this.m_socket.setSoTimeout(0);
            this.m_reader = new PacketStreamReader(this.m_socket.getInputStream());
            this.m_writer = new PacketStreamWriter(this.m_socket.getOutputStream());
            final DataOutputStream stream = this.m_writer.getStream();
            stream.writeByte(b ? 1 : 0);
            stream.writeUTF(s);
            stream.writeUTF(s2);
            stream.writeInt(n);
            stream.writeShort(n2);
            stream.writeShort(n3);
            this.m_writer.sendPacket();
            this.m_bConnected = true;
        }
        catch (Exception ex2) {
            return "Network error.";
        }
        return null;
    }
    
    public DataOutput getStream(final int n) {
        final DataOutputStream stream = this.m_writer.getStream();
        try {
            stream.writeShort(n);
        }
        catch (Exception ex) {}
        return stream;
    }
    
    synchronized void sendNoop() {
        this.sendGeneric((byte)0, (short)(-2));
    }
    
    synchronized void createTable(final String s, final boolean b, final boolean b2, final boolean b3, final int n, final boolean b4, final String[][] array) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(20);
            stream.writeByte((byte)(byte)(b ? 1 : 0));
            final boolean b5 = s.length() > 0;
            stream.writeByte((byte)(byte)(b5 ? 1 : 0));
            if (b5) {
                stream.writeUTF(s);
            }
            stream.writeByte((byte)(byte)(b2 ? 1 : 0));
            stream.writeByte((byte)(byte)(b3 ? 1 : 0));
            if (b3) {
                stream.writeByte((byte)n);
                stream.writeByte(b4 ? 1 : 0);
            }
            if (array != null) {
                stream.writeByte((byte)array.length);
                for (int i = 0; i < array.length; ++i) {
                    if (array[i] != null) {
                        stream.writeUTF(array[i][0]);
                        stream.writeUTF(array[i][1]);
                    }
                }
            }
            else {
                stream.writeByte(0);
            }
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    void createTable(final String s, final boolean b) {
        this.createTable(s, b, false, false, 0, false, null);
    }
    
    public void disconnect() {
        this.m_bConnected = false;
        try {
            this.m_reader.m_iStream.close();
        }
        catch (Exception ex) {}
        this.m_reader = null;
        try {
            this.m_writer.m_oStream.close();
        }
        catch (Exception ex2) {}
        this.m_writer = null;
        try {
            this.m_socket.close();
        }
        catch (Exception ex3) {}
        this.m_socket = null;
    }
    
    synchronized void tableSay(final String s) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(18);
            stream.writeUTF(s);
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    public DataInputStream readPacket() {
        try {
            super.count = this.m_reader.readPacket(super.buf) + 2;
            super.pos = 0;
        }
        catch (IOException ex) {
            return null;
        }
        return this.m_dataStream;
    }
    
    synchronized void changeTeams(final byte b) {
        this.sendGeneric((byte)40, b);
    }
    
    synchronized void submitCredits(final int n, final int n2) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(27);
            stream.writeShort((short)n);
            stream.writeInt(n2);
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    synchronized void startGame(final int n) {
        this.sendGeneric((byte)30, (short)n);
    }
    
    synchronized void say(final String s) {
        final DataOutput stream = this.getStream(0);
        try {
            stream.writeByte(5);
            stream.writeUTF(s);
            this.sendPacket();
        }
        catch (Exception ex) {}
    }
    
    synchronized void listUserNames() {
        this.sendGeneric((byte)9, (short)(-2));
    }
    
    synchronized void listTables() {
        this.sendGeneric((byte)50, (short)(-2));
    }
    
    synchronized void leaveTable() {
        this.sendGeneric((byte)22, (short)(-2));
    }
}
