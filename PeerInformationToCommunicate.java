import java.io.*;

public class PeerInformationToCommunicate implements Serializable {
    // private String token_id;
    private String ip_address;
    private int port;
    private String user_name;
    private int count_downloads;
    private int count_failures;
    // private String token_id;

    // Constructor
    public PeerInformationToCommunicate(String ip_address, int port, String user_name,
            int count_downloads, int count_failures) {
        // this.token_id = token_id;
        this.ip_address = ip_address;
        this.port = port;
        this.user_name = user_name;
        this.count_downloads = count_downloads;
        this.count_failures = count_failures;
    }

    // Getters and setters
    // public String getTokenId() {
    // return token_id;
    // }

    public String getIpAddress() {
        return ip_address;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return user_name;
    }

    public int getCountDownloads() {
        return count_downloads;
    }

    public int getCountFailures() {
        return count_failures;
    }

    // Method to increment download count
    public void incrementCountDownloads() {
        count_downloads++;
    }

    // Method to increment failure count
    public void incrementCountFailures() {
        count_failures++;
    }
}
