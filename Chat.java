import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
public class Chat {
    public static boolean waitingForInput;
    public static ArrayList<Socket> connections = new ArrayList<>();

    /**
     * Checks for a port number in the args parameter which is defined when running the program.
     * This method also starts the server thread to listen for incomming connections.
     * This method also listens for user input from the user.
     * @param args contains the port number that will be used by the server
     */
    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Start program with the port argument\nEx: java Chat 6565\n");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Scanner input = new Scanner(System.in);
        new Server(port).start();

        while(true) {
            waitingForInput = true;
            System.out.print(">> ");
            String userInput = input.nextLine();
            waitingForInput = false;
            String[] userInputs = userInput.split(" ");
            switch(userInputs[0]) {
                case "help":
                    displayHelp();
                    break;
                case "myip":
                    displayIPAddress();
                    break;
                case "myport":
                    System.out.println("Port: " + Server.getMyPort());
                    break;
                case "connect":
                    if(userInputs.length == 3) {
                        connect(userInputs[1], Integer.parseInt(userInputs[2]));
                    }
                    else {
                        System.out.println("Wrong usage\nEX: connect 192.168.1.1 6565");
                    }
                    break;
                case "terminate":
                    terminateConnection(Integer.parseInt(userInputs[1]));
                    break;
                case "send":
                    // Call send function here
                    //Alejandro Urbano added 
                    if(userInputs.length < 3){
                        System.out.println("Wrong usage\\nEX: send <connection id> <message>");
                    }
                    else{
                        try{
                            int connectionId = Integer.parseInt(userInputs[1]);
                            String message = userInput.substring(userInput.indexOf(userInputs[2]));
                            sendMessage(connectionId, message);
                        }catch(NumberFormatException e){
                            System.out.println("Connection ID must be a valid number");
                        }
                    }          
                    break;
                case "list":
                    listConnections();
                    break;
                case "exit":
                    closeAllConnections();
                    System.exit(0);
                default:
                    System.out.println('"'+userInputs[0]+'"' + " is not a command. Type help for a list of commands.");
            }
        }
    }
    // Alejandro Urbano
    public static void sendMessage(int connectionId, String message) {
        if(message.length() > 100){
            System.out.println("Message is too long.");
            return;
        }

         if (connectionId <= 0 || connectionId > connections.size() || connections.get(connectionId - 1) == null) {
             System.out.println("Invalid connection ID")
                 return;
         }
        Socket socket = connections.get(connectionId -1);
        try{
            //Send the Message
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

            System.out.println("Message sent to " + connectionId);
        }catch(IOException e){
             System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private static void displayHelp() {
        System.out.println("Available Commands:");
        System.out.println("help  - Display this help menu.");
        System.out.println("myip  - Display the IP address of this process.");
        System.out.println("myport  - Display the port on which this process is listening for incoming connections.");
        System.out.println("connect  - Establishes a new TCP connection to the specified <destination> at the specified < port no.>");
        System.out.println("terminate  - Will terminate the connection listed under the specified number when LIST is used to display all connections");
        System.out.println("send  - Send the message to the host on the connection that is designated");
        System.out.println("list  - Display a numbered list of all the connections this process is part of");
        System.out.println("exit  - Close all connections and terminate this process.");
    }

    /**
     * Displays the ip address of the computer running this process. It does this
     * by connecting to google.com and getting the local address.
     */
    private static void displayIPAddress() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("google.com", 80));
            System.out.println(socket.getLocalAddress().toString().substring(1));
            socket.close();
        } catch (SocketException e) {
            System.out.println("Error getting network interfaces: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    } //Urban added

    /**
     * Removes the socket from the list of connections. This is used
     * in the ClientThread run method which will remove connections if they
     * were closed by the client or if the connection is lost.
     * @param closedSocket
     * @return This method returns a boolean signifying if the connection was removed or not
     * @see SocketThread This is where this method is used
     */
    public static boolean removeConnection(Socket closedSocket) {
        int index = 0;
        for (Socket socket : connections) {
            if(socket == closedSocket) {
                connections.set(index, null);
                return false;
            }
            index++;
        }
        return true;
    }

    /**
     * Closes all connections in the list of connections and null values
     * are ignored.
     */
    public static void closeAllConnections() {
        for(Socket socket : connections) {
            if(socket == null) continue;

            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Terminates the connection with the specified id. The id of the connection
     * can be found with the list command. This also sets the value
     * to null in the list of connections.
     * @param id The id of the connection
     */
    public static void terminateConnection(int id) {
        Socket socket = connections.get(id-1);
        if(socket == null) {
            System.out.println("This connection was already terminated");
            return;
        }

        try {
            String socketAddress = socket.getRemoteSocketAddress().toString();
            socket.close();
            System.out.println("Successfully closed connection with " + socketAddress);
            connections.set(id-1, null);
        } catch (Exception ex) {
            System.out.println("I/0 Error: " + ex.getMessage());
        }
    }

    /**
     * Prints out all user connections in the following format
     * id.  ip:port
     * ex:
     * 1.   192.168.1.1:5656
     */
    public static void listConnections() {
        System.out.println("#\tip:port");
        int socketId = 1;
        for(Socket socket : connections) {
            if(socket == null) socketId++;
            else
                System.out.println(socketId++ + ".\t"+socket.getRemoteSocketAddress().toString());
        }
    }

    /**
     * Attempts to establish a tcp connection with the hostname and port.
     * It also starts a clientThread which will listen for messages from this connection.
     * @param hostname hostname of the computer you are trying to connect to
     * @param port port of the process you are trying to connect to
     * @see SocketThread Has the code that listens for a message from this connection.
     */
    public static void connect(String hostname, int port) {
        Socket socket = null;
        try{
            socket = new Socket(hostname, port);
            Chat.connections.add(socket);
            System.out.println("Successfully connected to " + socket.getRemoteSocketAddress().toString());
            new SocketThread(socket).start();
        } catch ( UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/0 Error: " + ex.getMessage());
        }
    }

    /**
     * Prints a string from a thread that is not the main thread. The purpose
     * of this method is to ensure that the line in the console that
     * signifies the program is listening for user input isn't overwritten.
     * Prevents the following string from being overwritten in the console
     * ">> "
     *
     * Use this if you are printing from a thread that is not the main thread.
     * @param s The string that will be printed
     */
    public static void printFromThread(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append((Chat.waitingForInput) ? "\n>> ":"");
        sb.insert(0,(Chat.waitingForInput) ? '\r':"");
        System.out.print((Chat.waitingForInput) ? sb : sb.toString() + '\n');
    }
}

class Server extends Thread {
    private static int port;
    private static ServerSocket serverSocket;

    /**
     * This constructor creates a serverSocket with the specified port number.
     * @param port The port that will be used in the Server socket.
     */
    public Server(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
            System.out.println("Server is listening on port " + this.port);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Continuously listens for new connections and accepts them.
     * This socket will be added to the list of connections and a new
     * SocketThread is created to listen for a message from this connection.
     * The start function will run this method on a new thread.
     * @See SocketThread Has the code that listens for a message from this connection.
     */
    public void run() {
        try {
            while(true) {
                Socket socket = serverSocket.accept();
                Chat.printFromThread("New connection with "+socket.getRemoteSocketAddress());
                Chat.connections.add(socket);
                new SocketThread(socket).start();
            }
        } catch(IOException ex) {
            Chat.printFromThread("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     *
     * @return Returns the port that the server is running on.
     */
    public static int getMyPort() {
        return port;
    }
}

class SocketThread extends Thread {
    private Socket socket;

    public SocketThread(Socket socket) {
        this.socket = socket;
    }

    /**
     * Creates a buffered reader from the sockets input stream
     * which will be used to listen for messages that arrive from this connection.
     * If the message is null, then the connection has been closed. This method will
     * also print the message when it is received.
     */
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String text;
            do {
                text = reader.readLine();
                if(text == null) {
                    break;
                }
                //Display sender's IP and port, along with the message 
                //Alejandro Urbano added 
                String senderIp = socket.getInetAddress().getHostAddress();
                int senderPort = socket.getPort();
                Chat.printFromThread("Message received from " + senderIp);
                Chat.printFromThread("Sender's Port: " + senderPort);
                 Chat.printFromThread("Message: \"" + text + "\"");
                Chat.printFromThread(socket.getRemoteSocketAddress() + " sent a message\nMessage: "+ text);
            } while(true);

            Chat.printFromThread("The connection with "+ socket.getRemoteSocketAddress().toString() + " was terminated.");
            socket.close();
            Chat.removeConnection(socket);
        } catch(SocketException ex) {
            Chat.printFromThread("Lost connection with " + socket.getRemoteSocketAddress());
            Chat.removeConnection(socket);
        } catch (IOException ex) {
            Chat.printFromThread("Server exception: " + ex.getMessage());
        }
    }
}
