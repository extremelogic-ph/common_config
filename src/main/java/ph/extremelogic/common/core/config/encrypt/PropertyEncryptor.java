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
