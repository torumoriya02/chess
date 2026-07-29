package client;

import java.util.Scanner;

public class ChessClient {

    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);

    private final ServerFacade facade;
    private String authToken;

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.facade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println("♕ 240 Chess Client");
        System.out.println("Type help to see available commands.");

        while (true) {
            System.out.print(">>> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                return;
            }

            if (input.equalsIgnoreCase("help")) {
                printPreloginHelp();
            } else if (input.equalsIgnoreCase("register")) {
                register();
            } else {
                System.out.println("Unknown command. Type help.");
            }
        }
    }

    private void printPreloginHelp() {
        System.out.println("help     - show available commands");
        System.out.println("login    - log in to your account");
        System.out.println("register - create a new account");
        System.out.println("quit     - exit the program");
    }

    private void register() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            var result = facade.register(username, password, email);
            authToken = result.authToken();

            System.out.println("Registered and logged in as " + result.username());

        } catch (Exception ex) {
            System.out.println("Unable to register. Please check your information.");
        }
    }
    private void login() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            var result = facade.login(username, password);
            authToken = result.authToken();

            System.out.println("Logged in as " + result.username());

        } catch (Exception ex) {
            System.out.println("Unable to log in. Check your username and password.");
        }
    }
}