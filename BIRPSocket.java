import java.net.*;
import java.io.*;

public class BIRPSocket{

	private DatagramSocket socket;

	public BIRPSocket(int port){
		try{	
			socket = new DatagramSocket(port);
			socket.setSendBufferSize(8192);
			socket.setReceiveBufferSize(8192);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public BIRPSocket(){
		try{	
			socket = new DatagramSocket();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void send(BIRPPacket packet){
		try{
			socket.send(packet.getPacket());
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void receive(BIRPPacket packet){
		try{
			socket.receive(packet.getPacket());
			packet.processPacket();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}