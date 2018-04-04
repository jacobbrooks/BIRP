public class BIRPClient{

	private String serverAddress;
	private int serverPort;

	private BIRPSocket socket;

	private byte[] imageBytes;

	public BIRPClient(String serverAddress, int serverPort){
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		socket = new BIRPSocket();
		imageBytes = new byte[0];
	}

	public void sendImageRequest(String imageURL){
		BIRPPacket packet = new BIRPPacket(BIRPPacket.RRQ, imageURL, serverAddress, serverPort);
		socket.send(packet);
	}

	public void receiveImage(){
		int blockLength = 512;

		while(blockLength == 512){
			BIRPPacket packet = new BIRPPacket();
			socket.receive(packet);

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

			BIRPPacket ack = new BIRPPacket(BIRPPacket.ACK, packet.getSequenceNumber(), serverAddress, serverPort);
			socket.send(ack);
		}
	}

	public byte[] getImageBytes(){
		return imageBytes;
	}

}