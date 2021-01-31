package ir.markazandroid.JSONParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ali on 3/29/2018.
 */
public class JsonProfile implements Serializable {

    private ArrayList<String> includes;
    private ArrayList<String> excludes;
    private Map<String, Object> extras;
    private Object entity;

    public JsonProfile(Object entity) {
        includes = new ArrayList<>();
        excludes = new ArrayList<>();
        extras = new HashMap<>();
        this.entity = entity;
    }

    public JsonProfile includeField(String... fields) {
        Collections.addAll(includes, fields);
        return this;
    }

    public JsonProfile excludeFields(String... fields) {
        Collections.addAll(excludes, fields);
        return this;
    }

    ArrayList<String> getExcludes() {
        return excludes;
    }

    ArrayList<String> getIncludes() {
        return includes;
    }

    Object getEntity() {
        return entity;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }
}
