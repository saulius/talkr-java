/**
 *  Talkr server, event based
 */
 
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class TalkrServer implements Runnable {
	private InetAddress address;
	private int port;

	private ServerSocketChannel serverChannel;

	private Selector selector;

	private ByteBuffer buffer = ByteBuffer.allocate(4096);

	private ChatServer worker;

	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	private Map<SocketChannel, List<ByteBuffer> > pendingData = new HashMap<SocketChannel, List<ByteBuffer> >();

	public TalkrServer(InetAddress address, int port, ChatServer worker) throws IOException {
		this.address = address;
		this.port = port;
		this.selector = this.initSelector();
		this.worker = worker;
	}

	public void send(SocketChannel socket, String data) {
    send(socket, data.getBytes());
	}

	public void send(SocketChannel socket, byte[] data) {
		synchronized (this.pendingChanges) {
		    this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			synchronized (this.pendingData) {
				List<ByteBuffer> queue = this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList<ByteBuffer>();
					this.pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}

		this.selector.wakeup();
	}

	public void run() {
		while (true) {
			try {
				synchronized (this.pendingChanges) {
					Iterator changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
                            if (change.socket.isConnected()) {
						        key.interestOps(change.ops);
                            } else {
                                worker.removeDeadClient(this, change.socket);
                            }
						}
					}
					this.pendingChanges.clear();
				}

				this.selector.select();

				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey event = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!event.isValid()) {
						continue;
					}

					if (event.isAcceptable()) {
						this.accept(event);
					} else if (event.isReadable()) {
						this.read(event);
					} else if (event.isWritable()) {
						this.write(event);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();
		socketChannel.configureBlocking(false);

		socketChannel.register(this.selector, SelectionKey.OP_READ);
		this.worker.on_accept(this, socketChannel);
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		int num;

		this.buffer.clear();

		try {
			num = socketChannel.read(this.buffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();
			return;
		}

		if (num == -1) {
			key.channel().close();
			key.cancel();
			return;
		}
		this.worker.processData(this, socketChannel, this.buffer.array(), num);
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private Selector initSelector() throws IOException {
		Selector ss = SelectorProvider.provider().openSelector();
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress(this.address, this.port);
		serverChannel.socket().bind(isa);
		serverChannel.register(ss, SelectionKey.OP_ACCEPT);
		return ss;
	}

	public static void main(String[] args) {
	    if (args.length > 0) {
    		try {
    			ChatServer worker = new ChatServer();
    			new Thread(worker).start();
    			new Thread(new TalkrServer(null, Integer.valueOf(args[0]), worker)).start();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
	    } else {
	        System.out.println("Usage:");
	        System.out.println("java TalkrServer <port>");
	    }
	}
}
