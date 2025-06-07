import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ThreadLocalRandom;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class Peer implements Serializable {

    private String username; // Onoma
    private String password; // Kwdikos
    String token_id; // token_id monadiko anagnwristiko
    static Scanner input = new Scanner(System.in);
    private String sharedDirectoryPath; // Path pou tha einai o fakelos shared_directory
    private List<String> FilePaths = new ArrayList<String>(); // Lista me paths arxeiwn pou theloume na exoume sto
                                                              // shared_directory
    private String ip_address; // Ip
    private int port; // Port
    private boolean loggedIn = false; // If peer connected
    private int count_downloads;
    private int count_failures;
    List<String> AllavailableFiles; // System available files
    List<PeerInformationToCommunicate> helperList;
    List<PeerInfo> peeriInfoForCounter;
    String NameOfFilePeerWant; // In detail operation

    // Default constructor
    public Peer() {
    }

    // Constructor
    public Peer(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Constructor
    public Peer(int port, String ip) {
        this.port = port;
        this.ip_address = ip;

    }

    // Setters & Getters
    public String getPassword() {
        return this.password;
    }

    public String getIp() {
        return this.ip_address;
    }

    public int getPort() {
        return this.port;
    }

    public void setDirectoryPath(String path) {
        this.sharedDirectoryPath = path;
    }

    public void addFilePath(String path) { // Add a path of file we want to add in shared_directory
        FilePaths.add(path);

    }

    public String getDirectoryPath() {
        return this.sharedDirectoryPath;
    }

    public String getTokenId() {
        return this.token_id;
    }

    public List<String> getFilePaths() {
        return this.FilePaths;
    }

    public List<String> getSharedFiles() { // Pairnoume ta arxeia apo ton fakelo shared_directory
        List<String> sharedFiles = new ArrayList<>();
        File sharedDirectory = new File(sharedDirectoryPath);
        File[] files = sharedDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    sharedFiles.add(file.getName());
                }
            }
        }

        return sharedFiles;
    }

    // Antigrafei to arxeio sto shared_directory apo ton disko
    public void shareFiles(List<String> sourceFilePaths) { // Dexetai lista apo monopatia arxeiwn
        File sharedDirectory = new File(sharedDirectoryPath); // Shared_diredtory
        if (!sharedDirectory.exists() || !sharedDirectory.isDirectory()) { // Elegxei an yparxei to shared_directory h
                                                                           // oxi wste na to dhmioyrghsei
            sharedDirectory.mkdirs();
        }

        for (String sourceFilePath : sourceFilePaths) { // Gia kathe path arxeiou
            File sourceFile = new File(sourceFilePath); // dhmiourgei arxeio

            if (sourceFile.exists() && sourceFile.isFile()) { // An einai pragmati arxeio kai yparxei sto path
                String targetFilePath = sharedDirectoryPath + File.separator + sourceFile.getName();
                Path targetPath = Paths.get(targetFilePath);

                try {
                    Files.copy(sourceFile.toPath(), targetPath); // Antigrafh arxeiou sto shared_directory
                    System.out.println("File '" + sourceFile.getName() + "' copied successfully.");
                } catch (IOException e) {
                    System.out.println("Error copying file '" + sourceFile.getName() + "': " + e.getMessage());
                }
            } else {
                System.out.println("Source file '" + sourceFilePath + "' does not exist or is not a regular file.");
            }
        }
    }

    // Diagrafh arxeiou apo shared_directory
    public void removeFile(String fileName) {
        File fileToRemove = new File(sharedDirectoryPath + File.separator + fileName);

        if (fileToRemove.exists() && fileToRemove.isFile()) {
            if (fileToRemove.delete()) {
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("Failed to delete file.");
            }
        } else {
            System.out.println("File does not exist or is not a regular file.");
        }
    }

    // Thelei na kanei eggrafh
    // O Tracker elegxei na mhn yparxei Peer me to idio username
    public void register() {
        Socket connection = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            boolean registrationSuccessful = false; // An exei petyxei h oxi h eggrafh
            HashMap<String, String> registerData = new HashMap<>(); // username , password pou tha steilei o Peer ston
                                                                    // Tracker

            while (!registrationSuccessful) { // while error occuring during registration -> try again the registration
                registerData.clear();
                System.out.println("--- REGISTER NOW ---");
                System.out.print("Enter username: "); // Username of new Peer
                username = input.nextLine();
                System.out.print("Enter password: "); // Password of new Peer
                password = input.nextLine();
                int passwordsize = password.length();
                String m = "*";
                System.out.println(
                        "-> Request to register as Peer: " + username + " with password: " + m.repeat(passwordsize));

                registerData.put(username, password); // Object to send to Tracker

                // Connect to server to send them the input data
                connection = new Socket("127.0.0.1", 35671); // Port of Tracker
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());

                // Send message
                out.writeUTF("PeerRegisterRequest"); // Type of request , helps Tracker to understand the type of
                                                     // request he accept
                out.writeObject(registerData); // Send the register values to Tracker
                out.flush();

                String resultOfRegister = in.readUTF(); // Answer of Tracker
                if ("Epityxia".equals(resultOfRegister)) { // If registration succeeded
                    registrationSuccessful = true; // To exit the while
                    System.out.println("Registration successful!");
                } else { // if registration failed
                    System.out.println("Username already exists. Please try again...");

                    // Tha steiloume ksana stoixeia
                    out.close();
                    in.close();
                    registerData = new HashMap<>(); // clear the old data to sent new object

                }
            }
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    } // End register method

    /*
     * Login:
     * 1) O Peer bazei ta stoixeia eisodou
     * 2) O Tracker tautopoiei ton Peer
     * 3) An petyxei h tautoppoihsei , dhmiourgeitai monadiko anagnwsristiko
     * token_Id pou tautopoiei thn syndesh
     * 4) An den petyxei , o Peer bazei ek neou stoixeia eisodou
     */
    public void Login() {
        Socket connection = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        // Check if already logged in
        if (loggedIn) {
            System.out.println("You are already logged in.");
        }

        else { // if you are not login
            try {

                boolean LoginComplete = false;

                while (!LoginComplete) { // while login fail(wrong input data or Peer that doesnt exists)

                    System.out.println("--- Login ---");
                    System.out.print("Enter username: ");
                    username = input.nextLine();
                    System.out.print("Enter password: ");
                    password = input.nextLine();
                    System.out.println(
                            "-> Request to connect the account with username: " + username);

                    // Connect to Tracker to send them the login values
                    connection = new Socket("127.0.0.1", 35671); // Port of Tracker
                    out = new ObjectOutputStream(connection.getOutputStream());
                    in = new ObjectInputStream(connection.getInputStream());
                    HashMap<String, String> LoginData = new HashMap<>(); // Object to send to Tracker

                    LoginData.put(username, password);
                    out.writeUTF("PeerLoginRequest"); // Type of message that Tracker receive

                    // Send message and receive response
                    out.writeObject(LoginData); // Send the object to Tracker
                    out.flush();

                    String resultOfRegister = in.readUTF(); // Response of Tracker
                    if ("Epityxia".equals(resultOfRegister)) { // An syndethei tote tha dhmioyrghthei token_id
                        LoginComplete = true;
                        // O Tracker thelei na diathrei domh dedomenwn me ta stoixeia
                        // energwn Peer , otan kapoios kanei login tou stelnei stoixeia
                        HashMap<String, String> IpAndPort = new HashMap<>();
                        String portString = String.valueOf(this.port);
                        IpAndPort.put(this.ip_address, portString);
                        out.writeObject(IpAndPort); // send the Ip and Port of new Peer
                        out.flush();
                        HashMap<Integer, Integer> Counters = new HashMap<>();
                        out.writeObject(Counters); // send the counter_download and counter_failuresof new Peer
                        out.flush();
                        token_id = in.readUTF(); // token_id of new Peer
                        System.out.println("Token_id: " + token_id);
                        System.out.println("Login Complete!");
                        loggedIn = true; // Peer declare that is Loggedin
                    } else {
                        System.out.println("Error occured during Login...check your input data");

                        // Tha steiloume ksana stoixeia
                        out = new ObjectOutputStream(connection.getOutputStream());
                        in = new ObjectInputStream(connection.getInputStream());
                        LoginData = new HashMap<>(); // clear the old sent object to create the new

                    }
                }
            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                try {
                    out.close();
                    in.close();
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    } // End method Login

    /*
     * O Peer enimeronei ton tracker gia ta periexomena tou shared_directory kai gia
     * ip, port
     * Einai aparaihto meta apo kathe login na enhmerwnei
     * ton Tracker(Inform) gia ta periexomena tou kathws exei monadiko tokenId
     * se kathe synthesh kai o Tracker prepei na enhmerwsh thn domh me files kai
     * Token_id
     */
    public void informTracker() {
        Socket connection = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        if (!loggedIn) { // Prepei na einai syndemenos wste na enhmerwsei ton Tracker
            System.out.println("You are not logged in.");

        }

        else {

            try {

                connection = new Socket("127.0.0.1", 35671); // Port of Tracker
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                List<String> sharedFiles = getSharedFiles(); // Lista me periexomena tou shared_directory

                /*
                 * Enhmerwnei ton Tracker gia :
                 * 1) Ip
                 * 2) Port
                 * 3) Periexomeno shared_directory
                 */
                String ipAddress = this.getIp();
                int port = this.getPort();
                String p = Integer.toString(port);
                out.writeUTF("Inform"); // Type of message that Tracker receives
                out.writeObject(sharedFiles); // Stelnei periexomeno shared_directory
                out.writeUTF(ipAddress); // Stelnei ip
                out.writeUTF(p); // Stelnei Port
                out.writeUTF(token_id); // Stelnei token_id ths syndeshs
                out.flush();

                // Apanthsh apo ton tracker
                String response = in.readUTF();
                System.out.println("Tracker response: " + response);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                    if (connection != null)
                        connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // O Peer apoxwrei , katargeitai to token_id ths syndeshs
    public void Logout() {
        if (token_id != null && !token_id.isEmpty()) { // An yparxei to token_id tou Peer
            Socket connection = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;

            try {

                // Connect to the tracker
                connection = new Socket("127.0.0.1", 35671); // Port of the Tracker
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                HashMap<String, String> a = new HashMap<>(); // username , password
                out.writeUTF("LOGOUT"); // Send logout request with the stored token_id
                out.flush();
                a.put(username, token_id);
                out.writeObject(a);
                out.flush();

                // Receive response from the tracker
                String response = in.readUTF();
                if ("SUCCESS".equals(response)) { // If logout is successful
                    System.out.println("Logout successful!");
                    loggedIn = false;
                } else {
                    System.out.println("Logout failed. Please try again.");
                }

            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                try {
                    out.close();
                    in.close();
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("You are not logged in.");
        }
    }

    // Zhta apo ton Tracker thn lista me ta onomata twn arxeiwn pou
    // einai synolika diathesima sto P2P
    public void list() {
        Socket connection = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            // Connect to the Tracker
            connection = new Socket("127.0.0.1", 35671); // Port of the Tracker
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());

            // Send the list request to the Tracker
            out.writeUTF("ListOfFilesRequest"); // Type of request that Tracker receives
            out.flush();

            // Receive the list of available files from the Tracker
            AllavailableFiles = (List<String>) in.readObject();

            // Print the list of available files
            System.out.println("Available files:");
            for (String file : AllavailableFiles) { // printing the name of files
                System.out.println("-> " + file);
            }
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (connection != null)
                    connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Zhta plhrofories gia ena arxeio
    public void details() {
        Socket connection = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        List<PeerInformationToCommunicate> InformOfPeers = new ArrayList<PeerInformationToCommunicate>(); // Plhrofories
                                                                                                          // twn Peer
                                                                                                          // pou
                                                                                                          // periexoun
                                                                                                          // to arxeio
        List<PeerInfo> PeerInformationTochangeCounter = new ArrayList<>();
        // Prwta pepei na exei mathei o Peer poia arxeia yparxoun sto systhma

        if (this.AllavailableFiles == null || loggedIn == false) { // Apo thn methodo list lambanoume ta
            if (this.AllavailableFiles == null && loggedIn == true) { // AvailableFiles
                System.out.println(
                        "Prwta prepei na deis poia arxeia einai diathesima sto systhma --> Leitouthia list  !");
            } else {
                System.out.println("Peer not loggedIn");
            }
        } else {
            try {
                System.out.println("Ta diathesima arxeia einai :");
                for (String file : this.AllavailableFiles) { // Emfanisi twn arxeiwn ston Peer
                    System.out.println(file);

                }
                while (true) {
                    System.out.print("Onoma arxeiou pou thes plhrofories: ");
                    String name = input.nextLine();
                    if (!AllavailableFiles.contains(name)) { // An den yparxei sto arxeio sto fileDownloadlist
                        System.out.println("To onoma tou arxeiou den brisketai ston Katalogo!");
                    } else if (fileExists(name)) { // Elegxei an to arxeio yparxei hdh ston Peer
                        System.out.println("To arxeio auto yparxei hdh se auto ton Peer");
                    } else { // Stelnei to onoma tou arxeiou

                        // Connect to the Tracker
                        connection = new Socket("127.0.0.1", 35671); // Port of the Tracker
                        out = new ObjectOutputStream(connection.getOutputStream());
                        in = new ObjectInputStream(connection.getInputStream());

                        // Send request to the Tracker
                        out.writeUTF("details"); // Type of request
                        out.flush();
                        NameOfFilePeerWant = name;
                        out.writeUTF(name); // Stelnei to onoma tou arxeiou
                        out.flush();
                        break; // Exit the loop if a valid file name is entered
                    }
                }

                String response = in.readUTF();
                if ("Epityxia".equals(response)) { // An elabe Peers pou exoun to arxeio
                    InformOfPeers = (List<PeerInformationToCommunicate>) in.readObject();
                    PeerInformationTochangeCounter = (List<PeerInfo>) in.readObject(); // Se ayth thn domh yparxoun
                                                                                       // kai
                                                                                       // ta counters

                    helperList = InformOfPeers;
                    peeriInfoForCounter = PeerInformationTochangeCounter;
                    System.out.println("H lista periexei tous Peers:");
                    for (PeerInformationToCommunicate a : InformOfPeers) {
                        System.out.println(a.getUserName());
                    }

                } else { // Apotyxia
                    System.out.println("To arxeio den yparxei se kapoio Peer");
                }

            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                    if (connection != null)
                        connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    } // end method details

    // Blepei an o Peer me peerIp , peerPort einai energos
    public boolean checkActive(String peerIp, int peerPort) { // Check if a Peer is Active
        boolean isActive = false;
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(peerIp, peerPort), 3000); // Timeout set to 3 seconds ,if
                                                                                    // timeout-> peer not exists
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeUTF("CheckActivePeer"); // Type of message
            out.flush();

            // Read the response
            String response = in.readUTF(); // response from Peer
            if ("Active".equals(response)) {
                System.out.println("--> The IP: " + peerIp + " port: " + peerPort +
                        " responded that it is active.");
                isActive = true; // Peer responded successfully
            } else {
                System.out.println("--> The IP: " + peerIp + " port: " + peerPort +
                        " responded that it is not active.");
            }
        } catch (ConnectException e) {
            System.out.println(
                    "Connection to the peer at IP: " + peerIp + " port: " + peerPort +
                            " failed. Peer may not exist.");
        } catch (SocketTimeoutException e) {
            System.out.println(
                    "Connection to the peer at IP: " + peerIp + " port: " + peerPort +
                            " timed out. Peer may not be found.");
        } catch (IOException e) {
            // Error occurred during the communication
            isActive = false;
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isActive;
    }

    // Apantaei se aithmata CheckActive || file requests
    public void respondToRequests() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try {
            // Create a server socket to listen for incoming connections
            serverSocket = new ServerSocket(this.port);

            // Listen for incoming connections
            while (true) {
                // Accept incoming connection
                socket = serverSocket.accept();
                System.out.println("Connection accepted from: " + socket.getInetAddress());
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                String request = in.readUTF(); // Request message

                // Handle "CheckActivePeer" request
                if ("CheckActivePeer".equals(request) && loggedIn) {
                    System.out.println("Eimai Energos");
                    out.writeUTF("Active");
                    out.flush();
                    // Handle file request
                } else if ("RequestFile".equals(request)) {
                    String fileName = in.readUTF(); // Read file name
                    System.out.println("Elegxos fileExists gia arxeio: " + fileName);
                    System.out.println("Apotelesma fileExists: " + fileExists(fileName));
                    if (fileExists(fileName)) {

                        out.writeUTF("FileAvailable"); // To arxeio yparxei ston Peer
                    } else {
                        out.writeUTF("FileNotAvailable");// To arxeio den yparxei ston Peer
                    }
                    out.flush();
                } else {
                    out.writeUTF("InvalidRequest");
                    out.flush();
                }
            }
        } catch (

        IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Elegxei an to arxeio yparxei sto fakelo shared_directory tou Peer
    private boolean fileExists(String fileName) {
        // Define the directory where the peer's files are stored
        String directoryPath = this.getDirectoryPath();
        // Create a Path object for the file
        String filePath = directoryPath + "/" + fileName;
        return Files.exists(Paths.get(filePath));
    }

    // Thelei na katebasei to arxeio apo Peer
    public void simpleDownload() {
        List<PeerInformationToCommunicate> peers = helperList; // Lista me Peers pou epestrepse h details
        HashMap<PeerInformationToCommunicate, Double> PeersAndTimes = new HashMap<>();
        List<Double> times = new ArrayList<>();
        String fileName = NameOfFilePeerWant; // Apo thn details
        PeerInfo helperPeer = null;

        for (PeerInformationToCommunicate peer : peers) { // Lista me Peers pou exoun to arxeio pou zhthse o Peer
            for (PeerInfo p : peeriInfoForCounter) {
                if (p.getUserName().equalsIgnoreCase(peer.getUserName())) {
                    helperPeer = p;
                    break;
                }
            }

            // Stelnei se kathe Peer checkActive kai metraei to response time
            long startTime = System.currentTimeMillis(); // Arxh metrhshs xronou
            checkActive(peer.getIpAddress(), peer.getPort()); // Stelnei checkActive
            long endTime = System.currentTimeMillis(); // Te;os metrhshs

            long responseTime = endTime - startTime; // ypologismos synolikou xronou

            count_downloads = helperPeer.getCountDownloads();
            count_failures = helperPeer.getCountFailures();

            double combinedIndex = responseTime * Math.pow(0.75, count_downloads) * Math.pow(1.25, count_failures);
            PeersAndTimes.put(peer, combinedIndex);
            times.add(combinedIndex);
            System.out.println("O Peer me username: " + helperPeer.getUserName() + " apokrithike se: " + combinedIndex);
            System.out.println("O Peer " + helperPeer.getUserName() + " exei: ");
            System.out.println("O Peer exei count_failures: " + helperPeer.getCountFailures() + " count_downloads: "
                    + helperPeer.getCountDownloads());
        } // end for

        PeerInformationToCommunicate peertosent = null;
        Collections.sort(times); // Sort thn lista me tous xronous apokrisi twn Peer
        /*
         * Briskoume ton Peer pou ekane ton syntomotero xrono kai stelnoume request se
         * auton
         * An den einai active tote tha steiloume ston epomeno kalytero
         */
        for (Double d : times) {
            System.out.println("\nO kalyteros xronos einai o: " + d);
            // Briskoume ton Peer me ton sygkekrimeno xrono apokrisis
            for (Map.Entry<PeerInformationToCommunicate, Double> entry : PeersAndTimes.entrySet()) {
                if (entry.getValue().equals(d)) {
                    peertosent = entry.getKey();
                    break; // Exit the loop once the desired value is found
                }
            }
            System.out.println("Send request to Peer: --> " + peertosent.getUserName());
            if (this.requestFileFromPeer(peertosent, fileName)) {
                return;
            }
            System.out.println("Aithma ston epomeno kalytero Peer");
        }
        System.out.println("Den brethike Peer me auto to arxeio");

    } // End simpleDownload

    /*
     * Enas Peer stelnei se enan allo Peer aithma gia na dei an exei to
     * arxeio me onoma: fileName, an to arxeio yparxei ston paralipti Peer kai autos
     * einai energos tote apantaei thetika ston apostolea Peer se diaforetiki
     * periptwsh apantaei arnitika. Epeita o apostoles Peer enhmerwnei ton Tracker
     * me thn leitourgia notifyP an petyxe h oxi to download tou arxeiou
     */
    private boolean requestFileFromPeer(PeerInformationToCommunicate peer, String fileName) {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket = new Socket(peer.getIpAddress(), peer.getPort());
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeUTF("RequestFile"); // type of request
            out.writeUTF(fileName); // send the name of file to Peer
            out.flush();

            String response = in.readUTF(); // response of Peer that we sent the request
            if ("FileAvailable".equals(response) && checkActive(peer.getIpAddress(), peer.getPort())) {

                // Ginetai to addittion tou arxeiou sto shared_directory tou Peer
                this.getFilePaths().clear();
                this.addFilePath("C:/Users/user/Downloads/3210107_3210149/" + fileName); // add to FilePaths
                this.shareFiles(this.getFilePaths()); // Add to folder the files from FilePaths
                notifyP(true, fileName, peer.getUserName()); // Enhmerwnei ton Tracker oti exei to arxeio
                return true;

            } else {
                System.out.println("To arxeio den yparxei ston Peer h o Peer einai inactive");
                notifyP(false, fileName, peer.getUserName()); // Enhmerwnei ton Tracker gia apotyxia
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // O Peer enhmerwnei ton Tracker
    public void notifyP(boolean success, String fileName, String requestedUserName) {

        /*
         * Inputs:
         * success --> An petyxe h oxi to simpleDownload
         * fileName --> Onoma arxeiou pou thelei o Peer
         * requestedUserName --> Onoma Peer pou zitithike apo auton na steilei to arxeio
         */
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            // Connect to the tracker
            socket = new Socket("127.0.0.1", 35671);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeUTF("notify");

            if (success) {
                // Notify the tracker about successful download
                out.writeUTF("NotifySuccess");
                out.writeUTF(fileName); // Send the file name
                out.writeUTF(token_id);
                out.writeUTF(requestedUserName); // Send the requested user name
            } else {
                // Notify the tracker about download failure
                out.writeUTF("NotifyFailure");
                out.writeUTF(fileName); // Send the file name
                out.writeUTF(token_id);
                out.writeUTF(requestedUserName); // Send the requested user name
            }

            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // check the format of ip
    public static boolean IpCheck(String ip) { // For example Ip: 127.0.01
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(ipPattern, ip);
    }

    // check the format of port
    public static boolean PortCheck(String port) { // Port must be integer [0,65535]
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber >= 0 && portNumber <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {

        /*
         * Dhmiourgia Peer:
         * 1) Dhmiourgia port
         * 2) Ip
         * 3) shared_directory
         * 4) Prosthiki arxeiwn sto shared_directory apo to filedownloadList.txt
         * 
         */
        Random random = new Random();
        int port = 3100 + random.nextInt(1000); // Generate a random port number between 5000 and 5999
        String ip = "127.0.0.1";
        Peer p = new Peer(port, ip);
        System.out.println("Peer: ip " + p.getIp() + " port: " + p.getPort());

        // Enas Peer mporei na dexetai apo allous Peer h apo ton Tracker
        // requests(checkActive, requestFile)
        Thread respondThread = new Thread(() -> {
            p.respondToRequests();
        });
        respondThread.start();

        // Folder shared_directory tou Peer
        String foldername = "shared" + random.nextInt(1000); // Dimiourgia shared_directory
        p.setDirectoryPath("C:/Users/user/Downloads/3210107_3210149/" + foldername); // Path tou shared_directory
        // Copy 2 arxeia apo to fileDownloadList sto shared_direcotry tou Peer
        int fileNumber1 = random.nextInt(5) + 1;
        String filename = "arxeio" + fileNumber1 + ".txt";
        p.addFilePath("C:/Users/user/Downloads/3210107_3210149/" + filename);

        int fileNumber2;
        do {
            fileNumber2 = random.nextInt(5) + 1;
        } while (fileNumber1 == fileNumber2); // An to arxeio 1 != arxeio 2

        String filename2 = "arxeio" + fileNumber2 + ".txt";
        p.addFilePath("C:/Users/user/Downloads/3210107_3210149/" + filename2);
        p.shareFiles(p.getFilePaths()); // add the files to the shared_direcotry

        // Menou Epilogwn
        while (true) {
            System.out.println("\nEpilogi leitourgias:");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3.Logout");
            System.out.println("4.Inform");
            System.out.println("5.List");
            System.out.println("6.checkActive");
            System.out.println("7.details");
            System.out.println("8.simpleDownload");
            String answer = input.nextLine();
            switch (answer) {
                case "1":
                    p.register();
                    break;

                case "2":
                    p.Login();
                    break;
                case "3":
                    p.Logout();
                    break;
                case "4":
                    p.informTracker();
                    break;
                case "5":
                    p.list();
                    break;
                case "6":
                    System.out.print("Bale stoixeia Peer pou thes na deis an einai energos: ");
                    while (true) {
                        System.out.print("Ip: ");
                        String peerIp = input.nextLine();
                        System.out.print("Port: ");
                        String peerPort = input.nextLine();

                        if (!IpCheck(peerIp)) {
                            System.out.println("Invalid IP address format. Please enter a valid IPv4 address.");
                        } else if (!PortCheck(peerPort)) {
                            System.out.println("Invalid port number. Please enter a port number between 0 and 65535.");
                        } else {
                            p.checkActive(peerIp, Integer.parseInt(peerPort));

                        }
                        break;
                    }
                    break;

                case "7":
                    p.details();
                    break;
                case "8":
                    p.simpleDownload();
                    break;
            } // END switch
        }
    }
}