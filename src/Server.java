import java.net.*;
import java.io.*;


public class Server {
	public static void main(String[] args) throws IOException {


		if (args.length != 1) {
			System.err.println("Usage: java -jar NetNinny.jar <port number>");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);

		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
		System.out.println("Started proxy at portnumber: " + portNumber);
		int i= 1;
		while(true) {
			(new Thread(new Client(serverSocket.accept(), i++))).start();
		}
	}
}
