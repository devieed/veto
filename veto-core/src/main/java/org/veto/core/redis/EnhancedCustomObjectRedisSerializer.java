package org.veto.core.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.io.*;
@Component
public class EnhancedCustomObjectRedisSerializer<T> implements RedisSerializer<T> {

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }

        // 针对基础类型的处理
        if (t instanceof Integer || t instanceof Long || t instanceof Double || t instanceof Float) {
            return t.toString().getBytes();
        }

        // 其他对象类型使用 ObjectOutputStream 序列化
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(t);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Could not serialize object", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // 处理基础类型的反序列化
        String value = new String(bytes);
        try {
            // 判断并返回基础类型
            if (value.matches("^-?\\d+$")) { // Integer 或 Long
                return (T) Long.valueOf(value);
            } else if (value.matches("^-?\\d+\\.\\d+$")) { // Float 或 Double
                return (T) Double.valueOf(value);
            }
        } catch (NumberFormatException e) {
            throw new SerializationException("Could not deserialize number", e);
        }

        // 其他对象类型使用 ObjectInputStream 反序列化
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize object", e);
        }
    }
}
