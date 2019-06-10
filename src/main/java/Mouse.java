import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

public class Mouse {
    private List<SessionItem> sessionList = new ArrayList<>();

    Mouse(){

    }

    String addSession(Request request, Response response){
        for (int i = 0; i < sessionList.size(); ++i) {
            SessionItem itemC = sessionList.get(i);
            if (itemC.getAddress().equals(request.queryParams("address"))) {
                sessionList.remove(sessionList.indexOf(itemC));
            }
        }
        SessionItem sessionItem = new SessionItem(request.queryParams("address"), request.queryParams("data"));
        sessionList.add(sessionItem);
        if(sessionList.size() > 150){
            sessionList.remove(sessionList.size() - 1);
        }
        return "";
    }

    String getSession(Request request, Response response){
        String res = "";
        if (request.queryParams("key").equals("5656")) {
            for (int i = 0; i < sessionList.size(); ++i) {
                SessionItem itemC = sessionList.get(i);
                if (itemC.getAddress().equals(request.queryParams("address"))) {
                    res = itemC.getData();
                }
            }
        }
        return res;
    }
}
