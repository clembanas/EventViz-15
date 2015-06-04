package controllers;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import containers.EventVizModelPopulationObject;
import database.EventViz15_DB_MySQLAccess;
import jsonGeneration.JsonResultGenerator;
import logic.clustering.ClusteringUtil;
import play.mvc.Controller;
import play.mvc.Result;
import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentimentComponent;
import views.html.clusteringtest;
import views.html.index;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;

public class Application extends Controller {

    static {
        try {
            EventViz15_DB_MySQLAccess.initializeDBAccess();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println();
    }

    public static Result index() {
        return ok(index.render());
    }

    public static Result clusteringtest() {
        return ok(clusteringtest.render());
    }

    public static Result getEventById(int eventId) {
        EventVizModelPopulationObject model = null;
        try {
            model = EventViz15_DB_MySQLAccess.getEventById(eventId);
        } catch (SQLException e) {
            return ok("");
        }
        JsonObject model_JSON = JsonResultGenerator.getEventById_JSON(model);
        return ok(model_JSON.toString());
    }

    public static Result getSentiment(String terms, String location) {
        JsonArray jso = (JsonArray) new JsonParser().parse(terms);
        System.out.println(jso.get(0));
        LinkedList<String> list = new LinkedList<String>();
        for (int i = 0; i < jso.size(); i++) {
            list.add(jso.get(i).toString());
        }
        SocialMentionData data = null;
        try {
            data = SocialMentionSentimentComponent.getSocialMentionData(list, location);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ok(JsonResultGenerator.getSocialMentionSentiment_JSON(data).toString());
    }

    public static Result getDefaultCluster() {
        return ok(ClusteringUtil.getDefaultClusterJsonNode());
    }

}