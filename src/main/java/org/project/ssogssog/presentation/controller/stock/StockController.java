package org.project.ssogssog.presentation.controller.stock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stock.api.StockService;
import org.project.ssogssog.global.paging.PageDTO;
import org.project.ssogssog.global.paging.SliceDTO;
import org.project.ssogssog.global.payload.ApiResponse;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock")
@Validated
public class StockController {

    private final StockService stockService;

    @GetMapping("")
    public ApiResponse<StockResponse.StockOverviewResponseDTO> getStockOverview(@RequestParam String stockCode){

        StockResponse.StockOverviewResponseDTO result = stockService.getStockOverview(stockCode);
        return ApiResponse.onSuccess(result);
    }


    @GetMapping("/themes/stats")
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

    @GetMapping("/themes")
    @Operation(
            summary = "테마별 주식 목록 조회 (최신 종가 기준 정렬 + 페이지네이션)",
            description = """
            입력된 테마(섹터)에 속한 주식 목록을 조회하여, 각 종목의 가장 최신 정보를 기반으로
            종가 내림차순으로 정렬한 뒤 페이지네이션하여 반환합니다.

            - 최신 일봉 기준: 각 종목의 DailyPrice 중 가장 최신 날짜(date)의 데이터
            - 정렬 기준: 최신 일봉의 closePrice DESC (가격 정보가 없는 종목은 뒤로 정렬)

            반환 데이터에는 종목 기본 정보와 함께 최신 일봉의 closePrice/volume/changeRate 등 핵심 지표가 포함됩니다.
            """
    )

    public ApiResponse<PageDTO<StockResponse.StockItemResponseDTO>> getStocksForTheme(
            @NotBlank
            @RequestParam
            String theme,
            @PageableDefault(
                    size=10,
                    sort="closePrice", direction=DESC
            ) Pageable pageable
            ){

        PageDTO<StockResponse.StockItemResponseDTO> result = stockService.getStocksForTheme(theme, pageable);
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
    public ApiResponse<SliceDTO<StockResponse.NewsResponseItemDTO>> getStockNews(
            @Parameter(description = "검색할 종목 코드 (필수)", required = true)
            @RequestParam @NotBlank(message = "종목 코드를 입력해주세요.")
            String stockCode,

            @Parameter(description = "페이지 번호(기본값 0")
            @RequestParam(name="page", defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이여야 합니다.")
            int page
    ) {

        // size 및 sort는 고정되어 있음
        SliceDTO<StockResponse.NewsResponseItemDTO> result = stockService.getStockNews(stockCode, page);
        return ApiResponse.onSuccess(result);
    }

    // TODO
    // 1. 뉴스 + 공시 탭에서 캐싱(Caching)을 통해 api 호출 시간을 줄이고 빠르게 정보 가져오기
    // 2. 매크로에 대한 토큰 사용 방어 정책 및 토큰 제한 문제 해결하기(하루 10,000회)
    // 3. DB + 뉴스 + 공시 ... 등의 로직에 대해 비동기 병렬 처리 로직
    @Operation(
            summary = "특정 종목의 실시간 공시(보고서) 목록 조회",
            description = """
            OpenDART API를 실시간으로 호출하여, 해당 종목의 최근 3개월 치(20개 제한) 주요 공시 및 보고서 리스트를 반환합니다.
            요청 시 종목코드(예: 005930)를 입력하면, 서버에서 stockCode로 변환 후 해당 주식을 찾아 공시 데이터를 가져옵니다.
            
            - DB에 저장하지 않고 OpenDART API를 통해 실시간 데이터를 가져옵니다.
            - response의 receiptNo(접수번호)를 이용해 아래와 같이 DART 전자공시 뷰어 링크를 생성할 수 있습니다.
              (링크 예시: http://dart.fss.or.kr/dsaf001/main.do?rcpNo={receiptNo})
            """
    )
    @GetMapping("/disclosures")
    public ApiResponse<SliceDTO<StockResponse.DisclosureItemResponseDTO>> getDisclosures(
            @Parameter(description = "검색할 종목 코드 (필수)", required = true)
            @RequestParam @NotBlank(message = "종목 코드를 입력해주세요.")
            String stockCode,

            @Parameter(description = "페이지 번호(기본값 0")
            @RequestParam(name="page", defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이여야 합니다.")
            int page
    ){

        SliceDTO<StockResponse.DisclosureItemResponseDTO> result = stockService.getDisclosures(stockCode, page);
        return ApiResponse.onSuccess(result);
    }

}