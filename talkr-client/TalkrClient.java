import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.Arrays;

public class TalkrClient extends Thread {
    private ByteBuffer writeBuffer;
    private ByteBuffer readBuffer;
    private SocketChannel channel;
    private String host;
    private int port;
    public String nick;
    private Selector selector;
    private ReaderThread it;
    private boolean running;

    TalkrClient(String host, int port, String nick) { 
        this.host = host;
        this.port = port;
        this.nick = nick;
        this.writeBuffer = ByteBuffer.allocate(4096);
        this.readBuffer = ByteBuffer.allocate(4096);
    }

    public void run() {
        connect();
        it = new ReaderThread(this);
        it.start();

        running = true;
        while (running) {
            read();
        }
    }

    private void connect() {
        try {
            selector = Selector.open();
            InetAddress addr = InetAddress.getByName(this.host);
            channel = SocketChannel.open(new InetSocketAddress(addr, this.port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ, new StringBuffer());
        } catch (Exception e) {}
    }

    private void read() {
        try {
            selector.select();

            Set readyKeys = selector.selectedKeys();

            Iterator i = readyKeys.iterator();
            while (i.hasNext()) {
                SelectionKey key = (SelectionKey) i.next();
                i.remove();
                SocketChannel channel = (SocketChannel) key.channel();
                Arrays.fill(readBuffer.array(), (byte)' ');
                readBuffer.clear();

                long nbytes = channel.read(readBuffer);

                if (nbytes == -1) {
                    channel.close();
                    shutdown();
                    it.shutdown();
                } else {
                    String messages = new String(readBuffer.array()).trim();
                    String[] lines = messages.split("\n");
                    for(int iter = 0; iter < lines.length; iter++) {
                        String message = lines[iter];
                        String command = getArgument(message, 0);

                        if (command.equals("NOTICE")) {
                            System.out.println("[!] " + getFull(message));
                        } else if (command.equals("PMSG")) {
                            System.out.println("[>] <" + getArgument(message, 1) + "> " + message.split(" ", 3)[2]);
                        } else if (command.equals("SJOIN")) {
                            System.out.println("[J] #" + getArgument(message, 1));
                        } else if (command.equals("S_PART")) {
                            System.out.println("[P] #" + getArgument(message, 1));
                        } else if (command.equals("S_CMSG")) {
                        } else if (command.equals("S_PMSG")) {
                        } else if (command.equals("CMSG")) {
                            System.out.println("[>] #" + getArgument(message, 1) + " <" + getArgument(message, 2) + "> " + message.split(" ", 4)[3]);
                        } else if (command.equals("E_PART")) {
                            System.out.println("[E] Taves nera tokiam kanale.");
                        } else if (command.equals("E_PMSG")) {
                            System.out.println("[E] Nera tokio vartotojo");
                        } else if (command.equals("E_CMSG")) {
                            System.out.println("[E] Neesi tokiam kanale.");
                        } else {
                            System.out.println("[E] " + message);
                        }
                    }
                    System.out.print("> ");
                }
            }
        }
        catch (Exception e) {}
    }

    public void send(String mesg) {
        writeBuffer.clear();
        writeBuffer.put(mesg.getBytes());
        writeBuffer.flip();
        write(channel, writeBuffer);
    }

    private void write(SocketChannel channel, ByteBuffer writeBuffer) {
        long nbytes = 0;
        long toWrite = writeBuffer.remaining();

        try {
            while (nbytes != toWrite) {
                nbytes += channel.write(writeBuffer);
            }
        } catch (Exception e) {}

        writeBuffer.rewind();
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    public String getArgument(String data, int argc) {
        return data.split(" ")[argc];
    }

    public String getFull(String data) {
        return data.substring(data.indexOf(" "), data.length());
    }

    public static void main(String args[]) {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("java TalkrClient <host> <port> <nick>");
            return;
        }
        TalkrClient cc = new TalkrClient(args[0], Integer.valueOf(args[1]), args[2]);
        cc.start();
    }
}
