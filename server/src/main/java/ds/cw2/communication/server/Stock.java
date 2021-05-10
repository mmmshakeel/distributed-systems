package ds.cw2.communication.server;

public class Stock {

    private String traderId;
    private String symbol;
    private int quantity;
    private double price;
    private String orderType;

    public static final String SELL_ORDER_TYPE = "sell";
    public static final String BUY_ORDER_TYPE = "buy";

    public Stock(String traderId, int quantity, double price, String orderType) {
        this.traderId = traderId;
        this.quantity = quantity;
        this.price = price;
        this.orderType = orderType;
    }

    public String getTraderId() {
        return traderId;
    }

    public void setTraderId(String traderId) {
        this.traderId = traderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

}
