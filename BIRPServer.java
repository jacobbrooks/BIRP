import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class BIRPServer{

	private BIRPSocket serverSocket;

	private String clientAddress;
	private int clientPort;

	private Random r;

	private boolean dropPackets;

	public BIRPServer(int port, boolean dropPackets){
		serverSocket = new BIRPSocket(port);
		this.dropPackets = dropPackets;
		r = new Random();
	}

	public void serve(){
		String url = receiveImageRequest();
		System.out.println(url);
		byte[] imageBytes = httpImageRequest(url);
		sendImageToClient(imageBytes);
	}

	private String receiveImageRequest(){
		BIRPPacket packet = new BIRPPacket();
		serverSocket.receive(packet);
		clientAddress = packet.getPacket().getAddress().getHostAddress();
		clientPort = packet.getPacket().getPort();
		return packet.getURL();
	}	

	private byte[] httpImageRequest(String s){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try{
		    URL url = new URL(s);
		    byte[] chunk = new byte[100000];
		    int bytesRead;
		    InputStream stream = url.openStream();

		    while ((bytesRead = stream.read(chunk)) > 0) {
				outputStream.write(chunk, 0, bytesRead);
		    }

		}catch(IOException e) {
		    e.printStackTrace();
		}
		return outputStream.toByteArray();
	}

	private void sendImageToClient(byte[] imageBytes){
		int seqNum = 1;
		int lastBlockSize = imageBytes.length % 512;
		int fullBlocks = (imageBytes.length - lastBlockSize) / 512;
		int blockCount = 0;

		BIRPPacket lastAck = new BIRPPacket(BIRPPacket.ACK, 0, clientAddress, clientPort); //dummy initialization, will become real ack inside while loop
		
		int[] packetsToDrop = new int[0];
		if(dropPackets){
			packetsToDrop = packetsToDrop(fullBlocks + 1);
		}

		while(blockCount < fullBlocks){
			
			byte[] block = new byte[512];
			for(int i = 0; i < 512; i++){
				block[i] = imageBytes[i + (512 * blockCount)];
			}

			BIRPPacket packet = new BIRPPacket(BIRPPacket.DATA, blockCount + 1, block, clientAddress, clientPort);

			if(dropPackets && packetsToDrop.length != 0){
				boolean dropThisPacket = false;
				for(int i = 0; i < packetsToDrop.length; i++){
					if(blockCount == packetsToDrop[i]){
						dropThisPacket = true;
					}
				}
				if(!dropThisPacket){
					serverSocket.send(packet);
				}
			}else{
				serverSocket.send(packet);
			}

			BIRPPacket ack = new BIRPPacket();
			serverSocket.receive(ack);

			while(ack.getSequenceNumber() == lastAck.getSequenceNumber()){
				serverSocket.send(packet);
				serverSocket.receive(ack);
			}

			lastAck = ack;

			System.out.println("Received ACK: " + ack.getSequenceNumber());

			blockCount++;

		}

		byte[] lastBlock = new byte[lastBlockSize];
		for(int i = 0; i < lastBlockSize; i++){
			lastBlock[i] = imageBytes[i + (512 * fullBlocks)];
		}

		BIRPPacket packet = new BIRPPacket(BIRPPacket.DATA, fullBlocks + 1, lastBlock, clientAddress, clientPort);
		serverSocket.send(packet);

		BIRPPacket ack = new BIRPPacket();
		serverSocket.receive(ack);

		System.out.println("Received ACK: " + ack.getSequenceNumber());

	}

	private int[] packetsToDrop(int blockCount){
		int packetDropCount = blockCount / 8;
		int[] packetNumbers = new int[packetDropCount];
		if(packetDropCount != 0){
			for(int i = 0; i < packetDropCount; i++){
				packetNumbers[i] = r.nextInt(blockCount - 3) + 3;
			}
		}
		return packetNumbers;
	}

}
