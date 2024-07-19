package ph.extremelogic.common.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyEncryptorTest {

    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES
    private PropertyEncryptor encryptor;

    @BeforeEach
    public void setUp() {
        encryptor = new PropertyEncryptor(ENCRYPTION_KEY);
    }

    @Test
    public void testEncryptDecrypt() {
        String originalValue = "HelloWorld";
        String encryptedValue = encryptor.encrypt(originalValue);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        assertEquals(originalValue, decryptedValue, "Decrypted value should match the original value");
    }

    @Test
    public void testEncryptNotNull() {
        String originalValue = "HelloWorld";
        String encryptedValue = encryptor.encrypt(originalValue);
        assertNotNull(encryptedValue, "Encrypted value should not be null");
    }

    @Test
    public void testDecryptNotNull() {
        String originalValue = "HelloWorld";
        String encryptedValue = encryptor.encrypt(originalValue);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        assertNotNull(decryptedValue, "Decrypted value should not be null");
    }

    @Test
    public void testDecryptInvalidData() {
        String invalidEncryptedValue = "InvalidData";
        Exception exception = assertThrows(RuntimeException.class, () -> {
            encryptor.decrypt(invalidEncryptedValue);
        });
        assertTrue(exception.getCause() instanceof IllegalBlockSizeException || exception.getCause() instanceof BadPaddingException,
                "Decrypting invalid data should throw IllegalBlockSizeException or BadPaddingException");
    }

    @Test
    public void testEncryptEmptyString() {
        String originalValue = "";
        String encryptedValue = encryptor.encrypt(originalValue);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        assertEquals(originalValue, decryptedValue, "Decrypted value should match the original value for an empty string");
    }

    @Test
    public void testEncryptNullString() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            encryptor.encrypt(null);
        });
        assertNotNull(exception, "Encrypting null should throw RuntimeException");
    }

    @Test
    public void testDecryptNullString() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            encryptor.decrypt(null);
        });
        assertNotNull(exception, "Decrypting null should throw RuntimeException");
    }

    @Test
    public void testEncryptSpecialCharacters() {
        String originalValue = "!@#$%^&*()_+";
        String encryptedValue = encryptor.encrypt(originalValue);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        assertEquals(originalValue, decryptedValue, "Decrypted value should match the original value with special characters");
    }

    @Test
    public void testEncryptLongString() {
        String originalValue = "A".repeat(1000);
        String encryptedValue = encryptor.encrypt(originalValue);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        assertEquals(originalValue, decryptedValue, "Decrypted value should match the original value for a long string");
    }

    @Test
    public void testEncryptKeyLength() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            new PropertyEncryptor("short");
        });
        assertNotNull(exception, "Creating PropertyEncryptor with short key should throw RuntimeException");
    }
}
