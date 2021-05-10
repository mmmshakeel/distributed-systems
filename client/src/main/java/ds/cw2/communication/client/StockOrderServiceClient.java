package ds.cw2.communication.client;

import ds.cw2.communication.grpc.generated.StockOrderRequest;
import ds.cw2.communication.grpc.generated.StockOrderResponse;
import ds.cw2.communication.grpc.generated.StockOrderServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class StockOrderServiceClient {

    private ManagedChannel channel = null;
    StockOrderServiceGrpc.StockOrderServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public StockOrderServiceClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        clientStub = StockOrderServiceGrpc.newBlockingStub(channel);
    }
    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests() throws InterruptedException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter Trader ID, Quantity, Price, Order type :");
            String input[] = userInput.nextLine().trim().split(",");

            String traderId = input[0];
            int quantity = Integer.parseInt(input[1]);
            double price = Double.parseDouble(input[2]);
            String orderType = input[3];

            System.out.println("Requesting server perform stock order");
            StockOrderRequest request = StockOrderRequest
                    .newBuilder()
                    .setTraderId(traderId)
                    .setQuantity(quantity)
                    .setPrice(price)
                    .setOrderType(orderType)
                    .build();

            StockOrderResponse response = clientStub.stockOrder(request);
            System.out.printf("Trade order processed for " + response.getTraderId() + " request");
            Thread.sleep(1000);
        }
    }
}
