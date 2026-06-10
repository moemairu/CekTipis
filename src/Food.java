class Food extends MenuItem {
    private int spicyLevel; // 0-5

    public Food(String name, double price, int stock, int spicyLevel) {
        super(name, price, stock, "Food");
        this.spicyLevel = spicyLevel;
    }

    @Override
    public String getDescription() {
        String spicy = spicyLevel > 0 ? " 🌶️x" + spicyLevel : "";
        return "🍽️ " + getName() + spicy + " - Rp " + String.format("%,.0f", getPrice());
    }
}
