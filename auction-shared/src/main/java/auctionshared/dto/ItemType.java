package auctionshared.dto;

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

    public String toDbValue() {
        return this.name();
    }

    public String attributeColumn() {
        return switch (this) {
            case ARTS -> "artist_name";
            case ELECTRONICS -> "model";
            case VEHICLES -> "brand";
        };
    }
}
