import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.*;
import java.net.*;
import java.io.File;
import java.util.ArrayList;
import java.nio.file.Files;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("all")
public class Tracker {
    Scanner scanner = new Scanner(System.in);
    ServerSocket providerSocket;
    Socket connection = null;
    ObjectInputStream in = null;
    ObjectOutputStream out = null;

    private ConcurrentHashMap<String, String> registeredUsers = new ConcurrentHashMap<>();
    private List<PeerInfo> informForRegisteredPeers = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<String, PeerInformationToCommunicate> activePeers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<String>> filesAndId = new ConcurrentHashMap<>();

    void openServer() { // Open the serverSocket and accept requests from Peers
        try {

            providerSocket = new ServerSocket(35671, 10); // Server Port for requests

            while (true) {

                connection = providerSocket.accept(); // Apodexetai synexws request
                System.out.println("-> Peer connect accepted! " + connection);
                Thread r = new PeerServices(connection, this); // Nhma pou eksipiretei to request
                r.start();

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    public boolean register(String username, String password) { // An den yparxei Peer me auto ton username tote
        synchronized (registeredUsers) {
            if (!registeredUsers.containsKey(username)) { // Den yparxei sto HashMap Peer me auto to onoma
                registeredUsers.put(username, password); // Apothikeuei ta stoixeia
                PeerInfo newPeer = new PeerInfo(username, password); // Dhmiourgei antikeimeno PeerInfo(onoma,kwdiko)
                informForRegisteredPeers.add(newPeer); // Apothikeuei ta stoixeia

                return true; // Complete registry
            } else {
                return false; // Peer with this name already exists
            }
        }
    }

    public boolean login(String username, String password) { // Gia aithma eisodou apo ton Peer
        synchronized (registeredUsers) {
            // Check if the username exists
            if (registeredUsers.containsKey(username)) {
                String storedPassword = registeredUsers.get(username); // If the username exists, check the stored
                                                                       // password
                if (password.equals(storedPassword)) { // Check if the provided password matches the stored password
                    // successful
                    return true;

                } else {
                    // wrong password
                    return false;
                }
            } else {
                // username doesn't exist, login failed due to unknown username
                return false;
            }
        }
    }

    public List<PeerInfo> getPeerInfoList() {
        synchronized (this.informForRegisteredPeers) {
            return this.informForRegisteredPeers;
        }
    }

    public String generateRandomToken() { // Dhmiourgia tokein_id gia na apodwsei otan o Peer kanei login
        // Generate a random token_id
        Random random = new Random();
        String token_id = Integer.toString(random.nextInt(2500000));
        return token_id;
    }

    public ConcurrentHashMap<String, String> getRegisteredUsers() {
        synchronized (this.registeredUsers) {
            return this.registeredUsers;
        }
    }

    public boolean deleteToken(String token_id) { // Akyrwsh token otan o Peer aposyndethei-logout
        boolean flag = true;
        synchronized (activePeers) {
            if (activePeers.containsKey(token_id)) { // An yparxei to token_id apothikeumeno shn lista me active Peers
                activePeers.remove(token_id); // To diagrafei
                System.out.println("Token " + token_id + " deleted.");
            } else {
                flag = false;
                System.out.println("Den brethike Peer me auto to token");
            }
            return flag;
        }
    }

    // Otan enas Peer kanei login tote apothikeuontai ta stoixeia tou sthn domh
    // activePeers
    public void addInformations(String tokein_id, String ip_address, int port, String user_name,
            int count_downloads, int count_failures) {
        synchronized (activePeers) {
            PeerInformationToCommunicate p = new PeerInformationToCommunicate(ip_address, port, user_name,
                    count_downloads,
                    count_failures);
            activePeers.put(tokein_id, p);
        }
    }

    // Leitourgia reply_List
    // Diabazei ta arxeia pou yparxoun ston arxeio fileDownloadList.txt , ta bazi se
    // lista kai stelnei ston Peer
    public List<String> reply_List() {
        List<String> filenames = new ArrayList<>(); // name of files

        try (BufferedReader reader = new BufferedReader( // Diabazei to periexomeno tou arxeiou
                new FileReader("C:/Users/user/Downloads/3210107_3210149/fileDownloadList.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                filenames.add(line.trim()); // Add filename to list after trimming whitespace
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filenames;
    }

    // Blepei an enas Peer einai Active
    public boolean checkActive(String peerIp, int peerPort) {
        boolean isActive = false;
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(peerIp, peerPort), 3000); // Timeout set to 3 seconds
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeUTF("CheckActivePeer"); // Message to the Peer
            out.flush();

            // Read the response
            String response = in.readUTF();
            if ("Active".equals(response)) {
                System.out.println("The IP: " + peerIp + " port: " + peerPort +
                        " responded that it is active.");
                isActive = true; // Peer responded successfully
            } else {
                System.out.println("The IP: " + peerIp + " port: " + peerPort +
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

    // check the format of ip
    public static boolean IpCheck(String ip) { // Example Ip: 127.0.0.1 -> True , Ip: 123 -> False
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(ipPattern, ip);
    }

    // check the format of port
    public static boolean PortCheck(String port) { // Port must be Integer [0,65525]
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber >= 0 && portNumber <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public ConcurrentHashMap<String, List<String>> getfilesAndId() { // return filesAndId
        synchronized (this.filesAndId) {
            return this.filesAndId;
        }
    }

    public void addTokenIdToFile(String filename, String tokenId) { // we use this method to add a file in hashmap
                                                                    // filesAndId
        synchronized (filesAndId) {
            List<String> tokenIds = filesAndId.getOrDefault(filename, new ArrayList<>());
            tokenIds.add(tokenId);
            filesAndId.put(filename, tokenIds);
        }
    }

    /*
     * An enas Peer periexei to arxeio, tote apothikeuei sthn domh filesAndId oti o
     * Peer me to sygkekrimeno token_id exei to arxeio
     */
    public void addTokenIdToMatchingFiles(String filenameToMatch, String tokenId) {
        synchronized (filesAndId) {
            if (filesAndId.containsKey(filenameToMatch)) {
                for (String filename : filesAndId.keySet()) {
                    if (filename.equalsIgnoreCase(filenameToMatch)) {
                        addTokenIdToFile(filename, tokenId);
                    }
                }
            } else { // if there is no file with this filename
                addTokenIdToFile(filenameToMatch, tokenId);
            }
        }
    }

    // Thn xrhsimopoioume sthn reply_details gia na broume tous Peers pou exoun to
    // arxeio
    public List<String> findPeersHaveTheFile(String filename) {
        synchronized (filesAndId) {
            List<String> TokenIdList = new ArrayList<>(); // h lista pou tha epistrepsei
            for (Map.Entry<String, List<String>> entry : filesAndId.entrySet()) { // blepoume sthn domh filesAndId
                if (entry.getKey().equalsIgnoreCase(filename)) {
                    TokenIdList.addAll(entry.getValue());
                }
            }
            return TokenIdList;
        }
    }

    public PeerInformationToCommunicate findPeerByName(String name) {
        for (Map.Entry<String, PeerInformationToCommunicate> entry : this.getactivePeers().entrySet()) {
            String token = entry.getKey();
            PeerInformationToCommunicate peer = entry.getValue();

            if (peer.getUserName().equals(name)) {
                System.out.println("Brhkaaaa");
                return peer;
            }
        }

        return null; // Return null if no matching peer is found
    }

    public PeerInfo findPeer(String name) {
        for (PeerInfo p : this.getPeerInfoList()) {
            if (p.getUserName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /*
     * Otan enas Peer kanei logout, briskoume sthn domh
     * filesAndId to token_id tou kai to afairoume kathos pleon exei katarghthei
     * kai otan paei na syndethei ksana tha exei kainourgio sthn nea syndesh
     */
    public void removeTokenFromLists(String token_Id) {
        synchronized (filesAndId) {
            for (List<String> list : filesAndId.values()) {
                list.remove(token_Id);
            }
        }
    }

    public void printFile() { // ektypwnei to periexomeno ths domhs poy exei ta arxeia antisxixoismena sta
                              // token_Id
        synchronized (filesAndId) {
            for (String filename : filesAndId.keySet()) {
                List<String> tokenIds = filesAndId.get(filename);
                System.out.println("File: " + filename + ", Token IDs: " + tokenIds);
            }
        }
    }

    public ConcurrentHashMap<String, PeerInformationToCommunicate> getactivePeers() {
        synchronized (this.activePeers) {
            return this.activePeers;
        }
    }

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        Scanner scanner = new Scanner(System.in);

        // Start a thread for server functionality
        Thread serverThread = new Thread(() -> {
            tracker.openServer(); // This method should contain the server logic
        });
        serverThread.start();

        // Wait for both threads to finish
        try {
            serverThread.join();
            // clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
