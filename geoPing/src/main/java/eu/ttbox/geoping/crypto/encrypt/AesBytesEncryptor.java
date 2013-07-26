package eu.ttbox.geoping.crypto.encrypt;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import eu.ttbox.geoping.crypto.codec.Hex;
import eu.ttbox.geoping.crypto.keygen.BytesKeyGenerator;

import static eu.ttbox.geoping.crypto.encrypt.CipherUtils.doFinal;
import static eu.ttbox.geoping.crypto.encrypt.CipherUtils.initCipher;
import static eu.ttbox.geoping.crypto.encrypt.CipherUtils.newCipher;
import static eu.ttbox.geoping.crypto.encrypt.CipherUtils.newSecretKey;
import static eu.ttbox.geoping.crypto.util.EncodingUtils.concatenate;
import static eu.ttbox.geoping.crypto.util.EncodingUtils.subArray;

/**
 * Encryptor that uses 256-bit AES encryption.
 * {@link http://android-developers.blogspot.fr/2013/02/using-cryptography-to-store-credentials.html}
 * @author Keith Donald
 */
public final class AesBytesEncryptor implements BytesEncryptor {

    private final SecretKey secretKey;

    private final Cipher encryptor;

    private final Cipher decryptor;

    private final BytesKeyGenerator ivGenerator;

    public AesBytesEncryptor(String password, CharSequence salt) {
        this(password, salt, null);
    }

    public AesBytesEncryptor(String password, CharSequence salt, BytesKeyGenerator ivGenerator) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(salt), 1024, 256);
        SecretKey secretKey = newSecretKey("PBKDF2WithHmacSHA1", keySpec);
        this.secretKey = new SecretKeySpec(secretKey.getEncoded(), "AES");
        encryptor = newCipher(AES_ALGORITHM);
        decryptor = newCipher(AES_ALGORITHM);
        this.ivGenerator = ivGenerator != null ? ivGenerator : NULL_IV_GENERATOR;
    }

    public byte[] encrypt(byte[] bytes) {
        synchronized (encryptor) {
            byte[] iv = ivGenerator.generateKey();
            initCipher(encryptor, Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = doFinal(encryptor, bytes);
            return ivGenerator != NULL_IV_GENERATOR ? concatenate(iv, encrypted) : encrypted;
        }
    }

    public byte[] decrypt(byte[] encryptedBytes) {
        synchronized (decryptor) {
            byte[] iv = iv(encryptedBytes);
            initCipher(decryptor, Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return doFinal(decryptor, ivGenerator != NULL_IV_GENERATOR ? encrypted(encryptedBytes, iv.length) : encryptedBytes);
        }
    }

    // internal helpers

    private byte[] iv(byte[] encrypted) {
        return ivGenerator != NULL_IV_GENERATOR ? subArray(encrypted, 0, ivGenerator.getKeyLength()) : NULL_IV_GENERATOR.generateKey();
    }

    private byte[] encrypted(byte[] encryptedBytes, int ivLength) {
        return subArray(encryptedBytes, ivLength, encryptedBytes.length);
    }

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final BytesKeyGenerator NULL_IV_GENERATOR = new BytesKeyGenerator() {

        private final byte[] VALUE = new byte[16];

        public int getKeyLength() {
            return VALUE.length;
        }

        public byte[] generateKey() {
            return VALUE;
        }

    };
}