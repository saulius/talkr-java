import java.nio.channels.SocketChannel;

class ServerDataEvent {
	public TalkrServer server;
	public SocketChannel socket;
	public byte[] data;
	
	public ServerDataEvent(TalkrServer server, SocketChannel socket, String data) {
	    this(server,socket,data.getBytes());
	}
	
	public ServerDataEvent(TalkrServer server, SocketChannel socket, byte[] data) {
		this.server = server;
		this.socket = socket;
		this.data = data;
	}
}