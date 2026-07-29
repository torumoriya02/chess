package client;

import java.util.Scanner;

public class ChessClient {

    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
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
}