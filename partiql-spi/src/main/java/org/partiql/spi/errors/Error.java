package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an error in the PartiQL ecosystem.
 * Errors consist of an error code and a collection of properties.
 * @see ErrorCode
 * @see Property
 */
public interface Error {

    /**
     * @return The error code associated with this error.
     */
    public int getCode();

    /**
     * Users should read the documentation for each {@link ErrorCode} to determine the potentially applicable properties
     * that may be present in this error. Users should read the documentation for each {@link Property} to determine
     * the meaning of the property's value as well as the expected type of the property.
     * @param key the key of the property to retrieve. See {@link Property}'s static keys.
     * @return a property that may provide additional information about the error; null if the property does not exist.
     */
    @Nullable
    public Object getProperty(int key);

    /**
     * @return The collection of properties associated with this error.
     * @see org.partiql.errors.Property
     * @see Property
     */
    @NotNull
    public Collection<Integer> getProperties();

    static Error of(int errorCode) {
        return of(errorCode, new HashMap<>());
    }

    static Error of(int errorCode, @NotNull Map<Integer, Object> properties) {
        return new Error() {
            @Override
            public int getCode() {
                return errorCode;
            }

            @Nullable
            @Override
            public Object getProperty(int key) {
                return properties.get(key);
            }

            @NotNull
            @Override
            public Collection<Integer> getProperties() {
                return properties.keySet();
            }
        };
    }
}
