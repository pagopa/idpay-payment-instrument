package it.gov.pagopa.payment.instrument.service.idpaycode;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.payment.instrument.dto.EncryptedDataBlock;
import it.gov.pagopa.payment.instrument.exception.custom.PinBlockException;
import it.gov.pagopa.payment.instrument.exception.custom.PinBlockSizeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.GENERIC_ERROR;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionCode.PIN_LENGTH_NOT_VALID;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_CREATING_PINBLOCK_MSG;
import static it.gov.pagopa.payment.instrument.constants.PaymentInstrumentConstants.ExceptionMessage.ERROR_PIN_LENGTH_NOT_VALID_MSG;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodeEncryptionServiceTest {

  public static final String DATA_BLOCK_KEY_ID = "dataBlockKeyId";
  public static final String SECRET_KEY_KEY_ID = "secretKeyKeyId";
  private IdpayCodeEncryptionService idpayCodeEncryptionService;
  private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";
  private static final String keyVaultUrl = "https://KEYVAULTNAME.vault.azure.net";
  private static final String keyNameDataBlock = "keyNameDataBlock";
  private static final String keyNameSecretKey = "testSecretKeyName";
  public static final EncryptionAlgorithm ENCRYPTION_ALGORITHM = EncryptionAlgorithm.RSA_OAEP_256;

  @Mock
  private KeyClient keyClientMock;

  private CryptographyClient dataBlockCryptographyClientMock;

  private CryptographyClient secretKeyCryptographyClientMock;

  @BeforeEach
  void setUp() throws IllegalAccessException {
    idpayCodeEncryptionService = new IdpayCodeEncryptionServiceImpl(CIPHER_INSTANCE,
        keyClientMock, keyNameDataBlock, keyNameSecretKey);

    final Field cryptoClientCacheField = ReflectionUtils.findField(IdpayCodeEncryptionServiceImpl.class,
        "cryptoClientCache");
    assertNotNull(cryptoClientCacheField);
    cryptoClientCacheField.setAccessible(true);
    //noinspection unchecked
    Map<String, CryptographyClient> cryptoClientCacheMocks = (Map<String, CryptographyClient>) cryptoClientCacheField.get(
        idpayCodeEncryptionService);

    KeyVaultKey dataBlockKeyMock = Mockito.mock(KeyVaultKey.class);
    Mockito.lenient().when(keyClientMock.getKey(keyNameDataBlock)).thenReturn(dataBlockKeyMock);
    Mockito.lenient().when(dataBlockKeyMock.getId()).thenReturn(DATA_BLOCK_KEY_ID);
    dataBlockCryptographyClientMock = Mockito.mock(CryptographyClient.class);
    cryptoClientCacheMocks.put(DATA_BLOCK_KEY_ID, dataBlockCryptographyClientMock);

    KeyVaultKey secretKeyKeyMock = Mockito.mock(KeyVaultKey.class);
    Mockito.lenient().when(keyClientMock.getKey(keyNameSecretKey)).thenReturn(secretKeyKeyMock);
    Mockito.lenient().when(secretKeyKeyMock.getId()).thenReturn(SECRET_KEY_KEY_ID);
    secretKeyCryptographyClientMock = Mockito.mock(CryptographyClient.class);
    cryptoClientCacheMocks.put(SECRET_KEY_KEY_ID, secretKeyCryptographyClientMock);
    
  }

  @Test
  void encryptIdpayCode(){
    byte[] idpayCode = idpayCodeEncryptionService.buildHashedDataBlock(
        "12345","0000FFFFFFFFFFFF", "salt");

    assertNotNull(idpayCode);
  }

  @Test
  void encryptIdpayCode_ko_code_length(){
    try{
      idpayCodeEncryptionService.buildHashedDataBlock(
          "1234","0000FFFFFFFFFFFF", "salt");
      fail();
    }catch (PinBlockSizeException e){
      assertEquals(PIN_LENGTH_NOT_VALID, e.getCode());
      assertEquals(ERROR_PIN_LENGTH_NOT_VALID_MSG, e.getMessage());
    }
  }

  @Test
  void encryptIdpayCode_ko_internalServerError() {
    try{
      idpayCodeEncryptionService.buildHashedDataBlock(
          "12345","testError", "salt");
      fail();
    }catch (PinBlockException e){
      assertEquals(GENERIC_ERROR, e.getCode());
      assertEquals(ERROR_CREATING_PINBLOCK_MSG, e.getMessage());
    }
  }
  @Test
  void decryptSymmetricKey(){
    // Given
    String plainValue = "PLAINVALUE";
    String cipherValue = "CIPHERVALUE";
    String cipherValueEncoded = Base64.getEncoder().encodeToString(cipherValue.getBytes(StandardCharsets.UTF_8));


    Mockito.when(secretKeyCryptographyClientMock.decrypt(ENCRYPTION_ALGORITHM, cipherValue.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(new DecryptResult(plainValue.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM, SECRET_KEY_KEY_ID));

    byte[] decryptedValue = idpayCodeEncryptionService.decryptSymmetricKey(cipherValueEncoded);

    // Then
    assertEquals(plainValue, new String(decryptedValue));
  }

  @Test
  void encryptSHADataBlock(){
    // Given
    String plainValue = "PLAINVALUE";
    String encodedPlainValue = Base64.getEncoder().encodeToString(plainValue.getBytes(StandardCharsets.UTF_8));
    byte[] decodedPlainValue = Base64.getDecoder().decode(encodedPlainValue.getBytes(StandardCharsets.UTF_8));

    Mockito.when(dataBlockCryptographyClientMock.encrypt(ENCRYPTION_ALGORITHM, plainValue.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(new EncryptResult(decodedPlainValue, ENCRYPTION_ALGORITHM, DATA_BLOCK_KEY_ID));

    // when
    EncryptedDataBlock encryptedDataBlock = idpayCodeEncryptionService.encryptSHADataBlock(plainValue.getBytes(StandardCharsets.UTF_8));

    // Then
    assertEquals(encodedPlainValue, encryptedDataBlock.getEncryptedDataBlock());

  }

  @Test
  void decryptIdpayCode(){
    // Given
    String plainValue = "PLAINVALUE";
    String cipherValue = "CIPHERVALUE";
    String cipherValueEncoded = Base64.getEncoder().encodeToString(cipherValue.getBytes(StandardCharsets.UTF_8));


    Mockito.when(dataBlockCryptographyClientMock.decrypt(ENCRYPTION_ALGORITHM, cipherValue.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(new DecryptResult(plainValue.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM, DATA_BLOCK_KEY_ID));

    byte[] decryptedValue = idpayCodeEncryptionService.decryptIdpayCode(new EncryptedDataBlock(cipherValueEncoded, DATA_BLOCK_KEY_ID));

    // Then
    assertEquals(plainValue, new String(decryptedValue));
  }

}
