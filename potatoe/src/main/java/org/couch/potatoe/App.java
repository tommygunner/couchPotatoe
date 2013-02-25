package org.couch.potatoe;

import com.couchbase.client.CouchbaseClient;
import net.spy.memcached.internal.OperationFuture;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class App {

  // the unique key of the document
  public static final String KEY = "beer_Wrath";

  // expiration time of the document (use 0 to persist forever)
  public static final int EXP_TIME = 10;

  // the JSON encoded document
  public static final String VALUE =
          "{\"name\":\"Wrath\",\"abv\":9.0,"
              + "\"type\":\"beer\",\"brewery_id\":\"110f1a10e7\","
              + "\"updated\":\"2010-07-22 20:00:20\","
              + "\"description\":\"WRATH Belgian-style \","
              + "\"style\":\"Other Belgian-Style Ales\","
              + "\"category\":\"Belgian and French Ale\"}";

  public static void main(String args[]) {

    // Set the URIs and get a client
    List<URI> uris = new LinkedList<URI>();

    // Connect to localhost or to the appropriate URI(s)
    uris.add(URI.create("http://ubuntu:8091/pools"));
    uris.add(URI.create("http://ubuntu:8091/pools"));

    CouchbaseClient client = null;
    try {
      // Use the "default" bucket with no password
      client = new CouchbaseClient(uris, "beer-sample", "");
    } catch (IOException e) {
      System.err.println("IOException connecting to Couchbase: " + e.getMessage());
      System.exit(1);
    }

    // Do an asynchronous set
    OperationFuture<Boolean> setOp = client.set(KEY, EXP_TIME, VALUE);

    // Check to see if our set succeeded
    try {
      if (setOp.get().booleanValue()) {
        System.out.println("Set Succeeded");
        String object = (String) client.get(KEY);
        System.out.println("get: " + KEY +": " +object);
      } else {
        System.err.println("Set failed: " + setOp.getStatus().getMessage());
      }
    } catch (InterruptedException e) {
      System.err.println("InterruptedException while doing set: " + e.getMessage());
    } catch (ExecutionException e) {
      System.err.println("ExecutionException while doing set: " + e.getMessage());
    }

    // Shutdown and wait a maximum of three seconds to finish up operations
    client.shutdown(3, TimeUnit.SECONDS);
    System.exit(0);
  }
}