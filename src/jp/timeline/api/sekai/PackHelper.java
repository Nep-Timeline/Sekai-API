package jp.timeline.api.sekai;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class PackHelper {
    private static final byte[] key = "g2fcC0ZczN9MTJ61".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] iv = "msx3IV0i9XE5uYZ1".getBytes(StandardCharsets.US_ASCII);

    private static final MessagePackSerializer messagePackSerializer = new MessagePackSerializer();

    public static byte[] Pack(String content) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(messagePackSerializer.json2msgpack(content, true));
    }

    public static String Unpack(byte[] crypted) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        return messagePackSerializer.msgpack2json(cipher.doFinal(crypted));
    }
}
