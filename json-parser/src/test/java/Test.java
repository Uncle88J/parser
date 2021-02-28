import ir.markazandroid.JSONParser.Parser;
import ir.markazandroid.JSONParser.annotations.JSON;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;

/**
 * Created by Ali on 2/28/2021.
 */
public class Test {

    private Parser parser;

    @Before
    public void initParser() throws NoSuchMethodException {
        parser = new Parser();
        parser.addClass(TestObject.class);
    }

    @org.junit.Test
    public void enumTest() {
        TestObject toSend = new TestObject();
        toSend.setTestEnum(TestEnum.secondParam);
        JSONObject json = parser.get(toSend);
        System.out.println(json.toString(1));
        TestObject toReceive = parser.get(TestObject.class, json);
        Assert.assertEquals(toSend.getTestEnum(), toReceive.getTestEnum());
    }

    @JSON
    public static class TestObject {

        private TestEnum testEnum;


        @JSON
        public TestEnum getTestEnum() {
            return testEnum;
        }

        public void setTestEnum(TestEnum testEnum) {
            this.testEnum = testEnum;
        }
    }

    public enum TestEnum {
        firstParam,
        secondParam;
    }
}
