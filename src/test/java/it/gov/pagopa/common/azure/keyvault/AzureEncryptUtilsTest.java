package it.gov.pagopa.common.azure.keyvault;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

class AzureEncryptUtilsTest {

    public static final EncryptionAlgorithm ENCRYPTION_ALGORITHM = EncryptionAlgorithm.RSA_OAEP_256;

    @Test
    void testGetKeyClient(){
        // Given
        String keyVaultUrl = "https://KEYVAULTNAME.vault.azure.net";

        // When
        KeyClient keyClient = AzureEncryptUtils.getKeyClient(keyVaultUrl);

        // Then
        Assertions.assertNotNull(keyClient);
        Assertions.assertEquals(keyVaultUrl, keyClient.getVaultUrl());
    }

    @Test
    void testBuildCryptographyClient() throws Exception {
        // Given
        String expectedKeyId = "https://KEYVAULTNAME.vault.azure.net/keys/KEYNAME/KEYID";

        KeyVaultKey keyVaultKeyMock = Mockito.mock(KeyVaultKey.class);
        Mockito.when(keyVaultKeyMock.getId()).thenReturn(expectedKeyId);

        // When
        CryptographyClient cryptographyClient = AzureEncryptUtils.buildCryptographyClient(keyVaultKeyMock);

        // Then
        Assertions.assertNotNull(cryptographyClient);
        Assertions.assertSame(expectedKeyId, ReflectionUtils.tryToReadFieldValue(CryptographyClient.class, "keyId", cryptographyClient).get());
    }

    @Test
    void testEncryptDecrypt(){
        // Given
        String keyId = "KEYID";
        String plainvalue = "PLAINVALUE";
        byte[] cipherValue = "CIPHERVALUE".getBytes(StandardCharsets.UTF_8);

        CryptographyClient cryptographyClientMock = Mockito.mock(CryptographyClient.class);
        Mockito.when(cryptographyClientMock.encrypt(ENCRYPTION_ALGORITHM, plainvalue.getBytes(StandardCharsets.UTF_8)))
                .thenReturn(new EncryptResult(cipherValue, ENCRYPTION_ALGORITHM, keyId));
        Mockito.when(cryptographyClientMock.decrypt(ENCRYPTION_ALGORITHM, cipherValue))
                .thenReturn(new DecryptResult(plainvalue.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM, keyId));

        // When
        String encryptedValue = AzureEncryptUtils.encrypt(plainvalue.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM, cryptographyClientMock);
        Assertions.assertNotNull(encryptedValue);
        byte[] decryptedValue = AzureEncryptUtils.decrypt(encryptedValue, ENCRYPTION_ALGORITHM, cryptographyClientMock);

        // Then
        Assertions.assertEquals(plainvalue, new String(decryptedValue));
    }
}
