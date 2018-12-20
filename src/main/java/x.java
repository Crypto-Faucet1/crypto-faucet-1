import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;

public class x {

    public static void main(String args[]) {
        Spark.port(9899);
        ClaimHandler claim = new ClaimHandler();
        GetBalance getBalance = new GetBalance();
        WithdrawHandler withdrawHandler = new WithdrawHandler();
        SetBonusHandler setBonusHandler = new SetBonusHandler();
        new Prices();
        new Stats();
        new IpHub();

        try {
            new Payments();
        } catch (Exception e) {
            e.printStackTrace();
        }

        post("/claim", claim::claim);
        path("/claim", () -> {
            post("/v2", claim::claim);
        });
        get("/addressInfo", getBalance::getAddressInfo);
        path("/withdraw", () -> {
            get("/top", withdrawHandler::getTop);
            post("/remove", withdrawHandler::removeBalance);
            post("/proccess", Payments::startProccessPayment);
        });
        post("/setbonus", setBonusHandler::setBonus);
        get("/payments", Payments::getPayments);
    }
}
