package it.gov.pagopa.payment.instrument.config;

import com.azure.security.keyvault.keys.KeyClient;
import it.gov.pagopa.common.azure.keyvault.AzureEncryptUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

  @Bean
  KeyClient getKeyVaultKeyClient(@Value("${crypto.azure.key-vault.url}") String keyVaultUrl){
    return AzureEncryptUtils.getKeyClient(keyVaultUrl);
  }

}
