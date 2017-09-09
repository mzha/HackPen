package com.ngnl.hackpen.myapplication.backend;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;

import java.util.Iterator;

/**
 * Created by Arthur on 9/9/17.
 */

public class Storage {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory keyFactory = datastore.newKeyFactory().setKind("data");

    public static Key addData(long timestamp, float x, float y, float z) {
        Key key = datastore.allocateId(keyFactory.newKey());
        Entity data = Entity.newBuilder(key)
                .set("timestamp", timestamp)
                .set("x", x)
                .set("y", y)
                .set("z", z)
                .build();
        datastore.put(data);
        return key;
    }

    public Iterator<Entity> getRecentData() {
        Query<Entity> query =
                Query.newEntityQueryBuilder().setKind("data").setLimit(100)
                        .setOrderBy(StructuredQuery.OrderBy.desc("timestamp")).build();
        return datastore.run(query);
    }
}
