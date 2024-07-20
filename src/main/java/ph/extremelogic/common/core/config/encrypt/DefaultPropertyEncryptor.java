/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ph.extremelogic.common.core.config.encrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class DefaultPropertyEncryptor implements PropertyEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_LENGTH = 16;

    private final SecretKeySpec secretKey;

    public DefaultPropertyEncryptor(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Encryption key must be 16 characters long");
        }
        this.secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
    }

    @Override
    public String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting value", e);
        }
    }

    @Override
    public String decrypt(String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }

    public static void main(String[] args) {
        var scanner = new Scanner(System.in);

        System.out.print("Enter a 16-character encryption key: ");
        String encryptionKey = scanner.nextLine();

        if (encryptionKey.length() != KEY_LENGTH) {
            System.out.println("Error: Encryption key must be 16 characters long.");
            return;
        }

        var encryptor = new DefaultPropertyEncryptor(encryptionKey);

        System.out.print("Enter a value to create an encrypted equivalent for your configuration: ");
        var valueToEncrypt = scanner.nextLine();

        var encryptedValue = encryptor.encrypt(valueToEncrypt);
        System.out.println();
        System.out.println("Place this in your configuration.");
        System.out.println("Encrypted Value: ENC(" + encryptedValue + ")");

        var decryptedValue = encryptor.decrypt(encryptedValue);
        System.out.println();
        System.out.println("Below is just a verification that we can decrypt it.");
        System.out.println("Decrypted Value: " + decryptedValue);
    }
}