package service.auth;

public interface Authenticatable {
    boolean login(String email, String password);
    void logout();
    boolean isLoggedIn();
    String getSessionToken();
}