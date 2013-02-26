package org.couch.potatoe;

import com.google.gson.GsonBuilder;

import com.couchbase.client.protocol.views.ViewRow;

import com.couchbase.client.protocol.views.ViewResponse;

import com.couchbase.client.protocol.views.Query;

import com.couchbase.client.protocol.views.View;

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
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
    public static final String KEY = "beer_Wrath";

    // expiration time (in seconds) of the document (use 0 to persist forever)
    public static final int EXP_TIME = 10;


    public static void main(String args[]) {

        // Set the URIs and get a client
        List<URI> uris = new LinkedList<URI>();

        // Connect to localhost or to the appropriate URI(s)
        uris.add(URI.create("http://ubuntu:8091/pools"));

        CouchbaseClient client = null;
        try {
            // Use the "default" bucket with no password
            client = new CouchbaseClient(uris, "beer-sample", "");
        }
        catch (IOException e) {
            log.error("IOException connecting to Couchbase: " + e.getMessage());
            System.exit(1);
        }

        set(client);
        view(client);

        // Shutdown and wait a maximum of three seconds to finish up operations
        client.shutdown(3, TimeUnit.SECONDS);
        System.exit(0);
    }


    /**
     * assumes view with following JS map
     * <code>
     * function (doc, meta) {
     *   if (doc.type && doc.name && doc.type == "beer") {
     *      emit(doc.name, meta.id);
     *    }
     *  }
     *  </code>
     */
    private static void view(CouchbaseClient client) {

        View view = client.getView("beer", "by_name");

        Query query = new Query();
        query.setKey("Wrath"); // Only retrieve this specific key
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


    private static void set(CouchbaseClient client) {
        Beer beer = new App().new Beer();
        beer.name = "Wrath";
        beer.abv = 0.9f;
        beer.type = "beer";
        beer.brewery_id = "110f1a10e7";
        beer.updated = new Date();
        beer.description = "WRATH Belgian-style";
        beer.style = "Other Belgian-Style Ales";
        beer.category = "Belgian and French Ale";

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

        // Do an asynchronous set
        OperationFuture<Boolean> setOp = client.set(KEY, EXP_TIME, gson.toJson(beer));

        try {
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