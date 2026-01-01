package org.project.ssogssog.global.paging;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageDTO<T> {

    private final List<T> content;
    private final int currentPage;
    private final int size; // 요청한 크기
    private final boolean hasNext;
    private final long totalContentCount; // 총 데이터 개수

    public PageDTO(Page<T> pageContent){
        this.content = pageContent.getContent();
        this.currentPage = pageContent.getNumber();
        this.size = pageContent.getSize();
        this.hasNext = pageContent.hasNext();
        this.totalContentCount = pageContent.getTotalElements();
    }

    public static <T> PageDTO<T> from(Page<T> pageContent){
        return new PageDTO<>(pageContent);
    }
}
