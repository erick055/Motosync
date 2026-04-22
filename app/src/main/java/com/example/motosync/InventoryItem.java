package com.example.motosync;

public class InventoryItem {
    public String itemId;
    public String itemName;
    public String category; // Added to match your web dashboard
    public double itemPrice;
    public int stockCount;

    public InventoryItem() { }

    public InventoryItem(String itemId, String itemName, String category, double itemPrice, int stockCount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.category = category;
        this.itemPrice = itemPrice;
        this.stockCount = stockCount;
    }
}