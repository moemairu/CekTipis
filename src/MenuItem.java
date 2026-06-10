abstract class MenuItem {
    private int id;
    private String name;
    private double price;
    private int stock;
    private String category;

    public MenuItem(String name, double price, int stock, String category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    // Setter for ID (used when loading from database)
    public void setId(int id) {
        this.id = id;
    }
    // Abstract method (must be implemented by children)
    public abstract String getDescription();
    // Encapsulation - Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCategory() { return category; }
    // Encapsulation - Setter with validation
    public void setStock(int newStock) throws InvalidQuantityException {
        if (newStock < 0) {
            throw new InvalidQuantityException("Stock cannot be negative");
        }
        this.stock = newStock;
    }

    public void reduceStock(int quantity) throws OutOfStockException {
        if (quantity > this.stock) {
            throw new OutOfStockException(this.name + " out of stock! Available: " + this.stock);
        }
        this.stock -= quantity;
    }

    public void addStock(int quantity) {
        this.stock += quantity;
    }
    @Override
    public String toString() {
        return name + " - Rp " + String.format("%,.0f", price);
    }
}

