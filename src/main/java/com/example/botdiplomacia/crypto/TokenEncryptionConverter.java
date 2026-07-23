package com.example.botdiplomacia.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra en reposo los tokens de sesion del juego con AES-256-GCM.
 * La clave sale de TOKEN_ENC_KEY (32 bytes en Base64). El IV va concatenado
 * al principio de cada valor cifrado, asi que no hace falta guardarlo aparte.
 */
@Converter
public class TokenEncryptionConverter implements AttributeConverter<String, String> {
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public TokenEncryptionConverter() {
        String encoded = System.getenv("TOKEN_ENC_KEY");
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno TOKEN_ENC_KEY (32 bytes en Base64)");
        }
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("TOKEN_ENC_KEY debe decodificar a exactamente 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error cifrando el token", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(storedValue);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error descifrando el token", e);
        }
    }
}
