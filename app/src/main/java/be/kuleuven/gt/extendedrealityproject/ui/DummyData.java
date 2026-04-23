package be.kuleuven.gt.extendedrealityproject.ui;

import be.kuleuven.gt.extendedrealityproject.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DummyData {

    public static List<MarketplaceItem> getItems() {
        return Arrays.asList(
            new MarketplaceItem(
                "item-001",
                "Vintage Lounge Chair",
                149.99,
                "Beautiful mid-century modern lounge chair in excellent condition. Light wear on the armrests. Non-smoking household. Pick-up only.",
                "Thomas V.",
                "Leuven, BE",
                "Furniture",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            ),
            new MarketplaceItem(
                "item-002",
                "Sony WH-1000XM4 Headphones",
                189.00,
                "Barely used noise-cancelling headphones. Comes with original case, cables and box. Great sound quality, perfect for home or travel.",
                "Sarah M.",
                "Ghent, BE",
                "Electronics",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            ),
            new MarketplaceItem(
                "item-003",
                "IKEA KALLAX Shelf 4x4",
                45.00,
                "White KALLAX shelving unit, 4x4 grid. Minor scratches on back panel, not visible when placed against wall. Disassembled for easy transport.",
                "Emma L.",
                "Brussels, BE",
                "Furniture",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            ),
            new MarketplaceItem(
                "item-004",
                "Trek Mountain Bike",
                320.00,
                "Trek Marlin 5, size M, 2021. New brake pads installed last month. Some scratches on frame. Great for trail riding.",
                "Jan P.",
                "Antwerp, BE",
                "Sports",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            ),
            new MarketplaceItem(
                "item-005",
                "Canon EOS M50 Camera Kit",
                440.00,
                "Mirrorless camera with 15-45mm kit lens. Used for about 1 year. In perfect working condition. Includes 2 batteries and 64GB SD card.",
                "Noor B.",
                "Bruges, BE",
                "Electronics",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            ),
            new MarketplaceItem(
                "item-006",
                "Wooden Dining Table 6-person",
                220.00,
                "Solid oak dining table, seats 6 comfortably. Minor scratches on surface. Pairs well with any chair style. Available for pick-up weekends only.",
                "Pieter D.",
                "Leuven, BE",
                "Furniture",
                null,
                Collections.singletonList(R.drawable.placeholder_item)
            )
        );
    }
}

