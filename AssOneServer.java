import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssOneServer {
    private static final int PORT = 2695;

    public static void main(String[] args) throws IOException {        
        try {
            CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();

            SocketAddress localPort = new InetSocketAddress(PORT);

            ServerSocketChannel tcpServer = ServerSocketChannel.open();
            tcpServer.socket().bind(localPort);

            DatagramChannel udpServer = DatagramChannel.open();
            udpServer.socket().bind(localPort);

            tcpServer.configureBlocking(false);
            udpServer.configureBlocking(false);

            Selector selector = Selector.open();

            tcpServer.register(selector, SelectionKey.OP_ACCEPT);
            udpServer.register(selector, SelectionKey.OP_READ);

            ByteBuffer receiveBuffer = ByteBuffer.allocate((int)Math.pow(2,20));

            for (;;) {
                try {
                    selector.select();

                    Set keys = selector.selectedKeys();

                    for (Iterator i = keys.iterator(); i.hasNext();) {
                        SelectionKey key = (SelectionKey) i.next();
                        i.remove();

                        Channel c = (Channel) key.channel();

                        if (key.isAcceptable() && c == tcpServer) {
                            SocketChannel client = tcpServer.accept();
                            System.out.println("Listening on TCP");
                            if (client != null) {
                                Socket clientSocket = client.socket();

                               BufferedReader in = 
                                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                                String inputLine;

                                inputLine = in.readLine();

                                switch (inputLine) {
                                    case ("echo") :  
                                        //System.out.println(inputLine); 
                                        System.out.println("Echoing TCP");                                
                                        tcpEchoTP(clientSocket);
                                        inputLine = "";
                                        break;
                                    case ("throughput") :                                    
                                        System.out.println("TCP Throughput");
                                        tcpEchoTP(clientSocket);
                                        break;
                                    case ("interaction") :
                                        System.out.println("TCP Message Interaction.");
                                        tcpInteraction(clientSocket);
                                        break;
                                }
                                
                                client.close();
                                System.out.println("Stopped listening on TCP");                            
                            }
                        } else if (key.isReadable() && c == udpServer) {
                            SocketAddress clientAddress = udpServer.receive(receiveBuffer);
                            byte[] buffer = receiveBuffer.array();
                            DatagramPacket hdr = new DatagramPacket(buffer, buffer.length);

                            System.out.println("Listening on UDP");
                            if (clientAddress != null) {
                                //DatagramSocket clientSocket = udpServer.socket();
                                //clientSocket.receive(hdr);

                                String hdrString = new String(hdr.getData(), StandardCharsets.UTF_8);
                                //System.out.println(hdrString);

                                switch (hdrString.trim()) {
                                    case ("echo") : 
                                        System.out.println("Echoing UDP");                           
                                        udpEchoTP(udpServer, clientAddress, selector);
                                        hdrString = "";
                                        break;
                                    case ("throughput") :
                                        System.out.println("UDP Throughput");
                                        udpEchoTP(udpServer, clientAddress, selector);
                                        break;
                                }

                                receiveBuffer.clear();
                                receiveBuffer.put(new byte[(int)Math.pow(2,20)]);
                                receiveBuffer.clear();
                                udpServer.disconnect();
                                System.out.println("Stopped listening on UDP"); 
                            }
                        }
                    }  
                } catch (IOException e) {
                    Logger l = Logger.getLogger(AssOneServer.class.getName());
                    l.log(Level.WARNING, "IOException in AssOneServer", e);
                } catch (Throwable t) {
                    // If anything else goes wrong (out of memory, for example)
                    // then log the problem and exit.
                    Logger l = Logger.getLogger(AssOneServer.class.getName());
                    l.log(Level.SEVERE, "FATAL error in AssOneServer", t);
                    System.exit(1);
                } 
            }
        }  catch (Exception e) {
            // This is a startup error: there is no need to log it;
            // just print a message and exit
            System.err.println(e);
            System.exit(1);
        }
    }

    private static void tcpEchoTP(Socket clientSocket) throws IOException {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("quit")) {
                    return;
                }

                //System.out.println("Recieved: " + inputLine);
                out.println(inputLine);
                //System.out.println("Echoing: " + inputLine);
            }
        } catch (IOException e) { }
    }

     private static void tcpInteraction(Socket clientSocket) throws IOException {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;

            //byte[] ack = "y".getBytes();

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("quit")) {
                    return;
                }

                //String byteString = new String(inputLine.getBytes(), StandardCharsets.UTF_8);
                //System.out.println("Recieved: " + inputLine + " bytes");
                out.println("y");
                //System.out.println("Ack sent. ");
            }
        } catch (IOException e) { }
    }

    private static void udpEchoTP(DatagramChannel udpServer, SocketAddress clientAddress, Selector selector)
                            throws IOException {
        String packetString = "";
        final int TIMEOUT = 5000;

        ByteBuffer echoBuffer = ByteBuffer.allocate((int)Math.pow(2,20));

        try { 
            while (true) {
                if (selector.select(TIMEOUT) == 0) {
                    System.out.println(".");
                    continue;
                }

                Iterator<SelectionKey> keyItr = selector.selectedKeys().iterator();

                while (keyItr.hasNext()) {
                    SelectionKey key = keyItr.next();

                    if (key.isReadable()) {
                        udpServer = (DatagramChannel) key.channel();
                        echoBuffer.clear();
                        echoBuffer.put(new byte[(int)Math.pow(2,20)]);
                        echoBuffer.clear();


                        clientAddress = udpServer.receive(echoBuffer);

                        byte[] buffer = echoBuffer.array();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                        packetString = new String(packet.getData(), StandardCharsets.UTF_8);

                        //System.out.println("Received: " + packetString.trim());

                        if (packetString.trim().equalsIgnoreCase("quit")) {
                            return;
                        }

                        if (clientAddress != null) {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }

                    if (key.isValid() && key.isWritable()) {
                        udpServer = (DatagramChannel) key.channel();
                        echoBuffer.flip();

                        //System.out.println("Echoing: " + packetString.trim());

                        int bytesSent = udpServer.send(echoBuffer, clientAddress);
                        if (bytesSent != 0) {
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }
        } catch (IOException io) { }
    }
}