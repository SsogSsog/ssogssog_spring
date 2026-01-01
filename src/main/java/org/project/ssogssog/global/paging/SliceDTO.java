package org.project.ssogssog.global.paging;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceDTO<T> {

    private final List<T> content;
    private final int currentPage;
    private final int size;
    private final boolean hasNext;

    private SliceDTO(List<T> content, int currentPage, int size, boolean hasNext) {
        this.content = content;
        this.currentPage = currentPage;
        this.size = size;
        this.hasNext = hasNext;
    }

    public static <T> SliceDTO<T> of(List<T> content, int currentPage, int size, boolean hasNext) {
        return new SliceDTO(content, currentPage, size,hasNext);
    }

    private SliceDTO(Slice<T> sliceContent) {
        this.content = sliceContent.getContent();
        this.currentPage = sliceContent.getNumber();
        this.size = sliceContent.getSize();
        this.hasNext = sliceContent.hasNext();
    }

    public static <T> SliceDTO<T> from(Slice<T> sliceContent) {
        return new SliceDTO(sliceContent);
    }
}
