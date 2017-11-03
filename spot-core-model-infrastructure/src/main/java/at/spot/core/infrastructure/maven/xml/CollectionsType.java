
package at.spot.core.infrastructure.maven.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für CollectionsType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="CollectionsType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="List"/>
 *     &lt;enumeration value="Set"/>
 *     &lt;enumeration value="Collection"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CollectionsType")
@XmlEnum
public enum CollectionsType {

    @XmlEnumValue("List")
    LIST("List"),
    @XmlEnumValue("Set")
    SET("Set"),
    @XmlEnumValue("Collection")
    COLLECTION("Collection");
    private final String value;

    CollectionsType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CollectionsType fromValue(String v) {
        for (CollectionsType c: CollectionsType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
