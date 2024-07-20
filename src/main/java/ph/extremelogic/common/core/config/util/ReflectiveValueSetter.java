/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ph.extremelogic.common.core.config.util;

import java.lang.reflect.Field;

/**
 * Utility class for setting field values reflectively.
 * This class provides methods to set values on object fields using reflection,
 * with automatic type conversion from String to the field's type.
 * <p>
 * Supported field types for conversion:
 * <ul>
 *   <li>String</li>
 *   <li>int / Integer</li>
 *   <li>long / Long</li>
 *   <li>float / Float</li>
 *   <li>double / Double</li>
 *   <li>boolean / Boolean</li>
 * </ul>
 * <p>
 * This class is not meant to be instantiated.
 */
public class ReflectiveValueSetter {

    /**
     * Private constructor to prevent instantiation.
     * This class only contains static methods and should not be instantiated.
     */
    private ReflectiveValueSetter() {
        // This class does not need a constructor because of static methods
    }

    /**
     * Sets the value of a field on the given object, converting the string value to the appropriate type.
     * This method uses reflection to set the field value, even if the field is private.
     *
     * @param field the field to set
     * @param obj   the object on which to set the field
     * @param value the string value to be converted and set
     * @throws IllegalAccessException   if the field cannot be accessed due to Java language access control
     * @throws IllegalArgumentException if the field type is not supported for conversion
     * @throws NumberFormatException    if the string value cannot be parsed to the required numeric type
     */
    public static void setFieldValue(Field field, Object obj, String value)
            throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            field.set(obj, value);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(obj, Integer.parseInt(value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(obj, Long.parseLong(value));
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(obj, Float.parseFloat(value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(obj, Double.parseDouble(value));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(obj, Boolean.parseBoolean(value));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported field type: " + fieldType);
        }
    }
}
