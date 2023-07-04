package it.gov.pagopa.payment.instrument.connector;

import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector{

    private final RewardCalculatorRestClient rewardCalculatorRestClient;

    public RewardCalculatorConnectorImpl(RewardCalculatorRestClient rewardCalculatorRestClient) {
        this.rewardCalculatorRestClient = rewardCalculatorRestClient;
    }

    @Override
    public void cancelInstruments(String userId, String initiativeId) {
        rewardCalculatorRestClient.cancelInstruments(userId,initiativeId);
    }

    @Override
    public void reactivateInstruments(String userId, String initiativeId) {
        rewardCalculatorRestClient.reactivateInstruments(userId,initiativeId);
    }
}
