public class ConfigItem {
    private double claimPercentage;
    private String cmcApiKey;
    private String mysqlHost;
    private String mysqlUsername;
    private String mysqlPassword;
    private String mysqlDb;
    private String sumoRpc;
    private String ryoRpc;
    private String intenseRpc;
    private String masariRpc;
    private String lokiRpc;

    public ConfigItem(double claimPercentage, String cmcApiKey, String mysqlHost, String mysqlUsername, String mysqlPassword, String mysqlDb, String sumoRpc, String ryoRpc, String intenseRpc, String masariRpc, String lokiRpc) {
        this.claimPercentage = claimPercentage;
        this.cmcApiKey = cmcApiKey;
        this.mysqlHost = mysqlHost;
        this.mysqlUsername = mysqlUsername;
        this.mysqlPassword = mysqlPassword;
        this.mysqlDb = mysqlDb;
        this.sumoRpc = sumoRpc;
        this.ryoRpc = ryoRpc;
        this.intenseRpc = intenseRpc;
        this.masariRpc = masariRpc;
        this.lokiRpc = lokiRpc;
    }

    public double getClaimPercentage() {
        return claimPercentage;
    }

    public String getCmcApiKey() {
        return cmcApiKey;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getMysqlDb() {
        return mysqlDb;
    }

    public String getSumoRpc() {
        return sumoRpc;
    }

    public String getRyoRpc() {
        return ryoRpc;
    }

    public String getIntenseRpc() {
        return intenseRpc;
    }

    public String getMasariRpc() {
        return masariRpc;
    }

    public String getLokiRpc() {
        return lokiRpc;
    }
}
