public class ServerMain{
	public static void main(String[] args){
		BIRPServer server = new BIRPServer(6145, true);
		server.serve();
	}
}