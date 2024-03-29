package it.gov.pagopa.common.azure.keyvault;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * In order to be allowed to execute this test, you must have AZ cli installed and logged into DEV-CSTAR subscription.<br />
 * Run the following command:
 * <pre>{@code az login --scope https://vault.azure.net/.default}</pre>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
class AzureEncryptUtilsTestIntegrated {
    @Test
    void test(){
        String plainvalue = "PLAINVALUE";
        EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithm.RSA_OAEP_256;

        KeyClient keyClient = AzureEncryptUtils.getKeyClient("https://cstar-d-idpay-kv.vault.azure.net");
        CryptographyClient cryptographyClient = AzureEncryptUtils.buildCryptographyClient(keyClient.getKey("idpay-mil-key"));
        String encryptedValue = AzureEncryptUtils.encrypt(plainvalue.getBytes(StandardCharsets.UTF_8), encryptionAlgorithm, cryptographyClient);
        byte[] decryptedValue = AzureEncryptUtils.decrypt(encryptedValue, encryptionAlgorithm, cryptographyClient);
        Assertions.assertEquals(plainvalue, new String(decryptedValue));
    }
}
