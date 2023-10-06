package it.gov.pagopa.payment.instrument.service.idpaycode;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.payment.instrument.exception.PaymentInstrumentException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EncryptCodeServiceImpl implements EncryptCodeService {

  public static final String GENERATE_PIN_BLOCK = "GENERATE_PIN_BLOCK";
  public static final String HASH_PIN_BLOCK = "HASH_PIN_BLOCK";
  private final String cipherInstance;
  private final String iv;

  public EncryptCodeServiceImpl(
      @Value("${util.crypto.aes.cipherInstance}") String cipherInstance,
      @Value("${util.crypto.aes.mode.gcm.iv}") String iv) {
    this.cipherInstance = cipherInstance;
    this.iv = iv;
  }

  @Override
  public String buildHashedPinBlock(String code, String secondFactor, String salt) {
    String pinBlock = calculatePinBlock(secondFactor, code);
    return createSHA256Digest(pinBlock, salt);
  }

  private String calculatePinBlock(String secondFactor, String code) {
    long startTime = System.currentTimeMillis();
    // Control code length, must be 5
    try {
      if (code.length() < 5) {
        throw new PaymentInstrumentException(400, "Pin length is not valid");
      }

      // Standardizes code (adds padding with "F" to reach 16 digits)
      final String codeData = StringUtils.rightPad(code, 16,'F');

      // Combine CODE and SECOND_FACTOR with XOR
      final byte[] codeBytes = Hex.decodeHex(codeData.toCharArray());
      final byte[] secondFactorBytes = Hex.decodeHex(secondFactor.toCharArray());
      final byte[] xorResult = new byte[8];
      for (int i = 0; i < 8; i++) {
        xorResult[i] = (byte) (codeBytes[i] ^ secondFactorBytes[i]);
      }

      // Converts the result to a hexadecimal representation
      String pinBlock = Hex.encodeHexString(xorResult);
      performanceLog(startTime, GENERATE_PIN_BLOCK);

      return pinBlock;
    } catch (DecoderException ex) {
      throw new PaymentInstrumentException(500, "Something went wrong while creating pinBlock");
    }
  }

  public String verifyPinBlock(String encryptedPinBlock, String encryptedKey) {

    return decryptPinBlockWithSimetricKey(encryptedPinBlock, encryptedKey);
  }

  // Hashing pinBlock with algorithm SHA-256
  private String createSHA256Digest(String pinBlock, String salt) {
    long startTime = System.currentTimeMillis();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hash = md.digest(pinBlock.getBytes(StandardCharsets.UTF_8));

      String hashedPinBlock = Base64.getEncoder().encodeToString(hash);

      log.debug("[{}] pinBlock hashing done successfully", HASH_PIN_BLOCK);
      performanceLog(startTime, HASH_PIN_BLOCK);
      return hashedPinBlock;
    } catch (NoSuchAlgorithmException e) {
      throw new PaymentInstrumentException(403, "Something went wrong creating SHA256 digest");
    }
  }

  @NotNull
  private String decryptPinBlockWithSimetricKey(String encryptedPinBlock, String encryptedKey) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptedKey.getBytes(), "AES");

    byte[] decryptedBytes = doFinal(secretKeySpec, Base64.getDecoder().decode(encryptedPinBlock));

    return new String(decryptedBytes);
  }

  @Override
  public String encryptWithAzureAPI(String hashedPinBlock){
    // URL del tuo Key Vault
    String keyVaultUrl = "https://cstar-d-idpay-kv.vault.azure.net/";

    // Nome della chiave crittografica in Key Vault
    String keyName = "testIdpayCodeRSA";

    try {
//      DefaultAzureCredential clientSecretCredential = new DefaultAzureCredentialBuilder()
//          .managedIdentityClientId("c3c860c9-4fcf-4132-98bd-96d182f8efe7")
//          .build();

//      ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
//          .clientId("7788edaf-0346-4068-9d79-c868aed15b3d")
//          .clientSecret("<YOUR_CLIENT_SECRET>")
//          .tenantId("7788edaf-0346-4068-9d79-c868aed15b3d")
//          .build();

      AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
      TokenCredential credential = new ManagedIdentityCredentialBuilder()
          .clientId("c3c860c9-4fcf-4132-98bd-96d182f8efe7")
          .build();
      AzureResourceManager azureResourceManager = AzureResourceManager
          .authenticate(credential, profile)
          .withTenantId("7788edaf-0346-4068-9d79-c868aed15b3d")
          .withDefaultSubscription();

      // Crea un client per le chiavi di Azure Key Vault
      KeyClient keyClient = new KeyClientBuilder()
          .vaultUrl(keyVaultUrl)
          .credential(credential)
          .buildClient();

      // Ottieni la chiave crittografica dal Key Vault
      KeyVaultKey key = keyClient.getKey(keyName);

      // Create client with key identifier from Key Vault.
      CryptographyClient cryptoClient = new CryptographyClientBuilder()
          .keyIdentifier(key.getId())
          .credential(new DefaultAzureCredentialBuilder().build())
          .buildClient();

      // Encrypt pin-block
      EncryptResult encryptionResult = cryptoClient.encrypt(EncryptionAlgorithm.RSA_OAEP, hashedPinBlock.getBytes(StandardCharsets.UTF_8));

      return  Hex.encodeHexString(encryptionResult.getCipherText());
    } catch (Exception e) {
      throw new PaymentInstrumentException(500, "");
    }
  }

  private byte[] doFinal(SecretKey secretKeySpec, byte[] bytes) {
    try{
      Cipher cipher = Cipher.getInstance(cipherInstance);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());

      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
      return cipher.doFinal(bytes);

    }catch (InvalidKeyException
            | InvalidAlgorithmParameterException
            | IllegalBlockSizeException
            | NoSuchPaddingException
            | NoSuchAlgorithmException
            | BadPaddingException e) {
      throw fail(e);
    }
  }

  private IllegalStateException fail(Exception e) {
    return new IllegalStateException(e);
  }

  private void performanceLog(long startTime, String service){
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }
}
