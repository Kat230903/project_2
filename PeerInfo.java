import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

public class PeerInfo implements Serializable {
    private String userName;
    private String password;
    private int countDownloads;
    private int countFailures;
    private final ReentrantLock countLock = new ReentrantLock();

    public PeerInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
        this.countDownloads = 0;
        this.countFailures = 0;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getCountDownloads() {
        return countDownloads;
    }

    public int getCountFailures() {
        return countFailures;
    }

    public void increaseCountDownloads() {
        countLock.lock();
        try {
            countDownloads++;
        } finally {
            countLock.unlock();
        }
    }

    public void increaseCountFailures() {
        countLock.lock();
        try {
            countFailures++;
        } finally {
            countLock.unlock();
        }
    }
}
