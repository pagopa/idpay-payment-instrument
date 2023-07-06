package it.gov.pagopa.payment.instrument.connector;

import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector{

    private final RewardCalculatorRestClient rewardCalculatorRestClient;

    public RewardCalculatorConnectorImpl(RewardCalculatorRestClient rewardCalculatorRestClient) {
        this.rewardCalculatorRestClient = rewardCalculatorRestClient;
    }

    @Override
    public void disableUserInitiativeInstruments(String userId, String initiativeId) {
        rewardCalculatorRestClient.disableUserInitiativeInstruments(userId,initiativeId);
    }

    @Override
    public void enableUserInitiativeInstruments(String userId, String initiativeId) {
        rewardCalculatorRestClient.enableUserInitiativeInstruments(userId,initiativeId);
    }
}
