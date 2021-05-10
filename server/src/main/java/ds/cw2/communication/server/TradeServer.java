package ds.cw2.communication.server;

import ds.cw2.synchronization.DistributedLock;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TradeServer {
    private int serverPort;
    private DistributedLock leaderLock;
    private AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;
//    private Map<String, Stock> orderBook = new HashMap();
    private ArrayList<Stock> orderBook = new ArrayList<>();

    public TradeServer(String host, int port) throws InterruptedException, IOException, KeeperException {
        this.serverPort = port;
        leaderLock = new DistributedLock("TradeServerTestCluster", buildServerData(host, port));
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(new StockOrderServiceImpl(this))
                .build();
        server.start();
        System.out.println("TradeServer Started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    public void setOrderBook(String traderId, double price, int quantity, String orderType) {
        Stock stock = new Stock(traderId, quantity, price, orderType);

        if (orderType.equals("sell")) {
            performSellOrderTransaction(stock);
        } else {
            performBuyOrderTransaction(stock);
        }
    }

    public void performSellOrderTransaction(Stock stock) {
        System.out.println("Performing sell order request, checking stock Order book");
        orderBook.add(stock);
        boolean transactionPerformed = false;

        for (Stock orderItem: this.orderBook) {
            if (orderItem.getOrderType().equals(Stock.BUY_ORDER_TYPE)) { // check if someone is ready to sell
                // check for only sell orders
                if (orderItem.getPrice() == stock.getPrice()
                        && orderItem.getQuantity() == stock.getQuantity()
                        && !orderItem.getTraderId().equals(stock.getTraderId())) {
                    System.out.println("Stock order match found. Perform transaction");
                    System.out.println("Seller details: Trader ID-" + stock.getTraderId());
                    System.out.println("Buyer details: Trader ID-" + orderItem.getTraderId());

                    orderBook.remove(orderItem);
                    transactionPerformed = true;
                    System.out.println("Sell order transaction complete");
                    break;
                }
            }
        }

        if (!transactionPerformed) {
            System.out.println("Currently there are no matching buy orders");
        }
    }

    private void performBuyOrderTransaction(Stock stock) {
        System.out.println("Performing buy order request, checking stock Order book");
        orderBook.add(stock);

        boolean transactionPerformed = false;

        for (Stock orderItem: this.orderBook) {
            if (orderItem.getOrderType().equals(Stock.SELL_ORDER_TYPE)) { // check if someone is ready to sell

                System.out.println("== buy order ==");

                System.out.println(stock.getTraderId()+"-"+stock.getQuantity()+"-"+stock.getPrice()+"-"+stock.getOrderType());
                System.out.println(orderItem.getTraderId()+"-"+orderItem.getQuantity()+"-"+orderItem.getPrice()+"-"+orderItem.getOrderType());

                // check for only buy orders
                if (orderItem.getPrice() == stock.getPrice()
                        && orderItem.getQuantity() == stock.getQuantity()
                        && !orderItem.getTraderId().equals(stock.getTraderId())) {
                    System.out.println("Stock order match found. Perform transaction");
                    System.out.println("Buyer details: Trader ID-" + stock.getTraderId());
                    System.out.println("Seller details: Trader ID-" + orderItem.getTraderId());

                    orderBook.remove(orderItem);
                    transactionPerformed = true;
                    System.out.println("Buy order transaction complete");
                    break;
                }
            }
        }

        if (!transactionPerformed) {
            System.out.println("Currently there are no matching sell orders");
        }
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();
        for (byte[] data : othersData) {
            String[] dataStrings = new
                    String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        DistributedLock.setZooKeeperURL("localhost:2181");

        if (args.length != 1) {
            System.out.println("Usage executable-name <port>");
        }

        int serverPort = Integer.parseInt(args[0]);

        TradeServer server = new TradeServer("localhost", serverPort);
        server.startServer();
    }

    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;

        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");
            try {
                boolean leader = leaderLock.tryAcquireLock();
                while (!leader) {
                    byte[] leaderData =
                            leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                System.out.println("I got the leader lock. Now acting as primary");
                isLeader.set(true);
                currentLeaderData = null;
            } catch (Exception e) {
            }
        }
    }
}
