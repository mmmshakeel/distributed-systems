package ds.cw2.communication.server;

import ds.cw2.communication.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.util.List;

public class StockOrderServiceImpl extends StockOrderServiceGrpc.StockOrderServiceImplBase {

    private ManagedChannel channel = null;
    StockOrderServiceGrpc.StockOrderServiceBlockingStub clientStub = null;
    private TradeServer server;

    public StockOrderServiceImpl(TradeServer server) {
        this.server = server;
    }

    @Override
    public void stockOrder(StockOrderRequest request, StreamObserver<StockOrderResponse> responseObserver) {

        String traderId = request.getTraderId();
        String symbol = "Apple";
        int quantity = request.getQuantity();
        double price = request.getPrice();
        String orderType = request.getOrderType();
        boolean status = false;

        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Updating order book as Primary");
                updateOrderBook(traderId, price, quantity, orderType);
                updateSecondaryServers(traderId, price, quantity, orderType);
                status = true;
            } catch (Exception e) {
                System.out.println("Error while updating the order book" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating order book on secondary, on Primary's command");
                updateOrderBook(traderId, price, quantity, orderType);
            } else {
                StockOrderResponse response = callPrimary(traderId, price, quantity, orderType);
                if (response.getStatus()) {
                    status = true;
                }
            }
        }
        StockOrderResponse response = StockOrderResponse.newBuilder()
                .setStatus(status)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void updateOrderBook(String traderId, double price, int quantity, String orderType) {
        server.setOrderBook(traderId, price, quantity, orderType);
        System.out.println("Trader ID: " + traderId + " placed a " + orderType + " order.");
    }

    private StockOrderResponse callServer(String traderId, double price, int quantity, String orderType, boolean isSentByPrimary, String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();

        clientStub = StockOrderServiceGrpc.newBlockingStub(channel);
        StockOrderRequest request = StockOrderRequest.newBuilder()
                .setTraderId(traderId)
                .setPrice(price)
                .setOrderType(orderType)
                .setSymbol("Apple")
                .setQuantity(quantity)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        StockOrderResponse response = clientStub.stockOrder(request);
        return response;
    }

    private StockOrderResponse callPrimary(String traderId, double price, int quantity, String orderType) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(traderId, price, quantity, orderType, false, IPAddress, port);
    }

    private void updateSecondaryServers(String traderId, double price, int quantity, String orderType) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();

        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(traderId, price, quantity,orderType,true, IPAddress, port);
        }
    }
}
