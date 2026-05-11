package auction_shared.dto;

public enum ItemType {
    ARTS, ELECTRONICS, VEHICLES;

    public static ItemType fromDbValue(String value) {
        return switch (value) {
            case "ARTS" -> ARTS;
            case "ELECTRONICS" -> ELECTRONICS;
            case "VEHICLES" -> VEHICLES;
            default -> throw new IllegalArgumentException(
                    "Unknown item_type in DB: " + value);
        };
    }

    /**
     * Dịch enum về String để ghi vào DB.
     * Thay thế resolveItemType() trong ItemDAO.
     */
    public String toDbValue() {
        return this.name(); // "ARTS", "ELECTRONICS", "VEHICLES"
    }
}
