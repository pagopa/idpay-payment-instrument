package it.gov.pagopa.common.azure.keyvault;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * In order to be allowed to execute this test, you must have AZ cli installed and logged into CSTAR-DEV subscription
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
class AzureEncryptUtilsTestIntegrated {
    @Test
    void test(){
        String plainvalue = "PLAINVALUE";

        KeyClient keyClient = AzureEncryptUtils.getKeyClient("https://cstar-d-idpay-kv.vault.azure.net");
        CryptographyClient cryptographyClient = AzureEncryptUtils.buildCryptographyClient(keyClient.getKey("testIdpayCodeRSA"));
        String encryptedValue = AzureEncryptUtils.encrypt(plainvalue, cryptographyClient);
        String decryptedValue = AzureEncryptUtils.decrypt(encryptedValue, cryptographyClient);
        Assertions.assertEquals(plainvalue, decryptedValue);
    }
}
