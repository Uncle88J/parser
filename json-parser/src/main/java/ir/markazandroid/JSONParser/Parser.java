package ir.markazandroid.JSONParser;

import ir.markazandroid.JSONParser.annotations.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Coded by Ali on 30/06/2017.
 * latest update 28/02/2021.
 * version 1.9.0
 */

@SuppressWarnings("unchecked")
public class Parser {

    ConcurrentHashMap<String, ArrayList<Methods>> classes;

    public Parser() {
        classes = new ConcurrentHashMap<>();
    }

    public void addClass(Class c) throws NoSuchMethodException {
        classes.put(c.getName(), extractClassAnnotatedMethods(c));
    }

    public void addWithSuperClasses(Class c) throws NoSuchMethodException {
        ArrayList<Methods> mMethods = new ArrayList<>();
        String className = c.getName();
        do {
            mMethods.addAll(extractClassAnnotatedMethods(c));
            c = c.getSuperclass();
        }
        while (!c.equals(Object.class));

        classes.put(className, mMethods);
    }


    private ArrayList<Methods> extractClassAnnotatedMethods(Class c) throws NoSuchMethodException {
        Method[] methods = c.getDeclaredMethods();
        ArrayList<Methods> mMethods = new ArrayList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JSON.class)) {
                if (method.getName().startsWith("get")) {
                    mMethods.add(new Methods(method,
                            c.getDeclaredMethod(
                                    method.getName().replaceFirst("get", "set")
                                    , method.getReturnType()), method.getAnnotation(JSON.class)));
                }
            }
        }
        return mMethods;
    }

    private <T> T getObject(Class<T> c, JSONObject json) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        ArrayList<Methods> methods = classes.get(c.getName());
        T object = c.newInstance();
        for (Methods method : methods) {
            Object o;
            if (!method.annotation.name().equals("")) {
                o = json.opt(method.annotation.name());
            } else {
                StringBuilder b = new StringBuilder(method.setter.getName());
                b.setCharAt(3, Character.toLowerCase(b.charAt(3)));
                o = json.opt(b.substring(3));
            }
            if (o != null && !JSONObject.NULL.getClass().equals(o.getClass())) {
                try {
                    if (method.annotation.classType().equals(""))
                        method.setter.invoke(object, o);
                    else {
                        invokeSetter(object, o, method);
                    }
                } catch (IllegalArgumentException | IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return object;
    }

    private JSONObject getJSON(String className, Object object, JsonProfile profile) throws IllegalAccessException, JSONException, InvocationTargetException, IOException {
        if (object == null) return null;
        ArrayList<Methods> methods = classes.get(className);
        if (profile == null)
            return makeJSON(object, methods);
        else
            return makeJSON(object, methods, profile);

    }


    private JSONObject getJSON(Object object, JsonProfile profile) throws IllegalAccessException, JSONException, InvocationTargetException, IOException {
        return getJSON(object.getClass().getName(), object, profile);
    }


    private JSONObject getJSON(Class clazz, Object object, JsonProfile profile) throws IllegalAccessException, JSONException, InvocationTargetException, IOException {
        return getJSON(clazz.getName(), object, profile);
    }

    private JSONObject makeJSON(Object object, ArrayList<Methods> methods) throws InvocationTargetException, IllegalAccessException, IOException {
        JSONObject json = new JSONObject();
        for (Methods method : methods) {
            String name = getName(method);
            if (method.annotation.classType().equals(""))
                json.put(name, method.getter.invoke(object));
            else
                json.put(name, invokeGetter(object, method, null));
        }
        return json;
    }

    private JSONObject makeJSON(Object o, ArrayList<Methods> methods, JsonProfile profile) throws InvocationTargetException, IllegalAccessException, IOException {
        JSONObject json = new JSONObject();
        for (Methods method : methods) {
            String name = getName(method);
            if (profile.getExcludes().contains(name)) continue;
            if (!profile.getIncludes().isEmpty() && !profile.getIncludes().contains(name)) continue;
            if (method.annotation.classType().equals(""))
                json.put(name, method.getter.invoke(o));
            else
                json.put(name, invokeGetter(o, method, profile));
        }
        for (Map.Entry<String, Object> entry : profile.getExtras().entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private static String getName(Methods method) {
        String name;
        if (!method.annotation.name().equals("")) {
            name = method.annotation.name();
        } else {
            StringBuilder b = new StringBuilder(method.getter.getName());
            b.setCharAt(3, Character.toLowerCase(b.charAt(3)));
            name = b.substring(3);
        }
        return name;
    }

    private Object invokeGetter(Object source, Methods methods, JsonProfile profile) throws InvocationTargetException, IllegalAccessException, IOException {
        String valueType = methods.annotation.classType();
        Method method = methods.getter;
        if (method.getReturnType().isEnum()) {
            return ((Enum) method.invoke(source)).name();
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_SHORT)) {
            try {
                return ((Short) method.invoke(source)).intValue();

            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_BYTE)) {
            try {
                return ((Byte) method.invoke(source)).intValue();
            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_TIMESTAMP)) {
            try {
                Object d = method.invoke(source);
                //  if (d instanceof Date)
                return ((Date) d)
                        .getTime();
                //  else return ((LocalDate) d).getMillisSinceEpoch();
            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_BOOLEAN)) {
            try {
                return (Byte) method.invoke(source) > 0;
            } catch (NullPointerException e) {
                return false;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_ARRAY)) {
            try {
                if (methods.annotation.clazz().equals(Object.class)) {
                    Collection collection = ((Collection) method.invoke(source));
                    if (collection == null) return null;
                    return new JSONArray(collection);
                }
                Collection list = ((Collection) method.invoke(source));
                //profile
                if (profile != null) {
                    JSONArray array = new JSONArray();
                    for (Object o : list) {
                        try {
                            array.put(getJSON(o, profile));
                        } catch (IllegalAccessException | JSONException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                    return array;
                }

                return getArray(list);
            } catch (NullPointerException e) {
                return null;

            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_OBJECT)) {
            try {
                //profile
                if (profile != null)
                    return getJSON(methods.annotation.clazz(), method.invoke(source), profile);

                return getJSON(methods.annotation.clazz(), method.invoke(source), null);
            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_JSON_ARRAY)) {
            try {
                return new JSONArray((String) method.invoke(source));
            } catch (NullPointerException e) {
                return null;

            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_JSON_OBJECT)) {
            try {
                return new JSONObject((String) method.invoke(source));
            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_MAP)) {
            try {
                JSONObject output = new JSONObject();
                Map<String, Object> map = (Map) method.invoke(source);
                map.forEach((key, value) -> {
                    if (classes.containsKey(value.getClass().getName()))
                        output.put(key, get(value));
                    else if (value instanceof Map)
                        output.put(key, (Map) value);
                    else
                        output.put(key, value);
                });
                return output;
            } catch (NullPointerException e) {
                return null;
            }
        } else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_SERIALIZABLE)) {
            ObjectOutputStream out = null;
            try {
                Serializable object = (Serializable) method.invoke(source);
                if (object == null) return null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                out = new ObjectOutputStream(outputStream);
                out.writeObject(object);
                out.flush();
                return new String(Base64.getEncoder().encode(outputStream.toByteArray()));
            } finally {
                try {
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else
            return method.invoke(source);
    }

    private void invokeSetter(Object source, Object parameter, Methods methods) throws
            InvocationTargetException, IllegalAccessException, IOException, ClassNotFoundException {
        String valueType = methods.annotation.classType();
        Method method = methods.setter;
        if (method.getParameterTypes()[0].isEnum())
            method.invoke(source, Enum.valueOf((Class) method.getParameterTypes()[0], parameter.toString()));
        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_SHORT))
            method.invoke(source, ((Integer) parameter).shortValue());
        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_BYTE))
            method.invoke(source, ((Integer) parameter).byteValue());
        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_TIMESTAMP))
            method.invoke(source, new Timestamp((long) (parameter)));
        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_ARRAY))
            if (methods.annotation.clazz().equals(Object.class))
                method.invoke(source, JSONArrayToArrayList((JSONArray) parameter));
            else if (!methods.annotation.classTypes().parameterName().isEmpty())
                method.invoke(source, get(methods.annotation.collectionClass(), methods.annotation.clazz(), methods.annotation.classTypes(), (JSONArray) parameter));
            else
                method.invoke(source, get(methods.annotation.collectionClass(), methods.annotation.clazz(), (JSONArray) parameter));

        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_OBJECT))
            method.invoke(source, get(methods.annotation.clazz(), (JSONObject) parameter));
        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_MAP))
            method.invoke(source, ((JSONObject) parameter).toMap());

        else if (valueType.equalsIgnoreCase(JSON.CLASS_TYPE_SERIALIZABLE)) {
            if (parameter == null) return;
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(parameter.toString().getBytes()));
            try (ObjectInput in = new ObjectInputStream(bis)) {
                method.invoke(source, in.readObject());
            } catch (Exception ignored) {
                throw new RuntimeException("Cannot read object");
            }
        } else
            method.invoke(source, parameter);
    }

    public <T> T get(Class<T> c, JSONObject json) {
        try {
            return getObject(c, json);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> Collection<T> get(Class<T> c, JSONArray jarray) {
        return get(ArrayList.class, c, jarray);
    }

    public <T, C extends Collection<T>> C get(Class<C> collectionClass, Class<T> c, JSONArray jarray) {
        return get(collectionClass, c, null, jarray);
    }

    public <T, C extends Collection<T>> C get(Class<C> collectionClass, Class<T> parentClass, JSON.ClassType
            classType, JSONArray jarray) {
        C array;
        try {
            array = collectionClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            array = (C) new ArrayList();
        }
        for (int i = 0; i < jarray.length(); i++) {
            try {
                if (classType != null) {
                    JSONObject object = jarray.optJSONObject(i);
                    String type = object.optString(classType.parameterName());
                    Class<T> oC = parentClass;
                    for (JSON.Clazz clazz : classType.clazzes()) {
                        if (clazz.name().equals(type)) {
                            oC = clazz.clazz();
                            break;
                        }
                    }
                    array.add(getObject(oC, object));
                } else {
                    array.add(getObject(parentClass, jarray.optJSONObject(i)));
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public JSONObject get(Object object) {
        try {
            if (object instanceof JsonProfile) {
                JsonProfile profile = (JsonProfile) object;
                return getJSON(profile.getEntity(), profile);
            } else
                return getJSON(object, null);
        } catch (IllegalAccessException | JSONException | InvocationTargetException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> JSONArray getArray(Collection<T> list) {
        JSONArray array = new JSONArray();
        for (T o : list) {
            try {
                array.put(getJSON(o, null));
            } catch (IllegalAccessException | JSONException | InvocationTargetException | IOException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    <T> JSONArray getArray(JsonProfile profile) {
        JSONArray array = new JSONArray();
        Collection<T> list = (Collection) profile.getEntity();
        for (T o : list) {
            try {
                array.put(getJSON(o, profile));
            } catch (IllegalAccessException | JSONException | InvocationTargetException | IOException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    private ArrayList JSONArrayToArrayList(JSONArray jsonArray) {
        ArrayList arrayList = new ArrayList(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            arrayList.add(jsonArray.get(i));
        }
        return arrayList;
    }

    private class Methods {
        Method getter;
        Method setter;
        JSON annotation;

        Methods(Method getter, Method setter, JSON annotation) {
            this.getter = getter;
            this.setter = setter;
            this.annotation = annotation;
        }
    }
}
