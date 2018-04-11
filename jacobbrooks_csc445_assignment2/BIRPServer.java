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
	private boolean slidingWindow;

	private long[] fileBeginPointers;
	private long[] fileEndPointers;
	private int[] imageSize;
	private boolean[] exists;

	private RandomAccessFile file;

	public BIRPServer(int port){
		serverSocket = new BIRPSocket(port);
		this.dropPackets = dropPackets;
		r = new Random();
		try{
			file = new RandomAccessFile("cache", "rw");
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		resetFile();
		fileBeginPointers = new long[53];
		fileEndPointers = new long[53];
		imageSize = new int[53];
		exists = new boolean[53];
		dropPackets = false;
		slidingWindow = false;
	}

	public void setDropPackets(){
		dropPackets = true;
	}

	public void setSlidingWindow(){
		slidingWindow = true;
	}

	public void serve(){
		String url = receiveImageRequest();
		byte[] imageBytes = new byte[imageSize[urlHash(url)]];
		if(exists[urlHash(url)]){
			System.out.println("Fetching from cache.");
			try{
				file.seek((int) fileBeginPointers[urlHash(url)]);
				file.read(imageBytes, 0, (int) (fileEndPointers[urlHash(url)] - fileBeginPointers[urlHash(url)]));
			}catch(IOException e){	
				e.printStackTrace();
			}
		}else{
			System.out.println("Fetching from web.");
			imageBytes = httpImageRequest(url);
			cache(url, imageBytes);
		}
		if(slidingWindow){
			slidingWindow(imageBytes);
		}else{
			sendImageToClient(imageBytes);
		}
	}

	private void cache(String url, byte[] imageBytes){
		try{
			fileBeginPointers[urlHash(url)] = file.length();
			fileEndPointers[urlHash(url)] = file.length() + imageBytes.length;
			imageSize[urlHash(url)] = imageBytes.length;
			exists[urlHash(url)] = true;
			file.write(imageBytes);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	private int urlHash(String url){
		int asciiSum = 0;
		for(int i = 0; i < url.length(); i++){
			char c = url.charAt(i);
			int n = c -'0';
			asciiSum++;
		}
		return asciiSum % fileEndPointers.length;
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

	public void slidingWindow(byte[] imageBytes){
		serverSocket.setTimeout(1000);
		BIRPPacket[] buffer = createPacketBuffer(imageBytes);
		int nextToSend = 0;


		//This code is only meaningful when dropping packets
		int lastBlockSize = imageBytes.length % 512;
		int fullBlocks = (imageBytes.length - lastBlockSize) / 512;
		int[] packetsToDrop = new int[0];
		if(dropPackets){
			packetsToDrop = packetsToDrop(fullBlocks + 1);
		}

		while(nextToSend < buffer.length){

			if(dropPackets && packetsToDrop.length != 0){
				boolean dropThisPacket = false;
				for(int i = 0; i < packetsToDrop.length; i++){
					if(nextToSend == packetsToDrop[i]){
						dropThisPacket = true;
					}
				}
				if(!dropThisPacket){
					serverSocket.send(buffer[nextToSend]);
				}
			}else{
				serverSocket.send(buffer[nextToSend]);
			}

			BIRPPacket ack = new BIRPPacket();
			while(!serverSocket.receive(ack)){
				serverSocket.send(buffer[nextToSend]);
			}

			nextToSend = ack.getSequenceNumber() - 1;
		}
	}

	private BIRPPacket[] createPacketBuffer(byte[] imageBytes){
		int lastBlockSize = imageBytes.length % 512;
		int fullBlocks = (imageBytes.length - lastBlockSize) / 512;
		int blockCount = 0;

		BIRPPacket[] buffer = new BIRPPacket[fullBlocks + 1];

		for(int i = 0; i < fullBlocks; i++){
			byte[] blockData = new byte[512];
			for(int j = 0; j < blockData.length; j++){
				blockData[j] = imageBytes[(i * 512) + j];
			}
			buffer[i] = new BIRPPacket(BIRPPacket.DATA, i + 1, blockData, clientAddress, clientPort);
		}

		byte[] lastBlock = new byte[lastBlockSize];
		for(int i = 0; i < lastBlock.length; i++){
			lastBlock[i] = imageBytes[(fullBlocks * 512) + i];
		}

		buffer[fullBlocks] = new BIRPPacket(BIRPPacket.DATA, fullBlocks + 1, lastBlock, clientAddress, clientPort);

		return buffer;
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

	public void resetFile(){
		try{
			file.setLength(0);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
