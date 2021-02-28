package ir.markazandroid.JSONParser;

import org.json.JSONObject;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Created by Ali on 2/28/2021.
 */
public class JmsJsonMessageConverter extends SimpleMessageConverter {
    private final Parser parser;

    public static final String CLASS_PROPERTY_NAME = "ir.markazandroid.JSONParser.JmsJsonMessageConverter.CLASS_PROPERTY_NAME";

    public JmsJsonMessageConverter(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
        String json = parser.get(object).toString();
        Message message = super.toMessage(json, session);
        message.setStringProperty(CLASS_PROPERTY_NAME, object.getClass().getName());
        return message;
    }

    @Override
    public Object fromMessage(Message message) throws JMSException, MessageConversionException {
        try {
            JSONObject json = new JSONObject(super.fromMessage(message));
            return parser.get(Class.forName(message.getStringProperty(CLASS_PROPERTY_NAME)), json);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
