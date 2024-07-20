package ph.extremelogic.common.core.config.encrypt;

/**
 * Interface defining encryption and decryption methods.
 */
public interface PropertyEncryptor {
    /**
     * Encrypts the given value.
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    String encrypt(String value);

    /**
     * Decrypts the given encrypted value.
     *
     * @param encryptedValue The value to decrypt.
     * @return The decrypted value.
     */
    String decrypt(String encryptedValue);
}
