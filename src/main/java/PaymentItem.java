import org.json.JSONArray;

public class PaymentItem {
    private long date;
    private String txHash;
    private String txKey;
    private double amount;
    private JSONArray receipents;
    private String currency;

    public PaymentItem(long date, String txHash, String txKey, double amount, JSONArray receipents, String currency) {
        this.date = date;
        this.txHash = txHash;
        this.txKey = txKey;
        this.amount = amount;
        this.receipents = receipents;
        this.currency = currency;
    }

    public long getDate() {
        return date;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getTxKey(){return txKey;}

    public double getAmount() {
        return amount;
    }

    public JSONArray getReceipents() {
        return receipents;
    }

    public String getCurrency() {
        return currency;
    }
}
