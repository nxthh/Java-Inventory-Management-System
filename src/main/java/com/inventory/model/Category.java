package com.inventory.model;

/**
 * Category represents the type of a product.
 *
 * OOP concept: ENUM.
 * An enum is a special Java type used when a value can only be one of a
 * fixed, known set of options. Here, every product must belong to exactly
 * one of these four categories - nothing else is allowed. This is safer
 * than using a plain String, because the compiler will catch typos like
 * "Drnk" at compile time instead of causing bugs while the program runs.
 */
public enum Category {
    DRINK,
    FOOD,
    DAIRY,
    OTHER;

    /**
     * Converts text from the product file (e.g. "Drink") into the matching
     * enum constant (Category.DRINK). This is needed because the text file
     * stores the category as a simple word, not as a Java enum.
     *
     * If the text does not match any known category, we default to OTHER
     * instead of crashing the whole application.
     */
    public static Category fromString(String text) {
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(text)) {
                return category;
            }
        }
        return OTHER;
    }

    /**
     * Converts the enum back into a nicely capitalized word for saving to
     * the text file, e.g. Category.DRINK -> "Drink".
     */
    public String toDisplayString() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
