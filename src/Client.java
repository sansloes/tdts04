import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;



public class Client implements Runnable {


	private Socket browserSocket, serverSocket;
	private int nr;
	private String hostName;
	private String requestMessage;
	private InputStream inputBrowser;
	private OutputStream outputServer;
	private InputStream inputServer;
	private OutputStream outputBrowser;
	private DataOutputStream output;
	private Boolean filter = false;
	private String URL;

	/*
	 * Constructor for the Client class, binds the socket so it is accessible.
	 */
	public Client(Socket socket, int number){
		this.browserSocket = socket;
		this.nr = number;
	}

	/*
	 * Connects a socket to the WebServer.
	 */
	public void ServerConnect() throws UnknownHostException, IOException {
		this.serverSocket = new Socket(this.hostName, 80);
	}

	/*
	 * Filters the URL from the GET request with specified keywords.
	 */
	public void FilterURL() throws UnknownHostException, IOException {
		String redirection = ("HTTP/1.1 302 Found" + "\n" + "Location: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html" + "\r\n");
		if (this.URL.toLowerCase().contains("spongebob") || this.URL.toLowerCase().contains("britney") && this.URL.toLowerCase().contains("spears")
				|| this.URL.toLowerCase().contains("paris") && this.URL.toLowerCase().contains("hilton") || this.URL.toLowerCase().contains("norrkoping")
				|| this.URL.toLowerCase().contains("norrköping")) {
			this.filter = true;
			this.output.writeBytes(redirection);
		}
	}

	/*
	 * Reads and stores the GET request header from the browser.
	 * Stores the URL, hostname.
	 * Removes the GZIP encoding and changes to Connection: close
	 */
	public void GetRequest(BufferedReader reader) throws IOException {

		String tempRequestMessage = null;
		StringBuilder bufferRequestMessage = new StringBuilder();
		while (!(tempRequestMessage = reader.readLine()).equals("")) {
			if (tempRequestMessage.contains("GET")) {
				this.URL = tempRequestMessage;	
			}
			if (tempRequestMessage.contains("Host")){
				this.hostName = tempRequestMessage.substring(6, tempRequestMessage.length());
			}
			if (tempRequestMessage.contains("Accept-Encoding")) { //We can't use GZIP encoding
				tempRequestMessage = reader.readLine();
			}
			if (tempRequestMessage.contains("Connection")) {
				bufferRequestMessage.append("Connection: close" + System.getProperty("line.separator"));
				tempRequestMessage = reader.readLine();
				break;
			}
			bufferRequestMessage.append(tempRequestMessage + System.getProperty("line.separator"));
			
		}
		bufferRequestMessage.append(System.getProperty("line.separator"));
		this.requestMessage = bufferRequestMessage.toString();

	}


	/*
	 * Sends the request to the WebServer.
	 */
	public void SendRequest(PrintWriter writer, String requestMessage) {
		writer.flush();
		writer.print(requestMessage);
		writer.flush();
	}


	
	/*
	 * Reads content including response header from WebServer and sends it to the browser.
	 * If the Content-Type is text then it is filtered and if a keyword is found we redirect to the error page.
	 * Otherwise we just pass along the content.
	 */
	public void Data () throws IOException {

		DataInputStream in = new DataInputStream(this.inputServer);

		String contentType;
		Boolean type = false;
		String unfilteredText;
		StringBuilder unfilteredBuffer = new StringBuilder();

		int u = 0;
		int counter = 0;
		byte[] dataContent = new byte[1024];
		byte[] textContent = new byte[5000000];
		
		while ((u = in.read(dataContent)) != -1 ) {

			if (!type) { //flag for checking if content-type has been found yet
				contentType = new String(dataContent, "UTF-8");
				if (contentType.contains("Content-Type")) {
					type = true;
					
					if (contentType.contains("text")) {
						unfilteredBuffer.append(contentType);
						
						System.arraycopy(dataContent, 0, textContent, counter, u);
						counter = counter + u;
						while ((u = in.read(dataContent)) != -1 ) { //from here is the content filtering done
							System.arraycopy(dataContent, 0, textContent, counter, u);
							counter = counter + u;
							unfilteredText = new String(dataContent, "UTF-8");
							if (unfilteredText.toLowerCase().contains("spongebob") ||
									unfilteredText.toLowerCase().contains("britney") && unfilteredText.toLowerCase().contains("spears")
									|| unfilteredText.toLowerCase().contains("paris") && unfilteredText.toLowerCase().contains("hilton") || 
									unfilteredText.toLowerCase().contains("norrkoping")
									|| unfilteredText.toLowerCase().contains("norrköping"))
							{
								this.output.writeBytes("HTTP/1.1 302 Found" + "\n" 
										+ "Location: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html" + "\r\n"); //redirect if keyword found
							}
							
						}
						output.flush();
						this.output.write(textContent, 0, counter); //writes unfiltered text to browser
						output.flush();
						break; 
					}
				}
			}

				output.flush();
				output.write(dataContent, 0, u);
				output.flush();

			}

		} 

	/*
	 * This is the main, where the methods are executed. Some streams are created too.
	 */
		@Override
		public void run() {

			try {
				this.inputBrowser = (this.browserSocket.getInputStream());

			} catch (IOException e) {
				e.printStackTrace();
			}

			BufferedReader inBrowser = new BufferedReader(
					new InputStreamReader(this.inputBrowser));
			try {
				GetRequest(inBrowser);
			} catch (IOException e) {

				e.printStackTrace();
			}


			try {
				this.outputBrowser = browserSocket.getOutputStream();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			try {
				this.output = new DataOutputStream(this.outputBrowser);
				FilterURL();
				if(filter) {
					browserSocket.close();
				}
				ServerConnect();
			} catch (IOException e) {
	
				e.printStackTrace();
			}


			try {
				this.outputServer = serverSocket.getOutputStream();
			} catch (IOException e) {
		
				e.printStackTrace();
			}
			PrintWriter outServer = new PrintWriter(this.outputServer, true);
			SendRequest(outServer, this.requestMessage);


			try {
				this.inputServer = serverSocket.getInputStream();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Data();
			} catch (IOException e) {

				e.printStackTrace();
			}
			
			try {
				inputBrowser.close();
				outputBrowser.close();
				inputServer.close();
				outputServer.close();
				serverSocket.close();
				browserSocket.close();
				return;
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}



