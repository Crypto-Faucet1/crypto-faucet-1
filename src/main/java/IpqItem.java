public class IpqItem {
    private String ip;
    private int fraudScore;
    private String countryCode;
    private boolean proxy;
    private boolean recentAbuse;

    public IpqItem(String ip, int fraudScore, String countryCode, boolean proxy, boolean recentAbuse) {
        this.ip = ip;
        this.fraudScore = fraudScore;
        this.countryCode = countryCode;
        this.proxy = proxy;
        this.recentAbuse = recentAbuse;
    }

    public int getFraudScore() {
        return fraudScore;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isProxy() {
        return proxy;
    }

    public boolean isRecentAbuse() {
        return recentAbuse;
    }

    public String getIp() {
        return ip;
    }
}