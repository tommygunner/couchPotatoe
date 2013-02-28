package org.couch.potatoe;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.spy.memcached.internal.OperationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    // the unique key of the document
    public static final String KEY = "beer_Czar";

    // expiration time (in seconds) of the document (use 0 to persist forever)
    public static final int EXP_TIME = 10;


    public static void main(String args[]) {

        List<URI> uris = new LinkedList<URI>();
        uris.add(URI.create("http://ubuntu:8091/pools"));

        CouchbaseClient client = null;
        try {
            client = new CouchbaseClient(uris, "beer-sample", "");
        }
        catch (IOException e) {
            log.error("IOException connecting to Couchbase: " + e.getMessage());
            System.exit(1);
        }

        Beer beer = new App().new Beer();
        beer.name = "Czar";
        beer.abv = 8.5f;
        beer.type = "beer";
        beer.brewery_id = "Wigram";
        beer.updated = new Date();
        beer.description = "Brewed in the authentic historical style of an Imperial Russian Stout, this big bold beer is crafted from rich roasted and toasted malts, fermented with a classic English ale yeast this Czarist favourite is an intense, living, bottle condition, full bodied Stout. At 8.5% abv The Czar lives up to it's name.";
        beer.style = "Imperial Stout";
        beer.category = "Imperial Russian Stout";

        set(client, beer);
        view_By_Name(client);
        view_By_Abv(client);

        // Shutdown and wait a maximum of three seconds to finish up operations
        client.shutdown(3, TimeUnit.SECONDS);
        System.exit(0);
    }


    /**
     * assumes following view code as map 
     * <code>
     * function (doc, meta) {
     *   if (doc.type && doc.name && doc.type == "beer") {
     *      emit(doc.name, meta.id);
     *    }
     *  }
     *  </code>
     */
    private static void view_By_Name(CouchbaseClient client) {

        View view = client.getView("beer", "by_name");

        Query query = new Query();
        query.setKey("Czar"); // Only retrieve this specific key
        query.setIncludeDocs(true); // Include the full document as well

        ViewResponse result = client.query(view, query);

        Iterator<ViewRow> itr = result.iterator();

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        while (itr.hasNext()) {
            ViewRow row = itr.next();
            log.info("The Key is: " + row.getKey());
            log.info("The Value is: " + row.getValue());
            log.info("The full document is: " + row.getDocument());

            Beer beer = gson.fromJson((String) row.getDocument(), Beer.class);
            log.info("Found " + beer.name + "! Cheers!");
        }
    }
    
    /**
     * assumes following view code as map
     * <code>
     * function (doc, meta) {
     *   if (doc.type == "beer" && doc.brewery_id) {
     *     emit(doc.brewery_id, doc.abv);
     *   }
     * }
     * </code>
     * and <em>_stats</em> as reducer
     */
    private static void view_By_Abv(CouchbaseClient client) {
        
        View view = client.getView("alc", "by_abv");
        
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        query.setLimit(10);
        
        ViewResponse result = client.query(view, query);
        
        Iterator<ViewRow> itr = result.iterator();
        log.info("Alcohole stats by brewery");
        while (itr.hasNext()) {
            ViewRow row = itr.next();
            log.info("The Key is: " + row.getKey());
            log.info("The Value is: " + row.getValue());
        }
    }


    private static void set(CouchbaseClient client, Beer beer) {

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

        try {
            OperationFuture<Boolean> addOp = client.add(KEY, EXP_TIME, gson.toJson(beer));
            if (addOp.get().booleanValue()) {
                log.info("Add Succeeded");
                String object = (String) client.get(KEY);
                log.info("get: " + KEY + ": " + object);

            }
            else {
                log.error("Add failed: " + addOp.getStatus().getMessage());
            }

            OperationFuture<Boolean> setOp = client.set(KEY, EXP_TIME, gson.toJson(beer));

            if (setOp.get().booleanValue()) {
                log.info("Set Succeeded");
                String object = (String) client.get(KEY);
                log.info("get: " + KEY + ": " + object);
            }
            else {
                log.error("Set failed: " + setOp.getStatus().getMessage());
            }
        }
        catch (InterruptedException e) {
            log.error("InterruptedException while doing set: " + e.getMessage());
        }
        catch (ExecutionException e) {
            log.error("ExecutionException while doing set: " + e.getMessage());
        }
    }

    class Beer {

        String name;

        float abv;

        float ibu;

        float srm;

        int upc;

        String type;

        String brewery_id;

        Date updated;

        String description;

        String style;

        String category;
    }

}