package ds.cw2.communication.client;

public class MainClass {
    public static void main(String[] args) throws InterruptedException {
        String host = args[0];
        int port = Integer.parseInt(args[1].trim());

        if (args.length != 2) {
            System.out.println("Usage CheckBalanceServiceClient <host> <port> ");
            System.exit(1);
        }

        StockOrderServiceClient client = new StockOrderServiceClient(host, port);
        client.initializeConnection();
        client.processUserRequests();
        client.closeConnection();
    }
}
