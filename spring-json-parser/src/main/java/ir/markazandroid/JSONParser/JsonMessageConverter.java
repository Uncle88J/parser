package ir.markazandroid.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Ali on 01/11/2017.
 */
public class JsonMessageConverter extends Parser implements HttpMessageConverter, MessageConverter {

    private ArrayList<MediaType> supportedMediaTypes;

    public static JsonMessageConverter build(String basePackage) {
        JsonMessageConverter parser = new JsonMessageConverter();
        ParserInitializr initializr = new ParserInitializr(parser);
        initializr.findAnnotatedClasses(basePackage);
        return parser;
    }

    public JsonMessageConverter() {
        supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        supportedMediaTypes.add(new MediaType("application", "*+json"));
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return classes.containsKey(clazz.getName());
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return classes.containsKey(clazz.getName());
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null) {
            stringBuilder.append(s);
        }
        s = stringBuilder.toString();
        try {
            return readObject(clazz, s);
        } catch (JSONException e) {
            throw new HttpMessageNotReadableException("cannot pars to json", e, inputMessage);
        }
    }

    private Object readObject(Class clazz, String s) throws JSONException {
        if (s.charAt(0) == '{') {
            JSONObject jsonObject = new JSONObject(s);
            if (clazz.equals(JSONObject.class)) return jsonObject;
            return get(clazz, jsonObject);
        } else {
            JSONArray jsonArray = new JSONArray(s);
            return get(clazz, jsonArray);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        outputMessage.getBody().write(writeString(o));
    }

    private byte[] writeString(Object o) {
        String s;
        if (o instanceof Map) s = new JSONObject((Map) o).toString();
        else if (o instanceof JSONObject) s = o.toString();
        else if (o instanceof Collection) s = getArray((Collection) o).toString();
        else if (o instanceof JsonProfile && ((JsonProfile) o).getEntity() instanceof Collection)
            s = getArray((JsonProfile) o).toString();
        else s = get(o).toString();
        return s.getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        GenericMessage m = (GenericMessage) message;
        byte[] data = (byte[]) m.getPayload();
        return readObject(targetClass, new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        return MessageBuilder.withPayload(writeString(payload)).copyHeaders(headers).build();
    }
}
