package jp.timeline.api.sekai;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.escape.UnicodeEscaper;
import com.google.gson.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessagePackSerializer {
    public static final String JSON_STD_MIME_TYPE = "application/json";
    public static final String JSON_STD_MIME_TYPE_UTF8 = "application/json; charset=utf-8";
    public static final String JSON_DEPRECATED_MIME_TYPE = "text/json";
    public static final String MSGPACK_STD_MIME_TYPE = "application/msgpack";
    public static final String MSGPACK_X_MIME_TYPE = "application/x-msgpack";
    public static final String MSGPACK_LONG_MIME_TYPE = "application/messagepack";
    public static final String MSGPACK_X_LONG_MIME_TYPE = "application/x-messagepack";
    public static final String MSGPACK_VND_LONG_MIME_TYPE = "application/vnd.messagepack";
    public static final String MSGPACK_VND_SHORT_MIME_TYPE = "application/vnd.msgpack";

    public static final String HTTP_X_SEQUENCE_FLAG_HEADER = "X-msgpack2json-sequence";
    public static final String HTTP_X_SEQUENCE_FLAG_HEADER_VALUE = "1";

    boolean isSequence = false;

    public MessagePackSerializer() {
    }

    public boolean isSequence() {
        return this.isSequence;
    }

    public Object msgpack2object(final byte[] srcData) throws JsonParseException, JsonMappingException, IOException {
        if (srcData.length == 0) {
            throw new IllegalArgumentException("empty byte[] data is now allowed.");
        }
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        Object msgpack2obj = null;
        switch (MessageFormat.valueOf(srcData[0])) {
            case FIXMAP:
            case MAP16:
            case MAP32:
                TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<Map<String, Object>>() {
                };
                msgpack2obj = objectMapper.readValue(srcData, mapTypeRef);
                break;
            case FIXARRAY:
            case ARRAY16:
            case ARRAY32:
                TypeReference<List<Object>> listTypeRef = new TypeReference<List<Object>>() {
                };
                msgpack2obj = objectMapper.readValue(srcData, listTypeRef);
                break;
            default:
                this.isSequence = true;
                // no array header -> count up, then add array header :P
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(srcData);
                int numOfItems = 0;
                while (unpacker.hasNext()) {
                    unpacker.skipValue();
                    numOfItems++;
                }
                // count up completed, create buffer with original buffer size + maximum array header bytes(=5)
                ByteArrayOutputStream bytesArrayBuf = new ByteArrayOutputStream(srcData.length + 5);
                MessagePacker packer = MessagePack.newDefaultPacker(bytesArrayBuf);
                packer.packArrayHeader(numOfItems);
                packer.writePayload(srcData);
                packer.flush();
                // decode as array list
                byte[] arrayedSequenceMsgpackData = bytesArrayBuf.toByteArray();
                bytesArrayBuf.close();
                TypeReference<List<Object>> seqTypeRef = new TypeReference<List<Object>>() {
                };
                msgpack2obj = objectMapper.readValue(arrayedSequenceMsgpackData, seqTypeRef);
        }
        return msgpack2obj;
    }

    public Object json2object(String json) throws JsonProcessingException, IOException {
        json = json.trim();
        if (json.length() == 0) {
            throw new IllegalArgumentException("empty json string is now allowed.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        char c0 = json.charAt(0);
        if (c0 == '[') {
            ObjectReader reader = objectMapper.readerFor(ArrayList.class);
            Object newlist = reader.readValue(json);
            return this.walkthruObj2Json2ObjAdjuster(newlist);
        } else if (c0 == '{') {
            ObjectReader reader = objectMapper.readerFor(LinkedHashMap.class);
            Object newmap = reader.readValue(json);
            return this.walkthruObj2Json2ObjAdjuster(newmap);
        }
        throw new UnsupportedOperationException("single object json string is not supported.");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object walkthruObj2Json2ObjAdjuster(Object srcObj) {
        Object ret = srcObj;
        if (srcObj instanceof List) {
            List srcList = (List) srcObj;
            ArrayList<Object> newlist = new ArrayList<>(srcList.size());
            for (int i = 0; i < srcList.size(); i++) {
                Object srcObj1 = srcList.get(i);
                Object srcObj2 = this.walkthruObj2Json2ObjAdjuster(srcObj1);
                newlist.add(srcObj2);
            }
            ret = newlist;
        } else if (srcObj instanceof Map) {
            Map<Object, Object> srcMap = (Map<Object, Object>) srcObj;
            if (srcMap.containsKey("@type@") && "MessagePackExtensionType".equals(srcMap.get("@type@").toString())) {
                Object extObj0 = srcMap.get("value");
                if (extObj0 instanceof Map) {
                    Map extmap = (Map) extObj0;
                    if (extmap.containsKey("type") && extmap.containsKey("data")) {
                        Object type0 = extmap.get("type");
                        Object data0 = extmap.get("data");
                        if ((type0 instanceof Number) && (data0 instanceof String)) {
                            Number type1 = (Number) type0;
                            byte[] data1 = deserializeString2ByteArray((String) data0);
                            MessagePackExtensionType newObj = new MessagePackExtensionType(type1.byteValue(), data1);
                            return newObj;
                        }
                    }
                }
            }

            LinkedHashMap<Object, Object> newmap = new LinkedHashMap<>(srcMap.size());
            for (Map.Entry<Object, Object> e : srcMap.entrySet()) {
                Object keyObj1 = e.getKey();
                Object valObj1 = e.getValue();
                Object keyObj2 = this.walkthruObj2Json2ObjAdjuster(keyObj1);
                Object valObj2 = this.walkthruObj2Json2ObjAdjuster(valObj1);
                newmap.put(keyObj2, valObj2);
            }
            ret = newmap;
        } else if (srcObj instanceof String) {
            String srcStr = (String) srcObj;
            if (isByteArray2JsonString(srcStr)) {
                ret = deserializeString2ByteArray(srcStr);
            }
        }

        return ret;
    }

    public String object2json(final Object srcObj) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(MessagePackExtensionType.class, new MessagePackExtensionType2JsonSerializer());
        gsonBuilder.registerTypeAdapter(byte[].class, new ByteArray2JsonSerializer());
        Gson gson = gsonBuilder.create();
        return gson.toJson(srcObj);
    }

    static class ByteArray2JsonSerializer implements JsonSerializer<byte[]> {
        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(MessagePackSerializer.serializeByteArray2JsonString(src));
        }
    }

    static class MessagePackExtensionType2JsonSerializer implements JsonSerializer<MessagePackExtensionType> {
        @Override
        public JsonElement serialize(MessagePackExtensionType src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject extval = new JsonObject();
            extval.addProperty("type", src.getType());
            extval.addProperty("data", MessagePackSerializer.serializeByteArray2JsonString(src.getData()));
            JsonObject container = new JsonObject();
            container.addProperty("@type@", "MessagePackExtensionType");
            container.add("value", extval);
            return container;
        }
    }

    @SuppressWarnings("rawtypes")
    public byte[] object2msgpack(final Object srcObj, final boolean asSequence) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        if (asSequence && (srcObj instanceof List)) {
            ByteArrayOutputStream outbuf = new ByteArrayOutputStream();
            List srclist = (List) srcObj;
            for (int i = 0; i < srclist.size(); i++) {
                Object o0 = srclist.get(i);
                byte[] bytes0 = objectMapper.writeValueAsBytes(o0);
                outbuf.write(bytes0);
            }
            return outbuf.toByteArray();
        }
        return objectMapper.writeValueAsBytes(srcObj);
    }

    public String msgpack2json(final byte[] msgpackBytes) throws Exception {
        if (msgpackBytes.length == 0) {
            return "";
        }
        return object2json(msgpack2object(msgpackBytes));
    }

    public String json2json(String json) throws JsonProcessingException, IOException {
        if (Strings.isNullOrEmpty(json)) {
            return "";
        }
        return object2json(json2object(json));
    }

    public byte[] json2msgpack(String json, final boolean asSequence) throws JsonProcessingException, IOException {
        if (Strings.isNullOrEmpty(json)) {
            return new byte[] {};
        }
        return object2msgpack(json2object(json), asSequence);
    }

    public byte[] msgpack2msgpack(final byte[] msgpackBytes, final boolean asSequence)
            throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
        if (msgpackBytes.length == 0) {
            return new byte[] {};
        }
        return object2msgpack(msgpack2object(msgpackBytes), asSequence);
    }

    static class UnicodeAllEscaper extends UnicodeEscaper {
        @Override
        protected char[] escape(int c) {
            return String.format("\\u%04x", (int) c).toCharArray();
        }
    }

    public static String serializeByteArray2JsonString(byte[] src) {
        String str = new String(src, StandardCharsets.ISO_8859_1);
        Escaper escaper = new UnicodeAllEscaper();
        String escaped = escaper.escape(str);
        return "@byte[]@" + escaped;
    }

    public static boolean isByteArray2JsonString(String testee) {
        return testee.startsWith("@byte[]@");
    }

    public static byte[] deserializeString2ByteArray(String src) {
        if (src.startsWith("@byte[]@")) {
            src = src.substring("@byte[]@".length());
        }
        String s = StringEscapeUtils.unescapeJava(src);
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static boolean isMessagePackMimeType(String mimetype) {
        if (Strings.isNullOrEmpty(mimetype)) {
            return false;
        }
        if (mimetype.startsWith(MSGPACK_STD_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(MSGPACK_X_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(MSGPACK_LONG_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(MSGPACK_X_LONG_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(MSGPACK_VND_LONG_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(MSGPACK_VND_SHORT_MIME_TYPE)) {
            return true;
        }
        return false;
    }

    public static boolean isJsonMimeType(String mimetype) {
        if (Strings.isNullOrEmpty(mimetype)) {
            return false;
        }
        if (mimetype.startsWith(JSON_STD_MIME_TYPE)) {
            return true;
        }
        if (mimetype.startsWith(JSON_DEPRECATED_MIME_TYPE)) {
            return true;
        }
        return false;
    }
}
