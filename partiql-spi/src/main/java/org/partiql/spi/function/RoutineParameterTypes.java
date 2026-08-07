/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function;

import org.partiql.spi.types.IntervalCode;
import org.partiql.spi.types.PType;
import org.partiql.spi.types.PTypeField;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class RoutineParameterTypes {

    private RoutineParameterTypes() {
    }

    static List<PType> snapshot(List<PType> types) {
        Objects.requireNonNull(types, "parameterTypes");
        List<PType> result = new ArrayList<>(types.size());
        for (PType type : types) {
            result.add(snapshot(Objects.requireNonNull(type, "parameterType")));
        }
        return Collections.unmodifiableList(result);
    }

    @SuppressWarnings("deprecation")
    private static PType snapshot(PType type) {
        final PType result;
        switch (type.code()) {
            case PType.NUMERIC:
                result = PType.numeric(type.getPrecision(), type.getScale());
                break;
            case PType.DECIMAL:
                result = PType.decimal(type.getPrecision(), type.getScale());
                break;
            case PType.CHAR:
                result = PType.character(type.getLength());
                break;
            case PType.VARCHAR:
                result = PType.varchar(type.getLength());
                break;
            case PType.BLOB:
                result = PType.blob(type.getLength());
                break;
            case PType.CLOB:
                result = PType.clob(type.getLength());
                break;
            case PType.TIME:
                result = PType.time(type.getPrecision());
                break;
            case PType.TIMEZ:
                result = PType.timez(type.getPrecision());
                break;
            case PType.TIMESTAMP:
                result = PType.timestamp(type.getPrecision());
                break;
            case PType.TIMESTAMPZ:
                result = PType.timestampz(type.getPrecision());
                break;
            case PType.ARRAY:
                result = PType.array(snapshot(Objects.requireNonNull(type.getTypeParameter(), "typeParameter")));
                break;
            case PType.BAG:
                result = PType.bag(snapshot(Objects.requireNonNull(type.getTypeParameter(), "typeParameter")));
                break;
            case PType.ROW:
                result = snapshotRow(type);
                break;
            case PType.MAP:
                result = PType.map(
                    snapshot(Objects.requireNonNull(type.getKeyType(), "keyType")),
                    snapshot(Objects.requireNonNull(type.getValueType(), "valueType"))
                );
                break;
            case PType.INTERVAL_YM:
                result = snapshotYearMonthInterval(type);
                break;
            case PType.INTERVAL_DT:
                result = snapshotDateTimeInterval(type);
                break;
            default:
                result = PType.of(type.code());
        }
        Map<String, Object> metas = Objects.requireNonNull(type.metas, "metas");
        result.metas = snapshotMetadata(metas);
        return result;
    }

    private static Map<String, Object> snapshotMetadata(Map<String, Object> metadata) {
        Map<String, Object> result = new HashMap<>(metadata.size());
        IdentityHashMap<Object, Boolean> visiting = new IdentityHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "metadataKey");
            result.put(key, snapshotMetadataValue(entry.getValue(), visiting));
        }
        return result;
    }

    private static Object snapshotMetadataValue(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null || isImmutableMetadataValue(value)) {
            return value;
        }
        if (value instanceof List<?>) {
            enterContainer(value, visiting);
            try {
                List<Object> result = new ArrayList<>(((List<?>) value).size());
                for (Object element : (List<?>) value) {
                    result.add(snapshotMetadataValue(element, visiting));
                }
                return result;
            } finally {
                visiting.remove(value);
            }
        }
        if (value instanceof Set<?>) {
            enterContainer(value, visiting);
            try {
                Set<Object> result = new LinkedHashSet<>();
                for (Object element : (Set<?>) value) {
                    result.add(snapshotMetadataValue(element, visiting));
                }
                return result;
            } finally {
                visiting.remove(value);
            }
        }
        if (value instanceof Map<?, ?>) {
            enterContainer(value, visiting);
            try {
                Map<Object, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Object key = snapshotMetadataValue(entry.getKey(), visiting);
                    Object entryValue = snapshotMetadataValue(entry.getValue(), visiting);
                    result.put(key, entryValue);
                }
                return result;
            } finally {
                visiting.remove(value);
            }
        }
        if (value.getClass().isArray()) {
            return snapshotMetadataArray(value, visiting);
        }
        throw new IllegalArgumentException(
            "Unsupported routine parameter metadata value type: " + value.getClass().getName()
        );
    }

    private static Object snapshotMetadataArray(Object value, IdentityHashMap<Object, Boolean> visiting) {
        enterContainer(value, visiting);
        try {
            int length = Array.getLength(value);
            Class<?> componentType = value.getClass().getComponentType();
            Object result = Array.newInstance(componentType, length);
            if (componentType.isPrimitive()) {
                System.arraycopy(value, 0, result, 0, length);
                return result;
            }
            Object[] elements = new Object[length];
            boolean preserveComponentType = true;
            for (int index = 0; index < length; index++) {
                Object element = snapshotMetadataValue(Array.get(value, index), visiting);
                elements[index] = element;
                preserveComponentType &= element == null || componentType.isInstance(element);
            }
            if (!preserveComponentType) {
                result = new Object[length];
            }
            for (int index = 0; index < length; index++) {
                Array.set(result, index, elements[index]);
            }
            return result;
        } finally {
            visiting.remove(value);
        }
    }

    private static boolean isImmutableMetadataValue(Object value) {
        return value instanceof String
            || value instanceof Boolean
            || value instanceof Character
            || value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double;
    }

    private static void enterContainer(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic routine parameter metadata is not supported.");
        }
    }

    private static PType snapshotRow(PType type) {
        List<PTypeField> fields = new ArrayList<>();
        for (PTypeField field : Objects.requireNonNull(type.getFields(), "fields")) {
            PTypeField nonNullField = Objects.requireNonNull(field, "field");
            fields.add(
                PTypeField.of(
                    Objects.requireNonNull(nonNullField.getName(), "fieldName"),
                    snapshot(Objects.requireNonNull(nonNullField.getType(), "fieldType"))
                )
            );
        }
        return PType.row(Collections.unmodifiableList(fields));
    }

    private static PType snapshotYearMonthInterval(PType type) {
        switch (type.getIntervalCode()) {
            case IntervalCode.YEAR:
                return PType.intervalYear(type.getPrecision());
            case IntervalCode.MONTH:
                return PType.intervalMonth(type.getPrecision());
            case IntervalCode.YEAR_MONTH:
                return PType.intervalYearMonth(type.getPrecision());
            default:
                throw new IllegalArgumentException("Unsupported year-month interval code: " + type.getIntervalCode());
        }
    }

    private static PType snapshotDateTimeInterval(PType type) {
        switch (type.getIntervalCode()) {
            case IntervalCode.DAY:
                return PType.intervalDay(type.getPrecision());
            case IntervalCode.HOUR:
                return PType.intervalHour(type.getPrecision());
            case IntervalCode.MINUTE:
                return PType.intervalMinute(type.getPrecision());
            case IntervalCode.SECOND:
                return PType.intervalSecond(type.getPrecision(), type.getFractionalPrecision());
            case IntervalCode.DAY_HOUR:
                return PType.intervalDayHour(type.getPrecision());
            case IntervalCode.DAY_MINUTE:
                return PType.intervalDayMinute(type.getPrecision());
            case IntervalCode.DAY_SECOND:
                return PType.intervalDaySecond(type.getPrecision(), type.getFractionalPrecision());
            case IntervalCode.HOUR_MINUTE:
                return PType.intervalHourMinute(type.getPrecision());
            case IntervalCode.HOUR_SECOND:
                return PType.intervalHourSecond(type.getPrecision(), type.getFractionalPrecision());
            case IntervalCode.MINUTE_SECOND:
                return PType.intervalMinuteSecond(type.getPrecision(), type.getFractionalPrecision());
            default:
                throw new IllegalArgumentException("Unsupported date-time interval code: " + type.getIntervalCode());
        }
    }
}
