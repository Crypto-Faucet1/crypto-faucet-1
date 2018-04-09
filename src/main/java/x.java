import spark.Spark;

import java.util.Date;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;

public class x {
    public static void main(String args[]) {
        Spark.port(9899);

        ClaimHandler claim = new ClaimHandler();
        GetBalance getBalance = new GetBalance();
        WithdrawHandler withdrawHandler = new WithdrawHandler();
        RateHandler rateHandler = new RateHandler();
        SetBonusHandler setBonusHandler = new SetBonusHandler();

        post("/claim", claim::claimSumo);
        get("/balance", getBalance::getBalance);
        path("/balance", () -> {
            get("/current", getBalance::getBalance);
            get("/paid", getBalance::getPaid);
        });
        get("/addressInfo", getBalance::getAddressInfo);
        path("/withdraw", () -> {
            get("/top", withdrawHandler::getTop);
            post("/remove", withdrawHandler::removeBalance);
        });
        get("/rate", RateHandler::getClaimRate);
        post("/setbonus", setBonusHandler::setBonus);
    }
}
