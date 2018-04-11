public class ServerMain{
	public static void main(String[] args){

		String slidingWindow = args[0];
		String withDrops = args[1];

		BIRPServer server = new BIRPServer(6145);

		if(slidingWindow.equals("true")){
			server.setSlidingWindow();
		}

		if(withDrops.equals("true")){
			server.setDropPackets();
		}

		server.serve();
		//server.serve();
		server.resetFile();
	}
}