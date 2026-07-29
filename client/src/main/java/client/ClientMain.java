package client;

public class ClientMain {
    public static void main(String[] args) {
        var client = new ChessClient("http://localhost:8080");
        client.run();
    }
}