package org.project.ssogssog.infrastructure.adapter.stock;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockPort;
import org.project.ssogssog.infrastructure.client.ksi.KISClient;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StockAdapter implements StockPort {

    private final KISClient kisClient;
    private final OpenDartClient openDartClient;
    private final RateLimiter kisRateLimiter;
    private final RateLimiter openDartRateLimiter;

    public StockAdapter(KISClient kisClient,
                        OpenDartClient openDartClient,
                        @Qualifier("kisRateLimiter") RateLimiter kisRateLimiter,
                        @Qualifier("openDartRateLimiter") RateLimiter openDartRateLimiter) {
        this.kisClient = kisClient;
        this.openDartClient = openDartClient;
        this.kisRateLimiter = kisRateLimiter;
        this.openDartRateLimiter = openDartRateLimiter;
    }

    @Override
    public String fetchSector(String stockCode) {

        String accessToken = kisClient.getValidAccessToken();

        // KIS access token 발급 실패
        if (accessToken == null) {
            return null;
        }

        kisRateLimiter.acquire();
        return kisClient.fetchSector(accessToken, stockCode);

    }

    // OpenDart 에서 가져온 전체 XML 데이터 전달
    @Override
    public byte[] getCorpCodeZip() {

        openDartRateLimiter.acquire();
        return openDartClient.getCorpCodeZip();
    }

}
