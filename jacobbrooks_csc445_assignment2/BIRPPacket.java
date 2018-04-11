import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class BIRPPacket{

	//TFTP Opcodes
	public static final int RRQ = 1; 
	public static final int DATA = 3;
	public static final int ACK = 4;

	//Only initialized/used by recipient to make decisions
	private int opcode;
	private int seqNum;
	private String url;

	private DatagramPacket packet;

	//Only used on the receiving end...contains entire byte buffer of received packet.
	private byte[] receivedBytes;

	//Only used by recipient...holds a 512 byte chunk of requested file.
	private byte[] dataBytes;

	//Constructor for sending RRQ Packet
	public BIRPPacket(int opcode, String url, String address, int port){ 
		byte[] opcodeBytes = ByteBuffer.allocate(4).putInt(opcode).array();
		byte[] urlBytes = url.getBytes();
		byte[] allBytes = new byte[opcodeBytes.length + urlBytes.length];
		for(int i = 0; i < opcodeBytes.length; i++){
			allBytes[i] = opcodeBytes[i];
		}
		for(int i = 0; i < urlBytes.length; i++){
			allBytes[i + opcodeBytes.length] = urlBytes[i];
		}
		try{
			InetAddress addr = InetAddress.getByName(address);
			packet = new DatagramPacket(allBytes, allBytes.length, addr, port);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
	}

	//Constructor for sending data packet
	public BIRPPacket(int opcode, int seqNum, byte[] data, String address, int port){
		byte[] opcodeBytes = ByteBuffer.allocate(4).putInt(opcode).array();
		byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array();
		byte[] allBytes = new byte[opcodeBytes.length + seqNumBytes.length + data.length];
		for(int i = 0; i < opcodeBytes.length; i++){
			allBytes[i] = opcodeBytes[i];
		}
		for(int i = 0; i < seqNumBytes.length; i++){
			allBytes[i + opcodeBytes.length] = seqNumBytes[i];
		}
		for(int i = 0; i < data.length; i++){
			allBytes[i + opcodeBytes.length + seqNumBytes.length] = data[i];
		}
		try{
			InetAddress addr = InetAddress.getByName(address);
			packet = new DatagramPacket(allBytes, allBytes.length, addr, port);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
	}

	//Constructor for sending ACK packet
	public BIRPPacket(int opcode, int seqNum, String address, int port){
		byte[] opcodeBytes = ByteBuffer.allocate(4).putInt(opcode).array();
		byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array();
		byte[] allBytes = new byte[opcodeBytes.length + seqNumBytes.length];
		for(int i = 0; i < opcodeBytes.length; i++){
			allBytes[i] = opcodeBytes[i];
		}
		for(int i = 0; i < seqNumBytes.length; i++){
			allBytes[i + opcodeBytes.length] = seqNumBytes[i];
		}
		try{
			InetAddress addr = InetAddress.getByName(address);
			packet = new DatagramPacket(allBytes, allBytes.length, addr, port);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
	}

	//Constructor for duplicating received packet
	public BIRPPacket(int seqNum, byte[] data){
		this.seqNum = seqNum;
		dataBytes = data;
	}

	//Constructor for receiving any packet
	public BIRPPacket(){
		receivedBytes = new byte[520];
		packet = new DatagramPacket(receivedBytes, 520);
	}

	//Called by BIRPSocket.receive() to extract header info/data and assign to global vars.
	public void processPacket(){
		byte[] opcodeBytes = new byte[4];
		for(int i = 0; i < 4; i++){
			opcodeBytes[i] = receivedBytes[i]; 
		}

		int opcode = ByteBuffer.wrap(opcodeBytes).getInt();
		this.opcode = opcode;

		if(opcode == RRQ){
			byte[] urlBytes = new byte[packet.getLength() - 4];
			for(int i = 0; i < urlBytes.length; i++){
				urlBytes[i] = receivedBytes[i + 4];
			}
			try{
				url = new String(urlBytes, "UTF8");
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();
			}
		}else if(opcode == DATA){
			byte[] seqNumBytes = new byte[4];
			dataBytes = new byte[packet.getLength() - 8]; 
			
			for(int i = 0; i < seqNumBytes.length; i++){
				seqNumBytes[i] = receivedBytes[i + 4];
			}
			seqNum = ByteBuffer.wrap(seqNumBytes).getInt();

			for(int i = 0; i < dataBytes.length; i++){
				dataBytes[i] = receivedBytes[i + 8];
			}
		}else if(opcode == ACK){
			byte[] seqNumBytes = new byte[4];
			for(int i = 0; i < seqNumBytes.length; i++){
				seqNumBytes[i] = receivedBytes[i + 4];
			}
			seqNum = ByteBuffer.wrap(seqNumBytes).getInt();
		}
	}

	public DatagramPacket getPacket(){
		return packet;
	}

	public int getOpcode(){
		return opcode;
	}

	public int getSequenceNumber(){
		return seqNum;
	}

	public String getURL(){
		return url;
	}

	public byte[] getData(){
		return dataBytes;
	}

}