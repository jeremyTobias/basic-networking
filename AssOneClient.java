import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class AssOneClient {
    private static final int PORT = 2695;
    private static final int UDPMAX = ((int)Math.pow(2,14));

    private static long start;
    private static long end;
    private static long tpStart;
    private static long tpEnd;
    private static long totalTime;
    private static long elapsedTime;
    private static long aveTime;
                
    private static double throughput;
                
	private static byte[] msg;
        
   	private static DecimalFormat df = new DecimalFormat();
    private static Scanner scanner;
	private static ArrayList<Long> times;
	private static FileWriter writer;
        
    private static String usrInput;
    private static String host;
    private static String msgString;    
    
    private static String sixtyFourPacket;
    private static String twoFiveSixPacket;
    private static String fiveOneTwoPacket;
    private static String megPacket;

	public static void main(String args[]) throws IOException {
		
		try {
			writer = new FileWriter("netOneCSV.csv");
		} catch (IOException io) {
			System.err.println("Could not write file.");
		}
		preBuildPackets();
        welcome();
	}
        
    private static void welcome() {
        displayMenu();

        scanner = new Scanner(System.in);
        usrInput = scanner.nextLine();

        if (usrInput.equalsIgnoreCase("quit")) {
        	try {
        		writer.flush();
    	    	writer.close();
        	} catch (IOException io) { }
            System.exit(1);
        }
        
        switch (usrInput) {
            case "1":              	
               	usrInput = tcpOrudp();
                	
               	if (usrInput.equalsIgnoreCase("TCP")) {
               		try {
               			calcTRTT();
               		} catch (IOException io) {
               		
            		}
                } else if (usrInput.equalsIgnoreCase("UDP")) {                		
                	try {
                		calcURTT("");
                	} catch (IOException io) {

                	}
                } else {              
                   	break;
                }
                    
            case "2":
               	usrInput = tcpOrudp();
                	
               	if (usrInput.equalsIgnoreCase("TCP")) {
               		try {
               			calcTThroughput();
               		} catch (IOException io) {
               			uhoh();             		
            		}
                } else if (usrInput.equalsIgnoreCase("UDP")) {                		
                	try {
                		calcUThroughput("");
                	} catch (IOException io) {
                		uhoh();
                	}
                } else {              
                   	break;
                }

            case "3":
            	try {
               		tcpPktInteraction();
                } catch (IOException io) {
                	uhoh();
                }
                break;
        }

        makeSpace();
        welcome();
    }           
    
    //******************************************************************************//
	//																				//
	//								   TCP Methods									//
	//																				//
	//******************************************************************************//

    private static void calcTRTT() throws IOException {          
        host = gimmeHost();   

        toHome(host);

        try (
            Socket echoSock = new Socket(host, PORT);
			PrintWriter out = new PrintWriter(echoSock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(echoSock.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))			
			){

        	out.println("echo");

        	setPkgSize();

			while ((usrInput = stdIn.readLine()) != null) {

				if (usrInput.equalsIgnoreCase("quit")) {
					out.println("quit");
					makeSpace();
					echoSock.close();
					welcome();
				}

			  	times = new ArrayList<>();
			    totalTime = 0;
			                        
			    int byteSize = Integer.parseInt(usrInput);
			    builPkgMsg();
			    msgString = fillMsg(byteSize);

			    sendPkgMsg();

			    for (int i = 0; i < 1000; i++) {
			        start = System.nanoTime();
					//System.out.println("Start time: " + start);
												
					out.println(msgString);
					//out.flush();			
												
					in.readLine();
					end = System.nanoTime();
					//System.out.println("Finish time: " + end + "\n");

					long timer = end - start;
					//System.out.println("Current timer = " + timer);
					times.add(timer);
			    } 
			                                
			    for (Long t : times) {
			        totalTime += t;
			    }
			   
			   	df = new DecimalFormat("0.##");        	                     
			    aveTime = totalTime / times.size();
			    long millAveTime = (long)(aveTime / Math.pow(10,6));

			    String rttTime = String.valueOf(aveTime);

			    System.out.println("Packet size: " + byteSize + " byte(s)");
			    System.out.println("Times sent: " + times.size());
			    System.out.println("Average RTT: " + aveTime + "ns");
			    makeSpace();
			    writeToCSV("TCP", byteSize, rttTime, "");
			    setPkgSize();
			}
		} catch (UnknownHostException e) {
		    System.err.println("Don't know host " + host);
		    makeSpace();
			calcTRTT();
		} catch (IOException io) {
		    System.err.println("Could not connect to " + host);
		    makeSpace();
			calcTRTT();
		}
    }
	
	private static void calcTThroughput() throws IOException {
		host = gimmeHost();   

        toHome(host);

        try (
            Socket echoSock = new Socket(host, PORT);
			PrintWriter out = new PrintWriter(echoSock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(echoSock.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))			
			){

        	out.println("throughput");

        	selectPkg();

			while ((usrInput = stdIn.readLine()) != null) {			
				if (usrInput.equalsIgnoreCase("quit")) {
					out.println("quit");
					makeSpace();
					echoSock.close();
					welcome();
				}

			    switch (usrInput) {
			    	case "1" :
			    		builPkgMsg();			    		
			    		msgString = fillMsg((int)Math.pow(2,10));
			    		break;
			    	case "2" :
			    		builPkgMsg();
			    		msgString = fillMsg((int)Math.pow(2,12));
			    		break;
			    	case "3" :
			    		builPkgMsg();
						msgString = fillMsg((int)Math.pow(2,14));
						break;
			    	case "4" :
			    		builPkgMsg();
						msgString = sixtyFourPacket;
						break;
			    	case "5" :
			    		builPkgMsg();
						msgString = twoFiveSixPacket;
						break;	
					case "6" :
						builPkgMsg();
						msgString = megPacket;
						break;	    	
			    }

			    sendPkgMsg();
			    
			    times = new ArrayList<Long>();
			    double totalTransTime = 0;
				double rttTotalThroughput = 0;
			    double rttThroughput;

			   	for (int i = 0; i < 500; i++) {                       
			    	tpStart = System.nanoTime();											
					out.println(msgString);
					//out.flush();			
												
					in.readLine();
					tpEnd = System.nanoTime();

					long rtt = tpEnd - tpStart;	
					times.add(rtt);
				}

			    for (Long t : times) {
			        long rttMillis = (long)(t / Math.pow(10,6));			    
					double rttTransferTime = (double)(rttMillis + ((1 / Math.pow(2,30)) * msgString.getBytes().length * 8 * 1000));
					//System.out.println(rttTransferTime);
					rttThroughput = ((msgString.getBytes().length * 8) / Math.pow(2,20)) / (rttTransferTime / 1000);
					rttTotalThroughput += rttThroughput;
					totalTransTime += rttTransferTime;
					//System.out.println(totalTransTime);
			    }
			   
			   	df = new DecimalFormat("0.##");        	                     
			    double aveTransTime = totalTransTime / times.size();
			    double rttAveThroughput = rttTotalThroughput / times.size();

			    System.out.println("Packet size: " + msgString.getBytes().length + " byte(s)");
			   	System.out.println("Average transferTime: " + df.format(aveTransTime) +"ms");			    
			    System.out.println("Average throughput: " + df.format(rttAveThroughput) + "Mbps");
			    makeSpace();
			    writeToCSV("TCP", msgString.getBytes().length, "", df.format(rttAveThroughput));
			    selectPkg();
			}
		} catch (UnknownHostException e) {
		    System.err.println("Don't know host " + host);
		    makeSpace();
			calcTThroughput();
		} catch (IOException io) {
		    System.err.println("Could not connect to " + host);
		    makeSpace();
			calcTThroughput();
		}
	}

	private static void tcpPktInteraction() throws IOException {
		host = gimmeHost();   

        toHome(host);
		try (
            Socket echoSock = new Socket(host, PORT);
			PrintWriter out = new PrintWriter(echoSock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(echoSock.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))			
			){

        	out.println("interaction");

        	interactionMsg();

			while ((usrInput = stdIn.readLine()) != null) {

				if (usrInput.equalsIgnoreCase("quit")) {
					out.println("quit");
					makeSpace();
					echoSock.close();
					welcome();
				}

				ArrayList<Integer> tcpChunks = new ArrayList<Integer>();
			  	times = new ArrayList<>();
			    totalTime = 0;
			                        
			    int numMsgs = Integer.parseInt(usrInput);

			    int chunkSize = (int)Math.pow(2,20) / numMsgs;

			    builPkgMsg();
			    msgString = fillMsg(chunkSize);

			    sendPkgMsg();

			    double rttTotalThroughput = 0;
			    double rttThroughput;

			    tpStart = System.nanoTime();
			    for (int i = 0; i < numMsgs; i++) {

			        start = System.nanoTime();
					System.out.println("Sending " + msgString.getBytes().length +" bytes.");
					makeSpace();
												
					out.println(msgString);
					//out.flush();

					if (!waitForAck(out, in, msgString)) {
						System.out.println("No acknowledgement from server. Resetting.");
						makeSpace();
						tcpPktInteraction();
					}

					end = System.nanoTime();
					//System.out.println("Finish time: " + end + "\n");

					long timer = end - start;
					//System.out.println("Current timer = " + timer);
					times.add(timer);
			    } 
			    tpEnd = System.nanoTime();

			    for (Long t : times) {
			        totalTime += t;
			        long rttMillis = (long)(t / Math.pow(10,6));			    
					double rttTransferTime = (double)(rttMillis + ((1 / Math.pow(2,30)) * msgString.getBytes().length * 8 * 1000));
					rttThroughput = ((msgString.getBytes().length * 8) / Math.pow(2,20)) / (rttTransferTime / 1000);
					rttTotalThroughput += rttThroughput;
			    }
			   
			   	df = new DecimalFormat("0.##");        	                     
			    aveTime = totalTime / times.size();
			    long millAveTime = (long)(aveTime / Math.pow(10,6));
			    double rttAveThroughput = rttTotalThroughput / times.size();

			    long elapTime = tpEnd - tpStart;	

				double elapMillis = (double)(elapTime / Math.pow(10,6));			    
				double transferTime = (double)(elapMillis + ((1 / Math.pow(2,30)) * msgString.getBytes().length * 8 * 1000));
				throughput = ((msgString.getBytes().length * 8) / Math.pow(2,20)) / (transferTime / 1000);

			    System.out.println("Packet size: " + msgString.getBytes().length + " byte(s)");
			    System.out.println("Times sent: " + times.size());
			    System.out.println("Start time: " + tpStart);
			   	System.out.println("Stop time: " + tpEnd);
			   	System.out.println("Overall TransferTime: " + df.format(transferTime) +"ms");
			    System.out.println("Average RTT: " + df.format(millAveTime) + "ms");			    
			    System.out.println("Average Throughput/RTT: " + df.format(rttAveThroughput) + "Mbps");			    
			    System.out.println("Overall Throughput: " + df.format(throughput) + "Mbps");
			    makeSpace();

			    writeToCSV("TCP", msgString.getBytes().length, String.valueOf(aveTime), df.format(rttAveThroughput));
			    interactionMsg();
			}
		} catch (UnknownHostException e) {
		    System.err.println("Don't know host " + host);
		    makeSpace();
			tcpPktInteraction();
		} catch (IOException io) {
		    System.err.println("Could not connect to " + host);
		    makeSpace();
			tcpPktInteraction();
		}
	}

	//******************************************************************************//
	//																				//
	//								 UDP Methods									//
	//																				//
	//******************************************************************************//

	private static void calcURTT(String h) throws IOException {
		if (h.equals("")) {
			h = gimmeHost();
		}

		host = h;

		toHome(host);

		InetAddress address = InetAddress.getByName(host);

		try (
			DatagramSocket daSock = new DatagramSocket();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
			){
			
			try {
				daSock.connect(address, PORT);
				byte[] udpHdr = "echo".getBytes();
				DatagramPacket hdrPacket = new DatagramPacket(udpHdr, udpHdr.length, address, PORT);
				daSock.send(hdrPacket);
			} catch (IOException io) {
				System.err.println("Could not send header packet.");
				makeSpace();
				daSock.close();
				welcome();
			}

			setPkgSize();

			usrInput = reader.readLine();

			times = new ArrayList<>();
			df = new DecimalFormat("0.#####E0");
			totalTime = 0;

			if (usrInput.equalsIgnoreCase("quit")) {
				byte[] qByte = usrInput.getBytes();
				DatagramPacket quitPkt = new DatagramPacket(qByte, qByte.length, address, PORT);
				daSock.send(quitPkt);
				makeSpace();
				daSock.close();
				welcome();
			}

			int byteSize = Integer.parseInt(usrInput);

			if (byteSize > UDPMAX) {
				System.out.println("Packet exceeds max size of " + UDPMAX + " bytes.");
				calcURTT(host);
			}

			builPkgMsg();
			msgString = fillMsg(byteSize);

			msg = msgString.getBytes();

			byte[] byteBuffer = new byte[byteSize];

			DatagramPacket sndrPkt = new DatagramPacket(msg, msg.length, address, PORT);
			DatagramPacket rcvrPkt = new DatagramPacket(byteBuffer, byteBuffer.length);
			
			sendPkgMsg();

		    for (int i = 0; i < 1000; i++) {
		        start = System.nanoTime();
				//System.out.println("Start time: " + start);
				
				daSock.send(sndrPkt);

				if (!udpResend(daSock, sndrPkt, rcvrPkt)) {
					System.out.println("Exceeded retry attempts. Restarting.");
					calcURTT(host);
				}

				end = System.nanoTime();
				//System.out.println("Finish time: " + end + "\n");

				long timer = end - start;
				//System.out.println("Current timer = " + timer);
				times.add(timer);
		    }                                
			                                
		    for (Long t : times) {
		        totalTime += t;
		    }
		                                
		   	df = new DecimalFormat("0.##");        	                     
			aveTime = totalTime / times.size();
			long millAveTime = (long)(aveTime / Math.pow(10,6));

			String rttTime = String.valueOf(aveTime);

			System.out.println("Packet size: " + byteSize + " byte(s)");
			System.out.println("Times sent: " + times.size());
			System.out.println("Average RTT: " + aveTime + "ns");
		    
		    makeSpace();
		    writeToCSV("UDP", byteSize, rttTime, "");
			calcURTT(host);
		} catch (UnknownHostException e) {
		    System.err.println("Don't know host " + host);
		    makeSpace();
			calcURTT("");
		} catch (IOException io) {
		    System.err.println("Could not connect to " + host);
		    makeSpace();
			calcURTT("");
		}
	}  

	private static void calcUThroughput(String h) throws IOException {
		if (h.equals("")) {
			h = gimmeHost();
		}

		host = h;

		toHome(host);

		InetAddress address = InetAddress.getByName(host);

		try (
			DatagramSocket daSock = new DatagramSocket();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
			){
			
			try {
				daSock.connect(address, PORT);
				byte[] udpHdr = "throughput".getBytes();
				DatagramPacket hdrPacket = new DatagramPacket(udpHdr, udpHdr.length, address, PORT);
				daSock.send(hdrPacket);
			} catch (IOException io) {
				System.err.println("Could not send header packet.");
				makeSpace();
				daSock.close();
				welcome();
			}

			selectPkg();

			usrInput = reader.readLine();

			if (usrInput.equalsIgnoreCase("quit")) {
				byte[] qByte = usrInput.getBytes();
				DatagramPacket quitPkt = new DatagramPacket(qByte, qByte.length, address, PORT);
				daSock.send(quitPkt);
				makeSpace();
				daSock.close();
				welcome();
			}

			ArrayList<Integer> udpChunks = new ArrayList<Integer>();
			times = new ArrayList<Long>();

			switch (usrInput) {
			    case "1" :
			    	msg = new byte[(int)Math.pow(2,10)];
			    	break;
			    case "2" :
			    	msg = new byte[(int)Math.pow(2,12)];
			    	break;
			    case "3" :
					msg = new byte[(int)Math.pow(2,14)];
					break;
			    case "4" :
					msg = new byte[(int)Math.pow(2,16)];
					break;
			    case "5" :
					msg = new byte[(int)Math.pow(2,18)];
					break;	
				case "6" :
					msg = new byte[(int)Math.pow(2,20)];
					break;	    	
			}

			if (msg.length > UDPMAX) {
				makeUDPChunks(udpChunks, msg.length);
				builPkgMsg();
				msgString = fillMsg(udpChunks.get(0));
				msg = msgString.getBytes();
			} else {
				builPkgMsg();
				msgString = fillMsg(msg.length);
				msg = msgString.getBytes();
			}	

			byte[] byteBuffer = new byte[msg.length];

			DatagramPacket sndrPkt = new DatagramPacket(msg, msg.length, address, PORT);
			DatagramPacket rcvrPkt = new DatagramPacket(byteBuffer, byteBuffer.length);

			sendPkgMsg();
			for (int i = 0; i < 500; i++) {
				if (udpChunks.size() != 0) {
				    tpStart = System.nanoTime();

					for (Integer chunk : udpChunks) {
						daSock.send(sndrPkt);

						if (!udpResend(daSock, sndrPkt, rcvrPkt)) {
							System.out.println("Exceeded retry attempts. Restarting.");
							calcUThroughput(host);
						}
					}

					tpEnd = System.nanoTime();

					times.add(tpEnd - tpStart);
				} else {
				    tpStart = System.nanoTime();

					daSock.send(sndrPkt);

					if (!udpResend(daSock, sndrPkt, rcvrPkt)) {
						System.out.println("Exceeded retry attempts. Restarting.");
						calcUThroughput(host);
					}
				
					tpEnd = System.nanoTime();
					times.add(tpEnd - tpStart);
				}
			}

			df = new DecimalFormat("0.##");

			long rttMillis;

			double aveTransTime;
			double totalTransTime = 0;

			double rttTotalThroughput = 0;
			double rttThroughput;
			double rttTransferTime;
			double rttAveThroughput;

			int actualMsgSize = 0;

		    if (udpChunks.size() != 0) {
		    	actualMsgSize = msg.length * udpChunks.size();

		    	for (Long t : times) {				    
				    rttMillis = (long)(t / Math.pow(10,6));			    
					rttTransferTime = (double)(rttMillis + ((1 / Math.pow(2,30)) * actualMsgSize * 8 * 1000));
					rttThroughput = ((actualMsgSize * 8) / Math.pow(2,20)) / (rttTransferTime / 1000);
					rttTotalThroughput += rttThroughput;
					totalTransTime += rttTransferTime;
				}			   
				        	                     
				aveTransTime = totalTransTime / times.size();
				rttAveThroughput = rttTotalThroughput / times.size();				 

			    System.out.println("Packet size: " + actualMsgSize + " byte(s)");
				System.out.println("Average transferTime: " + df.format(aveTransTime) +"ms");			    
				System.out.println("Average throughput: " + df.format(rttAveThroughput) + "Mbps");
		    } else {
		    	for (Long t : times) {
				    rttMillis = (long)(t / Math.pow(10,6));   
				    //System.out.println("RTT: " + rttMillis);
					rttTransferTime = (double)(rttMillis + ((1 / Math.pow(2,30)) * msgString.getBytes().length * 8 * 1000));
					//System.out.println("Trans Time: " + rttTransferTime);
					rttThroughput = ((msg.length * 8) / Math.pow(2,20)) / (rttTransferTime / 1000);
					rttTotalThroughput += rttThroughput;
					totalTransTime += rttTransferTime;
				}

				aveTransTime = (double)((totalTransTime / times.size()) / Math.pow(10,6));
				rttAveThroughput = rttTotalThroughput / times.size();				 

			    System.out.println("Packet size: " + msg.length + " byte(s)");
				System.out.println("Average transferTime: " + df.format(aveTransTime) +"ms");			    
				System.out.println("Average throughput: " + df.format(rttAveThroughput) + "Mbps");
		    }			

			makeSpace();
			if (udpChunks.size() != 0) {
		    	writeToCSV("UDP", actualMsgSize, "", df.format(rttAveThroughput));
		    } else {
		    	writeToCSV("UDP", msg.length, "", df.format(rttAveThroughput));
		    }
			    
			calcUThroughput(host);
		} catch (UnknownHostException e) {
		    System.err.println("Don't know host " + host);
		    makeSpace();
			calcUThroughput("");
		} catch (IOException io) {
		    System.err.println("Could not connect to " + host);
		    makeSpace();
			calcUThroughput("");
		}
	}

	//******************************************************************************//
	//																				//
	//								   Helpers										//
	//																				//
	//******************************************************************************//

    private static void displayMenu() {
    	System.out.println("Please make a selection or type \"quit\"");
    	System.out.println("1. Calculate RTT");
        System.out.println("2. Calculate Throughput");
        System.out.println("3. Packet Interaction");
        System.out.print("-> ");
    }

    private static void selectPkg() {
    	System.out.println("Select a packet size:");
    	System.out.println("1. 1KB");
		System.out.println("2. 4KB");
		System.out.println("3. 16KB");
		System.out.println("4. 64KB");
		System.out.println("5. 256KB");
		System.out.println("6. 1MB"); 
		System.out.print("-> ");  	
    }

    private static String gimmeHost() {
    	System.out.print("Enter host or \"quit\": ");
        scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    private static String tcpOrudp() {
    	System.out.print("TCP or UDP? ");
    	scanner = new Scanner(System.in);
    	return scanner.nextLine();
    }

    private static void preBuildPackets() {
    	System.out.println("Prebuilding large packets. Please wait.");

    	sixtyFourPacket = fillMsg((int)Math.pow(2,16));

    	for (int i = 0; i < 4; i++) {
    		twoFiveSixPacket += sixtyFourPacket;
    	}

    	for (int i = 0; i < 2; i++) {
    		fiveOneTwoPacket += twoFiveSixPacket;
    	}

    	for (int i = 0; i < 2; i++) {
    		megPacket += fiveOneTwoPacket;
    	}

    	System.out.println("Packet prebuild complete.");
    	makeSpace();
    }

    private static String fillMsg(int size) {
    	String fill = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    	char[] fillArray = fill.toCharArray();

    	String msgFill = "";

    	int i = 0;
    	int j = 0;
    	while (i != size) {
    		if (j == fillArray.length) {
    			j = 0;
    		}

    		msgFill += Character.toString(fillArray[j]);

    		i++;
    		j++;
    	}
    	return msgFill;
    }

    private static int retries = 0;
    private static boolean waitForAck(PrintWriter out, BufferedReader in, String m) {
    	msgString = m;

    	byte[] ack = "y".getBytes();

    	System.out.println("Waiting for ack.");

    	try {
	    	if (in.readLine().getBytes().length == ack.length) {
	    		System.out.println("Ack received.");
	    		System.out.println("Retries reset");
	    		retries = 0;
	    		return true;
	    	} else {
	    		if (retries < 5) {
	    			System.out.println("Ack not received. Retries left " + (5 - retries));
	    			out.println(msgString);
	    			retries++;
	    			waitForAck(out, in, msgString);
	    		}

	    		return false;
	    	}

	    } catch (IOException io) {
	    	System.out.println("Something happened.");
	    	welcome();
	    }

	    return false;
    }

    private static boolean udpResend(DatagramSocket daSock, DatagramPacket sndrPkt, DatagramPacket rcvrPkt) 
    throws IOException {

    	if (retries == 10) {
    		return false;
    	}

    	try {
    		daSock.setSoTimeout(1000);
			daSock.receive(rcvrPkt);
		} catch (SocketTimeoutException e) {
			System.err.println("Socket timed out. Resending packet.");
			System.err.println((10 - retries) + " retries left.");
			makeSpace();
			retries++;
			daSock.send(sndrPkt);
		}

		if (retries > 0) {
			System.out.println("Retries reset.");
		}
		retries = 0;
		return true;
    }

    private static ArrayList<Integer> makeUDPChunks(ArrayList<Integer> udpChunks, int block) {
    	int maxPktSize = (int)Math.pow(2,13);
    	int numChunks = block / maxPktSize;

    	for (int i = 0; i < numChunks; i++) {
    		udpChunks.add(maxPktSize);
    	}

    	return udpChunks;
    }

    private static boolean hdrsWritten = false;
    private static void writeToCSV(String type, int pktSize, String roundTt, String thPut) throws IOException{
    	try {
    		if (hdrsWritten == false) {
    	    		writer.append("Connection Type");
    	    		writer.append(',');
    	    		writer.append("Packet Size");
    	    		writer.append(',');
    	    		writer.append("Round Trip Time");
    	    		writer.append(',');
    	    		writer.append("Throughput");
    	    		writer.append('\n');
    	
    	    		hdrsWritten = true;
    	    }
    	
    	    	writer.append(type);
    	    	writer.append(',');
    	    	writer.append(Integer.toString(pktSize));
    	    	writer.append(',');
    	    	writer.append(roundTt);
    	    	writer.append(',');
    	    	writer.append(thPut);
    	    	writer.append('\n');
    	} catch (IOException io) { 
    		System.err.println("Could not write to file.");
    	}
    }

    private static void makeSpace() {
    	System.out.println();
    }

    private static void uhoh() {
    	System.err.println("Something bad happened.");
    	makeSpace();
    }

    private static void toHome(String host) {
    	if (host.equalsIgnoreCase("quit")) { welcome(); }
   	}

   	private static void setPkgSize() {
   		System.out.println("Enter packet size in bytes or \"quit\"");
   		System.out.print("-> ");
   	}

   	private static void sendPkgMsg() {
   		System.out.println("Sending packet(s). Please wait.");
		makeSpace();
   	}

   	private static void builPkgMsg() {
   		System.out.println("Building packet(s). Please wait.");
   		makeSpace();
   	}

   	private static void interactionMsg() {
   		System.out.println("Enter number of messages to send or \"quit\"");
        System.out.println("Number of messages will equal 1MB of data.");
        System.out.print("-> ");
   	}
}