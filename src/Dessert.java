class Dessert extends MenuItem {
    private boolean hasIceCream;

    public Dessert(String name, double price, int stock, boolean hasIceCream) {
        super(name, price, stock, "Dessert");
        this.hasIceCream = hasIceCream;
    }

    @Override
    public String getDescription() {
        String icon = hasIceCream ? "🍨" : "🍰";
        return icon + " " + getName() + " - Rp " + String.format("%,.0f", getPrice());
    }
}