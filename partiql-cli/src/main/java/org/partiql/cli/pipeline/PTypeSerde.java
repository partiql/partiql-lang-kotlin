package org.partiql.cli.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.IntervalCode;
import org.partiql.spi.types.PType;
import org.partiql.spi.types.PTypeField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal utility for serializing/deserializing {@link PType} to/from JSON and DDL formats.
 * <p>
 * This is NOT part of the public API. It is used by the PartiQL CLI debug mode.
 */
final class PTypeSerde {

    private PTypeSerde() {}

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION";

    // -----------------------------------------------
    // JSON serialization / deserialization
    // -----------------------------------------------

    @NotNull
    public static String toJson(@NotNull PType type) throws JsonProcessingException {
        Map<String, Object> node = toJsonNode(type, null);
        return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    @NotNull
    public static PType fromJson(@NotNull String json) throws JsonProcessingException {
        JsonNode node = JSON_MAPPER.readTree(json);
        return fromJsonNode(node);
    }

    private static Map<String, Object> toJsonNode(PType p, String fieldNameIfRow) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (fieldNameIfRow != null) {
            out.put("name", fieldNameIfRow);
        }
        out.put("type", p.name());
        switch (p.code()) {
            case PType.TIME:
            case PType.TIMEZ:
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ: {
                if (!isUnspecifiedPrecision(p.metas)) {
                    out.put("precision", p.getPrecision());
                }
                break;
            }
            case PType.DECIMAL:
            case PType.NUMERIC: {
                out.put("precision", p.getPrecision());
                out.put("scale", p.getScale());
                break;
            }
            case PType.CHAR:
            case PType.VARCHAR:
            case PType.CLOB: {
                out.put("length", p.getLength());
                break;
            }
            case PType.BLOB: {
                int len = p.getLength();
                if (len > 0) out.put("length", len);
                break;
            }
            case PType.INTERVAL_YM: {
                out.put("intervalCode", intervalCodeToName(p.getIntervalCode()));
                out.put("precision", p.getPrecision());
                break;
            }
            case PType.INTERVAL_DT: {
                out.put("intervalCode", intervalCodeToName(p.getIntervalCode()));
                out.put("precision", p.getPrecision());
                out.put("fractionalPrecision", p.getFractionalPrecision());
                break;
            }
            default:
                break;
        }
        if (p.metas != null && !p.metas.isEmpty()) {
            out.put("metas", JSON_MAPPER.convertValue(p.metas, Object.class));
        }
        if (p.code() == PType.ROW) {
            List<Map<String, Object>> fields = p.getFields().stream()
                    .map(f -> toJsonNode(f.getType(), f.getName()))
                    .collect(Collectors.toList());
            out.put("fields", fields);
        }
        if (p.code() == PType.ARRAY || p.code() == PType.BAG) {
            out.put("element", toJsonNode(p.getTypeParameter(), null));
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "MethodLength"})
    private static PType fromJsonNode(JsonNode node) {
        if (node == null || node instanceof NullNode) {
            throw new IllegalArgumentException("Null JSON node for PType");
        }
        String typeStr = jsonRequiredText(node, "type").trim();
        Map<String, Object> metas = new LinkedHashMap<>();
        if (node.has("metas") && !node.get("metas").isNull()) {
            metas = JSON_MAPPER.convertValue(node.get("metas"), Map.class);
        }
        PType result;
        switch (typeStr.toUpperCase(Locale.ROOT)) {
            case "ROW": {
                JsonNode fieldsNode = node.get("fields");
                if (fieldsNode == null || !fieldsNode.isArray()) {
                    throw new IllegalArgumentException("ROW requires 'fields'");
                }
                List<PTypeField> fields = new ArrayList<>();
                for (JsonNode f : fieldsNode) {
                    String fname = jsonRequiredText(f, "name");
                    PType ftype = fromJsonNode(f);
                    fields.add(PTypeField.of(fname, ftype));
                }
                result = PType.row(fields);
                break;
            }
            case "ARRAY": {
                JsonNode elemNode = node.get("element");
                if (elemNode == null) throw new IllegalArgumentException("ARRAY requires 'element'");
                result = PType.array(fromJsonNode(elemNode));
                break;
            }
            case "BAG": {
                JsonNode elemNode = node.get("element");
                if (elemNode == null) throw new IllegalArgumentException("BAG requires 'element'");
                result = PType.bag(fromJsonNode(elemNode));
                break;
            }
            case "DECIMAL": {
                int precision = jsonRequiredInt(node, "precision");
                int scale = jsonRequiredInt(node, "scale");
                result = PType.decimal(precision, scale);
                break;
            }
            case "NUMERIC": {
                int precision = jsonRequiredInt(node, "precision");
                int scale = jsonRequiredInt(node, "scale");
                result = PType.numeric(precision, scale);
                break;
            }
            case "CHAR": {
                int len = jsonRequiredInt(node, "length");
                result = PType.character(len);
                break;
            }
            case "VARCHAR": {
                int len = jsonRequiredInt(node, "length");
                result = PType.varchar(len);
                break;
            }
            case "BLOB": {
                if (node.has("length") && !node.get("length").isNull()) {
                    result = PType.blob(node.get("length").intValue());
                } else {
                    result = PType.blob();
                }
                break;
            }
            case "CLOB": {
                int len = jsonRequiredInt(node, "length");
                result = PType.clob(len);
                break;
            }
            case "DYNAMIC":    result = PType.dynamic(); break;
            case "BOOL":
            case "BOOLEAN":    result = PType.bool(); break;
            case "TINYINT":    result = PType.tinyint(); break;
            case "SMALLINT":   result = PType.smallint(); break;
            case "INTEGER":
            case "INT":        result = PType.integer(); break;
            case "BIGINT":     result = PType.bigint(); break;
            case "REAL":       result = PType.real(); break;
            case "DOUBLE":
            case "DOUBLE PRECISION": result = PType.doublePrecision(); break;
            case "STRING":     result = PType.string(); break;
            case "DATE":       result = PType.date(); break;
            case "TIME": {
                if (isUnspecifiedPrecision(metas)) { result = PType.time(); }
                else { result = PType.time(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMEZ": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timez(); }
                else { result = PType.timez(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMESTAMP": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timestamp(); }
                else { result = PType.timestamp(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMESTAMPZ": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timestampz(); }
                else { result = PType.timestampz(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "STRUCT":     result = PType.struct(); break;
            case "UNKNOWN":    result = PType.unknown(); break;
            case "VARIANT":    result = PType.variant("ion"); break;
            case "INTERVAL_YM": {
                int intervalCode = intervalCodeFromName(jsonRequiredText(node, "intervalCode"));
                int precision = jsonRequiredInt(node, "precision");
                switch (intervalCode) {
                    case IntervalCode.YEAR:       result = PType.intervalYear(precision); break;
                    case IntervalCode.MONTH:      result = PType.intervalMonth(precision); break;
                    case IntervalCode.YEAR_MONTH: result = PType.intervalYearMonth(precision); break;
                    default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_YM: " + jsonRequiredText(node, "intervalCode"));
                }
                break;
            }
            case "INTERVAL_DT": {
                int intervalCode = intervalCodeFromName(jsonRequiredText(node, "intervalCode"));
                int precision = jsonRequiredInt(node, "precision");
                int fp = jsonRequiredInt(node, "fractionalPrecision");
                switch (intervalCode) {
                    case IntervalCode.DAY:           result = PType.intervalDay(precision); break;
                    case IntervalCode.HOUR:          result = PType.intervalHour(precision); break;
                    case IntervalCode.MINUTE:        result = PType.intervalMinute(precision); break;
                    case IntervalCode.SECOND:        result = PType.intervalSecond(precision, fp); break;
                    case IntervalCode.DAY_HOUR:      result = PType.intervalDayHour(precision); break;
                    case IntervalCode.DAY_MINUTE:    result = PType.intervalDayMinute(precision); break;
                    case IntervalCode.DAY_SECOND:    result = PType.intervalDaySecond(precision, fp); break;
                    case IntervalCode.HOUR_MINUTE:   result = PType.intervalHourMinute(precision); break;
                    case IntervalCode.HOUR_SECOND:   result = PType.intervalHourSecond(precision, fp); break;
                    case IntervalCode.MINUTE_SECOND: result = PType.intervalMinuteSecond(precision, fp); break;
                    default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_DT: " + jsonRequiredText(node, "intervalCode"));
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported type: " + typeStr);
        }
        if (!metas.isEmpty()) result.metas.putAll(metas);
        return result;
    }

    // -----------------------------------------------
    // JSON helpers
    // -----------------------------------------------

    private static boolean isUnspecifiedPrecision(Map<String, Object> metas) {
        if (metas == null) return false;
        Object v = metas.get(UNSPECIFIED_PRECISION);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private static String jsonRequiredText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).asText();
    }

    private static int jsonRequiredInt(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).intValue();
    }

    private static String intervalCodeToName(int code) {
        switch (code) {
            case IntervalCode.YEAR:          return "YEAR";
            case IntervalCode.MONTH:         return "MONTH";
            case IntervalCode.DAY:           return "DAY";
            case IntervalCode.HOUR:          return "HOUR";
            case IntervalCode.MINUTE:        return "MINUTE";
            case IntervalCode.SECOND:        return "SECOND";
            case IntervalCode.YEAR_MONTH:    return "YEAR_MONTH";
            case IntervalCode.DAY_HOUR:      return "DAY_HOUR";
            case IntervalCode.DAY_MINUTE:    return "DAY_MINUTE";
            case IntervalCode.DAY_SECOND:    return "DAY_SECOND";
            case IntervalCode.HOUR_MINUTE:   return "HOUR_MINUTE";
            case IntervalCode.HOUR_SECOND:   return "HOUR_SECOND";
            case IntervalCode.MINUTE_SECOND: return "MINUTE_SECOND";
            default: throw new IllegalArgumentException("Unknown interval code: " + code);
        }
    }

    private static int intervalCodeFromName(String name) {
        switch (name.toUpperCase(Locale.ROOT)) {
            case "YEAR":          return IntervalCode.YEAR;
            case "MONTH":         return IntervalCode.MONTH;
            case "DAY":           return IntervalCode.DAY;
            case "HOUR":          return IntervalCode.HOUR;
            case "MINUTE":        return IntervalCode.MINUTE;
            case "SECOND":        return IntervalCode.SECOND;
            case "YEAR_MONTH":    return IntervalCode.YEAR_MONTH;
            case "DAY_HOUR":      return IntervalCode.DAY_HOUR;
            case "DAY_MINUTE":    return IntervalCode.DAY_MINUTE;
            case "DAY_SECOND":    return IntervalCode.DAY_SECOND;
            case "HOUR_MINUTE":   return IntervalCode.HOUR_MINUTE;
            case "HOUR_SECOND":   return IntervalCode.HOUR_SECOND;
            case "MINUTE_SECOND": return IntervalCode.MINUTE_SECOND;
            default: throw new IllegalArgumentException("Unknown interval code name: " + name);
        }
    }

    // -----------------------------------------------
    // DDL serialization / deserialization
    // -----------------------------------------------

    @NotNull
    public static String toDDL(@NotNull PType type) {
        return toDDLInternal(type);
    }

    @NotNull
    public static PType fromDDL(@NotNull String ddl) {
        String trimmed = ddl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty DDL type string");
        }
        return parseDDL(trimmed);
    }

    private static String toDDLInternal(PType p) {
        switch (p.code()) {
            case PType.DYNAMIC:    return "DYNAMIC";
            case PType.BOOL:       return "BOOL";
            case PType.TINYINT:    return "TINYINT";
            case PType.SMALLINT:   return "SMALLINT";
            case PType.INTEGER:    return "INTEGER";
            case PType.BIGINT:     return "BIGINT";
            case PType.REAL:       return "REAL";
            case PType.DOUBLE:     return "DOUBLE PRECISION";
            case PType.STRING:     return "STRING";
            case PType.DATE:       return "DATE";
            case PType.STRUCT:     return "STRUCT";
            case PType.UNKNOWN:    return "UNKNOWN";
            case PType.VARIANT:    return "VARIANT";
            case PType.NUMERIC:
                return "NUMERIC(" + p.getPrecision() + ", " + p.getScale() + ")";
            case PType.DECIMAL:
                return "DECIMAL(" + p.getPrecision() + ", " + p.getScale() + ")";
            case PType.CHAR:
                return "CHAR(" + p.getLength() + ")";
            case PType.VARCHAR:
                return "VARCHAR(" + p.getLength() + ")";
            case PType.BLOB:
                return "BLOB(" + p.getLength() + ")";
            case PType.CLOB:
                return "CLOB(" + p.getLength() + ")";
            case PType.TIME:
                return "TIME(" + p.getPrecision() + ")";
            case PType.TIMEZ:
                return "TIME(" + p.getPrecision() + ") WITH TIME ZONE";
            case PType.TIMESTAMP:
                return "TIMESTAMP(" + p.getPrecision() + ")";
            case PType.TIMESTAMPZ:
                return "TIMESTAMP(" + p.getPrecision() + ") WITH TIME ZONE";
            case PType.ARRAY:
                return "ARRAY<" + toDDLInternal(p.getTypeParameter()) + ">";
            case PType.BAG:
                return "BAG<" + toDDLInternal(p.getTypeParameter()) + ">";
            case PType.ROW: {
                StringBuilder sb = new StringBuilder("ROW(");
                boolean first = true;
                for (PTypeField f : p.getFields()) {
                    if (!first) sb.append(", ");
                    sb.append(f.getName()).append(" ").append(toDDLInternal(f.getType()));
                    first = false;
                }
                sb.append(")");
                return sb.toString();
            }
            case PType.INTERVAL_YM:
                return intervalYmToDDL(p.getIntervalCode(), p.getPrecision());
            case PType.INTERVAL_DT:
                return intervalDtToDDL(p.getIntervalCode(), p.getPrecision(), p.getFractionalPrecision());
            default:
                throw new IllegalArgumentException("Cannot convert PType code " + p.code() + " to DDL");
        }
    }

    private static String intervalYmToDDL(int intervalCode, int precision) {
        switch (intervalCode) {
            case IntervalCode.YEAR:       return "INTERVAL YEAR(" + precision + ")";
            case IntervalCode.MONTH:      return "INTERVAL MONTH(" + precision + ")";
            case IntervalCode.YEAR_MONTH: return "INTERVAL YEAR(" + precision + ") TO MONTH";
            default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_YM: " + intervalCode);
        }
    }

    private static String intervalDtToDDL(int intervalCode, int precision, int fractionalPrecision) {
        switch (intervalCode) {
            case IntervalCode.DAY:           return "INTERVAL DAY(" + precision + ")";
            case IntervalCode.HOUR:          return "INTERVAL HOUR(" + precision + ")";
            case IntervalCode.MINUTE:        return "INTERVAL MINUTE(" + precision + ")";
            case IntervalCode.SECOND:        return "INTERVAL SECOND(" + precision + ", " + fractionalPrecision + ")";
            case IntervalCode.DAY_HOUR:      return "INTERVAL DAY(" + precision + ") TO HOUR";
            case IntervalCode.DAY_MINUTE:    return "INTERVAL DAY(" + precision + ") TO MINUTE";
            case IntervalCode.DAY_SECOND:    return "INTERVAL DAY(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            case IntervalCode.HOUR_MINUTE:   return "INTERVAL HOUR(" + precision + ") TO MINUTE";
            case IntervalCode.HOUR_SECOND:   return "INTERVAL HOUR(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            case IntervalCode.MINUTE_SECOND: return "INTERVAL MINUTE(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_DT: " + intervalCode);
        }
    }

    @SuppressWarnings("MethodLength")
    private static PType parseDDL(String ddl) {
        String upper = ddl.toUpperCase(Locale.ROOT).trim();

        if (upper.startsWith("INTERVAL ")) {
            return parseIntervalDDL(ddl.trim());
        }
        if (upper.startsWith("TIMESTAMP") && upper.endsWith("WITH TIME ZONE")) {
            String inner = ddlExtractParens(upper, "TIMESTAMP");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timestampz(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("TIMESTAMP(")) {
            String inner = ddlExtractParens(upper, "TIMESTAMP");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timestamp(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("TIME") && upper.endsWith("WITH TIME ZONE")) {
            String inner = ddlExtractParens(upper, "TIME");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timez(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("TIME(")) {
            String inner = ddlExtractParens(upper, "TIME");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.time(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("DECIMAL(")) {
            String inner = ddlExtractParens(upper, "DECIMAL");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            String[] parts = inner.split(",");
            if (parts.length != 2) throw new IllegalArgumentException("DECIMAL requires (precision, scale): " + ddl);
            return PType.decimal(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }
        if (upper.startsWith("NUMERIC(")) {
            String inner = ddlExtractParens(upper, "NUMERIC");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            String[] parts = inner.split(",");
            if (parts.length != 2) throw new IllegalArgumentException("NUMERIC requires (precision, scale): " + ddl);
            return PType.numeric(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }
        if (upper.startsWith("CHAR(")) {
            String inner = ddlExtractParens(upper, "CHAR");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.character(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("VARCHAR(")) {
            String inner = ddlExtractParens(upper, "VARCHAR");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.varchar(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("BLOB(")) {
            String inner = ddlExtractParens(upper, "BLOB");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.blob(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("CLOB(")) {
            String inner = ddlExtractParens(upper, "CLOB");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.clob(Integer.parseInt(inner.trim()));
        }
        if (upper.startsWith("ARRAY<") && upper.endsWith(">")) {
            String inner = ddl.trim().substring(6, ddl.trim().length() - 1);
            return PType.array(parseDDL(inner));
        }
        if (upper.startsWith("BAG<") && upper.endsWith(">")) {
            String inner = ddl.trim().substring(4, ddl.trim().length() - 1);
            return PType.bag(parseDDL(inner));
        }
        if (upper.startsWith("ROW(") && upper.endsWith(")")) {
            String inner = ddl.trim().substring(4, ddl.trim().length() - 1);
            List<PTypeField> fields = parseRowFields(inner);
            return PType.row(fields);
        }
        switch (upper) {
            case "DYNAMIC":          return PType.dynamic();
            case "BOOL":
            case "BOOLEAN":          return PType.bool();
            case "TINYINT":          return PType.tinyint();
            case "SMALLINT":         return PType.smallint();
            case "INTEGER":
            case "INT":              return PType.integer();
            case "BIGINT":           return PType.bigint();
            case "REAL":             return PType.real();
            case "DOUBLE PRECISION": return PType.doublePrecision();
            case "STRING":           return PType.string();
            case "DATE":             return PType.date();
            case "STRUCT":           return PType.struct();
            case "UNKNOWN":          return PType.unknown();
            case "VARIANT":          return PType.variant("ion");
            case "DECIMAL":          return PType.decimal(38, 0);
            case "NUMERIC":          return PType.numeric(38, 0);
            case "CHAR":             return PType.character(1);
            case "VARCHAR":          return PType.varchar(1);
            case "BLOB":             return PType.blob();
            case "CLOB":             return PType.clob();
            case "TIME":             return PType.time(6);
            case "TIMESTAMP":        return PType.timestamp(6);
            default:
                throw new IllegalArgumentException("Unsupported DDL type: " + ddl);
        }
    }

    private static String ddlExtractParens(String upper, String prefix) {
        int start = upper.indexOf('(', prefix.length());
        if (start < 0) return null;
        int depth = 1;
        int i = start + 1;
        while (i < upper.length() && depth > 0) {
            if (upper.charAt(i) == '(') depth++;
            else if (upper.charAt(i) == ')') depth--;
            i++;
        }
        if (depth != 0) return null;
        return upper.substring(start + 1, i - 1);
    }

    private static List<PTypeField> parseRowFields(String inner) {
        List<PTypeField> fields = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<' || c == '(') depth++;
            else if (c == '>' || c == ')') depth--;
            else if (c == ',' && depth == 0) {
                fields.add(parseOneField(inner.substring(start, i).trim()));
                start = i + 1;
            }
        }
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) {
            fields.add(parseOneField(last));
        }
        return fields;
    }

    private static PTypeField parseOneField(String fieldStr) {
        int spaceIdx = fieldStr.indexOf(' ');
        if (spaceIdx < 0) {
            throw new IllegalArgumentException("Invalid ROW field (expected 'name type'): " + fieldStr);
        }
        String name = fieldStr.substring(0, spaceIdx).trim();
        String typeDDL = fieldStr.substring(spaceIdx + 1).trim();
        return PTypeField.of(name, parseDDL(typeDDL));
    }

    private static PType parseIntervalDDL(String ddl) {
        String body = ddl.substring("INTERVAL ".length()).trim();
        String upper = body.toUpperCase(Locale.ROOT);
        int toIdx = findToKeyword(upper);
        if (toIdx < 0) {
            return parseSingleFieldInterval(body);
        } else {
            String leading = body.substring(0, toIdx).trim();
            String trailing = body.substring(toIdx + 3).trim();
            return parseRangeInterval(leading, trailing);
        }
    }

    private static int findToKeyword(String upper) {
        int depth = 0;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && i + 4 <= upper.length() && upper.substring(i, i + 4).equals(" TO ")) {
                return i;
            }
        }
        return -1;
    }

    private static PType parseSingleFieldInterval(String body) {
        String upper = body.toUpperCase(Locale.ROOT);
        if (upper.startsWith("YEAR(")) {
            return PType.intervalYear(Integer.parseInt(ddlExtractParens(upper, "YEAR")));
        } else if (upper.startsWith("MONTH(")) {
            return PType.intervalMonth(Integer.parseInt(ddlExtractParens(upper, "MONTH")));
        } else if (upper.startsWith("DAY(")) {
            return PType.intervalDay(Integer.parseInt(ddlExtractParens(upper, "DAY")));
        } else if (upper.startsWith("HOUR(")) {
            return PType.intervalHour(Integer.parseInt(ddlExtractParens(upper, "HOUR")));
        } else if (upper.startsWith("MINUTE(")) {
            return PType.intervalMinute(Integer.parseInt(ddlExtractParens(upper, "MINUTE")));
        } else if (upper.startsWith("SECOND(")) {
            String inner = ddlExtractParens(upper, "SECOND");
            String[] parts = inner.split(",");
            int p = Integer.parseInt(parts[0].trim());
            int fp = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return PType.intervalSecond(p, fp);
        }
        throw new IllegalArgumentException("Unsupported INTERVAL DDL: INTERVAL " + body);
    }

    private static PType parseRangeInterval(String leading, String trailing) {
        String leadUpper = leading.toUpperCase(Locale.ROOT);
        String trailUpper = trailing.toUpperCase(Locale.ROOT);
        int parenIdx = leadUpper.indexOf('(');
        if (parenIdx < 0) {
            throw new IllegalArgumentException("INTERVAL range requires precision on leading field: " + leading);
        }
        String leadField = leadUpper.substring(0, parenIdx).trim();
        int precision = Integer.parseInt(ddlExtractParens(leadUpper, leadField));
        String trailField;
        int fractionalPrecision = 0;
        int trailParenIdx = trailUpper.indexOf('(');
        if (trailParenIdx >= 0) {
            trailField = trailUpper.substring(0, trailParenIdx).trim();
            fractionalPrecision = Integer.parseInt(ddlExtractParens(trailUpper, trailField));
        } else {
            trailField = trailUpper.trim();
        }
        String combo = leadField + "_" + trailField;
        switch (combo) {
            case "YEAR_MONTH":    return PType.intervalYearMonth(precision);
            case "DAY_HOUR":      return PType.intervalDayHour(precision);
            case "DAY_MINUTE":    return PType.intervalDayMinute(precision);
            case "DAY_SECOND":    return PType.intervalDaySecond(precision, fractionalPrecision);
            case "HOUR_MINUTE":   return PType.intervalHourMinute(precision);
            case "HOUR_SECOND":   return PType.intervalHourSecond(precision, fractionalPrecision);
            case "MINUTE_SECOND": return PType.intervalMinuteSecond(precision, fractionalPrecision);
            default:
                throw new IllegalArgumentException("Unsupported INTERVAL range: " + leadField + " TO " + trailField);
        }
    }
}