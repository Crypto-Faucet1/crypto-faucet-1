public class StatsItem {
    private String date;
    private int sumoClaimsToday;
    private int ryoClaimsToday;
    private int intenseClaimsToday;
    private int masariClaimsToday;
    private int lokiClaimsToday;

    public StatsItem(String date, int sumoClaimsToday, int ryoClaimsToday, int intenseClaimsToday, int masariClaimsToday, int lokiClaimsToday) {
        this.date = date;
        this.sumoClaimsToday = sumoClaimsToday;
        this.ryoClaimsToday = ryoClaimsToday;
        this.intenseClaimsToday = intenseClaimsToday;
        this.masariClaimsToday = masariClaimsToday;
        this.lokiClaimsToday = lokiClaimsToday;
    }

    public String getDate() {
        return date;
    }

    public int getSumoClaimsToday() {
        return sumoClaimsToday;
    }

    public int getRyoClaimsToday() {
        return ryoClaimsToday;
    }

    public int getIntenseClaimsToday() {
        return intenseClaimsToday;
    }

    public int getMasariClaimsToday() {
        return masariClaimsToday;
    }

    public int getLokiClaimsToday() {
        return lokiClaimsToday;
    }
}
