package org.project.ssogssog.presentation.controller.stock;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stock.api.StockService;
import org.project.ssogssog.global.payload.ApiResponse;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock")
@Validated
public class StockController {

    private final StockService stockService;

    @GetMapping("/themes")
    @Operation(
            summary = "테마별 주식 개수 + 변동률 평균 목록 조회",
            description = """
            테마 단위로 주식을 그룹핑하여 최신 변동률(changeRate)의 평균을 계산한 목록을 반환합니다.
            응답의 totalCount는 해당 테마의 주식 개수입니다.
            """
    )
    public ApiResponse<StockResponse.ThemeResponseDTO> getThemeStockStats(){

        StockResponse.ThemeResponseDTO result = stockService.getThemeStockStats();
        return ApiResponse.onSuccess(result);

    }

    // TODO
    // 1. 뉴스 + 공시 탭에서 캐싱(Caching)을 통해 api 호출 시간을 줄이고 빠르게 정보 가져오기
    // 2. 매크로에 대한 토큰 사용 방어 정책 및 토큰 제한 문제 해결하기(하루 25,000회)
    // 3. DB + 뉴스 + 공시 ... 등의 로직에 대해 비동기 병렬 처리 로직
    @GetMapping("/news")
    @Operation(
            summary = "특정 종목(키워드) 관련 실시간 뉴스 조회",
            description = """
            네이버 뉴스 검색 API를 실시간으로 호출하여, 입력된 키워드(예: 종목명)와 관련된 최신 뉴스 리스트를 반환합니다.
            
            - DB에 저장하지 않고 네이버 API를 통해 실시간 데이터를 가져옵니다.
            - response의 link를 통해 웹뷰나 브라우저로 기사 원문을 띄울 수 있습니다.
            """
    )
    public ApiResponse<StockResponse.NewsResponseDTO> getStockNews(String keyword){

        StockResponse.NewsResponseDTO result = stockService.getStockNews(keyword);
        return ApiResponse.onSuccess(result);
    }

}