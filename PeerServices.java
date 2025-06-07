import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("all")
public class PeerServices extends Thread implements Serializable {
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket connection;
    String typeOfRequest;
    Tracker tracker;
    boolean registrationSuccessful = false;
    String token_id;
    private final ReentrantLock countLock = new ReentrantLock();

    // Constructor
    public PeerServices(Socket connection, Tracker tracker) {
        try {
            this.connection = connection;
            this.out = new ObjectOutputStream(connection.getOutputStream());
            this.in = new ObjectInputStream(connection.getInputStream());
            this.tracker = tracker;
            this.typeOfRequest = in.readUTF(); // typoes aithmatos pou lambanei o Peer gia na eksyphrethsh me to Thread
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() { // Execute the method which satisfies the request

        if (typeOfRequest.equalsIgnoreCase("PeerRegisterRequest")) {
            System.out.println("Tracker: elaba request apo Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            registerPeer(); // Handle peer registration
        }

        else if (typeOfRequest.equalsIgnoreCase("PeerLoginRequest")) {
            System.out.println("Tracker: elaba request apo Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            LoginPeer(); // Handle Login peer
        }

        else if (typeOfRequest.equalsIgnoreCase("LOGOUT")) {
            System.out.println("Tracker: elaba request apo Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            Logout(); // Handle Peer logout
        }

        else if (typeOfRequest.equalsIgnoreCase("Inform")) {
            System.out.println("Tracker: elaba request apo Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            Inform(); // Handle Peer Inform

        } else if (typeOfRequest.equalsIgnoreCase("ListOfFilesRequest")) {
            System.out.println("Tracker: elaba request apo Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            reply_List(); // Handle Peer list
        }

        else if (typeOfRequest.equalsIgnoreCase("details")) {
            System.out.println("Tracker: elaba request apo ton Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            reply_details(); // Handle Peer details
        }

        else if (typeOfRequest.equalsIgnoreCase("notify")) {
            System.out.println("Tracker: elaba request apo ton Peer! ");
            System.out.println("Type of request= " + typeOfRequest);
            notifyPeer(); // Handle Peer notify
        }
    }

    // Check if the given username already exists ,otherwise create the new Peer and
    // save his informations
    public void registerPeer() { // --> Use for registration
        try {
            String username = null;
            String password = null;
            HashMap<String, String> receiveObject;
            boolean registrationSuccessful = false;

            while (!registrationSuccessful) { // while the registration did not succeed
                try {
                    Object object = in.readObject(); // request object
                    receiveObject = (HashMap<String, String>) object; // Object that Peer send
                    for (Map.Entry<String, String> entry : receiveObject.entrySet()) { // Anlayoume to object pou labame
                        username = entry.getKey();
                        password = entry.getValue();
                    }
                    registrationSuccessful = tracker.register(username, password); // metablhth boolean , elegxei oti
                                                                                   // den yparxei Peer me to idio
                                                                                   // username

                    if (registrationSuccessful) { // An den yparxei Peer me auto to username
                        System.out.println("Registration of Peer " + username + " succeeded!");
                        out.writeUTF("Epityxia"); // O Peer lambanei Epityxia
                        out.flush();
                    } else { // An yparxei Peer me to idio username
                        System.out.println("Username " + username + " already exists. Please try again.");
                        out.writeUTF("Apotyxia"); // O Peer lambanei Apotyxia
                        out.flush();
                        break;
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Tracker check if input data is the same as the user gave to the registration
    public void LoginPeer() {
        try {
            String username = null;
            String password = null;
            HashMap<String, String> receiveObject;
            boolean LoginSuccessful = false;

            while (!LoginSuccessful) {
                try {
                    Object object = in.readObject();
                    receiveObject = (HashMap<String, String>) object; // Object that Peer send
                    for (Map.Entry<String, String> entry : receiveObject.entrySet()) { // Anlyoume to object pou labame
                        username = entry.getKey();
                        password = entry.getValue();
                    }
                    LoginSuccessful = tracker.login(username, password); // checks if the given values ​​exist with any
                                                                         // peer
                    if (LoginSuccessful) { // now we must get the ip,port,count_downloads,dount_failures
                        String ip = "";
                        int port = 0;
                        int count_downloads = 0;
                        int count_failures = 0;
                        HashMap<String, String> receiveObject1; // ip and port
                        HashMap<Integer, Integer> receiveObject2; // counter_downloads, counter_failures
                        System.out.println("Login " + username + " succeeded!");
                        out.writeUTF("Epityxia");
                        out.flush();
                        token_id = tracker.generateRandomToken(); // create token_Id for the connection between this
                                                                  // peer and tracker

                        // Receive Ip and Port
                        Object object2 = in.readObject(); // request object
                        receiveObject1 = (HashMap<String, String>) object2;
                        for (Map.Entry<String, String> entry : receiveObject1.entrySet()) { // Anlyoume to object pou
                                                                                            // labame
                            ip = entry.getKey();
                            port = Integer.valueOf(entry.getValue());
                        }
                        System.out.println("Elaba kai to ip: " + ip + " kai to port " + port);

                        // Reiceve counters
                        Object object3 = in.readObject(); // request object
                        receiveObject2 = (HashMap<Integer, Integer>) object3;
                        for (Map.Entry<Integer, Integer> entry : receiveObject2.entrySet()) { // Anlyoume to object pou
                                                                                              // labame
                            count_downloads = Integer.valueOf(entry.getKey());
                            count_failures = Integer.valueOf(entry.getValue());
                        }
                        System.out.println("Elaba kai to count_downloads " + count_downloads
                                + " kai to  count_failures " + count_failures);

                        // Apothikeuontai ta stixeia tou Peer sthn domh activePeers
                        tracker.addInformations(token_id, ip, port, username, count_downloads, count_failures);
                        System.out.println("Tracker : Dimiourghsa token_id ths syndeshs: " + token_id);
                        out.writeUTF(token_id);
                        out.flush();
                    } else {
                        System.out.println("Login " + username + " fail... Please try again.");
                        out.writeUTF("Apotyxia");
                        out.flush();

                        // Tha dextei ksana stoixeia an elabe lathasmena stoixeia eisodou
                        receiveObject.clear();
                        out = new ObjectOutputStream(connection.getOutputStream());
                        in = new ObjectInputStream(connection.getInputStream());
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /*
     * O tracker apodechetai tin apochorisi, stelnei minyma epitychous
     * apochorisis ston peer, kai enimeronei katallila tis domes dedomenon pou
     * diatirei
     * sxetika me tous peers kai akyronei to token_id.
     */

    public void Logout() {
        try {
            String username = null;
            String password = null;
            HashMap<String, String> receiveObject;
            boolean LoginSuccessful = false;
            try {

                Object object = in.readObject(); // request object
                receiveObject = (HashMap<String, String>) object; // username . token_id
                for (Map.Entry<String, String> entry : receiveObject.entrySet()) { // Anlyoume to object pou labame
                    username = entry.getKey();
                    token_id = entry.getValue();
                }
                System.out.println("Elaba request apo ton Peer " + username + " me id " + token_id + " gia Logout");
                if (tracker.deleteToken(token_id)) { // Diagrafei to username - tokein_id apo thn domi dedomenwn
                    tracker.removeTokenFromLists(token_id);

                    out.writeUTF("SUCCESS"); // Stelnei ston Peer oti peryxe h aposyndesi
                    out.flush();
                } else {
                    out.writeUTF("NOTSUCCESS"); // Stelnei ston Peer oti apetyxe h aposyndesi
                    out.flush();

                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Lambanei apo ton Peer ta periexomena tou share_directory kai tis plhrofroies
    // epikoinwnias (ip,port)
    public void Inform() {
        try {
            List<String> sharedFiles = null; // Periexomeno shared_drectory
            String ip;
            String port;
            String token_id;
            HashMap<String, String> receiveObject;
            boolean LoginSuccessful = false;
            try {

                Object object = in.readObject(); // request object
                sharedFiles = (List<String>) object; // arxeia pou exei
                ip = in.readUTF();
                port = in.readUTF();
                token_id = in.readUTF();
                for (String arxeio : sharedFiles) { // enhmerwnei thn domh pou exei gia kathe arxeio ta token_id pou to
                                                    // periexoun
                    tracker.addTokenIdToMatchingFiles(arxeio, token_id);
                }

                System.out.println("Elaba apo ton Peer " + port + " me ip " + ip + " list " + sharedFiles);
                out.writeUTF("Elaba oles tis plirofories epikoinwnias tou Peer");
                System.out.println("Periexomeno filesAndId meta thn inform tou Peer: ");
                tracker.printFile(); // Emfanizei to periexomeno ths domhs
                out.flush();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // return to the Peer the list of names of files in the system
    public void reply_List() {
        List<String> fileNames = null;
        try {

            fileNames = tracker.reply_List(); // Read the files from fileDownloadList.txt and save in List
            out.writeObject(fileNames); // Send the List to Peer
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Lambanei apo ton Peer onoma enos arxeiou kai epistrefei plhrofories gia toys
    // Peers pou to periexoun
    public void reply_details() {
        String nameOfFile;
        // Peers pou exoun to arxeio
        List<PeerInformationToCommunicate> PeersThatContainsTheFile = new ArrayList<PeerInformationToCommunicate>();
        List<PeerInfo> listWithPeerInfo = tracker.getPeerInfoList();
        try {
            nameOfFile = in.readUTF(); // Onoma arxeiou pou esteile o Peer
            System.out.println("Elaba oti o Peer thelei plhrofories gia to arxeio: " + nameOfFile);

            // Peers pou exoun to arxeio auto
            List<String> tokenIds = tracker.findPeersHaveTheFile(nameOfFile);

            // Apo autous tha broume poioi einai Active stelnontas checkActive ston kathena
            // Tha baloume ta stoixeia twn Active se lista wste na steiloume auta stwn Peer
            for (String tokenid : tokenIds) {
                for (Map.Entry<String, PeerInformationToCommunicate> entry : tracker.getactivePeers().entrySet()) {
                    String Otoken_id = entry.getKey();
                    PeerInformationToCommunicate p = entry.getValue();
                    if (Otoken_id.equals(tokenid)) {
                        String ip = p.getIpAddress();
                        int port = p.getPort();
                        if (tracker.checkActive(ip, port)) { // Rwtaei an o Peer einai energos
                            PeersThatContainsTheFile.add(p); // an einai energos to bazoume gia na steiloume ta stoixeia
                            System.out.println("Me port: " + port + " einai energos");
                        }
                        break;
                    }
                }
            }
            if (PeersThatContainsTheFile.size() > 0) { // An h lista exei toulaxiston ena stoixeio
                out.writeUTF("Epityxia");
                out.flush();
                out.writeObject(PeersThatContainsTheFile); // Stelnoume ston Peer thn lista
                out.flush();
                out.writeObject(listWithPeerInfo);
                out.flush();
                System.out.println(
                        "Esteila lista me " + PeersThatContainsTheFile.size() + " energous Peers pou exoun to arxeio");
            } else { // An h lista den exei kanena Peer -> Apotyxia
                out.writeUTF("Apotyxia");
                out.flush();
                System.out.println("Kanenas Peer den exei to arxeio");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void notifyPeer() {
        /*
         * --> An to katebasma tou arxeiou petyxe ston Peer
         * tote enhmerwnei thn bash me ta fileNames kai ta token_Id
         * kai auksanei to count_downloads tou Peer pou esteile to arxeio
         * --> An to katebasma den petyxe , auksanei to count_failures tou
         * Peer pou esteile to arxeio
         */
        //countLock.lock();
        try {

            String TypeOfNotification = in.readUTF(); // Read notification type

            if ("NotifySuccess".equals(TypeOfNotification)) {
                String fileName = in.readUTF(); // Read file name
                String tokenId = in.readUTF(); // Read token ID
                String requestedUserName = in.readUTF(); // Read requested user name

                // Handle successful download notification
                System.out.println("Received successful download notification:");
                System.out.println("File Name: " + fileName);
                System.out.println("Token ID: " + tokenId);
                System.out.println("Requested User Name: " + requestedUserName);
                tracker.addTokenIdToMatchingFiles(fileName, tokenId); // Enhmerwsh domhs me arxeia-token_id

                PeerInfo p = tracker.findPeer(requestedUserName); // Briskei sthn domh ta stoixeia tou Peer
                                                                  // pou esteile to arxeio
                if (p == null) {
                    System.out.println("Den brhka Peer me auta ta stoixeia");
                } else {
                    p.increaseCountDownloads();
                }

                System.out.println("Ananewmenh lista token_id - files");
                tracker.printFile();

            } else if ("NotifyFailure".equals(TypeOfNotification)) {
                String fileName = in.readUTF(); // Read file name
                String tokenId = in.readUTF(); // Read token ID
                String requestedUserName = in.readUTF(); // Read requested user name

                // Handle download failure notification
                System.out.println("Received download failure notification:");
                System.out.println("File Name: " + fileName);
                System.out.println("Token ID: " + tokenId);
                System.out.println("Requested User Name: " + requestedUserName);

                PeerInfo p = tracker.findPeer(requestedUserName); // Briskei ton Peer pou astoxhse
                p.increaseCountFailures();

                // handle the download failure
            } else {
                System.out.println("Invalid notification type received");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //countLock.unlock();
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}