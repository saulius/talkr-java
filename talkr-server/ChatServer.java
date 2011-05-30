import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

public class ChatServer implements Runnable {
	public List<ServerDataEvent> queue = new LinkedList<ServerDataEvent>();
	private HashMap<String,SocketChannel> clients = new HashMap<String,SocketChannel>();
	private HashMap<SocketChannel, String> clientMap = new HashMap<SocketChannel, String>();
	private HashMap<String, ArrayList<SocketChannel> > channels = new HashMap<String, ArrayList<SocketChannel> >();
	
	public void processData(TalkrServer server, SocketChannel socket, byte[] data, int count) {
	    String message = new String(data).trim();
	    String recipient, sender, arg, nick;
	    SocketChannel sck = null;
      Arrays.fill(data, (byte)' ');

	    if (!message.startsWith("HANDSHAKE") && unregisteredClient(socket)) {
            say(server, socket, "You dont have a nickname, no other actions can be performed. Please use HANDSHAKE command");
	    } else if (message.startsWith("HANDSHAKE") && hasArguments(message, 1)) {
	        System.out.println(message);
            arg = getArgument(message, 1);
	        if (clients.get(arg) != null) {
	            nick = "svecias" + Math.round(Math.random() * 1000000);
                clients.put(nick, socket);
                clientMap.put(socket, nick);
                say(server, socket, "NOTICE there is already a user with such nickname, yours has been changed to " + nick);
            } else {
                clients.put(arg, socket);
                clientMap.put(socket, arg);
            }
            say(server, socket, getMOTD());

        } else if (message.startsWith("SVERSION")) {
            say(server, socket, "NOTICE talkr server");
        } else if (message.startsWith("JOIN") && hasArguments(message, 2)) {
            arg = getArgument(message, 1);
            if (channels.get(arg) == null) {
                channels.put(arg, new ArrayList<SocketChannel>());
            }
            if (channels.get(arg).indexOf(socket) == -1) {
                channels.get(arg).add(socket);
                for (Iterator it = channels.get(arg).iterator(); it.hasNext(); ) {
                    SocketChannel obj = (SocketChannel)it.next();
                    if (obj != socket) {
                        say(server, obj, "CMSG " + arg + " JOINS: " + clientMap.get(socket));
                    }
                }
                say(server, socket, "SJOIN " + arg);
            } else {
                say(server, socket, "E_JOIN " + arg + " - you are already in this channel");
            }
        } else if (message.startsWith("PART") && hasArguments(message, 1)) {
            arg = getArgument(message, 1);
            if (channels.get(arg) != null) {
                if (channels.get(arg).indexOf(socket) != -1) {
                    for (Iterator it = channels.get(arg).iterator(); it.hasNext(); ) {
                        SocketChannel obj = (SocketChannel)it.next();
                        if (obj != socket) {
                            say(server, obj, "CMSG " + arg + " PARTS: " + clientMap.get(socket));
                        } else {
                            sck = obj;
                        }
                    }
                    channels.get(arg).remove(sck);
                    if (channels.get(arg).size() == 0) {
                        channels.remove(arg);
                    }
                    say(server, socket, "S_PART " + arg);
                } else {
                    say(server, socket, "E_PART " + arg + " - you are not in such channel.");
                }
            } else {
                say(server, socket, "E_PART " + arg + " - you are not in such channel.");
            }
        } else if (message.startsWith("CMSG") && hasArguments(message, 2)) {
            arg = getArgument(message, 1);
            if (channels.get(arg) != null) {
                if (channels.get(arg).indexOf(socket) != -1) {
                    for (Iterator it = channels.get(arg).iterator(); it.hasNext(); ) {
                        SocketChannel obj = (SocketChannel)it.next();
                            say(server, obj, "CMSG " + arg + " " + clientMap.get(socket) + " " + message.split(" ", 3)[2]);
                    }
                } else {
                    say(server, socket, "E_CMSG " + arg);
                }
            } else {
                say(server, socket, "E_CMSG " + arg);
            }
        } else if (message.startsWith("PMSG") && hasArguments(message, 2)) {
            recipient = getArgument(message, 1);
            sender = clientMap.get(socket);
            if (clients.get(recipient) != null) {
                say(server, clients.get(recipient), "PMSG " + sender + " " + message.split(" ",3)[2]);
            } else {
                say(server, socket, "E_PMSG " + recipient);
            }
        } else if (message.startsWith("quit")) {
            say(server, socket, "BYE");
	    } else {
	        say(server, socket, "UNRECOGNIZED COMMAND OR WRONG SYNTAX");
	    }
	}
	
	
	public void say(TalkrServer server, SocketChannel socket, String message) {
	    synchronized(queue) {
            queue.add(new ServerDataEvent(server, socket, message + "\n"));
            queue.notify();
        }
	}

	public boolean unregisteredClient(SocketChannel sock) {
	    return clientMap.get(sock) == null;
	}

	public boolean hasArguments(String data, int argc) {
	    return data.split(" ").length >= argc;
	}

	public String getArgument(String data, int argc) {
	    return data.split(" ")[argc];
	}

	public Integer getAllFrom(String data, int argc) {
	    int found = 0;
	    for (int i = 0; i < argc - 1; i++) {
	        found = data.indexOf(" ", found);
	        if (found != -1) {
	            data = data.substring(found,data.length());
            }
	    }
	    System.out.println(found);

	    return found;
	}
	
	public String getMOTD() {
	    String data = "NOTICE Hi, this is talkr server! \n";
        try {
            FileInputStream fstream = new FileInputStream("motd");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                data += "NOTICE " + line + "\n";
            }
            in.close(); 
        } catch (Exception e){
            System.err.println("Error reading from motd file: " + e.getMessage());
        }

        return data;
	}

	public synchronized void removeDeadClient(TalkrServer server, SocketChannel sock) {
	    String nick = clientMap.get(sock);
	    SocketChannel sck;
	    clients.remove(sock);
	    clientMap.remove(nick);
	    for (Iterator<ArrayList<SocketChannel>> it = channels.values().iterator(); it.hasNext(); ) {
            ArrayList<SocketChannel> chan = it.next();
            if (chan.contains(sock)) {
                chan.remove(chan.indexOf(sock));
                for (Iterator<SocketChannel> iter = chan.iterator(); it.hasNext(); ) {
                    SocketChannel obj = (SocketChannel)iter.next();
                    if (obj != sock) {
                        say(server, obj, "CMSG " + "aaa" + " QUITS: " + nick);
                    } else {
                        sck = obj;
                    }
                }
            }
        }
	}

	public void on_accept(TalkrServer server, SocketChannel socket) {
	}

	public void run() {
		ServerDataEvent dataEvent;

		while(true) {
			synchronized(queue) {
				while(queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				dataEvent = (ServerDataEvent) queue.remove(0);
			}
			dataEvent.server.send(dataEvent.socket, dataEvent.data);
		}
	}
}
