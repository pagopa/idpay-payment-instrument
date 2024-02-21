package it.gov.pagopa.common.azure.keyvault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;

import java.util.Base64;

public class AzureEncryptUtils {

    private static final DefaultAzureCredential DEFAULT_AZURE_CREDENTIAL = new DefaultAzureCredentialBuilder().build();

    private AzureEncryptUtils(){}

    public static KeyClient getKeyClient(String keyVaultUrl){
        return new KeyClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .buildClient();
    }

    public static CryptographyClient buildCryptographyClient(KeyVaultKey key){
        return buildCryptographyClient(key.getId());
    }

    public static CryptographyClient buildCryptographyClient(String keyId){
        return new CryptographyClientBuilder()
                .keyIdentifier(keyId)
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .buildClient();
    }

    public static String encrypt(byte[] plainValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        // byte[] -> RSA -> Base64
        return Base64.getEncoder().encodeToString(cryptoClient.encrypt(encryptionAlgorithm, plainValue).getCipherText());
    }

    public static byte[] decrypt(String encryptedValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        // Base64 -> RSA -> byte[]
        return cryptoClient.decrypt(encryptionAlgorithm, Base64.getDecoder().decode(encryptedValue)).getPlainText();
    }
}
