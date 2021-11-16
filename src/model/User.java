package model;

public class User {
    private String username;
    private boolean online, connected;
   // private String userOtherUsername;

    public User() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        else if (obj != null) {
            User other = (User) obj;
            if (other.username == username) return true;
        }
        return false;
    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
