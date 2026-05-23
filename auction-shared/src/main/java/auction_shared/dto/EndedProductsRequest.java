package auction_shared.dto;

import java.io.Serializable;

public class EndedProductsRequest implements Serializable {
    private String categoryFilter;
    private int page;
    private int pageSize;

    public EndedProductsRequest(String categoryFilter, int page, int pageSize) {
        this.categoryFilter = categoryFilter;
        this.page = page;
        this.pageSize = pageSize;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}
