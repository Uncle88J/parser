package ir.markazandroid.JSONParser.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Coded by Ali on 30/06/2017.
 * version 2.0
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface JSON {

    String CLASS_TYPE_SHORT = "short";
    String CLASS_TYPE_BYTE = "byte";
    String CLASS_TYPE_TIMESTAMP = "timestamp";
    String CLASS_TYPE_BOOLEAN = "boolean";
    String CLASS_TYPE_ARRAY = "array";
    String CLASS_TYPE_OBJECT = "object";
    String CLASS_TYPE_JSON_ARRAY = "jsonarray";
    String CLASS_TYPE_JSON_OBJECT = "jsonobject";
    String CLASS_TYPE_MAP = "map";
    String CLASS_TYPE_SERIALIZABLE = "serializable";

    //@AliasFor("name")
    String value() default "";

    //@AliasFor("value")
    String name() default "";

    String classType() default "";

    ClassType classTypes() default @ClassType(parameterName = "", clazzes = @Clazz(name = "", clazz = Object.class));

    Class clazz() default Object.class;

    Class<? extends Collection> collectionClass() default ArrayList.class;


    @Target(METHOD)
    @Retention(RUNTIME)
    @interface ClassType {

        String parameterName();

        Clazz[] clazzes();

    }


    @Target(METHOD)
    @Retention(RUNTIME)
    @interface Clazz {

        String name();

        Class clazz();
    }
}
