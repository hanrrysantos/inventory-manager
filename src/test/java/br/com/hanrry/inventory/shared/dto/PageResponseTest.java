package br.com.hanrry.inventory.shared.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseTest {

    @Test
    void shouldMapPageMetadataWithoutMapper() {
        Page<String> page = new PageImpl<>(
                List.of("alpha", "beta"),
                PageRequest.of(1, 10),
                25
        );

        PageResponse<String> response = PageResponse.from(page);

        assertEquals(List.of("alpha", "beta"), response.content());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(25, response.totalElements());
        assertEquals(3, response.totalPages());
    }

    @Test
    void shouldMapContentWithMapper() {
        Page<String> page = new PageImpl<>(
                List.of("ab", "cde"),
                PageRequest.of(0, 20),
                2
        );

        PageResponse<Integer> response = PageResponse.from(page, String::length);

        assertEquals(List.of(2, 3), response.content());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void shouldHandleEmptyPage() {
        Page<String> page = Page.empty(PageRequest.of(0, 20));

        PageResponse<String> response = PageResponse.from(page);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }
}
