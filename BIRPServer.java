import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.net.*;

public class BIRPServer{

	private BIRPSocket serverSocket;

	private String clientAddress;
	private int clientPort;

	public BIRPServer(int port){
		serverSocket = new BIRPSocket(port);
	}

	public void serve(){
		String url = receiveImageRequest();
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
		
		while(blockCount < fullBlocks){
			
			byte[] block = new byte[512];
			for(int i = 0; i < 512; i++){
				block[i] = imageBytes[i + (512 * blockCount)];
			}

			BIRPPacket packet = new BIRPPacket(BIRPPacket.DATA, blockCount + 1, block, clientAddress, clientPort);
			serverSocket.send(packet);

			BIRPPacket ack = new BIRPPacket();
			serverSocket.receive(ack);

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

	}

}