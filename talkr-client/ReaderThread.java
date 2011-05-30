import java.io.*;

class ReaderThread extends Thread {

    private TalkrClient talkr;
    private boolean running;

    public ReaderThread(TalkrClient talkr) {
        this.talkr = talkr;
    }

    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        running = true;
        talkr.send("HANDSHAKE " + this.talkr.nick);
        while (running) {
            try {
                String s;
                System.out.flush();
                s = br.readLine();
                if (s.length() > 0) talkr.send(s);
                if (s.equals("quit")) running = false;
            }
            catch (IOException e) {
                running = false;
            }
        }
        talkr.shutdown();
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
