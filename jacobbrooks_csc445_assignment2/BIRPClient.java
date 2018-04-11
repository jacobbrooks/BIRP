import java.net.*;
import java.io.*;

public class BIRPClient{

	private String serverAddress;
	private int serverPort;

	private BIRPSocket socket;

	private byte[] imageBytes;

	public BIRPClient(String serverAddress, int serverPort){
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		socket = new BIRPSocket();
		socket.setTimeout(2000);
	}

	public void sendImageRequest(String imageURL){
		BIRPPacket packet = new BIRPPacket(BIRPPacket.RRQ, imageURL, serverAddress, serverPort);
		socket.send(packet);
	}

	public void receiveImage(){
		imageBytes = new byte[0];
		int blockLength = 512;
		BIRPPacket ack = new BIRPPacket(BIRPPacket.ACK, 1, serverAddress, serverPort);

		while(blockLength == 512){
	
			BIRPPacket packet = new BIRPPacket();

			while(!socket.receive(packet)){
				socket.send(ack);
			}

			byte[] block = packet.getData();
			blockLength = block.length;

			byte[] temp = new byte[imageBytes.length + block.length];
			for(int i = 0; i < imageBytes.length; i++){
				temp[i] = imageBytes[i];
			}
			for(int i = 0; i < block.length; i++){
				temp[i + imageBytes.length] = block[i];
			}
			imageBytes = temp;

			ack = new BIRPPacket(BIRPPacket.ACK, packet.getSequenceNumber(), serverAddress, serverPort);
			socket.send(ack);
			
		}
	}

	public void slidingWindow(){
		BIRPPacket[] buffer = new BIRPPacket[0];
		int blockLength = 512;

		while(blockLength == 512){	
			BIRPPacket packet = new BIRPPacket();
			socket.receive(packet);

			blockLength = packet.getData().length;

			BIRPPacket[] temp = new BIRPPacket[buffer.length + 1];
			for(int i = 0; i < buffer.length; i++){
				temp[i] = buffer[i];
			}

			temp[buffer.length] = packet;
			buffer = temp;

			BIRPPacket ack = new BIRPPacket(BIRPPacket.ACK, packet.getSequenceNumber() + 1, serverAddress, serverPort);
			socket.send(ack);
		}

		extractImageBytes(buffer);
	}

	private void extractImageBytes(BIRPPacket[] buffer){
		imageBytes = new byte[0];
		for(int i = 0; i < buffer.length; i++){
			System.out.println(buffer[i].getSequenceNumber());
			byte[] data = buffer[i].getData();
			byte[] temp = new byte[imageBytes.length + data.length];
			for(int j = 0; j < imageBytes.length; j++){
				temp[j] = imageBytes[j];
			}
			for(int j = 0; j < data.length; j++){
				temp[imageBytes.length + j] = data[j];
			}
			imageBytes = temp;
		}
	}

	public byte[] getImageBytes(){
		return imageBytes;
	}

}