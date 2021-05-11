package ds.cw2.communication.server;

import ds.cw2.communication.grpc.generated.*;
import ds.cw2.synchronization.tx.DistributedTxCoordinator;
import ds.cw2.synchronization.tx.DistributedTxParticipant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;
import ds.cw2.synchronization.tx.DistributedTxListner;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class StockOrderServiceImpl extends StockOrderServiceGrpc.StockOrderServiceImplBase implements DistributedTxListner {

    private ManagedChannel channel = null;
    StockOrderServiceGrpc.StockOrderServiceBlockingStub clientStub = null;
    private TradeServer server;

    private Stock tempDataHolder;
    private boolean transactionStatus = false;

    public StockOrderServiceImpl(TradeServer server) {
        this.server = server;
    }

    private void startDistributedTx(String traderId, double price, int quantity, String orderType) {
        try {
            server.getTransaction().start(traderId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new Stock(traderId, quantity, price, orderType);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                startDistributedTx(traderId, price, quantity, orderType);
                updateSecondaryServers(traderId, price, quantity, orderType);
                System.out.println("going to perform");

                if (quantity > 0) {
                    ((DistributedTxCoordinator) server.getTransaction()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while updating the order book" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating order book on secondary, on Primary's command");
                startDistributedTx(traderId, price, quantity, orderType);

                if (quantity > 0) {
                    ((DistributedTxParticipant) server.getTransaction()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransaction()).voteAbort();
                }
            } else {
                StockOrderResponse response = callPrimary(traderId, price, quantity, orderType);
                if (response.getStatus()) {
                    transactionStatus = true;
                }
            }
        }

        StockOrderResponse response = StockOrderResponse.newBuilder()
                .setStatus(transactionStatus)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void updateOrderBook() {
        if (tempDataHolder != null) {
            String traderId = tempDataHolder.getTraderId();
            double price = tempDataHolder.getPrice();
            int quantity = tempDataHolder.getQuantity();
            String orderType = tempDataHolder.getOrderType();
            server.setOrderBook(traderId, price, quantity, orderType);

            System.out.println("Trader ID: " + traderId + " placed a " + orderType + " order - committed.");
            tempDataHolder = null;
        }
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
            callServer(traderId, price, quantity, orderType, true, IPAddress, port);
        }
    }

    @Override
    public void onGlobalCommit() {
        updateOrderBook();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
